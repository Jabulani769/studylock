package studylock.app.ui.screens.documents

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import studylock.app.AppUiState
import studylock.app.documents.DocumentMeta
import studylock.app.documents.DocumentStore
import studylock.app.documents.DocumentType
import studylock.app.documents.DocumentTypes
import studylock.app.ui.components.EmptyState
import studylock.app.ui.theme.Spacing

@Composable
fun DocumentReaderScreen(
    documentId: String,
    state: AppUiState,
    store: DocumentStore,
    onPositionChanged: (String, Int) -> Unit
) {
    val document = state.documents.firstOrNull { it.id == documentId }
    if (document == null) {
        EmptyState(title = "Document unavailable", body = "This file is no longer on this phone.")
        return
    }
    val file = store.file(document)
    if (file == null) {
        EmptyState(title = "Document unavailable", body = "This file is no longer on this phone.")
        return
    }
    when (document.type) {
        DocumentType.PDF -> PdfReader(document, file, onPositionChanged)
        DocumentType.IMAGE -> ImageReader(file)
        DocumentType.TEXT -> TextReader(document, file, onPositionChanged)
    }
}

@Composable
private fun PdfReader(
    document: DocumentMeta,
    file: java.io.File,
    onPositionChanged: (String, Int) -> Unit
) {
    val pageCount = remember(file.path) {
        runCatching {
            val renderer = PdfRenderer(
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            )
            val count = renderer.pageCount
            renderer.close()
            count
        }.getOrDefault(0)
    }
    var page by remember(document.id) {
        mutableIntStateOf(DocumentTypes.clampPosition(document.lastPosition, pageCount))
    }
    val bitmap = remember(file.path, page) {
        renderPdfPage(file, page)
    }
    LaunchedEffect(document.id, page) { onPositionChanged(document.id, page) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = 88.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().background(Color.White)
                )
            } else {
                Text(
                    text = "This page could not be rendered on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "Page ${page + 1} of $pageCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pageCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                PageArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    description = "Previous page",
                    enabled = page > 0,
                    onClick = { page-- }
                )
                PageArrow(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    description = "Next page",
                    enabled = page < pageCount - 1,
                    onClick = { page++ }
                )
            }
        }
    }
}

@Composable
private fun PageArrow(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun renderPdfPage(file: java.io.File, page: Int): Bitmap? = try {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)
    val target = renderer.openPage(page)
    val width = 1000
    val height = (target.height * width.toFloat() / target.width).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    // PDF pages are transparent where there is no ink. Without an opaque white
    // base, dark text disappears against the app's dark background.
    bitmap.eraseColor(android.graphics.Color.WHITE)
    target.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    target.close()
    renderer.close()
    descriptor.close()
    bitmap
} catch (e: Exception) {
    null
}

@Composable
private fun ImageReader(file: java.io.File) {
    val bitmap = remember(file.path) { BitmapFactory.decodeFile(file.path) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        contentAlignment = Alignment.TopCenter
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Study image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = "This image could not be opened on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextReader(
    document: DocumentMeta,
    file: java.io.File,
    onPositionChanged: (String, Int) -> Unit
) {
    val text = remember(file.path) {
        runCatching { file.readText(Charsets.UTF_8) }
            .getOrDefault("This text file could not be opened on this device.")
    }
    val scroll = rememberScrollState()
    LaunchedEffect(document.id) { scroll.scrollTo(document.lastPosition) }
    DisposableEffect(document.id) {
        onDispose { onPositionChanged(document.id, scroll.value) }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(Spacing.lg)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
