package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.ADVERB_PURPLE
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.OBJECT_YELLOW
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.OFF_WHITE
import edu.b4kancs.languagePuzzleApp.app.model.CustomColors.SUBJECT_GREEN
import edu.b4kancs.languagePuzzleApp.app.serialization.PuzzleBlankSerializer
import edu.b4kancs.languagePuzzleApp.app.serialization.PuzzlePieceSerializer
import edu.b4kancs.languagePuzzleApp.app.serialization.PuzzleTabSerializer
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ktx.log.logger
import java.util.Optional
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

@Serializable
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
    var owner: PuzzlePiece?
    val side: Side

    @Transient
    var isGlowing: Boolean

    enum class Type {
        TAB, BLANK
    }

    fun getType(): Type {
        return if (this is PuzzleTab) Type.TAB else Type.BLANK
    }

    // Method to calculate the midpoint of the feature
    fun getFeatureMidpoint(): Vector2 {
        val (featureWidth, featureHeight) = when (this) {
            is PuzzleTab -> PuzzleTab.WIDTH to PuzzleTab.HEIGHT + 5f    // Some minor adjustments to the midpoint calculation due to various factors
            is PuzzleBlank -> PuzzleBlank.WIDTH to ((PuzzleBlank.HEIGHT) * -1f) // The blank is drawn in the opposite direction
            else -> throw IllegalArgumentException("Unknown feature type")
        }

        return owner?.let { o ->
            when (this.side) {
                Side.TOP -> Vector2(
                    o.pos.x + o.size / 2,
                    o.pos.y + o.size + featureHeight / 2
                )

                Side.BOTTOM -> Vector2(
                    o.pos.x + o.size / 2,
                    o.pos.y - featureHeight / 2
                )

                Side.LEFT -> Vector2(
                    o.pos.x - featureHeight / 2,
                    o.pos.y + o.size / 2
                )

                Side.RIGHT -> Vector2(
                    o.pos.x + o.size + featureHeight / 2,
                    o.pos.y + o.size / 2
                )
            }
        } ?: Vector2(0f, 0f)
    }

    fun isPointOverFeature(mousePos: Vector2): Boolean {
        val midpoint = getFeatureMidpoint()
        val width = when (this) {
            is PuzzleTab -> PuzzleTab.WIDTH
            is PuzzleBlank -> PuzzleBlank.WIDTH
        }
        val height = when (this) {
            is PuzzleTab -> PuzzleTab.HEIGHT / 1.5f
            is PuzzleBlank -> PuzzleBlank.HEIGHT / 1.75f
        }

        val heightOffset = when (this) {
            is PuzzleTab -> 40f
            is PuzzleBlank -> -30f
        }

        // Adjust the bounding box calculation based on the side
        return when (this.side) {
            Side.TOP -> mousePos.x in (midpoint.x - width / 2f)..(midpoint.x + width / 2f) &&
                mousePos.y in (midpoint.y - height / 2f) - heightOffset..(midpoint.y + height / 2f) - heightOffset

            Side.BOTTOM -> mousePos.x in (midpoint.x - width / 2f)..(midpoint.x + width / 2f) &&
                mousePos.y in (midpoint.y - height / 2f) + heightOffset..(midpoint.y + height / 2f) + heightOffset

            Side.LEFT -> mousePos.x in midpoint.x..(midpoint.x + width / 2f) &&
                mousePos.y in (midpoint.y - width / 2f)..(midpoint.y + width / 2f)

            Side.RIGHT -> mousePos.x in (midpoint.x - height / 2f) - heightOffset..(midpoint.x + height / 2f) - heightOffset &&
                mousePos.y in (midpoint.y - width / 2f)..(midpoint.y + width / 2f)
        }
    }
}

