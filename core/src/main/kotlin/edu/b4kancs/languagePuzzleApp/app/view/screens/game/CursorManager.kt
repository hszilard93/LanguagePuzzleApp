package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CustomCursorLoader.CustomCursor.*
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.CustomCursorLoader.loadCustomCursor

class CursorManager(private val environment: Environment) {

    companion object {
        val logger = ktx.log.logger<CursorManager>()
    }

    var currentCursor: Cursor? = null
        private set

    val handOpenCursor = if (!environment.isMobile) loadCustomCursor(OPEN_HAND_CURSOR) else null
    val handClosedCursor = if (!environment.isMobile) loadCustomCursor(CLOSED_HAND_CURSOR) else null
    val rotateLeftCursor = if (!environment.isMobile) loadCustomCursor(ROTATE_LEFT_CURSOR) else null
    val rotateRightCursor = if (!environment.isMobile) loadCustomCursor(ROTATE_RIGHT_CURSOR) else null
    val removeFeatureCursor = if (!environment.isMobile) loadCustomCursor(REMOVE_FEATURE_CURSOR) else null
    val addFeatureCursor = if (!environment.isMobile) loadCustomCursor(ADD_FEATURE_CURSOR) else null
    val editTextCursor = if (!environment.isMobile) loadCustomCursor(EDIT_TEXT_CURSOR) else null
    val handPointingCursor = if (!environment.isMobile) loadCustomCursor(POINTING_HAND_CURSOR) else null
    val gearCursor = if (!environment.isMobile) loadCustomCursor(GEAR_CURSOR) else null

    fun setCursor(cursor: Cursor?) {
        logger.debug { "setCursor cursor=$cursor" }

        if (cursor == currentCursor) return

        if (cursor == null) {
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow)
            currentCursor = null
            return
        }

        Gdx.graphics.setCursor(cursor)
        currentCursor = cursor
    }

    fun dispose() {
        handOpenCursor?.dispose()
        handClosedCursor?.dispose()
        rotateLeftCursor?.dispose()
        rotateRightCursor?.dispose()
    }
}
