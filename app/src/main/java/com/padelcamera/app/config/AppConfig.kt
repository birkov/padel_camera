package com.padelcamera.app.config

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AppConfig(
    @SerializedName("test_mode") val testMode: Boolean,
    @SerializedName("players_api_url") val playersApiUrl: String,
    @SerializedName("score_api_url") val scoreApiUrl: String,
    @SerializedName("score_refresh_interval_seconds") val scoreRefreshIntervalSeconds: Int,
    @SerializedName("youtube_rtmp_base_url") val youtubeRtmpBaseUrl: String
) {
    companion object {
        fun load(context: Context): AppConfig {
            val json = context.assets.open("config.json").bufferedReader().use { it.readText() }
            return Gson().fromJson(json, AppConfig::class.java)
        }
    }
}
