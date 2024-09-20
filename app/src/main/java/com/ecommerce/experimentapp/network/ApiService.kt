package com.ecommerce.experimentapp.network

import com.ecommerce.experimentapp.model.FCMTokenData
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("uploadToRemote") // Replace with your API endpoint
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody
    ): Call<Void>

    @POST("fcmToken") // Replace with your API endpoint
    fun sendFcmToken(
        @Body fcmToken: FCMTokenData
    ): Call<Void>
}