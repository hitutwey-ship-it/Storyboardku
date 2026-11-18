package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.example.R
import com.example.model.AspectRatioOption
import com.example.model.CharacterUi
import com.example.model.SceneUi
import com.example.model.VisualStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed interface AiGenerationResult {
    data class Success(
        val variation1Path: String,
        val variation2Path: String,
        val isPlaceholder: Boolean,
        val notice: String
    ) : AiGenerationResult

    data class Error(val message: String) : AiGenerationResult
}

interface AiImageRepository {
    val isRealAiEnabled: Boolean
    suspend fun generateSceneVariations(
        scene: SceneUi,
        characters: List<CharacterUi>,
        aspectRatio: AspectRatioOption,
        visualStyle: VisualStyle,
        prompt: String
    ): AiGenerationResult
}

/**
 * Placeholder implementation used when Firebase AI Logic is not enabled.
 * Renders two clearly labeled sample preview storyboard frames (Variasi A & B)
 * so all UI workflows remain verifiable while transparently informing the user
 * that real cloud AI image generation requires Firebase AI Logic.
 */
class PlaceholderAiImageRepository(
    private val context: Context
) : AiImageRepository {

    override val isRealAiEnabled: Boolean = false

    override suspend fun generateSceneVariations(
        scene: SceneUi,
        characters: List<CharacterUi>,
        aspectRatio: AspectRatioOption,
        visualStyle: VisualStyle,
        prompt: String
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        delay(850L)

        try {
            val outDir = File(context.filesDir, "storyboard_images").apply { mkdirs() }
            val timestamp = System.currentTimeMillis()
            val file1 = File(outDir, "scene_${scene.id}_v1_${timestamp}.jpg")
            val file2 = File(outDir, "scene_${scene.id}_v2_${timestamp}.jpg")

            val width = if (aspectRatio == AspectRatioOption.RATIO_16_9) 960 else 540
            val height = if (aspectRatio == AspectRatioOption.RATIO_16_9) 540 else 960

            renderSampleStoryboardBitmap(
                width = width,
                height = height,
                scene = scene,
                characters = characters,
                visualStyle = visualStyle,
                variantLabel = "VARIASI A • PRATINJAU SAMPEL",
                accentTop = Color.parseColor("#1B2432"),
                accentBottom = Color.parseColor("#D95D39"),
                outFile = file1
            )

            renderSampleStoryboardBitmap(
                width = width,
                height = height,
                scene = scene,
                characters = characters,
                visualStyle = visualStyle,
                variantLabel = "VARIASI B • PRATINJAU SAMPEL",
                accentTop = Color.parseColor("#2A3B4C"),
                accentBottom = Color.parseColor("#D9822B"),
                outFile = file2
            )

            AiGenerationResult.Success(
                variation1Path = file1.absolutePath,
                variation2Path = file2.absolutePath,
                isPlaceholder = true,
                notice = "Generator AI bawaan memerlukan Firebase AI Logic. Menampilkan pratinjau sampel — tekan 'Salin Prompt' & 'Buka Generator' atau impor gambar asli."
            )
        } catch (e: Exception) {
            AiGenerationResult.Error(
                "Gagal membuat pratinjau gambar: ${e.localizedMessage ?: "Kesalahan penyimpanan lokal"}"
            )
        }
    }

    private fun renderSampleStoryboardBitmap(
        width: Int,
        height: Int,
        scene: SceneUi,
        characters: List<CharacterUi>,
        visualStyle: VisualStyle,
        variantLabel: String,
        accentTop: Int,
        accentBottom: Int,
        outFile: File
    ) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Base backdrop: use reference image if user uploaded one, otherwise gradient
        val refBitmap = scene.referenceImagePath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
        } ?: runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_scene_placeholder)
        }.getOrNull()

        if (refBitmap != null) {
            val srcRect = Rect(0, 0, refBitmap.width, refBitmap.height)
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(refBitmap, srcRect, dstRect, null)
            val tintPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    Color.argb(175, Color.red(accentTop), Color.green(accentTop), Color.blue(accentTop)),
                    Color.argb(200, Color.red(accentBottom), Color.green(accentBottom), Color.blue(accentBottom)),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tintPaint)
            refBitmap.recycle()
        } else {
            val bgPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    accentTop, accentBottom, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }

        // Storyboard framing guides
        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(24f, 24f, width - 24f, height - 24f, guidePaint)

        // Draw small locked character portraits on the side if present
        val thumbSize = (minOf(width, height) * 0.22f).toInt().coerceIn(72, 136)
        var thumbX = 36f
        val thumbY = 78f
        characters.take(3).forEach { char ->
            val charBmp = loadCharacterBitmap(char)
            if (charBmp != null) {
                val dst = RectF(thumbX, thumbY, thumbX + thumbSize, thumbY + thumbSize)
                val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
                canvas.drawRoundRect(
                    RectF(dst.left - 3f, dst.top - 3f, dst.right + 3f, dst.bottom + 3f),
                    14f, 14f, cardBg
                )
                canvas.drawBitmap(charBmp, null, dst, null)
                charBmp.recycle()
                thumbX += thumbSize + 14f
            }
        }

        // Top badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(220, 20, 24, 34)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (width > height) 22f else 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(240, 245, 247, 250)
            textSize = if (width > height) 17f else 16f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFD180")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawRoundRect(RectF(36f, 34f, width - 36f, 70f), 10f, 10f, badgePaint)
        canvas.drawText(
            "SCENE ${scene.orderIndex + 1} • ${scene.cameraShot.label.uppercase()} • ${visualStyle.label.uppercase()}",
            50f, 58f, subPaint
        )

        // Scene summary panel
        val panelTop = height - 154f
        canvas.drawRoundRect(
            RectF(36f, panelTop, width - 36f, height - 34f),
            14f, 14f, badgePaint
        )
        canvas.drawText(scene.title.take(44), 52f, panelTop + 34f, titlePaint)
        val snippet = scene.visualDescription.ifBlank { scene.location }.take(68)
        canvas.drawText(snippet, 52f, panelTop + 64f, bodyPaint)

        // Explicit placeholder notice banner
        val noticePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFE082")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("$variantLabel • Butuh Firebase AI Logic", 52f, panelTop + 98f, noticePaint)

        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, fos)
        }
        bitmap.recycle()
    }

    private fun loadCharacterBitmap(char: CharacterUi): Bitmap? {
        char.photoPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(path) }.getOrNull()?.let { return it }
        }
        return when (char.bundledDrawableName) {
            "img_char_sireum_boy" -> runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.img_char_sireum_boy)
            }.getOrNull()
            "img_char_kricik" -> runCatching {
                BitmapFactory.decodeResource(context.resources, R.drawable.img_char_kricik)
            }.getOrNull()
            else -> null
        }
    }
}
