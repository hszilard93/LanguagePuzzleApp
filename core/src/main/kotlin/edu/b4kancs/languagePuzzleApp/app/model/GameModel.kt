package edu.b4kancs.languagePuzzleApp.app.model

const val LOG_LEVEL_MISC = 4

class GameModel(
    val environment: Environment = Environment(Platform.DESKTOP, null, null, null)
) {
    companion object {
        const val LOG_LEVEL = com.badlogic.gdx.utils.Logger.DEBUG
//        const val LOG_LEVEL = LOG_LEVEL_MISC
    }
    var isDebugModeOn = false
}
