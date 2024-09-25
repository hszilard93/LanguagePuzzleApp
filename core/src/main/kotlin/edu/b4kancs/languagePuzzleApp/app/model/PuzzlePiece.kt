package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
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
    val color: Color? = null,
) : PuzzlePieceFeature {
    override var isGlowing: Boolean = false

    companion object {
        const val WIDTH = 110f
        const val HEIGHT = WIDTH * 0.88f
    }
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
    pos: Vector2,
    width: Float = MIN_WIDTH,
    height: Float = MIN_HEIGHT,
    var depth: Int = 0,
    val color: Color = Color.YELLOW
) {
    val tabs: MutableList<PuzzleTab> = mutableListOf()
    val blanks: MutableList<PuzzleBlank> = mutableListOf()

    // Automatically update the renderPos every time the position of the puzzle changes
    var pos: Vector2 = Vector2(0f, 0f)
        set(value) {
            field = value
            boundingBoxPos = calculateRenderPosition(field)
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

        logger.info { "PuzzlePiece initialized\t pos=$pos color=$color" }
    }

    fun getAllFeatures(): List<PuzzlePieceFeature> = tabs + blanks

    private fun calculateRenderPosition(pos: Vector2): Vector2 = Vector2(pos.x - PuzzleTab.HEIGHT, pos.y - PuzzleTab.HEIGHT)

    private fun calculateRenderSize(width: Float, height: Float): Pair<Float, Float> =
        width + 2 * PuzzleTab.HEIGHT to height + 2 * PuzzleTab.HEIGHT
}

class InvalidPuzzlePieceException(message: String) : IllegalArgumentException(message)
