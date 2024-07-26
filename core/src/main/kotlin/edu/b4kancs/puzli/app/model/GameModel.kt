package edu.b4kancs.puzli.app.model

import com.badlogic.gdx.Gdx
import ktx.log.Logger

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
