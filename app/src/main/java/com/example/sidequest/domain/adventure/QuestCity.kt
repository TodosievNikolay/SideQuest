package com.example.sidequest.domain.adventure

data class QuestCity(
    val id: String,
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val sourceUrl: String,
) {
    val displayName: String
        get() = listOf(name, region, country)
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .joinToString(", ")
}
