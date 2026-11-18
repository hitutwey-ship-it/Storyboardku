package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.model.CharacterUi
import com.example.model.ProjectUi
import com.example.model.SceneUi
import com.example.model.buildAutoVideoPrompt
import com.example.model.formatClockSeconds
import com.example.model.formatTimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportHelper(private val context: Context) {

    suspend fun exportStoryboardPdfToUri(
        uri: Uri,
        project: ProjectUi,
        characters: List<CharacterUi>,
        scenes: List<SceneUi>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                writePdfDocument(out, project, characters, scenes)
            } ?: error("Gagal membuka lokasi penyimpanan PDF.")
        }
    }

    suspend fun exportStoryboardPdfToTempFile(
        project: ProjectUi,
        characters: List<CharacterUi>,
        scenes: List<SceneUi>
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = sanitizeFilename(project.title)
            val file = File(context.cacheDir, "Storyboard_${safeName}.pdf")
            file.outputStream().use { out ->
                writePdfDocument(out, project, characters, scenes)
            }
            file
        }
    }

    private fun writePdfDocument(
        outputStream: OutputStream,
        project: ProjectUi,
        characters: List<CharacterUi>,
        scenes: List<SceneUi>
    ) {
        val doc = PdfDocument()
        val pageWidth = 595 // A4 points
        val pageHeight = 842
        val margin = 36f
        val contentWidth = pageWidth - (margin * 2)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1B2432")
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5A616E")
            textSize = 11f
        }
        val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D95D39")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#181C24")
            textSize = 11f
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#DCD3C8")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val totalDur = scenes.sumOf { it.durationSec }
        var cumulative = 0

        if (scenes.isEmpty()) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            page.canvas.drawText("StoryboardKu — ${project.title}", margin, 60f, titlePaint)
            page.canvas.drawText("Belum ada scene dalam proyek ini.", margin, 95f, bodyPaint)
            doc.finishPage(page)
        } else {
            scenes.forEachIndexed { index, scene ->
                val page = doc.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                )
                val canvas = page.canvas
                var y = margin + 12f

                // Header
                canvas.drawText("StoryboardKu • ${project.title}", margin, y, titlePaint)
                y += 18f
                canvas.drawText(
                    "Gaya: ${project.visualStyle.label} | Rasio: ${project.aspectRatio.label} | Genre: ${project.genre.label} | Total: ${scenes.size} scene (${formatClockSeconds(totalDur)})",
                    margin, y, subtitlePaint
                )
                y += 14f
                canvas.drawLine(margin, y, pageWidth - margin, y, borderPaint)
                y += 20f

                // Scene Banner
                val timeRange = formatTimeRange(cumulative, scene.durationSec)
                cumulative += scene.durationSec
                canvas.drawText(
                    "SCENE ${index + 1}: ${scene.title}",
                    margin, y, sectionPaint
                )
                y += 16f
                canvas.drawText(
                    "Durasi: ${scene.durationSec} detik ($timeRange) • Status: ${scene.status.label}",
                    margin, y, subtitlePaint
                )
                y += 14f

                // Thumbnail Box
                val thumbWidth = 260f
                val thumbHeight = if (project.aspectRatio.ratioFloat > 1f) 146f else 220f
                val thumbRect = RectF(margin, y, margin + thumbWidth, y + thumbHeight)
                val bmp = scene.activeImagePath?.let { runCatching { BitmapFactory.decodeFile(it) }.getOrNull() }
                    ?: runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.img_scene_placeholder) }.getOrNull()
                if (bmp != null) {
                    canvas.drawBitmap(bmp, null, thumbRect, null)
                    bmp.recycle()
                }
                canvas.drawRect(thumbRect, borderPaint)

                // Metadata beside thumbnail
                val metaX = margin + thumbWidth + 16f
                val metaW = (pageWidth - margin - metaX).toInt().coerceAtLeast(120)
                val sceneChars = characters.filter { it.id in scene.characterIds }
                val metaText = buildString {
                    appendLine("Lokasi: ${scene.location.ifBlank { "-" }}")
                    appendLine("Kamera: ${scene.cameraShot.label} (${scene.cameraMovement.label})")
                    appendLine("Transisi: ${scene.transition.label}")
                    appendLine("Properti: ${scene.importantProps.ifBlank { "-" }}")
                    appendLine("Suasana: ${scene.mood.ifBlank { "-" }}")
                    appendLine("Musik/SFX: ${scene.musicSfx.ifBlank { "-" }}")
                    append("Karakter: ${sceneChars.joinToString(", ") { it.name }.ifBlank { "-" }}")
                }
                drawWrappedText(canvas, metaText, metaX, y + 4f, metaW, bodyPaint)
                y += thumbHeight + 20f

                // Detail Sections
                y = drawLabeledParagraph(canvas, "Deskripsi Visual:", scene.visualDescription.ifBlank { "-" }, margin, y, contentWidth.toInt(), sectionPaint, bodyPaint)
                val dialogLine = if (scene.dialogueText.isBlank()) "-" else "${scene.dialogueSpeaker.ifBlank { "Karakter" }}: \"${scene.dialogueText}\""
                y = drawLabeledParagraph(canvas, "Dialog (${project.dialogueLanguage.label}):", dialogLine, margin, y, contentWidth.toInt(), sectionPaint, bodyPaint)
                y = drawLabeledParagraph(canvas, "Narasi / Voiceover:", scene.narrationText.ifBlank { "-" }, margin, y, contentWidth.toInt(), sectionPaint, bodyPaint)
                if (scene.notes.isNotBlank()) {
                    drawLabeledParagraph(canvas, "Catatan Produksi:", scene.notes, margin, y, contentWidth.toInt(), sectionPaint, bodyPaint)
                }

                // Footer
                canvas.drawText(
                    "Halaman ${index + 1} dari ${scenes.size} • Dibuat dengan StoryboardKu",
                    margin, pageHeight - 24f, subtitlePaint
                )
                doc.finishPage(page)
            }
        }

        doc.writeTo(outputStream)
        doc.close()
    }

    private fun drawLabeledParagraph(
        canvas: Canvas,
        label: String,
        text: String,
        x: Float,
        startY: Float,
        width: Int,
        labelPaint: TextPaint,
        bodyPaint: TextPaint
    ): Float {
        canvas.drawText(label, x, startY, labelPaint)
        val afterBody = drawWrappedText(canvas, text, x, startY + 6f, width, bodyPaint)
        return afterBody + 14f
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        paint: TextPaint
    ): Float {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.1f)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height
    }

    suspend fun exportAllImagesZip(
        uri: Uri,
        scenes: List<SceneUi>
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            var count = 0
            context.contentResolver.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    scenes.forEachIndexed { idx, scene ->
                        val path = scene.activeImagePath ?: return@forEachIndexed
                        val file = File(path)
                        if (!file.exists()) return@forEachIndexed
                        val safeTitle = sanitizeFilename(scene.title).take(40)
                        val entryName = "Scene_${"%02d".format(idx + 1)}_${safeTitle}.jpg"
                        zos.putNextEntry(ZipEntry(entryName))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                        count++
                    }
                }
            } ?: error("Gagal membuka file ZIP tujuan.")
            if (count == 0) error("Belum ada gambar scene yang dibuat atau diimpor untuk diunduh.")
            count
        }
    }

    suspend fun saveImageToGallery(
        sourceImagePath: String,
        sceneNumber: Int,
        sceneTitle: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(sourceImagePath)
            if (!file.exists()) error("File gambar tidak ditemukan.")

            val safeTitle = sanitizeFilename(sceneTitle).take(32)
            val displayName = "StoryboardKu_Scene_${"%02d".format(sceneNumber)}_${safeTitle}_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/StoryboardKu")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Gagal menyimpan gambar ke Galeri.")

            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(file).use { it.copyTo(out) }
            } ?: error("Gagal menulis gambar ke Galeri.")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            displayName
        }
    }

    fun buildShotListCsv(
        project: ProjectUi,
        characters: List<CharacterUi>,
        scenes: List<SceneUi>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("No Scene,Judul Scene,Waktu,Durasi (Detik),Lokasi,Kamera,Gerakan Kamera,Transisi,Dialog,Narasi,Prompt Video AI")
        var cumulative = 0
        scenes.forEachIndexed { idx, scene ->
            val timeRange = formatTimeRange(cumulative, scene.durationSec)
            cumulative += scene.durationSec
            val sceneChars = characters.filter { it.id in scene.characterIds }
            val videoPrompt = buildAutoVideoPrompt(project, scene, sceneChars)
            val dialog = if (scene.dialogueText.isBlank()) "" else "${scene.dialogueSpeaker}: ${scene.dialogueText}"
            sb.appendLine(
                listOf(
                    "Scene ${idx + 1}",
                    csvEscape(scene.title),
                    timeRange,
                    scene.durationSec.toString(),
                    csvEscape(scene.location),
                    scene.cameraShot.label,
                    scene.cameraMovement.label,
                    scene.transition.label,
                    csvEscape(dialog),
                    csvEscape(scene.narrationText),
                    csvEscape(videoPrompt)
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun buildAllVideoPromptsText(
        project: ProjectUi,
        characters: List<CharacterUi>,
        scenes: List<SceneUi>
    ): String {
        var cumulative = 0
        return scenes.mapIndexed { idx, scene ->
            val range = formatTimeRange(cumulative, scene.durationSec)
            cumulative += scene.durationSec
            val sceneChars = characters.filter { it.id in scene.characterIds }
            val prompt = buildAutoVideoPrompt(project, scene, sceneChars)
            val dialog = if (scene.dialogueText.isBlank()) "-" else "${scene.dialogueSpeaker}: \"${scene.dialogueText}\""
            """
            |=== Scene ${idx + 1}: ${scene.title} ($range • ${scene.durationSec}s) ===
            |Prompt Video: $prompt
            |Dialog: $dialog
            |Narasi: ${scene.narrationText.ifBlank { "-" }}
            """.trimMargin()
        }.joinToString("\n\n")
    }

    private fun csvEscape(value: String): String {
        val cleaned = value.replace("\"", "\"\"").replace("\n", " ")
        return "\"$cleaned\""
    }

    private fun sanitizeFilename(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").replace(Regex("_+"), "_").trim('_')
            .ifBlank { "Storyboard" }
    }
}
