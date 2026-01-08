package edu.b4kancs.languagePuzzleApp.app.platform

data class Environment(
    val platform: Platform,
    val browser: String?,
    val screenWidth: Int?,
    val screenHeight: Int?
) {
    val isMobile: Boolean = platform in setOf(Platform.WEB_ANDROID, Platform.WEB_IOS)
}
