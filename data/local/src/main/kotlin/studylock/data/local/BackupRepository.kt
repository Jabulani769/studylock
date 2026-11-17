package studylock.data.local

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only backup of every user-owned table. Files stay inside the app's
 * private storage, so no encryption is required (D-12). Nothing is uploaded.
 */
class BackupRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val backupDir: File
        get() = File(context.filesDir, "backups").apply { mkdirs() }

    suspend fun listBackups(): List<String> = withContext(Dispatchers.IO) {
        backupDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.map { it.name }
            ?.sortedDescending()
            ?: emptyList()
    }

    suspend fun exportBackup(nowMillis: Long): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val tables = JSONObject()
        val db = database.openHelper.readableDatabase
        PARENT_FIRST_TABLES.forEach { table ->
            val rows = JSONArray()
            val cursor = db.query("SELECT * FROM $table")
            cursor.use { c ->
                while (c.moveToNext()) {
                    rows.put(cursorToJson(c))
                }
            }
            tables.put(table, rows)
        }
        root.put("format", "studylock-local-backup-v1")
        root.put("exportedAt", nowMillis)
        root.put("tables", tables)

        val name = "studylock-backup-$nowMillis.json"
        File(backupDir, name).writeText(root.toString())
        name
    }

    suspend fun restoreBackup(fileName: String) = withContext(Dispatchers.IO) {
        require(VALID_NAME.matches(fileName)) { "Invalid backup name" }
        val file = File(backupDir, fileName)
        require(file.isFile) { "Backup not found" }
        val root = JSONObject(file.readText())
        require(root.optString("format") == "studylock-local-backup-v1") {
            "Unsupported backup format"
        }
        val tables = root.getJSONObject("tables")
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            CHILD_FIRST_TABLES.forEach { db.delete(it, null, null) }
            PARENT_FIRST_TABLES.forEach { table ->
                val rows = tables.optJSONArray(table) ?: return@forEach
                for (i in 0 until rows.length()) {
                    val values = android.content.ContentValues()
                    val row = rows.getJSONObject(i)
                    row.keys().forEach { key ->
                        val value = row.get(key)
                        when (value) {
                            JSONObject.NULL -> values.putNull(key)
                            is String -> values.put(key, value)
                            is Boolean -> values.put(key, if (value) 1 else 0)
                            is Int -> values.put(key, value)
                            is Long -> values.put(key, value)
                            is Double -> values.put(key, value)
                            else -> values.put(key, value.toString())
                        }
                    }
                    db.insert(table, SQLiteDatabase.CONFLICT_REPLACE, values)
                }
            }
        }
    }

    private fun cursorToJson(cursor: Cursor): JSONObject = JSONObject().apply {
        cursor.columnNames.forEachIndexed { index, name ->
            when (cursor.getType(index)) {
                Cursor.FIELD_TYPE_NULL -> put(name, JSONObject.NULL)
                Cursor.FIELD_TYPE_STRING -> put(name, cursor.getString(index))
                Cursor.FIELD_TYPE_INTEGER -> put(name, cursor.getLong(index))
                Cursor.FIELD_TYPE_FLOAT -> put(name, cursor.getDouble(index))
                Cursor.FIELD_TYPE_BLOB -> put(name, "")
                else -> put(name, JSONObject.NULL)
            }
        }
    }

    companion object {
        private val VALID_NAME = Regex("[A-Za-z0-9._-]+\\.json")

        // Parents before children so foreign keys are satisfied during restore.
        private val PARENT_FIRST_TABLES = listOf(
            "semesters",
            "modules",
            "topics",
            "academic_sources",
            "module_outlines",
            "study_availability",
            "calendar_exceptions",
            "exams",
            "exam_sections",
            "exam_question_rules",
            "topic_aliases",
            "learning_topic_matches",
            "study_sessions",
            "focus_sessions",
            "session_interruptions",
            "learning_events"
        )
        private val CHILD_FIRST_TABLES = PARENT_FIRST_TABLES.reversed()
    }
}
