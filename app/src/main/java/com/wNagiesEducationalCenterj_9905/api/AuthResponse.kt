package com.wNagiesEducationalCenterj_9905.api

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("id")
    val Id:String,
    @SerializedName("status")
    val Status:Int,
    @SerializedName("message")
    val Message:String,
    @SerializedName("token")
    val Token:String,
    @SerializedName("imageUrl")
    val image:String
)