package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Exercise
import edu.b4kancs.languagePuzzleApp.app.model.exercise.TaskType
import ktx.log.logger

const val LOG_LEVEL_MISC = 4

class GameModel {

    val puzzlePieces: MutableList<PuzzlePiece> = ArrayList()

    //    val basePosition = Vector2(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f)
    private val basePosition = Vector2(0f, -100f)
    private var lastPuzzlePosition = basePosition
    var currentExercise: Exercise

    companion object {
        val logger = logger<GameModel>()
    }

    init {
        logger.debug { "Initializing GameModel with example exercise." }

        currentExercise = loadExampleExercise1()

        // Initialize puzzlePieces based on the predefinedPieces of the exercise
        initializePuzzlePieces(currentExercise.predefinedPieces)
    }

    fun isSolved(): Boolean {
        // Collect all unique current connections in the game
        val currentConnections = puzzlePieces.flatMap { it.copyOfConnections }.toSet()

        val isSolved = ConnectionUtils.areSetsOfConnectionsLogicallyEqual(currentConnections, currentExercise.solutionConfiguration)
        logger.info { "isSolved = $isSolved" }

        return isSolved
    }

    private fun calculateNextPuzzlePosition(): Vector2 {
        var nextX = if (lastPuzzlePosition == basePosition) 2 * 340f * -1 else lastPuzzlePosition.x + 450f
        var nextY = 350f

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
        val solutionConfiguration = setOf(
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
            taskDescription = "Helyezd el a puzzle darabokat úgy, hogy a következő mondatot alkossák:\n\"Peti virágot ad Annának névnapjára.\"",
            predefinedPieces = setOf(
                verbPuzzle,
                subjectPuzzle,
                objectPuzzle,
                adverbial1Puzzle,
                adverbial2Puzzle
            ),
            solutionConfiguration = solutionConfiguration
        )
    }

    /**
     * Creates the central VERB puzzle piece with its tabs.
     */
    private fun createExampleVerbPuzzle(): PuzzlePiece {
        return PuzzlePiece(
            text = "ad",
            grammaticalRole = GrammaticalRole.VERB,
            pos = Vector2(basePosition.x, basePosition.y),
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
            grammaticalRole = GrammaticalRole.UNDEFINED,
            pos = calculateNextPuzzlePosition()
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
            grammaticalRole = GrammaticalRole.UNDEFINED,
            pos = calculateNextPuzzlePosition()
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
            grammaticalRole = GrammaticalRole.UNDEFINED,
            pos = calculateNextPuzzlePosition()
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
            grammaticalRole = GrammaticalRole.UNDEFINED,
            pos = calculateNextPuzzlePosition()
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
