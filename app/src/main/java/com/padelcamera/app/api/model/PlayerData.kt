package com.padelcamera.app.api.model

import com.google.gson.annotations.SerializedName

data class PlayerData(
    @SerializedName("player1_name") val player1Name: String,
    @SerializedName("player2_name") val player2Name: String
)
