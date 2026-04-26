package com.padelcamera.app.stream

import android.util.Log
import com.pedro.encoder.input.gl.OpenGlView
import com.pedro.encoder.input.gl.render.filters.`object`.ObjectFilterRender
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.padelcamera.app.overlay.ScoreOverlayRenderer

private const val TAG = "StreamManager"

// Video stream dimensions (720p landscape)
private const val VIDEO_WIDTH = 1280
private const val VIDEO_HEIGHT = 720
private const val VIDEO_FPS = 30
private const val VIDEO_BITRATE = 4_000_000  // 4 Mbps

// Audio settings
private const val AUDIO_BITRATE = 128_000
private const val AUDIO_SAMPLE_RATE = 44100
private const val AUDIO_STEREO = true

class StreamManager(
    openGlView: OpenGlView,
    connectChecker: ConnectCheckerRtmp
) {
    private val rtmpCamera2 = RtmpCamera2(openGlView, connectChecker)
    private val overlayFilter = ObjectFilterRender()
    private val overlayRenderer = ScoreOverlayRenderer(VIDEO_WIDTH, VIDEO_HEIGHT)

    val isStreaming: Boolean get() = rtmpCamera2.isStreaming
    val isOnPreview: Boolean get() = rtmpCamera2.isOnPreview

    fun startPreview() {
        if (!rtmpCamera2.isOnPreview) {
            rtmpCamera2.startPreview(VIDEO_WIDTH, VIDEO_HEIGHT)
            attachOverlayFilter()
        }
    }

    fun stopPreview() {
        if (rtmpCamera2.isOnPreview) {
            rtmpCamera2.stopPreview()
        }
    }

    fun startStream(rtmpUrl: String): Boolean {
        val audioReady = rtmpCamera2.prepareAudio(AUDIO_BITRATE, AUDIO_SAMPLE_RATE, AUDIO_STEREO)
        val videoReady = rtmpCamera2.prepareVideo(VIDEO_WIDTH, VIDEO_HEIGHT, VIDEO_FPS, VIDEO_BITRATE)

        if (!audioReady || !videoReady) {
            Log.e(TAG, "Failed to prepare encoders (audio=$audioReady, video=$videoReady)")
            return false
        }

        rtmpCamera2.startStream(rtmpUrl)
        return true
    }

    fun stopStream() {
        if (rtmpCamera2.isStreaming) {
            rtmpCamera2.stopStream()
        }
    }

    fun switchCamera() {
        rtmpCamera2.switchCamera()
    }

    fun updateOverlay(player1Name: String, player2Name: String, score1: Int, score2: Int) {
        val bitmap = overlayRenderer.render(player1Name, player2Name, score1, score2)
        overlayFilter.setImage(bitmap)
    }

    fun release() {
        stopStream()
        stopPreview()
    }

    private fun attachOverlayFilter() {
        try {
            val glInterface = rtmpCamera2.glInterface
            glInterface.addFilter(overlayFilter)
            // Scale the filter to cover the full frame (ObjectFilterRender uses percentage scale)
            overlayFilter.setPercent(100f, 100f, 0f, 0f)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay filter", e)
        }
    }
}
