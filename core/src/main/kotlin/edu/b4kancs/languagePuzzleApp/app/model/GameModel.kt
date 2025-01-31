package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Exercise
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Task
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.UIManager
import kotlinx.serialization.json.Json
import ktx.log.logger
import kotlin.math.ceil
import kotlin.math.sqrt

const val LOG_LEVEL_MISC = 4

class GameModel {

    var currentExercise: Exercise? = null
        private set
    var currentTask: Task? = null
        private set
    var currentTaskNumber: Int = 0
        private set
    var totalTaskCount: Int = 0
        private set
    var isSolved: Boolean = false
        private set

    var puzzlePieces: MutableList<PuzzlePiece> = ArrayList()
        private set

    // VALUES USED TO POSITION THE PUZZLE PIECES
    private val puzzlePieceSize = PuzzlePiece.MIN_SIZE
    private val puzzlePieceSpacingX = 175f // Horizontal spacing between pieces
    private val puzzlePieceSpacingY = 175f // Vertical spacing between pieces
    private val gridOffsetX = 0f // Padding around the grid horizontally
    private val gridOffsetY = 0f // Padding around the grid vertically

    // Currently unused, can be used to load exercises from disk.
    private val jsonSerializer = Json {
        prettyPrint = true
    }

    private lateinit var uiManager: UIManager

    companion object {
        val logger = logger<GameModel>()
    }

    init {
        logger.debug { "Initializing GameModel." }
    }

    private fun serializeExercise(exercise: Exercise): String {
        val jsonSerializer = Json {
            prettyPrint = true
        }
        val serializedExercise = jsonSerializer.encodeToString(Exercise.serializer(), exercise)
        logger.info { "serializedExercise = $serializedExercise" }

        return serializedExercise
    }

    fun loadExerciseFromDisk(fileHandle: FileHandle) {
        logger.info { "Loading exercise from file: ${fileHandle.path()}" }

        initializeExercise(deserializeExerciseFromFile(fileHandle))
    }

    fun initializeExercise(exercise: Exercise) {
        logger.info { "Initializing exercise: $exercise" }

        currentExercise = exercise
        setUpTask(currentExercise!!.tasks.first())
        currentTaskNumber = 1
        totalTaskCount = currentExercise!!.tasks.size
    }

    fun setUpTask(task: Task) {
        logger.debug { "Setting up task: ${task.taskDescription.take(12)}" }

        currentTask = task
        initializePuzzlePieces(currentTask!!.predefinedPieces)
    }

    fun setUpNextTask(onTaskChange: () -> Unit) {
        logger.info { "nextTask $currentTaskNumber/$totalTaskCount" }

        if (currentTaskNumber < totalTaskCount) {
            currentTaskNumber++
            setUpTask(currentExercise!!.tasks[currentTaskNumber - 1])
            onTaskChange()
        }
    }

    fun setUpPreviousTask(onTaskChange: () -> Unit) {
        logger.info { "previousTask $currentTaskNumber/$totalTaskCount" }

        if (currentTaskNumber > 1) {
            currentTaskNumber--
            setUpTask(currentExercise!!.tasks[currentTaskNumber - 1])
            onTaskChange()
        }
    }

    fun updateIsSolved(isSolved: Boolean) {
        this.isSolved = isSolved
    }

    private fun deserializeExerciseFromFile(jsonFile: FileHandle): Exercise {
        val jsonContents = jsonFile.readString()

        val jsonSerializer = Json {
            prettyPrint = true
        }
        val deserializedExercise = jsonSerializer.decodeFromString(Exercise.serializer(), jsonContents)
        logger.info { "deserializedExercise = $deserializedExercise" }

        return deserializedExercise
    }

    private fun repositionPuzzlePieces() {
        positionPuzzlePiecesInGrid()
    }

    private fun positionPuzzlePiecesInGrid() {
        if (puzzlePieces.isEmpty()) return

        val numPieces = puzzlePieces.size

        // Calculate grid dimensions (aim for roughly square)
        val numColumns = ceil(sqrt(numPieces.toDouble())).toInt()
        val numRows = ceil(numPieces.toDouble() / numColumns).toInt()

        // Calculate total grid width and height
        val gridWidth = (numColumns * puzzlePieceSize) + ((numColumns - 1) * puzzlePieceSpacingX)
        val gridHeight = (numRows * puzzlePieceSize) + ((numRows - 1) * puzzlePieceSpacingY)

        // Calculate starting position to center the grid
        val startX = 0 - gridWidth / 2f + gridOffsetX
        val startY = 0 - gridHeight / 2f + gridOffsetY

        var pieceIndex = 0
        for (row in 0 until numRows) {
            for (col in 0 until numColumns) {
                if (pieceIndex < numPieces) {
                    val piece = puzzlePieces[pieceIndex]
                    val x = startX + (col * (puzzlePieceSize + puzzlePieceSpacingX))
                    val y = startY + (row * (puzzlePieceSize + puzzlePieceSpacingY))
                    piece.pos = Vector2(x, y)
                    pieceIndex++
                }
                else {
                    break // No more pieces
                }
            }
        }
    }

    /**
     * Initializes the puzzlePieces list based on the predefinedPieces.
     * Positions are arranged centrally and above at regular intervals.
     */
    private fun initializePuzzlePieces(predefinedPieces: Set<PuzzlePiece>) {
        puzzlePieces.clear()
        puzzlePieces.addAll(predefinedPieces)
        repositionPuzzlePieces()
    }

    /**
     * Rebase puzzle depths to ensure they are non-negative and start from zero.
     */
    fun rebasePuzzleDepths() {
        logger.debug { "Rebasing puzzle depths." }
        if (puzzlePieces.isNotEmpty()) {
            val minDepth = puzzlePieces.minOf { it.depth }
            puzzlePieces.forEach { it.depth -= minDepth }
        }
    }
}
