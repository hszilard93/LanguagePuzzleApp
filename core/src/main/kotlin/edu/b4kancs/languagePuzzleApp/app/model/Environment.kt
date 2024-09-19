package edu.b4kancs.languagePuzzleApp.app.model


enum class Platform() {
    DESKTOP,
    WEB,
    WEB_ANDROID,
    WEB_IOS,
    WEB_IPAD,
    UNKNOWN
}

data class Environment(
    val platform: Platform,
    val browser: String?,
    val screenWidth: Int?,
    val screenHeight: Int?
) {
    fun isMobile(): Boolean = platform in setOf(Platform.WEB_ANDROID, Platform.WEB_IOS)
}
