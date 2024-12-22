package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import edu.b4kancs.languagePuzzleApp.app.GameViewport
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawer
import ktx.collections.GdxMap
import ktx.collections.set
import ktx.graphics.use

class PuzzleRenderer(
    private val batch: Batch,
    private val gameCamera: OrthographicCamera,
    private val gameViewport: GameViewport,
    private val gameModel: GameModel,
    private val puzzlePieceDrawer: PuzzlePieceDrawer
) {
    private val puzzlePieceFrameBufferMap: GdxMap<PuzzlePiece, FrameBuffer> = GdxMap()
    private val frameBufferCamera: OrthographicCamera = OrthographicCamera()
    private val puzzlePieceTextureMap: GdxMap<PuzzlePiece, Texture> = GdxMap()

    fun render(delta: Float) {
        batch.projectionMatrix = gameCamera.combined
        updatePuzzlePieceAnimations(delta)
        renderGameWorld()
    }

    private fun updatePuzzlePieceAnimations(delta: Float) {
        for (puzzlePiece in gameModel.puzzlePieces) {
            if (puzzlePiece.size != puzzlePiece.targetSize) {
                puzzlePiece.animateSize(delta)
            }
        }
    }

    private fun renderGameWorld() {
        val puzzlesByLayers = gameModel.puzzlePieces.groupBy { it.depth }
        val sortedLayers = puzzlesByLayers.keys.sorted()
        for (layer in sortedLayers) {
            puzzlesByLayers[layer]?.forEach { puzzlePiece ->
                if (isPuzzleVisible(puzzlePiece)) {
                    val texture = getTextureByPuzzlePiece(puzzlePiece)
                    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
                    texture.bind()
                    batch.use {
                        batch.draw(
                            texture,
                            puzzlePiece.boundingBoxPos.x,
                            puzzlePiece.boundingBoxPos.y,
                            puzzlePiece.boundingBoxSize,
                            puzzlePiece.boundingBoxSize
                        )
                    }
                }
            }
        }
    }

    private fun isPuzzleVisible(puzzle: PuzzlePiece): Boolean {
        return gameCamera.frustum.boundsInFrustum(
            puzzle.boundingBoxPos.x,
            puzzle.boundingBoxPos.y,
            0f,
            puzzle.boundingBoxSize,
            puzzle.boundingBoxSize,
            1f
        )
    }

    private fun getTextureByPuzzlePiece(puzzlePiece: PuzzlePiece): Texture {
        if (!puzzlePiece.hasChangedAppearance) {
            puzzlePieceTextureMap[puzzlePiece]?.let { return it }
        }
        puzzlePiece.hasChangedAppearance = true
        val newTexture = renderPuzzle(puzzlePiece)
        puzzlePieceTextureMap[puzzlePiece] = newTexture
        return newTexture
    }

    private fun renderPuzzle(puzzlePiece: PuzzlePiece): Texture {
        val frameBuffer = getFrameBufferByPuzzlePiece(puzzlePiece)
        frameBuffer.use {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            frameBufferCamera.setToOrtho(false, puzzlePiece.boundingBoxSize, puzzlePiece.boundingBoxSize)
            frameBufferCamera.update()
            batch.projectionMatrix = frameBufferCamera.combined
            puzzlePieceDrawer.render(puzzlePiece)
        }
        batch.projectionMatrix = gameCamera.combined
        return frameBuffer.colorBufferTexture
    }

    private fun getFrameBufferByPuzzlePiece(puzzle: PuzzlePiece): FrameBuffer {
        return if (puzzlePieceFrameBufferMap.containsKey(puzzle) && !puzzle.hasChangedAppearance) {
            puzzlePieceFrameBufferMap[puzzle]
        }
        else {
            puzzlePieceFrameBufferMap[puzzle]?.dispose()
            val newFrameBuffer = FrameBuffer(
                Pixmap.Format.RGBA8888,
                puzzle.boundingBoxSize.toInt(),
                puzzle.boundingBoxSize.toInt(),
                false
            )
            puzzlePieceFrameBufferMap.put(puzzle, newFrameBuffer)
            puzzle.hasChangedAppearance = false
            newFrameBuffer
        }
    }

    fun dispose() {
        puzzlePieceFrameBufferMap.forEach { it.value.dispose() }
        puzzlePieceTextureMap.forEach { it.value.dispose() }
    }
}
