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
     * Performs layout in two phases: first preconnected groups, then individual pieces.
     */
    private fun positionPuzzlePiecesInGrid() {
        if (puzzlePieces.isEmpty()) return

        // 1. Handle Preconnected Groups
        val connectedGroups = getConnectedGroups() // Assuming getConnectedGroups() correctly identifies groups

        val connectedGroupsLayoutRect: Rectangle? =
            if (connectedGroups.isNotEmpty()) { // Changed variable type to Rectangle?
                alignGroupsToGrid(connectedGroups)
            } else {
                null
            }

        // 2. Handle Individual Pieces
        val individualPieces = puzzlePieces.filter { piece ->
            connectedGroups.none { group -> group.contains(piece) }
        }

        if (individualPieces.isNotEmpty()) {
            positionIndividualPieces(individualPieces, connectedGroupsLayoutRect) // Pass Rectangle?
        }

        // 3. Perform forced snapping among connected pieces (as before)
        val verbPiecesWithConnections = puzzlePieces.filter { it.grammaticalRole == GrammaticalRole.VERB && it.copyOfConnections.isNotEmpty() }
        verbPiecesWithConnections.forEach { verbPiece ->
            verbPiece.copyOfConnections.forEach { connection ->
                val piece1 = connection.puzzlesConnected.first()
                val piece2 = connection.puzzlesConnected.last()

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
     * After separating overlapping groups, align each connected group
     * to a grid layout so that they are more uniformly distributed.
     * @param groups The list of connected groups to align.
     * @return Rectangle? Bounding rectangle of the laid out groups' area, or null if no groups.
     */
    private fun alignGroupsToGrid(groups: List<Set<PuzzlePiece>>): Rectangle? {
        if (groups.isEmpty()) return null

        // Determine grid dimensions based on the number of groups.
        val numColumns = ceil(sqrt(groups.size.toFloat())).toInt()
        val numRows = if (groups.size > numColumns * numColumns) numColumns + 1 else numColumns

        val maxGroupWidth = 900f
        val maxGroupHeight = 900f

        val extraSpacing = 50f
        val cellSpacingX = maxGroupWidth + extraSpacing
        val cellSpacingY = maxGroupHeight + extraSpacing

        val gridWidth = numColumns * cellSpacingX
        val gridHeight = numRows * cellSpacingY

        // Calculate the starting position so that the grid is centered at (0,0).
        val startX = -gridWidth / 2f + cellSpacingX / 2f
        val startY = -gridHeight / 2f + cellSpacingY / 2f

        val groupRectangles = mutableListOf<Rectangle>() // To store rectangles of each group after positioning

        val colWidths = HashMap<Int, Float>()
        val rowHeights = HashMap<Int, Float>()
        val colCenterX = HashMap<Int, Float>()
        val rowCenterY = HashMap<Int, Float>()
        for (i in 0 until numColumns) {
            for (j in 0 until numRows) {
                val width = colWidths.getOrPut(i) {
                    val colGroups = groups.indices.filter { it % numColumns == i }.map { groups[it] }
                    colGroups.maxOf { g ->
                        val numSidePieces =
                            g.first { it.grammaticalRole == GrammaticalRole.VERB }.tabs.count { it.side == Side.LEFT || it.side == Side.RIGHT }
                        (numSidePieces + 1) * PuzzlePiece.MIN_SIZE
                    } + extraSpacing
                }
                val height = rowHeights.getOrPut(j) {
                    val rowGroups = groups.indices.filter { it % numColumns == j }.map { groups[it] }
                    rowGroups.maxOf { g ->
                        val numVertPieces =
                            g.first { it.grammaticalRole == GrammaticalRole.VERB }.tabs.count { it.side == Side.TOP || it.side == Side.BOTTOM }
                        (numVertPieces + 1) * PuzzlePiece.MIN_SIZE
                    } + extraSpacing
                }

                val cellCenterX = colCenterX.getOrPut(i) {
                    // Get sum of width of previous cols
                    var sum = 0f
                    for (k in 0 until i) {
                        sum += colWidths[k]!!
                    }
                    sum + width / 2
                } + startX
                val cellCenterY = rowCenterY.getOrPut(j) {
                    // Get sum of height of previous rows
                    var sum = 0f
                    for (k in 0 until j) {
                        sum += rowHeights[k]!!
                    }
                    sum + height / 2
                } + startY

                val thisGroup = groups.getOrNull(numColumns * j + i)    // It is not guaranteed that every row of the grid has a group

                if (thisGroup != null) {
                    val targetCenter = Vector2(cellCenterX - width / 2, cellCenterY - height / 2)
                    val currentCenter = Vector2()
                    thisGroup.first { it.grammaticalRole == GrammaticalRole.VERB }.getBoundingRectangle().getCenter(currentCenter)
                    val delta = targetCenter.cpy().sub(currentCenter)
                    thisGroup.forEach { piece ->
                        piece.pos = piece.pos.add(delta)
                        val boundingRectangle = piece.getBoundingRectangle()
                        if (piece.grammaticalRole == GrammaticalRole.VERB) {
                            if (piece.tabs.count { it.side == Side.LEFT } != 0) {
                                boundingRectangle.x -= PuzzlePiece.MIN_SIZE
                                boundingRectangle.width += PuzzlePiece.MIN_SIZE
                            }
                            if (piece.tabs.count { it.side == Side.RIGHT } != 0) {
                                boundingRectangle.width += PuzzlePiece.MIN_SIZE
                            }
                            if (piece.tabs.count { it.side == Side.TOP } != 0) {
                                boundingRectangle.height += PuzzlePiece.MIN_SIZE
                            }
                            if (piece.tabs.count { it.side == Side.BOTTOM } != 0) {
                                boundingRectangle.y -= PuzzlePiece.MIN_SIZE
                                boundingRectangle.height += PuzzlePiece.MIN_SIZE
                            }
                        }
                        groupRectangles.add(boundingRectangle)
                    }
                }
            }
        }

        if (groupRectangles.isEmpty()) return null // No groups laid out, return null

        // Calculate overall bounding rectangle of all group rectangles
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (rect in groupRectangles) {
            minX = minOf(minX, rect.x)
            minY = minOf(minY, rect.y)
            maxX = maxOf(maxX, rect.x + rect.width)
            maxY = maxOf(maxY, rect.y + rect.height)
        }

        return Rectangle(minX, minY, maxX - minX, maxY - minY) // Return the bounding Rectangle
    }

    /**
     * Positions individual puzzle pieces around the connected groups layout.
     * First, a column to the right, then rows above, alternating columns and rows.
     * @param individualPieces List of puzzle pieces that are not part of any group.
     * @param connectedGroupsLayoutRect Bounding rectangle of the area used by connected groups, or null if no groups.
     */
    private fun positionIndividualPieces(individualPieces: List<PuzzlePiece>, connectedGroupsLayoutRect: Rectangle?) {
        if (individualPieces.isEmpty()) return

        val numPieces = individualPieces.size
        val numColumns = ceil(sqrt(numPieces.toDouble())).toInt() + 1
        val numRows = ceil(numPieces.toDouble() / numColumns).toInt() - 1

        val pieceWidth = puzzlePieceSize + puzzlePieceSpacingX
        val pieceHeight = puzzlePieceSize + puzzlePieceSpacingY

        val individualGridWidth = (numColumns * puzzlePieceSize) + ((numColumns - 1) * puzzlePieceSpacingX)
        val individualGridHeight = (numRows * puzzlePieceSize) + ((numRows - 1) * puzzlePieceSpacingY)

        var currentPieceIndex = 0

        if (connectedGroupsLayoutRect != null) {
            // --- 1. Column to the Right ---
            var startXColumn = connectedGroupsLayoutRect.x + connectedGroupsLayoutRect.width + puzzlePieceSpacingX
            var startYColumn = connectedGroupsLayoutRect.y // Align bottom edge

            for (row in 0 until numRows) { // Fill column from bottom to top
                if (currentPieceIndex < numPieces) {
                    val piece = individualPieces[currentPieceIndex]
                    piece.pos = Vector2(startXColumn, startYColumn + row * pieceHeight)
                    currentPieceIndex++
                } else {
                    break // No more pieces
                }
            }

            var startXRow = 0f
            var startYRow = 0f

            // --- 2. Row Above --- (Start placing row only if pieces remain after column)
            if (currentPieceIndex < numPieces) {
                startXRow = connectedGroupsLayoutRect.x // Align left edge
                startYRow = connectedGroupsLayoutRect.y + connectedGroupsLayoutRect.height + puzzlePieceSpacingY
                startYRow -= startXRow % (puzzlePieceSize + puzzlePieceSpacingY / 2)

                for (col in 0 until numColumns) { // Fill row from left to right
                    if (currentPieceIndex < numPieces) {
                        val piece = individualPieces[currentPieceIndex]
                        piece.pos = Vector2(startXRow + col * pieceWidth, startYRow)
                        currentPieceIndex++
                    } else {
                        break // No more pieces
                    }
                }
            }

            // --- 3. Additional Columns to the Right and Rows Above (Alternating) ---
            var columnRowCycleIndex = 0
            while (currentPieceIndex < numPieces) {
                columnRowCycleIndex++
                // --- Additional Column to the Right ---
                startXColumn += pieceWidth // Shift X to the right for new column
                startYColumn = connectedGroupsLayoutRect.y // Reset Y to bottom edge

                for (row in 0 until numRows) {
                    if (currentPieceIndex < numPieces) {
                        val piece = individualPieces[currentPieceIndex]
                        piece.pos = Vector2(startXColumn, startYColumn + row * pieceHeight)
                        currentPieceIndex++
                    } else {
                        break
                    }
                }
                if (currentPieceIndex >= numPieces) break // All pieces placed

                // --- Additional Row Above ---
                startXRow = connectedGroupsLayoutRect.x // Reset X to left edge
                startYRow += pieceHeight // Shift Y upwards for new row

                for (col in 0 until numColumns) {
                    if (currentPieceIndex < numPieces) {
                        val piece = individualPieces[currentPieceIndex]
                        piece.pos = Vector2(startXRow + col * pieceWidth, startYRow)
                        currentPieceIndex++
                    } else {
                        break
                    }
                }
            }
        } else {
            // No connected groups, use default centered grid layout
            val startX = 0 - individualGridWidth / 2f + gridOffsetX
            val startY = 0 - individualGridHeight / 2f + gridOffsetY

            for (col in 0 until numColumns) {
                for (row in 0 until numRows) {
                    if (currentPieceIndex < numPieces) {
                        val piece = individualPieces[currentPieceIndex]
                        piece.pos = Vector2(startX + (col * pieceWidth), startY + (row * pieceHeight))
                        currentPieceIndex++
                    } else {
                        break
                    }
                }
            }
        }
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
