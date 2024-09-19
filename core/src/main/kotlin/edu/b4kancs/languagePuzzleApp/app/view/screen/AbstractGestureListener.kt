package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2

abstract class AbstractGestureListener : GestureDetector.GestureListener {

    override fun touchDown(
        x: Float,
        y: Float,
        pointer: Int,
        button: Int
    ): Boolean = false

    override fun tap(
        x: Float,
        y: Float,
        count: Int,
        button: Int
    ): Boolean = false

    override fun longPress(x: Float, y: Float): Boolean = false

    override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean = false

    override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean = false

    override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

    override fun zoom(originalDistance: Float, currentDistance: Float): Boolean = false

    override fun pinch(
        initialFirstPointer: Vector2,
        initialSecondPointer: Vector2,
        firstPointer: Vector2,
        secondPointer: Vector2
    ): Boolean = false

    override fun pinchStop() {}
}
