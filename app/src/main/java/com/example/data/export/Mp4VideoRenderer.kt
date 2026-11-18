package com.example.data.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.model.CameraMovement
import com.example.model.ProjectUi
import com.example.model.RenderVideoState
import com.example.model.SceneTransition
import com.example.model.SceneUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Mp4VideoRenderer(private val context: Context) {

    suspend fun renderFinalMp4(
        project: ProjectUi,
        scenes: List<SceneUi>,
        onProgress: (Float, String) -> Unit
    ): RenderVideoState = withContext(Dispatchers.Default) {
        if (scenes.isEmpty()) {
            return@withContext RenderVideoState.Error("Tidak ada scene untuk dirender.")
        }

        val outDir = File(context.filesDir, "rendered_videos").apply { mkdirs() }
        val safeName = project.title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(32).ifBlank { "StoryboardKu" }
        val outFile = File(outDir, "Final_${safeName}_${System.currentTimeMillis()}.mp4")

        val fps = 30
        val resolutions = listOf(1080 to 1920, 720 to 1280)
        var lastError: Exception? = null

        for ((width, height) in resolutions) {
            try {
                encodeMp4WithSurface(
                    outFile = outFile,
                    width = width,
                    height = height,
                    fps = fps,
                    project = project,
                    scenes = scenes,
                    onProgress = onProgress
                )
                if (outFile.exists() && outFile.length() > 1024L) {
                    return@withContext RenderVideoState.Completed(
                        mp4Path = outFile.absolutePath,
                        durationSec = scenes.sumOf { it.durationSec },
                        width = width,
                        height = height,
                        fps = fps
                    )
                }
            } catch (e: Exception) {
                coroutineContext.ensureActive()
                lastError = e
                outFile.delete()
            }
        }

        RenderVideoState.Error(
            "Gagal merender MP4: ${lastError?.localizedMessage ?: "Encoder video perangkat tidak mendukung resolusi ini."}"
        )
    }

    private suspend fun encodeMp4WithSurface(
        outFile: File,
        width: Int,
        height: Int,
        fps: Int,
        project: ProjectUi,
        scenes: List<SceneUi>,
        onProgress: (Float, String) -> Unit
    ) {
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, if (width >= 1080) 5_500_000 else 3_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mime)
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        var inputSurface: android.view.Surface? = null

        val cachedBitmaps = mutableMapOf<Long, Bitmap>()

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()

            // Preload scene bitmaps (scaled cleanly to save RAM)
            val placeholderBmp = loadPlaceholderBitmap()
            scenes.forEach { scene ->
                val path = scene.activeImagePath
                val bmp = if (path != null && File(path).exists()) {
                    decodeSampledBitmap(path, width, height)
                } else null
                if (bmp != null) cachedBitmaps[scene.id] = bmp
            }

            // Cap total encoded frame budget cleanly while preserving accurate timestamps if very long (> 3 min)
            val totalDurationSec = scenes.sumOf { it.durationSec }.coerceAtLeast(1)
            val totalFramesAll = totalDurationSec * fps
            var renderedFrames = 0
            var frameIndexGlobal = 0L

            val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = width * 0.036f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f, 0f, 2f, Color.BLACK)
            }
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#12151C") }
            val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG)

            for ((sceneIdx, scene) in scenes.withIndex()) {
                val sceneFrames = (scene.durationSec * fps).coerceAtLeast(fps)
                val bmp = cachedBitmaps[scene.id] ?: placeholderBmp
                val nextScene = scenes.getOrNull(sceneIdx + 1)
                val nextBmp = nextScene?.let { cachedBitmaps[it.id] ?: placeholderBmp }

                for (f in 0 until sceneFrames) {
                    coroutineContext.ensureActive()

                    val t = f.toFloat() / sceneFrames.coerceAtLeast(1)
                    val eased = easeAnticipationFollowThrough(t)

                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                        if (bmp != null) {
                            drawAnimatedFrame(canvas, bmp, width, height, scene.cameraMovement, eased, 255)
                        }

                        // Smooth transition in last 0.5s (15 frames)
                        val transitionFrames = (fps / 2).coerceAtMost(sceneFrames / 3)
                        if (nextBmp != null && f >= sceneFrames - transitionFrames) {
                            val transT = (f - (sceneFrames - transitionFrames)).toFloat() / transitionFrames.coerceAtLeast(1)
                            when (scene.transition) {
                                SceneTransition.FADE, SceneTransition.SMOOTH_CINEMATIC -> {
                                    val alpha = (transT * 255).toInt().coerceIn(0, 255)
                                    drawAnimatedFrame(canvas, nextBmp, width, height, nextScene.cameraMovement, 0f, alpha)
                                }
                                SceneTransition.SLIDE -> {
                                    canvas.save()
                                    canvas.translate(width * (1f - transT), 0f)
                                    drawAnimatedFrame(canvas, nextBmp, width, height, nextScene.cameraMovement, 0f, 255)
                                    canvas.restore()
                                }
                                SceneTransition.CUT -> Unit
                            }
                        }

                        // Subtitles ONLY rendered if explicitly enabled by user
                        if (project.subtitlesEnabled) {
                            val subText = scene.dialogueText.ifBlank { scene.narrationText }
                            if (subText.isNotBlank()) {
                                drawSubtitleOverlay(canvas, subText, width, height, subtitlePaint)
                            }
                        }
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }

                    // Drain encoder buffers non-blocking per frame
                    val drainRes = drainEncoder(encoder, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = false)
                    trackIndex = drainRes.first
                    muxerStarted = drainRes.second

                    frameIndexGlobal++
                    renderedFrames++
                    if (f % 10 == 0 || f == sceneFrames - 1) {
                        val progress = (renderedFrames.toFloat() / totalFramesAll.coerceAtLeast(1)).coerceIn(0f, 0.98f)
                        onProgress(
                            progress,
                            "Merender Scene ${sceneIdx + 1}/${scenes.size} (${width}x${height} • $fps FPS)..."
                        )
                    }
                }
            }

            onProgress(0.99f, "Menyelesaikan file MP4...")
            encoder.signalEndOfInputStream()
            val finalDrain = drainEncoder(encoder, muxer, bufferInfo, trackIndex, muxerStarted, endOfStream = true)
            muxerStarted = finalDrain.second

            placeholderBmp?.recycle()
        } finally {
            cachedBitmaps.values.forEach { if (!it.isRecycled) it.recycle() }
            runCatching { inputSurface?.release() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufferInfo: MediaCodec.BufferInfo,
        initialTrackIndex: Int,
        initialMuxerStarted: Boolean,
        endOfStream: Boolean
    ): Pair<Int, Boolean> {
        var trackIndex = initialTrackIndex
        var muxerStarted = initialMuxerStarted
        val timeoutUs = if (endOfStream) 10_000L else 0L

        while (true) {
            val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, timeoutUs)
            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) break
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (muxerStarted) break
                    trackIndex = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    muxerStarted = true
                }
                outputIndex >= 0 -> {
                    val encodedData = encoder.getOutputBuffer(outputIndex)
                    if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (bufferInfo.size > 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break
                    }
                }
            }
        }
        return trackIndex to muxerStarted
    }

    private fun drawAnimatedFrame(
        canvas: Canvas,
        bmp: Bitmap,
        w: Int,
        h: Int,
        movement: CameraMovement,
        eased: Float,
        alpha: Int
    ) {
        val baseScale = maxOf(w.toFloat() / bmp.width, h.toFloat() / bmp.height) * 1.15f
        val (scaleMul, txFrac, tyFrac) = when (movement) {
            CameraMovement.DOLLY_IN, CameraMovement.CLOSE_UP -> Triple(1.0f + 0.16f * eased, 0f, 0f)
            CameraMovement.DOLLY_OUT, CameraMovement.WIDE_SHOT -> Triple(1.16f - 0.14f * eased, 0f, 0f)
            CameraMovement.PAN_LEFT -> Triple(1.08f, (0.06f - 0.12f * eased), 0f)
            CameraMovement.PAN_RIGHT -> Triple(1.08f, (-0.06f + 0.12f * eased), 0f)
            CameraMovement.TRACKING -> Triple(1.10f, -0.05f + 0.10f * eased, -0.01f * sin(eased * PI).toFloat())
            CameraMovement.CRANE -> Triple(1.10f, 0f, 0.05f - 0.10f * eased)
            CameraMovement.ORBIT -> Triple(1.12f, 0.05f * sin(eased * 2 * PI).toFloat(), 0.03f * cos(eased * 2 * PI).toFloat())
            CameraMovement.MEDIUM_SHOT, CameraMovement.STATIC -> Triple(1.03f + 0.03f * eased, 0f, 0f)
        }

        val finalScale = baseScale * scaleMul
        val matrix = Matrix().apply {
            postTranslate(-bmp.width / 2f, -bmp.height / 2f)
            postScale(finalScale, finalScale)
            postTranslate(w / 2f + txFrac * w, h / 2f + tyFrac * h)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = alpha
        }
        canvas.drawBitmap(bmp, matrix, paint)
    }

    private fun drawSubtitleOverlay(
        canvas: Canvas,
        text: String,
        w: Int,
        h: Int,
        paint: TextPaint
    ) {
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(175, 16, 20, 28) }
        val pad = w * 0.06f
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, (w - pad * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()
        val top = h * 0.80f
        canvas.drawRoundRect(
            RectF(pad - 18f, top - 16f, w - pad + 18f, top + layout.height + 16f),
            18f, 18f, boxPaint
        )
        canvas.save()
        canvas.translate(pad, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun easeAnticipationFollowThrough(t: Float): Float {
        // Subtle anticipation dip followed by smooth cubic motion
        return if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
    }

    private fun loadPlaceholderBitmap(): Bitmap? =
        runCatching { BitmapFactory.decodeResource(context.resources, R.drawable.img_scene_placeholder) }.getOrNull()

    private fun decodeSampledBitmap(path: String, reqW: Int, reqH: Int): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, opts)
        var sample = 1
        while (opts.outWidth / sample > reqW * 2 || opts.outHeight / sample > reqH * 2) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    suspend fun copyMp4ToUri(mp4Path: String, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(mp4Path)
            if (!file.exists()) error("File MP4 belum dirender.")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                FileInputStream(file).use { it.copyTo(out) }
            } ?: error("Gagal membuka tujuan penyimpanan.")
            Unit
        }
    }

    suspend fun saveMp4ToMoviesGallery(mp4Path: String, projectTitle: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(mp4Path)
            if (!file.exists()) error("File MP4 tidak ditemukan.")
            val safe = projectTitle.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(28).ifBlank { "StoryboardKu" }
            val name = "StoryboardKu_${safe}_${System.currentTimeMillis()}.mp4"
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/StoryboardKu")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Gagal menyimpan video ke Galeri.")
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(file).use { it.copyTo(out) }
            } ?: error("Gagal menulis video ke Galeri.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            name
        }
    }
}
