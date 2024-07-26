@file:JvmName("Lwjgl3Launcher")

package edu.b4kancs.puzli.app.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.HdpiMode
import edu.b4kancs.puzli.app.Game
import edu.b4kancs.puzli.app.model.Environment
import edu.b4kancs.puzli.app.model.Platform
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
        setTitle("PuzliApp")
        setWindowedMode(defaultWidth, defaultHeight)
        setWindowIcon(*(arrayOf(128, 64, 32, 16).map { "libgdx$it.png" }.toTypedArray()))
        setForegroundFPS(90)
        setIdleFPS(10)
        setBackBufferConfig(8, 8, 8, 8, 16, 0, 8)
    }

    Lwjgl3Application(game, config)
}
