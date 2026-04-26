package com.padelcamera.app.api.model

import com.google.gson.annotations.SerializedName

data class ScoreData(
    @SerializedName("player1_score") val player1Score: Int,
    @SerializedName("player2_score") val player2Score: Int
)
