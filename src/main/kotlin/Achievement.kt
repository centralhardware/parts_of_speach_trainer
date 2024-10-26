object Achievement {

    fun isAchievement(size: Int): Boolean {
        return when (size) {
            4,
            8,
            16,
            32,
            64,
            128 -> true
            else -> false
        }
    }
}
