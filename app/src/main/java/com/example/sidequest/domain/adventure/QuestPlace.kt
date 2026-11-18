package com.example.sidequest.domain.adventure

data class QuestPlace(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val sourceUrl: String,
) {
    companion object {
        const val SETTLEMENT_AREA_CATEGORY = "Settlement area"
    }
}

val QuestPlace.isSettlementArea: Boolean
    get() = category == QuestPlace.SETTLEMENT_AREA_CATEGORY
