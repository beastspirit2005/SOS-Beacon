package com.example.meshsosrelay.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface BeaconApi {
    @POST("/api/v1/sos/ingest")
    suspend fun ingestSos(
        @Header("X-Gateway-Id") gatewayId: String,
        @Header("X-App-Version") appVersion: String = "2.0.0",
        @Body request: IngestRequest
    ): Response<IngestResult>
}
