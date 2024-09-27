package com.ecommerce.experimentapp.network

import com.ecommerce.experimentapp.model.ContactReq
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
    @POST("uploadToRemote")
    fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("description") description: RequestBody
    ): Call<Void>

    @POST("fcmToken")
    fun sendFcmToken(
        @Body fcmToken: FCMTokenData
    ): Call<Void>

    @POST("contacts")
    fun sendContacts(
        @Body contactReq: ContactReq
    ): Call<String>
}