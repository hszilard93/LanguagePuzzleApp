package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Exercise
import edu.b4kancs.languagePuzzleApp.app.model.exercise.SolutionResult
import edu.b4kancs.languagePuzzleApp.app.model.exercise.SolutionConfiguration
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Task
import edu.b4kancs.languagePuzzleApp.app.model.exercise.TaskType
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.UIManager
import kotlinx.serialization.json.Json
import ktx.log.logger

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


    private val basePosition = Vector2(-135f, -400f)
    private var lastPuzzlePosition = basePosition

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
        puzzlePieces.forEach { it.pos = calculateNextPuzzlePosition(it.grammaticalRole) }
    }

    private fun calculateNextPuzzlePosition(role: GrammaticalRole): Vector2 {
        if (role == GrammaticalRole.VERB) {
            lastPuzzlePosition = basePosition
            return basePosition
        }

        var nextX: Float = if (lastPuzzlePosition == basePosition) {
            (puzzlePieces.size - 1) * 400f / 2 * -1
        }
        else {
            lastPuzzlePosition.x + 450f
        }
        var nextY = basePosition.y + 450f

        lastPuzzlePosition = Vector2(nextX, nextY)
        return lastPuzzlePosition
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
