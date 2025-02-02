package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Exercise
import edu.b4kancs.languagePuzzleApp.app.model.exercise.Task
import edu.b4kancs.languagePuzzleApp.app.view.screens.game.PuzzleSnapHelper
import kotlinx.serialization.json.Json
import ktx.log.logger
import kotlin.math.ceil
import kotlin.math.sqrt

class GameModel {

    val puzzleSnapHelper: PuzzleSnapHelper = PuzzleSnapHelper(this)

    var currentExercise: Exercise? = null
        private set
    var currentTask: Task? = null
        private set
    var currentTaskNumber: Int = 0
        private set
    var totalTaskCount: Int = 0
        private set
    private var isSolved: Boolean = false

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

    companion object {
        val logger = logger<GameModel>()
    }

    init {
        logger.debug { "Initializing GameModel." }
    }

    // Currently unused, needs to be revised if to be used.
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

    private fun initializeExercise(exercise: Exercise) {
        logger.info { "Initializing exercise: $exercise" }

        currentExercise = exercise
        setUpTask(currentExercise!!.tasks.first())
        currentTaskNumber = 1
        totalTaskCount = currentExercise!!.tasks.size

        logger.info { "Exercise initialized." }
    }

    private fun setUpTask(task: Task) {
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

    private fun initializePuzzlePieces(predefinedPieces: Set<PuzzlePiece>) {
        puzzlePieces.clear()
        puzzlePieces.addAll(predefinedPieces)

        for (piece in puzzlePieces) {
            for (tab in piece.tabs) {
                tab.connectedToText?.let { targetText ->
                    // Find the first puzzle piece whose text matches the target text.
                    val targetPiece = predefinedPieces
                        .firstOrNull {
                            it.text == targetText
                                && it.grammaticalRole == tab.grammaticalRole
                                && !it.isConnected  // Important to avoid connecting to already connected pieces
                                && it.blanks.firstOrNull()?.side == tab.side.opposite()
                        }
                    if (targetPiece != null) {
                        // Create a new connection linking the two pieces via this tab.
                        val connection = Connection(
                            puzzlesConnected = setOf(piece, targetPiece),
                            via = tab,
                            roleOfConnection = tab.grammaticalRole
                        )
                        piece.addConnection(connection)
                        targetPiece.addConnection(connection)
                    }
                }
            }
        }

        repositionPuzzlePieces()
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

    /**
     * Performs grid-based placement and then adjusts connected groups so they have
     * more breathing room.
     */
    private fun positionPuzzlePiecesInGrid() {
        if (puzzlePieces.isEmpty()) return

        // Identify preconnected pieces (used later for logging/snapping)
        val preConnectedPieces = mutableSetOf<PuzzlePiece>()
        val verbPiecesWithConnections = puzzlePieces.filter { it.grammaticalRole == GrammaticalRole.VERB && it.copyOfConnections.isNotEmpty() }

        // Identify all preconnected pieces and log them
        verbPiecesWithConnections.forEach { verbPiece ->
            verbPiece.copyOfConnections.forEach { connection ->
                preConnectedPieces.addAll(connection.puzzlesConnected)
            }
        }

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

                    // Log if the piece is preconnected (for debugging/awareness)
                    if (preConnectedPieces.contains(piece)) {
                        logger.debug { "Positioned preconnected piece: ${piece.text} at ($x, $y)" }
                    } else {
                        logger.debug { "Positioned piece: ${piece.text} at ($x, $y)" }
                    }

                    pieceIndex++
                } else {
                    break // No more pieces
                }
            }
        }

        if (preConnectedPieces.isEmpty()) {
            return
        }
        //
        // Let's deal with the groups
        //
        logger.debug { "Found preconnected pieces: $preConnectedPieces" }

        if (verbPiecesWithConnections.size > 1) {
            // Adjust connected groups so that their overall bounds have more breathing room.
            adjustConnectedGroupsSpacing()
            alignGroupsToGrid()
        }

