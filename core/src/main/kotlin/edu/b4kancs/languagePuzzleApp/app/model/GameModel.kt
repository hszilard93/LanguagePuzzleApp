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
        logger.debug { "Initializing exercise: $exercise" }

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

    private fun loadExampleExercise1(): Exercise {
        // Create the PuzzlePieces and store them in variables
        val verbPuzzle = createExampleVerbPuzzle()
        val subjectPuzzle = createExampleSubjectPuzzle()
        val objectPuzzle = createExampleObjectPuzzle()
        val adverbial1Puzzle = createExampleAdverbial1Puzzle()
        val adverbial2Puzzle = createExampleAdverbial2Puzzle()

        // Define the solutionConfiguration by specifying the correct connections
        val solutionSet = setOf(
            // Connection between verbPuzzle and subjectPuzzle via the LEFT tab of verbPuzzle
            Connection(
                puzzlesConnected = setOf(verbPuzzle, subjectPuzzle),
                via = verbPuzzle.tabs.first { it.side == Side.LEFT },
                roleOfConnection = GrammaticalRole.SUBJECT
            ),
            // Connection between verbPuzzle and objectPuzzle via the TOP tab of verbPuzzle
            Connection(
                puzzlesConnected = setOf(verbPuzzle, objectPuzzle),
                via = verbPuzzle.tabs.first { it.side == Side.TOP },
                roleOfConnection = GrammaticalRole.OBJECT
            ),
            // Connection between verbPuzzle and adverbial2Puzzle via the BOTTOM tab of verbPuzzle
            Connection(
                puzzlesConnected = setOf(verbPuzzle, adverbial2Puzzle),
                via = verbPuzzle.tabs.first { it.side == Side.BOTTOM },
                roleOfConnection = GrammaticalRole.ADVERBIAL
            ),
            // Connection between verbPuzzle and adverbial1Puzzle via the RIGHT tab of verbPuzzle
            Connection(
                puzzlesConnected = setOf(verbPuzzle, adverbial1Puzzle),
                via = verbPuzzle.tabs.first { it.side == Side.RIGHT },
                roleOfConnection = GrammaticalRole.ADVERBIAL
            )
        )

        // Define the Exercise instance
        return Exercise(
            type = TaskType.PLACE_PUZZLES_IN_ORDER,
            buttonDescription = "Példafeladat",
            tasks = listOf(
                Task(
                    taskDescription = "Helyezd el a puzzle darabokat úgy, hogy a következő mondatot alkossák:\n\"Peti virágot ad Annának névnapjára.\"",
                    predefinedPieces = setOf(
                        verbPuzzle,
                        subjectPuzzle,
                        objectPuzzle,
                        adverbial1Puzzle,
                        adverbial2Puzzle
                    ),
                    solutionConfiguration = SolutionConfiguration(solutionSet)
                )
            )
        )
    }

    /**
     * Creates the central VERB puzzle piece with its tabs.
     */
    private fun createExampleVerbPuzzle(): PuzzlePiece {
        return PuzzlePiece(
            text = "ad",
            grammaticalRole = GrammaticalRole.VERB,
            depth = 1
        ).apply {
            // Add tabs: Subject, Object, and two Adverbial tabs
            tabs.addAll(
                listOf(
                    PuzzleTab(this, Side.LEFT, GrammaticalRole.SUBJECT),
                    PuzzleTab(this, Side.TOP, GrammaticalRole.OBJECT, text = "-t"),
                    PuzzleTab(this, Side.RIGHT, GrammaticalRole.ADVERBIAL, text = "-ra\n/re"),
                    PuzzleTab(this, Side.BOTTOM, GrammaticalRole.ADVERBIAL, text = "-nak\n/nek")
                )
            )
        }
    }

    /**
     * Creates the SUBJECT puzzle piece with a blank on the bottom.
     */
    private fun createExampleSubjectPuzzle(): PuzzlePiece {
        return PuzzlePiece(
            text = "Peti",
            grammaticalRole = GrammaticalRole.UNDEFINED
        ).apply {
            // Add a blank on the bottom
            blanks.add(PuzzleBlank(this, Side.BOTTOM))
        }
    }

    /**
     * Creates the OBJECT puzzle piece with a blank on the bottom.
     */
    private fun createExampleObjectPuzzle(): PuzzlePiece {
        return PuzzlePiece(
            text = "virág",
            grammaticalRole = GrammaticalRole.UNDEFINED
        ).apply {
            // Add a blank on the bottom
            blanks.add(PuzzleBlank(this, Side.BOTTOM))
//            blanks.add(PuzzleBlank(this, Side.TOP))
//            blanks.add(PuzzleBlank(this, Side.LEFT))
//            blanks.add(PuzzleBlank(this, Side.RIGHT))
        }
    }

    /**
     * Creates the first ADVERBIAL puzzle piece with a blank on the bottom.
     */
    private fun createExampleAdverbial1Puzzle(): PuzzlePiece {
        return PuzzlePiece(
            text = "névnap",
            grammaticalRole = GrammaticalRole.UNDEFINED
        ).apply {
            // Add a blank on the bottom
            blanks.add(PuzzleBlank(this, Side.BOTTOM))
        }
    }

    /**
     * Creates the second ADVERBIAL puzzle piece with a blank on the bottom.
     */
    private fun createExampleAdverbial2Puzzle(): PuzzlePiece {
        return PuzzlePiece(
            text = "Anna",
            grammaticalRole = GrammaticalRole.UNDEFINED
        ).apply {
            // Add a blank on the bottom
            blanks.add(PuzzleBlank(this, Side.BOTTOM))
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
