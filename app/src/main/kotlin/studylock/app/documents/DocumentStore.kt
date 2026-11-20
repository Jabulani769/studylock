package studylock.app.documents

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Imports study material into app-private storage and keeps only a local index.
 * Documents never leave the phone and are not included in backups.
 */
class DocumentStore(private val context: Context) {

    private val directory: File = File(context.filesDir, "documents").apply {
        if (!exists()) mkdirs()
    }
    private val indexFile: File = File(directory, "index.json")

    fun list(): List<DocumentMeta> {
        return try {
        if (!indexFile.exists()) return emptyList()
        val array = JSONArray(indexFile.readText())
        (0 until array.length()).mapNotNull { index ->
            val item = array.getJSONObject(index)
            DocumentMeta(
                id = item.getString("id"),
                displayName = item.getString("displayName"),
                type = DocumentType.valueOf(item.getString("type")),
                fileName = item.getString("fileName"),
                lastPosition = item.optInt("lastPosition", 0),
                topicId = item.optString("topicId").takeIf { it.isNotEmpty() }
            )
        }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun import(uri: Uri, topicId: String? = null): ImportResult {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else "Document"
        } ?: "Document"
        val type = DocumentTypes.fromMime(resolver.getType(uri), displayName)
            ?: return ImportResult.Unsupported
        val id = UUID.randomUUID().toString()
        val fileName = "$id.${type.extension}"
        return try {
            val stream = resolver.openInputStream(uri) ?: return ImportResult.Failed
            val target = File(directory, fileName)
            var total = 0L
            stream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_BYTES) {
                            target.delete()
                            return ImportResult.TooLarge
                        }
                        output.write(buffer, 0, read)
                    }
                }
            }
            val meta = DocumentMeta(id, displayName, type, fileName, topicId = topicId)
            writeIndex(list() + meta)
            ImportResult.Success(meta)
        } catch (e: Exception) {
            File(directory, fileName).delete()
            ImportResult.Failed
        }
    }

    fun savePosition(id: String, position: Int) {
        writeIndex(list().map { meta ->
            if (meta.id == id) meta.copy(lastPosition = position.coerceAtLeast(0)) else meta
        })
    }

    fun delete(id: String) {
        val item = list().firstOrNull { it.id == id } ?: return
        File(directory, item.fileName).delete()
        writeIndex(list().filterNot { it.id == id })
    }

    fun file(meta: DocumentMeta): File? = File(directory, meta.fileName).takeIf { it.exists() }

    private fun writeIndex(items: List<DocumentMeta>) {
        val array = JSONArray()
        items.forEach { meta ->
            array.put(
                JSONObject().apply {
                    put("id", meta.id)
                    put("displayName", meta.displayName)
                    put("type", meta.type.name)
                    put("fileName", meta.fileName)
                    put("lastPosition", meta.lastPosition)
                    meta.topicId?.let { put("topicId", it) }
                }
            )
        }
        indexFile.writeText(array.toString())
    }

    sealed interface ImportResult {
        data class Success(val meta: DocumentMeta) : ImportResult
        data object Unsupported : ImportResult
        data object TooLarge : ImportResult
        data object Failed : ImportResult
    }

    private companion object {
        const val MAX_BYTES = 25L * 1024 * 1024
    }
}
