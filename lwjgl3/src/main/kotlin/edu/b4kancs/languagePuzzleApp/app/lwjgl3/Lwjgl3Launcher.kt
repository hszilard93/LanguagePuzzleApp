@file:JvmName("Lwjgl3Launcher")

package edu.b4kancs.languagePuzzleApp.app.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.Platform
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFW.GLFW_DECORATED
import org.lwjgl.glfw.GLFW.GLFW_FALSE
import org.lwjgl.glfw.GLFW.GLFW_SAMPLES

/** Launches the desktop (LWJGL3) application. */
fun main() {
    // This handles macOS support and helps on Windows.
    if (StartupHelper.startNewJvmIfRequired())
      return

    GLFW.glfwWindowHint(GLFW_SAMPLES, 4)
    GLFW.glfwWindowHint(GLFW_DECORATED, GLFW_FALSE)

    val defaultWidth = 1200
    val defaultHeight = 800
    val environment = Environment(Platform.DESKTOP, null, defaultWidth, defaultHeight)
    val game = Game(environment)
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("PuzzliApp")
        setWindowedMode(defaultWidth, defaultHeight)
        setResizable(false)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
        setForegroundFPS(90)
        setIdleFPS(10)
        setBackBufferConfig(8, 8, 8, 8, 16, 0, 0)
    }

    Lwjgl3Application(game, config)
}
