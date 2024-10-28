package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.ADVERB_PURPLE
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.OBJECT_YELLOW
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.OFF_WHITE
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.SUBJECT_GREEN
import ktx.log.logger

enum class Side {
    TOP, BOTTOM, LEFT, RIGHT;

    fun opposite(): Side = when (this) {
        TOP -> BOTTOM
        BOTTOM -> TOP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

enum class GrammaticalRole(val color: Color) {
    VERB(Color.WHITE),
    SUBJECT(SUBJECT_GREEN.value),
    OBJECT(OBJECT_YELLOW.value),
    ADVERBIAL(ADVERB_PURPLE.value),
    UNDEFINED(OFF_WHITE.value)
}

data class Connection(
    val puzzlesConnected: Set<PuzzlePiece>,
    val via: PuzzleTab,
    val roleOfConnection: GrammaticalRole
) {
    fun removeConnection() {
        puzzlesConnected.forEach { puzzle ->
            puzzle.connections.remove(this)
        }
    }
}

interface PuzzlePieceFeature {
    val owner: PuzzlePiece
    val side: Side
    var isGlowing: Boolean

    fun getType(): String {
        return if (this is PuzzleTab) "Tab" else "Blank"
    }

    // Method to calculate the midpoint of the feature
    fun getFeatureMidpoint(): Vector2 {
        val (featureWidth, featureHeight) = when (this) {
            is PuzzleTab -> PuzzleTab.WIDTH to PuzzleTab.HEIGHT - 5f    // Some minor adjustments to the midpoint calculation due to various factors
            is PuzzleBlank -> PuzzleBlank.WIDTH to PuzzleBlank.HEIGHT * -1f // The blank is drawn in the opposite direction
            else -> throw IllegalArgumentException("Unknown feature type")
        }

        return when (this.side) {
            Side.TOP -> Vector2(
                owner.pos.x + owner.width / 2,
                owner.pos.y + owner.height + featureHeight / 2
            )

            Side.BOTTOM -> Vector2(
                owner.pos.x + owner.width / 2,
                owner.pos.y - featureHeight / 2
            )

            Side.LEFT -> Vector2(
                owner.pos.x - featureHeight / 2,
                owner.pos.y + owner.height / 2
            )

            Side.RIGHT -> Vector2(
                owner.pos.x + owner.width + featureHeight / 2,
                owner.pos.y + owner.height / 2
            )
        }
    }
}

data class PuzzleTab(
    override val owner: PuzzlePiece,
    override val side: Side,
    val grammaticalRole: GrammaticalRole,
    val text: String = ""
) : PuzzlePieceFeature {
    override var isGlowing: Boolean = false

    companion object {
        const val WIDTH = 110f
        const val HEIGHT = WIDTH * 0.88f
    }

//    override fun equals(other: Any?): Boolean {
//        if (other is PuzzleTab) {
//            return owner == other.owner && grammaticalRole == other.grammaticalRole && text == other.text
//        }
//        return false
//    }
}

data class PuzzleBlank( // An indentation on a puzzle piece is called a 'blank'
    override val owner: PuzzlePiece,
    override val side: Side
) : PuzzlePieceFeature {
    override var isGlowing: Boolean = false

    companion object {
        const val WIDTH = 120f
        const val HEIGHT = WIDTH * 0.75f
    }
}

class PuzzlePiece(
    var text: String,
    val grammaticalRole: GrammaticalRole,
    pos: Vector2,
    width: Float = MIN_WIDTH,
    height: Float = MIN_HEIGHT,
    var depth: Int = 0
) {
    val tabs: MutableList<PuzzleTab> = mutableListOf()
    val blanks: MutableList<PuzzleBlank> = mutableListOf()
    val connections: MutableSet<Connection> = HashSet()

    // Automatically update the renderPos every time the position of the puzzle changes
    // Clear existing connections as well
    var pos: Vector2 = Vector2(0f, 0f)
        set(value) {
            field = value
            boundingBoxPos = calculateRenderPosition(field)
            connections.forEach(Connection::removeConnection)
        }
    var width: Float = MIN_WIDTH
        set(value) {
            field = value.coerceAtLeast(MIN_WIDTH)
            boundingBoxSize = calculateRenderSize(field, height)
        }
    var height: Float = MIN_HEIGHT
        set(value) {
            field = value.coerceAtLeast(MIN_HEIGHT)
            boundingBoxSize = calculateRenderSize(width, field)
        }

    var hasChangedSizeSinceLastRender = true

    // Properties of the bounding box drawn around the puzzle base and it's possible tabs
    lateinit var boundingBoxPos: Vector2
        private set
    lateinit var boundingBoxSize: Pair<Float, Float>
        private set

    companion object {
        val logger = logger<PuzzlePiece>()
        const val MIN_WIDTH = 300f
        const val MIN_HEIGHT = 300f
    }

    // Validate the PuzzlePiece on initialization
    init {
        this.pos = pos
        this.width = width
        this.height = height
        if (tabs.map { it.side }.intersect(blanks.map { it.side }.toSet()).isNotEmpty()) {
            throw InvalidPuzzlePieceException(
                "PuzzlePiece has overlapping tabs and blanks!" +
                    "\ntabs = $tabs" +
                    "\nblanks = $blanks"
            )
        }

        logger.info { "PuzzlePiece initialized\t text=$text grammaticalRole=$grammaticalRole pos=$pos" }
    }

    fun getAllFeatures(): List<PuzzlePieceFeature> = tabs + blanks

    fun rotateLeft() {
        logger.debug { "rotateLeft" }

        // Rotate each tab and blank's side
        val newTabs = tabs.map {
            val newSide = when (it.side) {
                Side.TOP -> Side.LEFT
                Side.BOTTOM -> Side.RIGHT
                Side.LEFT -> Side.BOTTOM
                Side.RIGHT -> Side.TOP
            }
            PuzzleTab(it.owner, newSide, it.grammaticalRole, it.text)
        }
        tabs.clear()
        tabs.addAll(newTabs)

        val newBlanks = blanks.map {
            val newSide = when (it.side) {
                Side.TOP -> Side.LEFT
                Side.BOTTOM -> Side.RIGHT
                Side.LEFT -> Side.BOTTOM
                Side.RIGHT -> Side.TOP
            }
            PuzzleBlank(it.owner, newSide)
        }
        blanks.clear()
        blanks.addAll(newBlanks)

        // Recalculate bounding box position and size
        boundingBoxSize = calculateRenderSize(width, height)
        boundingBoxPos = calculateRenderPosition(pos)

        // Mark that size has changed to recreate its FrameBuffer
        hasChangedSizeSinceLastRender = true
    }

    fun rotateRight() {
        logger.debug { "rotateRight" }

        // Rotate each tab and blank's side in the opposite direction
        val newTabs = tabs.map {
            val newSide = when (it.side) {
                Side.TOP -> Side.RIGHT
                Side.BOTTOM -> Side.LEFT
                Side.LEFT -> Side.TOP
                Side.RIGHT -> Side.BOTTOM
            }
            PuzzleTab(it.owner, newSide, it.grammaticalRole, it.text)
        }
        tabs.clear()
        tabs.addAll(newTabs)

        val newBlanks = blanks.map {
            val newSide = when (it.side) {
                Side.TOP -> Side.RIGHT
                Side.BOTTOM -> Side.LEFT
                Side.LEFT -> Side.TOP
                Side.RIGHT -> Side.BOTTOM
            }
            PuzzleBlank(it.owner, newSide)
        }
        blanks.clear()
        blanks.addAll(newBlanks)

        // Recalculate bounding box position and size
        boundingBoxSize = calculateRenderSize(width, height)
        boundingBoxPos = calculateRenderPosition(pos)

        // Mark that size has changed to recreate its FrameBuffer
        hasChangedSizeSinceLastRender = true
    }

    private fun calculateRenderPosition(pos: Vector2): Vector2 = Vector2(pos.x - PuzzleTab.HEIGHT, pos.y - PuzzleTab.HEIGHT)

    private fun calculateRenderSize(width: Float, height: Float): Pair<Float, Float> =
        width + 2 * PuzzleTab.HEIGHT to height + 2 * PuzzleTab.HEIGHT

//    override fun equals(other: Any?): Boolean {
//        if (other is PuzzlePiece)
//            return other.text == text
//                && other.grammaticalRole == grammaticalRole
//                && tabs.containsAll(other.tabs)
//                && tabs.size == other.tabs.size
//                && blanks.containsAll(other.blanks)
//                && blanks.size == other.blanks.size
//
//        return false
//    }
}

class InvalidPuzzlePieceException(message: String) : IllegalArgumentException(message)
