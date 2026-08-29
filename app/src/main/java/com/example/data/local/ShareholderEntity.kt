package com.example.data.local

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@IgnoreExtraProperties
@JsonClass(generateAdapter = true)
data class ShareholderEntity(
    @Json(name = "id")
    val id: String = "",
    @Json(name = "name")
    val name: String = "",
    @Json(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)


