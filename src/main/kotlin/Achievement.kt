object Achievement {

    fun isAchievement(size: Int): Boolean {
        return when(size) {
            3,5,8,11,16,22, 29,37,36 -> true
            else -> false
        }
    }

}