@Serializable(with = PuzzleTabSerializer::class)
class PuzzleTab(
    override var owner: PuzzlePiece? = null,
    override val side: Side,
    val grammaticalRole: GrammaticalRole,
    text: String = ""
) : PuzzlePieceFeature {
    var pos: Vector2 = Vector2(0f, 0f)
        get() = calculateRelativePosition()
        private set
    var textLayoutBounds: Rectangle = Rectangle(0f, 0f, 0f, 0f)
        get() = calculateLayoutBounds()
        private set

    init {
        calculateRelativePosition()
        calculateLayoutBounds()
    }

    var text: String = text
        set(value) {
            field = value
            owner?.hasChangedAppearance = true
        }

    @Transient
    override var isGlowing: Boolean = false
        set(value) {
            field = value
            owner?.hasChangedAppearance = true
        }

    companion object {
        const val WIDTH = 150f
        const val HEIGHT = WIDTH * 1f
    }

    private fun calculateRelativePosition(): Vector2 {
        if (owner == null) return Vector2(0f, 0f)

        val x: Float
        val y: Float
        val tabOffset = 10f
        when (side) {
            Side.TOP -> {
                x = HEIGHT + owner!!.size / 2f - WIDTH / 2
                y = tabOffset
            }

            Side.BOTTOM -> {
                x = HEIGHT + owner!!.size / 2 - WIDTH / 2
                y = HEIGHT + owner!!.size - tabOffset
            }

            Side.LEFT -> {
                x = HEIGHT - WIDTH + tabOffset
                y = HEIGHT + owner!!.size / 2 - HEIGHT / 2
            }

            Side.RIGHT -> {
                x = HEIGHT + owner!!.size - tabOffset
                y = HEIGHT + owner!!.size / 2 - HEIGHT / 2
            }
        }
        return Vector2(x, y)
    }

    private fun calculateLayoutBounds(): Rectangle {
        if (owner == null) return Rectangle(0f, 0f, 0f, 0f)

        // Start with the tab's world position
        val tabWorldX = owner!!.boundingBoxPos.x + pos.x
        val tabWorldY = owner!!.boundingBoxPos.y +
            if (side == Side.TOP || side == Side.BOTTOM) {
                (pos.y - (owner!!.size + HEIGHT)) * -1
            }
            else pos.y

        // Adjust for text layout position relative to the tab's origin
        val tLayoutWidth = 40f
        val tLayoutHeight = 40f
        val tLayoutOffsetX: Float
        val tLayoutOffsetY: Float

        when (side) {
            Side.LEFT -> {
                tLayoutOffsetX = WIDTH / 2f + 10f
                tLayoutOffsetY = HEIGHT / 3f //* -1
            }
            Side.RIGHT -> {
                tLayoutOffsetX = WIDTH / 4f - 10f
                tLayoutOffsetY = HEIGHT / 3f // * -1
            }
            Side.TOP -> {
                tLayoutOffsetX = WIDTH / 3f
                tLayoutOffsetY = HEIGHT / 4f - 10f
            }
            Side.BOTTOM -> {
                tLayoutOffsetX = WIDTH / 3f
                tLayoutOffsetY = HEIGHT / 2f + 10f
            }
        }

        val rectStartX = tabWorldX + tLayoutOffsetX
        val rectStartY = tabWorldY + tLayoutOffsetY

        return Rectangle(
            rectStartX,
            rectStartY,
            tLayoutWidth,
            tLayoutHeight
        )
    }

    private fun calculateLayoutBoundsOLD(): Rectangle {
        if (owner == null) return Rectangle(0f, 0f, 0f, 0f)

        val position = pos.cpy()

        val worldLayoutXOffset =
            when (side) {
                Side.LEFT -> WIDTH / 8f
                Side.RIGHT -> WIDTH / 4f * -1
                else -> WIDTH / 8f * -1
            }

        val worldLayoutYOffset =
            when (side) {
                Side.TOP -> HEIGHT / 2f * -1
                Side.BOTTOM -> HEIGHT / 5f * -1
                else -> HEIGHT / 2f * -1
            }

        val worldLayoutX = position.x + owner!!.boundingBoxPos.x + HEIGHT / 2 + worldLayoutXOffset
        val worldLayoutY = (position.y + owner!!.boundingBoxPos.y) * -1 + WIDTH / 2 + worldLayoutYOffset

        return Rectangle(
            worldLayoutX - 20f,
            worldLayoutY - 20f,
            75f,
            75f
        )
    }

    fun isPointerOverTextLayout(mousePos: Vector2): Boolean {
        return textLayoutBounds.contains(mousePos.x, mousePos.y)
    }
}

