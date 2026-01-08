package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.Viewport
import edu.b4kancs.languagePuzzleApp.app.Game.Companion.DEFAULT_ZOOM
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.view.util.toVector2
import edu.b4kancs.languagePuzzleApp.app.view.util.toVector3

class CameraController(
    val gameCamera: OrthographicCamera,
    private val hudCamera: OrthographicCamera,
    private val gameViewport: Viewport,
    private val hudViewport: Viewport,
    private val gameModel: GameModel
) {

    private val startZoom = DEFAULT_ZOOM

    fun setupCameras() {
        gameCamera.setToOrtho(false, Constants.GAME_VIRTUAL_WIDTH, Constants.GAME_VIRTUAL_HEIGHT)
        gameCamera.zoom = startZoom
        recenterCamera()
    }

    fun updateCameras() {
        gameCamera.update()
        hudCamera.update()
    }

    fun recenterCamera() {
//        gameModel.puzzlePieces.find { it.grammaticalRole == GrammaticalRole.VERB }?.let {
//            gameCamera.position.set(it.pos.x + it.size / 2, it.pos.y + it.size / 2 + 200f, 0f)
//            gameCamera.update()
//        }

        gameCamera.position.set(0f, 0f, 0f)
        gameCamera.update()
    }

    fun resize(newWidth: Int, newHeight: Int) {
        gameViewport.update(newWidth, newHeight, false)
        hudViewport.update(newWidth, newHeight, true)
    }

    fun unprojectGameCoords(pos: Vector2): Vector2 {
        return gameCamera.unproject(pos.toVector3()).toVector2()
    }
}
