package com.indusy55.mimottsapp.data.models

data class VoiceAsset(
    val id: String,
    val name: String,
    val type: AssetType,
    val data: String,
    val mimeType: String? = null
)

enum class AssetType {
    DESIGN, CLONE
}
