package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePieceFeature
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.model.Side
import ktx.log.logger

enum class RotationDirection {
    LEFT, RIGHT
}

class PuzzleManager(
    private val gameModel: GameModel,
    private val puzzleSnapHelper: PuzzleSnapHelper,
    private val uiManager: UIManager
) {

    companion object {
        val logger = logger<PuzzleManager>()
    }

    var draggedPuzzlePiece: PuzzlePiece? = null
        private set
    var puzzlePieceToRotate: PuzzlePiece? = null

    var featureTripleToAdd: Triple<PuzzlePiece, Side, PuzzlePieceFeature.Type>? = null
    var featureToRemove: Pair<PuzzlePiece,PuzzlePieceFeature>? = null

    private var editingPuzzlePiece: PuzzlePiece? = null
        private set

    fun startDragging(puzzlePiece: PuzzlePiece, mousePos: Vector2) {
        logger.debug { "startDragging puzzlePiece = $puzzlePiece mousePos = $mousePos" }
        draggedPuzzlePiece = puzzlePiece

        puzzlePiece.depth = gameModel.puzzlePieces.maxOfOrNull { it.depth }?.plus(1) ?: 0

        puzzleSnapHelper.updatePuzzleFeaturesByProximity()
        draggedPuzzlePiece!!.getAllFeatures()
            .forEach { puzzleSnapHelper.updatePuzzleFeatureCompatibilityMap(it) }
    }

    fun dragPuzzle(mousePos: Vector2, lastPos: Vector2) {
        logger.debug { "dragPuzzle mousePos = $mousePos lastPos = $lastPos" }

        val delta = mousePos.cpy().sub(lastPos)
        draggedPuzzlePiece!!.apply {
            pos = pos.add(delta)
        }
        puzzleSnapHelper.updatePuzzleFeaturesByProximity()
    }

    fun stopDragging() {
        logger.debug { "stopDragging" }

        draggedPuzzlePiece?.getAllFeatures()?.forEach { puzzleSnapHelper.clearPuzzleFeatureCompatibilityMap(it) }
        draggedPuzzlePiece = null
        puzzleSnapHelper.performSnapIfAny()
        puzzleSnapHelper.clearPuzzleFeaturesByProximity()
    }

    fun rotatePuzzlePiece(direction: RotationDirection) {
        logger.debug { "rotatePuzzlePiece direction = $direction" }

        puzzlePieceToRotate?.let {
            when (direction) {
                RotationDirection.LEFT -> it.rotateLeft()
                RotationDirection.RIGHT -> it.rotateRight()
            }
        }
    }

    fun openTextEditor(puzzlePiece: PuzzlePiece) {
        logger.debug { "openTextEditor puzzlePiece = $puzzlePiece" }

        if (editingPuzzlePiece != null) return

        editingPuzzlePiece = puzzlePiece
        uiManager.displayTextEditorPopup(
            puzzlePiece,
            onSave = { newText ->
                puzzlePiece.text = newText
                editingPuzzlePiece = null
            },
            onCancel = { editingPuzzlePiece = null }
        )
    }

    fun addFeature() {
        val (puzzle, side, type) = featureTripleToAdd!!

        if (type == PuzzlePieceFeature.Type.TAB) {
            uiManager.displayGrammaticalRolePopup(puzzle, side) { selectedRole ->
                // Add the tab with the selected role
                val addedFeature = puzzle.addFeature(type, side, selectedRole) // Modify addFeature to accept role
                featureToRemove = puzzle to addedFeature
                featureTripleToAdd = null
            }
        } else {
            // Directly add the blank
            val addedFeature = puzzle.addFeature(type, side) // Assuming blanks don't need role selection
            featureToRemove = puzzle to addedFeature
            featureTripleToAdd = null
        }
    }

    fun removeFeature() {
        val (puzzle, feature) = featureToRemove!!
        puzzle.removeFeature(feature)
        featureTripleToAdd = Triple(puzzle, feature.side, if (feature is PuzzleTab) PuzzlePieceFeature.Type.TAB else PuzzlePieceFeature.Type.BLANK)
        featureToRemove = null
    }
}
