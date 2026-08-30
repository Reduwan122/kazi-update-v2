package com.example.data.local

import androidx.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Keep
@IgnoreExtraProperties
@JsonClass(generateAdapter = true)
data class StaffPaymentEntity(
    @Json(name = "id")
    val id: String = "",
    @Json(name = "staffId")
    val staffId: String = "",
    @Json(name = "staffName")
    val staffName: String = "",
    @Json(name = "date")
    val date: String = "", // DD/MM/YYYY
    @Json(name = "amount")
    val amount: Double = 0.0,
    @Json(name = "paymentMethod")
    val paymentMethod: String = "Cash", // Cash, Bank, bKash, Other
    @Json(name = "note")
    val note: String = "",
    @Json(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @Json(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)

