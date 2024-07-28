package edu.b4kancs.languagePuzzleApp.app.view.utils

import com.badlogic.gdx.math.Vector2
import kotlin.math.cos
import kotlin.math.sin

fun Float.toRadians(): Float = this / 57.29578f

operator fun Vector2.plus(other: Vector2): Vector2 = Vector2(this.x + other.x, this.y + other.y)

fun calculateEndPosGivenRotation(startPos: Vector2, length: Float, rotation: Float): Vector2 {
    val angleRadians = Math.toRadians(rotation.toDouble())
    val endX = startPos.x + length * cos(angleRadians)
    val endY = startPos.y + length * sin(angleRadians)
    return Vector2(endX.toFloat(), endY.toFloat())
}

inline fun Int.toRGBFloat(): Float = this / 255f
