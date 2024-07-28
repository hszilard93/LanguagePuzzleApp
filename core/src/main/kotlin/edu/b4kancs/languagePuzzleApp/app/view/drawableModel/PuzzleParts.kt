package edu.b4kancs.languagePuzzleApp.app.view.drawableModel

import com.badlogic.gdx.math.Vector2
import edu.b4kancs.languagePuzzleApp.app.model.Side
import edu.b4kancs.languagePuzzleApp.app.view.utils.calculateEndPosGivenRotation
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRadians
import kotlin.math.sqrt

// For some reason, 0 degrees points right, not up.
const val RIGHT_DIR = 0f
const val DOWN_DIR = 270f
const val LEFT_DIR = 180f
const val UP_DIR = 90f

abstract class Feature {
    abstract val startPosition: Vector2
    abstract val endPosition: Vector2
    abstract val length: Float
    abstract val startDirection: Float
    abstract val side: Side
}

data class Line(
    override val startPosition: Vector2,
    override val length: Float,
    override val startDirection: Float,
    override val side: Side
) : Feature() {
    override val endPosition: Vector2
        by lazy { calculateEndPosGivenRotation(startPosition, length, startDirection) }
}

data class Corner(
    override val startPosition: Vector2,
    override val length: Float,
    override val startDirection: Float,
    override val side: Side,
) : Feature() {
    override val endPosition: Vector2
        by lazy {
            val lineLengthToEnd = sqrt((length * length) + (length * length))
            calculateEndPosGivenRotation(startPosition, lineLengthToEnd, startDirection - 45f)
        }
    val centerPosition: Vector2
        by lazy {
            calculateEndPosGivenRotation(startPosition, length, startDirection - 90f)
        }
    val startAngle: Float
        by lazy { (startDirection).toRadians() }
}

data class Tab(
    override val startPosition: Vector2,
    override val length: Float,
    override val startDirection: Float,
    val height: Float,
    override val side: Side
) : Feature() {
    override val endPosition: Vector2
        get() = calculateEndPosGivenRotation(startPosition, length, startDirection)
}

data class Blank(
    override val startPosition: Vector2,
    override val length: Float,
    override val startDirection: Float,
    val depth: Float,
    override val side: Side
) : Feature() {
    override val endPosition: Vector2
        get() = calculateEndPosGivenRotation(startPosition, length, startDirection)
}
