@file:JvmName("TeaVMLauncher")

package edu.b4kancs.languagePuzzleApp.app.teavm

import JsInterop
import com.github.xpenatan.gdx.backends.teavm.TeaApplication
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.model.Environment
import edu.b4kancs.languagePuzzleApp.app.model.Platform


/** Launches the TeaVM/HTML application. */

class TeaVMLauncher {
    fun main(args: Array<String>) {
        val config = TeaApplicationConfiguration("canvas")
        // change these to both 0 to use all available space, or both -1 for the canvas size.
        config.width = 0
        config.height = 0

        val environment = getEnvironment(config)

        TeaApplication(Game(environment), config)
    }

    private fun getEnvironment(config: TeaApplicationConfiguration): Environment {
        JsInterop.log("getEnvironment")

        val userAgent = JsInterop.getUserAgent()
        JsInterop.log("UserAgent: $userAgent")
        val platform: Platform
        if (userAgent.contains("""Mobile|Mini|Android|Miui""".toRegex())) {
            JsInterop.log("User is on mobile. Disabling AA.")
            config.antialiasing = false
            platform =
                when {
                    userAgent.contains("Android") -> Platform.WEB_ANDROID
                    userAgent.contains("iPad") -> Platform.WEB_IPAD
                    userAgent.contains("iPhone") -> Platform.WEB_IOS
                    else -> Platform.UNKNOWN
                }
        }
        else {
            JsInterop.log("User is not on mobile. Enabling AA.")
            config.antialiasing = true
            platform = Platform.WEB
        }
        val canvasSize = JsInterop.getCanvasSize().split(";").map(String::toInt)
        JsInterop.log("platform: $platform, canvasSize: ${canvasSize.first()}, ${canvasSize.last()}")
        return Environment(platform, null, canvasSize.first(), canvasSize.last())
    }
}