@Serializable(with = PuzzleBlankSerializer::class)
data class PuzzleBlank( // An indentation on a puzzle piece is called a 'blank'
    override var owner: PuzzlePiece? = null,
    override val side: Side
) : PuzzlePieceFeature {
    override var isGlowing: Boolean = false

    companion object {
        const val WIDTH = 151f
        const val HEIGHT = WIDTH * 1f
    }
}

@Serializable(with = PuzzlePieceSerializer::class)
class PuzzlePiece(
    text: String,
    grammaticalRole: GrammaticalRole,
    pos: Vector2 = Vector2(0f, 0f),
    var depth: Int = 0  // The larger the value, the more on top the piece is
) {
    val tabs: MutableList<PuzzleTab> = mutableListOf()
    val blanks: MutableList<PuzzleBlank> = mutableListOf()

    @Transient
    private val _connections = mutableSetOf<Connection>()

    @Transient
    var copyOfConnections: Set<Connection> = emptySet()
        get(): Set<Connection> = _connections.toSet()
        private set

    @Transient
    var connectionSize = _connections.size
        get() = _connections.size
        private set

    @Transient
    var isConnected = _connections.isNotEmpty()
        get() = _connections.isNotEmpty()
        private set

    @Transient
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
    @Transient
    var pos: Vector2 = Vector2(0f, 0f)
        set(value) {
            field = value
            boundingBoxPos = calculateRenderPosition()
//            _connections.forEach(Connection::removeConnection)
            hasChangedAppearance = true
        }

    @Transient
    var size: Float = MIN_SIZE
        private set(value) {
            val adjustedVal = value.coerceAtLeast(MIN_SIZE)
            val diff = adjustedVal - field
            field = adjustedVal
            pos = pos.add(Vector2(diff / 2, diff / 2))
            boundingBoxPos = calculateRenderPosition()
            hasChangedAppearance = true
        }

    @Transient
    var targetSize: Float = size
        private set

    // Properties of the bounding box drawn around the puzzle base and it's possible tabs
    @Transient
    lateinit var boundingBoxPos: Vector2
        private set

    @Transient
    var boundingBoxSize: Float = calculateRenderSize()
        private set
        get() = calculateRenderSize()

    @Transient
    var textLayoutBounds: Rectangle = Rectangle(0f, 0f, 0f, 0f)

    companion object {
        val logger = logger<PuzzlePiece>()
        const val MIN_SIZE = 300f
    }

    // Validate the PuzzlePiece on initialization
    init {
        this.pos = pos
        // Check if any tabs or blanks overlap
        if (tabs.map { it.side }.intersect(blanks.map { it.side }.toSet()).isNotEmpty()) {
            throw InvalidPuzzlePieceException(
                "PuzzlePiece has overlapping tabs and blanks!" +
                    "\ntabs = $tabs" +
                    "\nblanks = $blanks"
            )
        }

        tabs.forEach { it.owner = this }
        blanks.forEach { it.owner = this }

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
            PuzzleTab(this, newSide, it.grammaticalRole, it.text)
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

        if (this.grammaticalRole == GrammaticalRole.VERB) {
            return  // Central puzzles shouldn't be rotated
        }

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
            PuzzleBlank(this, newSide)
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
        connection.puzzlesConnected.filter { it != this }.forEach { other ->
            if (other.copyOfConnections.contains(connection)) other.removeConnection(connection)
        }
        hasChangedAppearance = true
    }

    fun addFeature(type: PuzzlePieceFeature.Type, side: Side, role: GrammaticalRole = GrammaticalRole.UNDEFINED, tabText: String = ""): PuzzlePieceFeature {
        logger.debug { "addFeature type=$type side=$side" }
        hasChangedAppearance = true
        when (type) {
            PuzzlePieceFeature.Type.TAB -> {
                val tabToReplace = tabs.firstOrNull { it.side == side }  // We might need to replace UNDEFINED tabs
                if (tabToReplace != null) {
                    removeFeature(tabToReplace)
                }

                tabs.add(PuzzleTab(this, side, role, tabText))
                return tabs.last()
            }

            PuzzlePieceFeature.Type.BLANK -> {
                blanks.add(PuzzleBlank(this, side))
                return blanks.last()
            }
        }
    }

    fun removeFeature(feature: PuzzlePieceFeature) {
        logger.debug { "removeFeature feature=$feature" }
        when (feature) {
            is PuzzleTab -> tabs.remove(feature)
            is PuzzleBlank -> blanks.remove(feature)
        }
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

    fun isPointerOverTextLayout(mousePos: Vector2): Boolean {
        return textLayoutBounds.contains(mousePos.x, mousePos.y)
    }

    fun findPotentialFeatureUnderPointer(mousePos: Vector2): Optional<Pair<PuzzlePieceFeature.Type, Side>> {

        if (this.grammaticalRole != GrammaticalRole.VERB) {
            return Optional.empty()
        }

        setOf(Side.TOP, Side.BOTTOM, Side.LEFT, Side.RIGHT)
            .filter { side ->
                this.getAllFeatures()
                    // We now have to handle UNDEFINED tabs as well
//                    .filter { !(it is PuzzleTab && it.grammaticalRole == GrammaticalRole.UNDEFINED) }
                    .map { it.side }
                    .contains(side)
                    .not()
            }
            .forEach { side ->
                val featureHeight = 40f
                val featureWidth = 50f

                var result: Optional<Pair<PuzzlePieceFeature.Type, Side>>? = null
                when (side) {
                    Side.TOP -> {
                        val zoneYStart = pos.y + size

                        val zoneXStart = pos.x + (size - featureWidth) / 2f
                        val zoneXEnd = zoneXStart + featureWidth

                        val isInVerticalSlice = mousePos.x in zoneXStart..zoneXEnd
                        if (isInVerticalSlice) {
                            val isTab = mousePos.y >= zoneYStart && mousePos.y <= zoneYStart + featureHeight
                            if (isTab) {
                                result = Optional.of(PuzzlePieceFeature.Type.TAB to side)
                            }
                        }
                    }

                    Side.BOTTOM -> {
                        val zoneYStart = pos.y

                        val zoneXStart = pos.x + (size - featureWidth) / 2f
                        val zoneXEnd = zoneXStart + featureWidth

                        val isInVerticalSlice = mousePos.x in zoneXStart..zoneXEnd
                        if (isInVerticalSlice) {
                            val isTab = zoneYStart >= mousePos.y && mousePos.y >= zoneYStart - featureHeight
                            if (isTab) {
                                result = Optional.of(PuzzlePieceFeature.Type.TAB to side)
                            }
                        }
                    }

                    Side.LEFT -> {
                        val zoneXStart = pos.x

                        val zoneYStart = pos.y + (size - featureWidth) / 2f
                        val zoneYEnd = zoneYStart + featureWidth

                        val isInVerticalSlice = mousePos.y in zoneYStart..zoneYEnd
                        if (isInVerticalSlice) {
                            val isTab = zoneXStart >= mousePos.x && mousePos.x >= zoneXStart - featureHeight
                            if (isTab) {
                                result = Optional.of(PuzzlePieceFeature.Type.TAB to side)
                            }
                        }
                    }

                    Side.RIGHT -> {
                        val zoneXStart = pos.x + size

                        val zoneYStart = pos.y + (size - featureWidth) / 2f
                        val zoneYEnd = zoneYStart + featureWidth

                        val isInVerticalSlice = mousePos.y in zoneYStart..zoneYEnd
                        if (isInVerticalSlice) {
                            val isTab = mousePos.x >= zoneXStart && mousePos.x <= zoneXStart + featureHeight
                            if (isTab) {
                                result = Optional.of(PuzzlePieceFeature.Type.TAB to side)
                            }
                        }
                    }
                }

                if (result != null) {
                    if (result.get().first == PuzzlePieceFeature.Type.BLANK && blanks.size > 0) {
                        return Optional.empty()
                    }
                    return result
                }
            }
        return Optional.empty()
    }
}


class InvalidPuzzlePieceException(message: String) : IllegalArgumentException(message)
