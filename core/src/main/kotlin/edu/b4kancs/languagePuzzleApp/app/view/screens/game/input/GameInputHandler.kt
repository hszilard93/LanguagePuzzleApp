package edu.b4kancs.languagePuzzleApp.app.view.screens.game.input

import com.badlogic.gdx.math.Vector2

interface GameInputHandler {

    fun emulatePuzzleDragging()

    fun getLastTouch(): Vector2
}
