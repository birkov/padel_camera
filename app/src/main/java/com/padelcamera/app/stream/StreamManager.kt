package com.padelcamera.app.stream

import android.content.Context
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.gl.render.filters.`object`.ObjectFilterRender
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.library.generic.GenericStream
import com.padelcamera.app.overlay.ScoreOverlayRenderer

private const val TAG = "StreamManager"

private const val VIDEO_WIDTH = 1280
private const val VIDEO_HEIGHT = 720
private const val VIDEO_BITRATE = 4_000_000   // 4 Mbps
private const val AUDIO_SAMPLE_RATE = 44100
private const val AUDIO_STEREO = true
private const val AUDIO_BITRATE = 128_000

class StreamManager(
    context: Context,
    surfaceView: SurfaceView,
    connectChecker: ConnectChecker
) {
    private val genericStream = GenericStream(
        context, connectChecker, Camera2Source(context), MicrophoneSource()
    ).apply {
        getGlInterface().autoHandleOrientation = true
        getStreamClient().setReTries(3)
    }

    private val overlayFilter = ObjectFilterRender()
    private val overlayRenderer = ScoreOverlayRenderer(VIDEO_WIDTH, VIDEO_HEIGHT)
    var isPrepared = false
        private set

    val isStreaming: Boolean get() = genericStream.isStreaming
    val isOnPreview: Boolean get() = genericStream.isOnPreview

    init {
        isPrepared = try {
            genericStream.prepareVideo(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_BITRATE) &&
            genericStream.prepareAudio(AUDIO_SAMPLE_RATE, AUDIO_STEREO, AUDIO_BITRATE)
        } catch (e: Exception) {
            Log.e(TAG, "Encoder preparation failed", e)
            false
        }

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (!genericStream.isOnPreview) {
                    genericStream.startPreview(surfaceView)
                    attachOverlayFilter()
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                genericStream.getGlInterface().setPreviewResolution(width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (genericStream.isOnPreview) genericStream.stopPreview()
            }
        })
    }

    fun startStream(rtmpUrl: String): Boolean {
        if (!isPrepared) return false
        genericStream.startStream(rtmpUrl)
        return true
    }

    fun stopStream() {
        if (genericStream.isStreaming) genericStream.stopStream()
    }

    fun switchCamera() {
        (genericStream.videoSource as? Camera2Source)?.switchCamera()
    }

    fun updateOverlay(player1Name: String, player2Name: String, score1: Int, score2: Int) {
        val bitmap = overlayRenderer.render(player1Name, player2Name, score1, score2)
        overlayFilter.setImage(bitmap)
    }

    fun release() {
        genericStream.release()
    }

    private fun attachOverlayFilter() {
        try {
            genericStream.getGlInterface().addFilter(overlayFilter)
            overlayFilter.setPercent(100f, 100f, 0f, 0f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay filter", e)
        }
    }
}
