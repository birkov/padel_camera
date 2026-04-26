package com.padelcamera.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.pedro.rtmp.utils.ConnectCheckerRtmp
import com.padelcamera.app.api.ApiClient
import com.padelcamera.app.api.model.PlayerData
import com.padelcamera.app.api.model.ScoreData
import com.padelcamera.app.config.AppConfig
import com.padelcamera.app.databinding.ActivityMainBinding
import com.padelcamera.app.stream.StreamManager
import kotlinx.coroutines.*

private const val TAG = "MainActivity"
private const val REQUEST_PERMISSIONS = 1001

class MainActivity : AppCompatActivity(), ConnectCheckerRtmp {

    private lateinit var binding: ActivityMainBinding
    private lateinit var config: AppConfig
    private lateinit var streamManager: StreamManager

    private var player1Name = "Player 1"
    private var player2Name = "Player 2"
    private var score1 = 0
    private var score2 = 0

    private var scorePollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        config = AppConfig.load(this)

        if (allPermissionsGranted()) {
            initCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                REQUEST_PERMISSIONS
            )
        }

        binding.btnStartStop.setOnClickListener {
            if (streamManager.isStreaming) {
                stopStreaming()
            } else {
                startStreaming()
            }
        }

        binding.btnSwitchCamera.setOnClickListener {
            streamManager.switchCamera()
        }
    }

    private fun initCamera() {
        streamManager = StreamManager(binding.openGlView, this)
        streamManager.startPreview()

        // Set initial overlay before first score fetch
        streamManager.updateOverlay(player1Name, player2Name, score1, score2)

        loadInitialData()
    }

    private fun loadInitialData() {
        if (config.testMode) {
            // Use test values — already set as defaults
            updateOverlay()
        } else {
            lifecycleScope.launch {
                fetchPlayers()
                fetchScore()
            }
        }
        startScorePolling()
    }

    private fun startScorePolling() {
        scorePollingJob?.cancel()
        scorePollingJob = lifecycleScope.launch {
            val intervalMs = config.scoreRefreshIntervalSeconds * 1_000L
            while (isActive) {
                delay(intervalMs)
                if (!config.testMode) {
                    fetchScore()
                }
            }
        }
    }

    private suspend fun fetchPlayers() {
        try {
            val data: PlayerData = ApiClient.service.getPlayers(config.playersApiUrl)
            player1Name = data.player1Name
            player2Name = data.player2Name
            updateOverlay()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch players", e)
        }
    }

    private suspend fun fetchScore() {
        try {
            val data: ScoreData = ApiClient.service.getScore(config.scoreApiUrl)
            score1 = data.player1Score
            score2 = data.player2Score
            updateOverlay()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch score", e)
        }
    }

    private fun updateOverlay() {
        streamManager.updateOverlay(player1Name, player2Name, score1, score2)
    }

    private fun startStreaming() {
        val key = binding.etStreamKey.text?.toString()?.trim()
        if (key.isNullOrEmpty()) {
            Toast.makeText(this, R.string.error_stream_key_empty, Toast.LENGTH_SHORT).show()
            return
        }

        val rtmpUrl = "${config.youtubeRtmpBaseUrl}/$key"
        val success = streamManager.startStream(rtmpUrl)

        if (!success) {
            Toast.makeText(this, R.string.error_prepare_stream, Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnStartStop.text = getString(R.string.btn_stop_stream)
        binding.tvStatus.text = getString(R.string.status_connecting)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.etStreamKey.isEnabled = false
    }

    private fun stopStreaming() {
        streamManager.stopStream()
        binding.btnStartStop.text = getString(R.string.btn_start_stream)
        binding.tvStatus.text = getString(R.string.status_idle)
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.white))
        binding.tvBitrate.isVisible = false
        binding.etStreamKey.isEnabled = true
    }

    // ── ConnectCheckerRtmp ────────────────────────────────────────────────────

    override fun onConnectionStartedRtmp(rtmpUrl: String) {
        runOnUiThread {
            binding.tvStatus.text = getString(R.string.status_connecting)
        }
    }

    override fun onConnectionSuccessRtmp() {
        runOnUiThread {
            binding.tvStatus.text = getString(R.string.status_streaming)
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.live_red))
        }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            Toast.makeText(this, "Connection failed: $reason", Toast.LENGTH_LONG).show()
            stopStreaming()
        }
    }

    override fun onNewBitrateRtmp(bitrate: Long) {
        runOnUiThread {
            val kbps = bitrate / 1000
            binding.tvBitrate.text = "$kbps kbps"
            binding.tvBitrate.isVisible = true
        }
    }

    override fun onDisconnectRtmp() {
        runOnUiThread {
            Toast.makeText(this, getString(R.string.status_disconnected), Toast.LENGTH_SHORT).show()
            stopStreaming()
        }
    }

    override fun onAuthErrorRtmp() {
        runOnUiThread {
            Toast.makeText(this, "Auth error — check your stream key", Toast.LENGTH_LONG).show()
            stopStreaming()
        }
    }

    override fun onAuthSuccessRtmp() {
        // no UI update needed
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        if (::streamManager.isInitialized && !streamManager.isOnPreview) {
            streamManager.startPreview()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::streamManager.isInitialized) {
            if (streamManager.isStreaming) {
                streamManager.stopStream()
            }
            streamManager.stopPreview()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scorePollingJob?.cancel()
        if (::streamManager.isInitialized) {
            streamManager.release()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && allPermissionsGranted()) {
            initCamera()
        } else {
            Toast.makeText(this, R.string.error_permissions, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun allPermissionsGranted() = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    ).all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}
