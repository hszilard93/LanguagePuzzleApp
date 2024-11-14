package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.ADVERB_PURPLE
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.OBJECT_YELLOW
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.OFF_WHITE
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.SUBJECT_GREEN
import edu.b4kancs.languagePuzzleApp.app.serialization.GdxSetSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ktx.collections.GdxSet
import ktx.collections.isNotEmpty
import ktx.collections.toGdxSet
import ktx.log.logger
import kotlin.math.sign

@Serializable
enum class Side {
    TOP, BOTTOM, LEFT, RIGHT;

    fun opposite(): Side = when (this) {
        TOP -> BOTTOM
        BOTTOM -> TOP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

@Serializable
enum class GrammaticalRole(val color: Color) {
    VERB(Color.WHITE),
    SUBJECT(SUBJECT_GREEN.value),
    OBJECT(OBJECT_YELLOW.value),
    ADVERBIAL(ADVERB_PURPLE.value),
    UNDEFINED(OFF_WHITE.value)
}

//@Serializable
data class Connection(
    val puzzlesConnected: Set<PuzzlePiece>,
    val via: PuzzleTab,
    val roleOfConnection: GrammaticalRole
) {
    fun removeConnection() {
        puzzlesConnected.forEach { puzzle ->
            puzzle.removeConnection(this)
        }
    }
}

@Serializable
@Polymorphic
sealed interface PuzzlePieceFeature {
    val owner: PuzzlePiece
    val side: Side
    var isGlowing: Boolean

    fun getType(): String {
        return if (this is PuzzleTab) "Tab" else "Blank"
    }

    // Method to calculate the midpoint of the feature
    fun getFeatureMidpoint(): Vector2 {
        val (featureWidth, featureHeight) = when (this) {
            is PuzzleTab -> PuzzleTab.WIDTH to PuzzleTab.HEIGHT + 5f    // Some minor adjustments to the midpoint calculation due to various factors
            is PuzzleBlank -> PuzzleBlank.WIDTH to ((PuzzleBlank.HEIGHT) * -1f) // The blank is drawn in the opposite direction
            else -> throw IllegalArgumentException("Unknown feature type")
        }

        return when (this.side) {
            Side.TOP -> Vector2(
                owner.pos.x + owner.size / 2,
                owner.pos.y + owner.size + featureHeight / 2
            )
            Side.BOTTOM -> Vector2(
                owner.pos.x + owner.size / 2,
                owner.pos.y - featureHeight / 2
            )
            Side.LEFT -> Vector2(
                owner.pos.x - featureHeight / 2,
                owner.pos.y + owner.size / 2
            )
            Side.RIGHT -> Vector2(
                owner.pos.x + owner.size + featureHeight / 2,
                owner.pos.y + owner.size / 2
            )
        }
    }
}

//@Serializable
data class PuzzleTab(
    override val owner: PuzzlePiece,
    override val side: Side,
    val grammaticalRole: GrammaticalRole,
    val text: String = ""
) : PuzzlePieceFeature {
    override var isGlowing: Boolean = false
    set(value) {
        field = value
        owner.hasChangedAppearance = true
    }

    companion object {
        const val WIDTH = 150f
        const val HEIGHT = WIDTH * 1f
    }

//    override fun equals(other: Any?): Boolean {
//        if (other is PuzzleTab) {
//            return owner == other.owner && grammaticalRole == other.grammaticalRole && text == other.text
//        }
//        return false
//    }
}

//@Serializable
data class PuzzleBlank( // An indentation on a puzzle piece is called a 'blank'
    override val owner: PuzzlePiece,
    override val side: Side
) : PuzzlePieceFeature {
    override var isGlowing: Boolean = false

    companion object {
        const val WIDTH = 151f
        const val HEIGHT = WIDTH * 1f
    }
}

//@Serializable
class PuzzlePiece(
    text: String,
    grammaticalRole: GrammaticalRole,
    pos: Vector2,
    var depth: Int = 0
) {
    val tabs: MutableList<PuzzleTab> = mutableListOf()
    val blanks: MutableList<PuzzleBlank> = mutableListOf()

    @Serializable(with=GdxSetSerializer::class)
    private val _connections = GdxSet<Connection>()
    @Serializable(with=GdxSetSerializer::class)
    var copyOfConnections: GdxSet<Connection> = GdxSet()
        get(): GdxSet<Connection> = _connections.toGdxSet()
        private set
    var connectionSize = _connections.size
        get() = _connections.size
        private set
    var isConnected = _connections.isNotEmpty()
        get() = _connections.isNotEmpty()
        private set

    var hasChangedAppearance = true

    var text: String = text
        set(value) {
            field = value
            hasChangedAppearance = true
        }

    var grammaticalRole: GrammaticalRole = grammaticalRole
        set(value) {
            field = value
            hasChangedAppearance = true
        }

    // Automatically update the renderPos every time the position of the puzzle changes
    // Clear existing connections as well
    var pos: Vector2 = Vector2(0f, 0f)
        set(value) {
            field = value
            boundingBoxPos = calculateRenderPosition()
            _connections.forEach(Connection::removeConnection)
            hasChangedAppearance = true
        }

    var size: Float = MIN_SIZE
        private set(value) {
            val adjustedVal = value.coerceAtLeast(MIN_SIZE)
            val diff = adjustedVal - field
            field = adjustedVal
            pos = pos.add(Vector2(diff / 2, diff / 2))
            boundingBoxPos = calculateRenderPosition()
            hasChangedAppearance = true
        }

    var targetSize: Float = size
        private set

    // Properties of the bounding box drawn around the puzzle base and it's possible tabs
    lateinit var boundingBoxPos: Vector2
        private set

    var boundingBoxSize: Float = calculateRenderSize()
        private set
        get() = calculateRenderSize()

    companion object {
        val logger = logger<PuzzlePiece>()
        const val MIN_SIZE = 300f
    }

    // Validate the PuzzlePiece on initialization
    init {
        this.pos = pos
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

        // Mark that size has changed to recreate its FrameBuffer
        hasChangedAppearance = true
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

        // Mark that size has changed to recreate its FrameBuffer
        hasChangedAppearance = true
    }

    fun addConnection(connection: Connection) {
        _connections.add(connection)
        hasChangedAppearance = true
    }

    fun removeConnection(connection: Connection) {
        _connections.remove(connection)
        hasChangedAppearance = true
    }

    private fun calculateRenderPosition(): Vector2 = Vector2(pos.x - PuzzleTab.HEIGHT, pos.y - PuzzleTab.HEIGHT)

    private fun calculateRenderSize(): Float = size + 2f * PuzzleTab.HEIGHT

    // Method to initiate animation
    fun changeSize(newTargetSize: Float, doAnimate: Boolean = true) {
        logger.debug { "animateSize newTargetSize=$newTargetSize doAnimate=$doAnimate" }
        targetSize = newTargetSize.coerceAtLeast(MIN_SIZE)
        if (!doAnimate) {
            size = targetSize
        }
    }

    // Method to update current size towards target size
    fun animateSize(delta: Float, speed: Float = 400f) {      // speed: larger value - faster speed
        logger.misc { "updateSize delta=$delta" }
        if (size != targetSize) {
            val sizeDifference = targetSize - size
            val sizeChange = speed * delta * sizeDifference.sign
            pos.sub(sizeChange / 2, sizeChange)
            size = (size + sizeChange).coerceIn(minOf(size, targetSize), maxOf(size, targetSize))
        }
    }

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
