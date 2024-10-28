package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Cursor
import com.badlogic.gdx.graphics.Pixmap

object CustomCursorLoader {
    enum class CustomCursor(val path: String) {
//        ARROW_CURSOR("cursors/pointer_arrow_large_1.png"),
        OPEN_HAND_CURSOR("cursors/pointer_grab_large.png"),
        CLOSED_HAND_CURSOR("cursors/pointer_grabbing_large.png"),
        ROTATE_LEFT_CURSOR("cursors/pointer_rotate-left.png"),
        ROTATE_RIGHT_CURSOR("cursors/pointer_rotate-right.png")
    }

    private val logger = ktx.log.logger<CustomCursorLoader>()

    fun loadCustomCursor(cursorEnum: CustomCursor): Cursor? {
        logger.debug { "loadCustomCursor cursorEnum=$cursorEnum" }

        try {
            logger.debug { "loadCustomCursor cursorEnum=$cursorEnum" }
            val defaultPixmap = Pixmap(Gdx.files.internal(cursorEnum.path))
            val size = 32
            val resizedPixmap = Pixmap(size, size, defaultPixmap.format)
            resizedPixmap.drawPixmap(
                defaultPixmap,
                0, 0, defaultPixmap.width, defaultPixmap.height,
                0, 0, resizedPixmap.width, resizedPixmap.height
            )
            val cursor = Gdx.graphics.newCursor(resizedPixmap, size/2, size/2)
            defaultPixmap.dispose()
            resizedPixmap.dispose()
            return cursor
        } catch (e: Exception) {
            logger.error { "loadCustomCursor error! cursorEnum=$cursorEnum message=\n${e.message}" }
            return null
        }
    }
}
