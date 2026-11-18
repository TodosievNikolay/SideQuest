package com.example.sidequest.domain.adventure

data class PlayerProgress(val totalXp: Long) {
    val level: Long
        get() = 1 + totalXp / XP_PER_LEVEL

    val xpToNextLevel: Int
        get() = XP_PER_LEVEL - (totalXp % XP_PER_LEVEL).toInt()

    companion object {
        const val XP_PER_CHAPTER = 50
        const val XP_PER_LEVEL = 100

        fun fromCompletedChapters(completedChapters: Long): PlayerProgress =
            PlayerProgress(completedChapters * XP_PER_CHAPTER)
    }
}
