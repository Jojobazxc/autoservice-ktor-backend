package com.example.features.parts

import kotlinx.serialization.Serializable

@Serializable
data class PartResponse(
    val id: Long,
    val name: String,
    val article: String,
    val price: String,
    val unit: String,
    val stockQuantity: Int
)

@Serializable
data class CreatePartRequest(
    val name: String,
    val article: String,
    val price: String,
    val unit: String = "pcs",
    val stockQuantity: Int = 0
)

@Serializable
data class UpdatePartRequest(
    val name: String,
    val article: String,
    val price: String,
    val unit: String,
    val stockQuantity: Int
)