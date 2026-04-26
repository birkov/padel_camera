package com.padelcamera.app.api

import com.padelcamera.app.api.model.PlayerData
import com.padelcamera.app.api.model.ScoreData
import retrofit2.http.GET
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun getPlayers(@Url url: String): PlayerData

    @GET
    suspend fun getScore(@Url url: String): ScoreData
}