        // Finally, perform forced snapping among connected pieces.
        verbPiecesWithConnections.forEach { verbPiece ->
            verbPiece.copyOfConnections.forEach { connection ->
                val piece1 = connection.puzzlesConnected.first() // Let's assume the verb piece is always first
                val piece2 = connection.puzzlesConnected.last()  // and the other is second

                val tabToSnap = connection.via
                val blankToSnapTo = piece2.blanks.firstOrNull()

                if (blankToSnapTo != null) {
                    logger.debug { "Forced snapping: Tab '${tabToSnap.text}' on '${piece1.text}' to Blank on '${piece2.text}'" }
                    puzzleSnapHelper.performForcedSnap(blankToSnapTo, tabToSnap)
                } else {
                    logger.error { "Could not find compatible blank to snap to for tab '${tabToSnap.text}' on '${piece1.text}' to '${piece2.text}'" }
                }
            }
        }
    }

    /**
     * Returns all groups of puzzle pieces. Pieces that are connected (via DFS)
     * are grouped together; free pieces (with no connections) are returned as singleton groups.
     */
    private fun getAllGroups(): List<Set<PuzzlePiece>> {
        val groups = mutableListOf<MutableSet<PuzzlePiece>>()
        val visited = mutableSetOf<PuzzlePiece>()

        fun dfs(piece: PuzzlePiece, group: MutableSet<PuzzlePiece>) {
            if (piece in visited) return
            visited.add(piece)
            group.add(piece)
            piece.copyOfConnections.forEach { connection ->
                connection.puzzlesConnected.forEach { connectedPiece ->
                    dfs(connectedPiece, group)
                }
            }
        }

        for (piece in puzzlePieces) {
            if (piece !in visited) {
                val group = mutableSetOf<PuzzlePiece>()
                dfs(piece, group)
                groups.add(group)
            }
        }
        return groups
    }

    /**
     * Groups connected pieces (using DFS over each piece’s connections) and returns
     * a list of groups that contain more than one piece.
     */
    private fun getConnectedGroups(): List<Set<PuzzlePiece>> {
        val groups = mutableListOf<MutableSet<PuzzlePiece>>()
        val visited = mutableSetOf<PuzzlePiece>()

        fun dfs(piece: PuzzlePiece, group: MutableSet<PuzzlePiece>) {
            if (piece in visited) return
            visited.add(piece)
            group.add(piece)
            piece.copyOfConnections.forEach { connection ->
                connection.puzzlesConnected.forEach { connectedPiece ->
                    dfs(connectedPiece, group)
                }
            }
        }

        for (piece in puzzlePieces) {
            if (piece !in visited) {
                val group = mutableSetOf<PuzzlePiece>()
                dfs(piece, group)
                if (group.size > 1) {  // only adjust groups with multiple pieces
                    groups.add(group)
                }
            }
        }
        return groups
    }

    /**
     * Computes the union of the bounding rectangles of all pieces in the group,
     * taking into account an optional offset and an extra margin.
     */
    private fun computeGroupBoundingRectangle(group: Set<PuzzlePiece>, offset: Vector2 = Vector2.Zero): Rectangle {
        val margin = 20f  // extra space around the group
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (piece in group) {
            // Retrieve the puzzle piece's bounding rectangle and apply the current offset.
            val rect = piece.getBoundingRectangle().also {
                it.x += offset.x
                it.y += offset.y
            }
            minX = minOf(minX, rect.x)
            minY = minOf(minY, rect.y)
            maxX = maxOf(maxX, rect.x + rect.width)
            maxY = maxOf(maxY, rect.y + rect.height)
        }

        // Create a rectangle that encloses all pieces, with extra margin.
        return Rectangle(minX - margin, minY - margin, (maxX - minX) + 2 * margin, (maxY - minY) + 2 * margin)
    }

    /**
     * After the grid placement, adjust the positions of connected groups so that
     * they do not overlap. Each group is shifted as a whole.
     */
    private fun adjustConnectedGroupsSpacing() {
        val groups = getConnectedGroups()
        if (groups.isEmpty()) return

        // Maintain an offset for each group.
        val groupOffsets = mutableMapOf<Set<PuzzlePiece>, Vector2>()
        groups.forEach { group -> groupOffsets[group] = Vector2(0f, 0f) }

        // Use a smaller separation factor.
        val separationFactor = 5f
        // Fraction of the computed push to apply per iteration.
        val pushFraction = 0.5f
        var changed: Boolean
        var iteration = 0
        // Iteratively adjust group offsets until no overlaps are found or an iteration limit is reached.
        do {
            changed = false
            for (i in groups.indices) {
                for (j in i + 1 until groups.size) {
                    val groupA = groups[i]
                    val groupB = groups[j]
                    val offsetA = groupOffsets[groupA]!!
                    val offsetB = groupOffsets[groupB]!!

                    val rectA = computeGroupBoundingRectangle(groupA, offsetA)
                    val rectB = computeGroupBoundingRectangle(groupB, offsetB)

                    if (rectA.overlaps(rectB)) {
                        // Calculate the vector from B's center to A's center.
                        val centerA = Vector2(rectA.x + rectA.width / 2f, rectA.y + rectA.height / 2f)
                        val centerB = Vector2(rectB.x + rectB.width / 2f, rectB.y + rectB.height / 2f)
                        val diff = centerA.cpy().sub(centerB)
                        if (diff.isZero) {
                            diff.set(separationFactor, separationFactor) // arbitrary direction if centers are equal
                        }
                        // Normalize and then scale down the push.
                        diff.nor().scl(separationFactor)
                        val push = diff.cpy().scl(pushFraction)

                        // Apply half the push to each group.
                        offsetA.add(push)
                        offsetB.sub(push)
                        changed = true
                    }
                }
            }
            iteration++
        } while (changed && iteration < 100)

        // Apply the final offset for each group to each puzzle piece.
        groups.forEach { group ->
            val offset = groupOffsets[group]!!
            group.forEach { piece ->
                piece.pos = piece.pos.add(offset)
                logger.debug { "Adjusted piece: ${piece.text} to new pos (${piece.pos.x}, ${piece.pos.y})" }
            }
        }
    }

    /**
     * After separating overlapping groups, align each connected group
     * to a grid layout so that they are more uniformly distributed.
     */
    private fun alignGroupsToGrid() {
        val groups = getConnectedGroups()
        if (groups.isEmpty()) return

        // For each group, compute its current center.
        // (At this point the group positions already include any offsets applied by adjustConnectedGroupsSpacing.)
        val groupRectangles = groups.associateWith { group ->
            computeGroupBoundingRectangle(group, Vector2.Zero)
        }

        // Determine grid dimensions based on the number of groups.
        val numColumns = groups.size / 2
        val numRows = groups.size / 2

        // Determine the maximum group sizes (width and height) to define grid cell spacing.
        val maxGroupWidth = 900f
        val maxGroupHeight = 900f

        // Set a margin between groups.
        val extraSpacing = 50f
        val cellSpacingX = maxGroupWidth + extraSpacing
        val cellSpacingY = maxGroupHeight + extraSpacing

        // Compute overall grid dimensions.
        val gridWidth = numColumns * cellSpacingX
        val gridHeight = numRows * cellSpacingY

        // Calculate the starting position so that the grid is centered at (0,0).
        // Here, the center of the first cell is at (startX, startY).
        val startX = -gridWidth / 2f + cellSpacingX / 2f
        val startY = -gridHeight / 2f + cellSpacingY / 2f

        // For each group, assign a grid cell based on its index and compute the delta
        // needed to move its current center to the target center.
        groups.forEachIndexed { index, group ->
            val col = index % numColumns
            val row = index / numColumns

            val targetCenter = Vector2(startX + col * cellSpacingX, startY + row * cellSpacingY)
            val currentCenter = Vector2()
            group.first { it.grammaticalRole == GrammaticalRole.VERB }.getBoundingRectangle().getCenter(currentCenter)

            // Compute the delta to move the group so that its center is at the target.
            val delta = targetCenter.cpy().sub(currentCenter)

            // Apply this delta to every puzzle piece in the group.
            group.forEach { piece ->
                piece.pos = piece.pos.add(delta)
            }
            logger.debug { "Aligned group $index (grid cell: [$col, $row]) with delta: $delta" }
        }
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
