package com.example.data.local

import com.google.firebase.database.IgnoreExtraProperties
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@IgnoreExtraProperties
@JsonClass(generateAdapter = true)
data class ShareholderPaymentEntity(
    @Json(name = "id")
    val id: String = "",
    @Json(name = "shareholderId")
    val shareholderId: String = "",
    @Json(name = "shareholderName")
    val shareholderName: String = "",
    @Json(name = "date")
    val date: String = "", // DD/MM/YYYY
    @Json(name = "amount")
    val amount: Double = 0.0,
    @Json(name = "paymentMethod")
    val paymentMethod: String = "Cash", // Cash, Bank, bKash, Other
    @Json(name = "note")
    val note: String = "",
    @Json(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis()
)

