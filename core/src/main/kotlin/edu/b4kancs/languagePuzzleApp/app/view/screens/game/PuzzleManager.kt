package edu.b4kancs.languagePuzzleApp.app.view.screens.game

import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.GrammaticalRole
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePieceFeature
import edu.b4kancs.languagePuzzleApp.app.model.PuzzleTab
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.model.Suffix
import edu.b4kancs.languagePuzzleApp.app.model.exercise.SolutionResult
import edu.b4kancs.languagePuzzleApp.app.model.exercise.TaskType
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

    private lateinit var gameInputManager: GameInputManager

    var potentialDragOrRotatePiece: PuzzlePiece? = null
    var draggedPuzzlePiece: PuzzlePiece? = null
        private set
    var puzzlePieceToRotate: PuzzlePiece? = null

    var featureTripleToAdd: Triple<PuzzlePiece, Side, PuzzlePieceFeature.Type>? = null
    var featureToRemove: Pair<PuzzlePiece, PuzzlePieceFeature>? = null

    var puzzlePieceToEdit: PuzzlePiece? = null
    var featureToTextEdit: PuzzlePieceFeature? = null
    var editingPuzzlePiece: PuzzlePiece? = null
        private set
    var editingPuzzleFeature: PuzzlePieceFeature? = null
        private set

    init {
        // Check for solution in case there are completed puzzle configurations in the initial state
        checkSolution()
    }

    fun registerGameInputManager(gameInputManager: GameInputManager) {
        this.gameInputManager = gameInputManager
    }

    fun startDragging(puzzlePiece: PuzzlePiece, toSnap: Boolean = true) {
        logger.debug { "startDragging puzzlePiece = $puzzlePiece" }
        draggedPuzzlePiece = puzzlePiece
        potentialDragOrRotatePiece = null

        puzzlePiece.depth = gameModel.puzzlePieces.maxOfOrNull { it.depth }?.plus(1) ?: 0

        if (toSnap) {
            puzzleSnapHelper.updatePuzzleFeaturesByProximity()
            draggedPuzzlePiece!!.getAllFeatures()
                .forEach { puzzleSnapHelper.updatePuzzleFeatureCompatibilityMap(it) }
        }

        uiManager.showClosedGarbageBin()
    }

    fun dragPuzzle(mousePos: Vector2, lastPos: Vector2) {
        logger.debug { "dragPuzzle mousePos = $mousePos lastPos = $lastPos" }

        val delta = mousePos.cpy().sub(lastPos)
        draggedPuzzlePiece!!.apply {
            pos = pos.add(delta)
            if (this.grammaticalRole == GrammaticalRole.VERB) {
                copyOfConnections
                    .flatMap { it.puzzlesConnected.minus(this) }
                    .forEach { otherPiece ->
                        otherPiece.pos = otherPiece.pos.add(delta)
                    }
            }
            else {
                this.copyOfConnections.forEach(::removeConnection)
            }
        }

        if (!draggedPuzzlePiece!!.isConnected && uiManager.isPuzzlePieceOverGarbageBin(draggedPuzzlePiece!!)) {
            if (!uiManager.isGarbageBinLifted) {
                uiManager.showLiftedGarbageBin()
            }
        }
        else if (uiManager.isGarbageBinLifted) {
            uiManager.showClosedGarbageBin()
        }

        puzzleSnapHelper.updatePuzzleFeaturesByProximity()
    }

    fun stopDragging() {
        logger.debug { "stopDragging" }

        if (draggedPuzzlePiece == null) {
            return
        }

        draggedPuzzlePiece!!.let {
            if (!draggedPuzzlePiece!!.isConnected && uiManager.isPuzzlePieceOverGarbageBin(it)) {
                gameModel.puzzlePieces.remove(it)
            }
        }

        draggedPuzzlePiece?.getAllFeatures()?.forEach { puzzleSnapHelper.clearPuzzleFeatureCompatibilityMap(it) }
        draggedPuzzlePiece = null
        puzzleSnapHelper.performSnapIfAny()
        puzzleSnapHelper.clearPuzzleFeaturesByProximity()

        checkSolution()

        uiManager.hideGarbageBin()
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

    fun openBaseTextEditor(puzzlePiece: PuzzlePiece) {
        logger.debug { "openTextEditor puzzlePiece = $puzzlePiece" }

        if (editingPuzzlePiece != null) return

        editingPuzzlePiece = puzzlePiece
        uiManager.displayTextEditorPopup(
            puzzlePiece.text,
            puzzlePiece.pos,
            onSave = { newText ->
                puzzlePiece.text = newText
                editingPuzzlePiece = null
                puzzlePieceToEdit = null
                checkSolution()
            },
            onCancel = {
                editingPuzzlePiece = null
                puzzlePieceToEdit = null
            }
        )
    }

    fun openTabTextEditor(puzzleFeature: PuzzleTab) {
        logger.debug { "openTextEditor puzzleFeature = $puzzleFeature" }

        if (editingPuzzleFeature != null) return

        editingPuzzleFeature = puzzleFeature
        uiManager.displayTextEditorPopup(
            puzzleFeature.text,
            puzzleFeature.getFeatureMidpoint(),
            onSave = { newText ->
                puzzleFeature.text = newText
                editingPuzzleFeature = null
                featureToTextEdit = null
                if (gameModel.currentExercise?.type == TaskType.COMPLETE_ARGUMENTS) {
                    checkSolution()
                }
            },
            onCancel = {
                editingPuzzleFeature = null
                featureToTextEdit = null
            }
        )
    }

    fun addFeature() {
        val (puzzle, side, type) = featureTripleToAdd!!

        if (type == PuzzlePieceFeature.Type.TAB) {

            if (gameModel.currentExercise?.ruleset?.canColorTabs == false) {
                finishAddFeature(puzzle, PuzzlePieceFeature.Type.TAB, side, GrammaticalRole.UNDEFINED)
                return
            }

            // Special case
            if (gameModel.currentExercise?.buttonDescription == "FB2/48–49/3.VI.2a-b") {
                val hasTabHere = puzzle.tabs.any { it.side == side }
                if (!hasTabHere) {
                    finishAddFeature(puzzle, PuzzlePieceFeature.Type.TAB, side, GrammaticalRole.UNDEFINED)
                    return
                }
            }

            uiManager.displayGrammaticalRolePopup(puzzle, side) { selectedRole ->
                val doesAllowTabText = gameModel.currentExercise?.ruleset?.doesAllowTabText ?: true

                if (selectedRole == GrammaticalRole.ADVERBIAL) {
                    if (doesAllowTabText) {
                        uiManager.displayTabTextPopups(
                            role = selectedRole,
                            puzzlePiece = puzzle,
                            side = side,
                            onEndingSelected = { ending ->
                                val indexOfSecondNewLine = ending
                                    .mapIndexed { i, c -> if (c == '\n') i else -1 }
                                    .filter { it != -1 }
                                    .getOrNull(1)
                                    ?.minus(1)
                                    ?: (ending.lastIndex + 1)
                                val trimmedText = ending
                                    .take(indexOfSecondNewLine)

                                finishAddFeature(puzzle, type, side, selectedRole, trimmedText)
                            },
                            onCancel = {

                            }
                        )
                    }
                    else {
                        finishAddFeature(puzzle, type, side, selectedRole, "")
                    }
                }
                else if (selectedRole == GrammaticalRole.OBJECT) {
                    if (doesAllowTabText) {
                        val tabText = Suffix.predefinedSuffixes
                            .firstOrNull { it.grammaticalRole == GrammaticalRole.OBJECT }
                            ?.text?.takeWhile { c -> c != '/' }
                            ?: ""
                        finishAddFeature(puzzle, type, side, selectedRole, tabText)
                    }
                    else {
                        finishAddFeature(puzzle, type, side, selectedRole, "")
                    }
                }
                else {
                    finishAddFeature(puzzle, type, side, selectedRole, "")
                }
            }
        }

        // Directly add the blank
        finishAddFeature(puzzle, type, side)
    }

    private fun finishAddFeature(
        puzzle: PuzzlePiece,
        type: PuzzlePieceFeature.Type,
        side: Side,
        selectedRole: GrammaticalRole = GrammaticalRole.UNDEFINED,
        tabText: String = ""
    ) {
        puzzle.addFeature(type, side, selectedRole, tabText)
        featureTripleToAdd = null
        if (gameModel.currentExercise?.type == TaskType.COMPLETE_ARGUMENTS) {
            checkSolution()
        }
        featureToRemove = null
    }

    fun removeFeature() {
        val (puzzle, feature) = featureToRemove!!
        puzzle.removeFeature(feature)
        featureTripleToAdd = Triple(
            puzzle,
            feature.side,
            if (feature is PuzzleTab) PuzzlePieceFeature.Type.TAB else PuzzlePieceFeature.Type.BLANK
        )
        featureToRemove = null

        if (gameModel.currentExercise?.type == TaskType.COMPLETE_ARGUMENTS) {
            checkSolution()
        }
    }

    fun addNewPuzzlePiece(mousePos: Vector2, isBlank: Boolean = false): PuzzlePiece {
        logger.info { "addNewPuzzlePiece mousePos = $mousePos \tisBlank = $isBlank" }

        val newPuzzlePiece = PuzzlePiece(
            text = "",
            grammaticalRole = if (!isBlank) GrammaticalRole.VERB else GrammaticalRole.UNDEFINED,
            depth = gameModel.puzzlePieces.maxOfOrNull { it.depth }?.plus(1) ?: 0,
            pos = mousePos.cpy()
        )

        if (!isBlank) {
//            newPuzzlePiece.addFeature(PuzzlePieceFeature.Type.TAB, Side.TOP, GrammaticalRole.SUBJECT)
        }
        else {
            newPuzzlePiece.addFeature(PuzzlePieceFeature.Type.BLANK, Side.BOTTOM)
        }

        gameModel.puzzlePieces.add(newPuzzlePiece)
        return newPuzzlePiece
    }

    fun addNewPuzzlePieceViaDrag(isBlank: Boolean = false) {
        logger.info { "addNewPuzzlePieceViaDrag isBlank = $isBlank" }

        val newPiece = addNewPuzzlePiece(gameInputManager.lastPublicMouseWorldPos, isBlank)

        startDragging(newPiece, false)
        gameInputManager.emulateDragging()
    }

    fun checkSolution() {
        val verbPuzzles = gameModel.puzzlePieces.filter { it.grammaticalRole == GrammaticalRole.VERB }

        if (verbPuzzles.size < gameModel.currentTask?.requiredSolutions ?: 1) {
            uiManager.hideCheckMark()
            return
        }

        val hasSameVerbTooManyTimes = verbPuzzles.any { thisPuzzle ->
            gameModel.currentExercise?.ruleset?.doesBaseTextCount == true &&
            verbPuzzles.count { it.text == thisPuzzle.text } > (gameModel.currentTask?.solutionConfigurations?.map { it.solutionCenterPiece.text }
                ?.count { it == thisPuzzle.text } ?: 0)
//                (verbPuzzles - thisPuzzle).any { otherPuzzle -> thisPuzzle.text == otherPuzzle.text && thisPuzzle.text.isNotEmpty() }
        }

        if (hasSameVerbTooManyTimes) {
            uiManager.hideCheckMark()
            return
        }

        val solutionResults = arrayListOf<SolutionResult>()
        verbPuzzles.forEach { verbPuzzle ->
            val solutionResult = checkSolutionsForVerb(verbPuzzle)
            solutionResults.add(solutionResult)
        }

        val aggregateSolutionState: SolutionResult =
            if (solutionResults.count { it == SolutionResult.CORRECT } >= (gameModel.currentTask?.requiredSolutions ?: 1)) {
                SolutionResult.CORRECT
            }
            else if (solutionResults.contains(SolutionResult.INELIGIBLE)) {
                SolutionResult.INELIGIBLE
            }
            else {
                SolutionResult.INCORRECT
            }

        when (aggregateSolutionState) {
            SolutionResult.CORRECT -> {
                uiManager.showCheckMark(isCorrect = true)
                gameModel.updateIsSolved(true)
            }

            SolutionResult.INCORRECT -> {
                uiManager.showCheckMark(isCorrect = false)
                gameModel.updateIsSolved(false)
            }

            SolutionResult.INELIGIBLE -> {
                uiManager.hideCheckMark()
                gameModel.updateIsSolved(false)
            }
        }
    }

    private fun checkSolutionsForVerb(centerPuzzle: PuzzlePiece): SolutionResult {
        if (centerPuzzle.connectionSize < centerPuzzle.tabs.size && gameModel.currentExercise?.type != TaskType.COMPLETE_ARGUMENTS) {
            return SolutionResult.INELIGIBLE
        }

        // All the tabs are connected, let's check the correctness of the result
        val solutionResult = gameModel.currentTask
            ?.solutionConfigurations
            ?.filter {
                val centerText = it.solutionCenterPiece.text
                if (gameModel.currentExercise?.ruleset?.doesBaseTextCount == true) centerPuzzle.text == centerText else true
            }
            ?.map {
                it.doesVerbMatchSolution(centerPuzzle, gameModel.currentExercise!!)
            }
            ?.fold(SolutionResult.INELIGIBLE) { acc, result ->
                if (result == SolutionResult.CORRECT || acc == SolutionResult.CORRECT) {
                    SolutionResult.CORRECT
                }
                else if (result == SolutionResult.INCORRECT || acc == SolutionResult.INCORRECT) {
                    SolutionResult.INCORRECT
                }
                else
                    SolutionResult.INELIGIBLE
            }
            ?: SolutionResult.INELIGIBLE

        GameModel.logger.info { "solutionResult = $solutionResult" }
        return solutionResult
    }
}
