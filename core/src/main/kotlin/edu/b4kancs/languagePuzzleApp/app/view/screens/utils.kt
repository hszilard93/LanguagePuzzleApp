package edu.b4kancs.languagePuzzleApp.app.view.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.Texture
import edu.b4kancs.languagePuzzleApp.app.view.util.toRGBFloat
import ktx.log.Logger

fun setBackgroundColor(red: Int, green: Int, blue: Int, alpha: Float) {
    Gdx.gl.glClearColor(red.toRGBFloat(), green.toRGBFloat(), blue.toRGBFloat(), alpha)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
}

fun saveTextureToPNG(texture: Texture, suffix: String = "0", logger: Logger) {
    val dest = Gdx.files.local("screenshots/frameBufferScreenshot_$suffix.png")
    val textureData = texture.textureData
    if (!textureData.isPrepared) {
        textureData.prepare()
    }
    val pixmap: Pixmap? = textureData.consumePixmap()
    if (pixmap != null) {
        try {
            val width = texture.width
            val height = texture.height
            val flippedPixmap = Pixmap(width, height, pixmap.format)
            for (y in 0 until height) {
                flippedPixmap.drawPixmap(pixmap, 0, y, 0, height - y - 1, width, 1)
            }
            PixmapIO.writePNG(dest, flippedPixmap)
            flippedPixmap.dispose()
            logger.debug { "Screenshot saved to ${dest.path()}" }
        } catch (e: Exception) {
            logger.error { "Failed to save screenshot: ${e.message}" }
        } finally {
            pixmap.dispose()
        }
    } else {
        logger.error { "Failed to obtain Pixmap from TextureData." }
    }
}
