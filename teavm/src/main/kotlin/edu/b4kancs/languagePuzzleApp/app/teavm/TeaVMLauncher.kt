@file:JvmName("TeaVMLauncher")

package edu.b4kancs.languagePuzzleApp.app.teavm

import JsInterop
import com.github.xpenatan.gdx.backends.teavm.TeaApplication
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration
import com.github.xpenatan.gdx.backends.teavm.TeaAssetPreloadListener
import com.github.xpenatan.gdx.backends.teavm.assetloader.AssetLoader
import edu.b4kancs.languagePuzzleApp.app.Game
import edu.b4kancs.languagePuzzleApp.app.platform.Environment
import edu.b4kancs.languagePuzzleApp.app.platform.EnvironmentalImplementations
import edu.b4kancs.languagePuzzleApp.app.platform.Platform
import edu.b4kancs.languagePuzzleApp.app.teavm.HtmlUtils.setFavicon
import edu.b4kancs.languagePuzzleApp.app.teavm.platform.ui.FilePickerJsImpl


/** Launches the TeaVM/HTML application. */

class TeaVMLauncher {
    fun main(args: Array<String>) {
        val config = TeaApplicationConfiguration("canvas")
        // change these to both 0 to use all available space, or both -1 for the canvas size.
        config.width = 0
        config.height = 0
        config.showDownloadLogs = true
        config.preloadListener = TeaAssetPreloadListener { assetLoader: AssetLoader ->
            assetLoader.loadScript("freetype.js")
        }
        config.antialiasing = true
        config.stencil = true            // Enable stencil buffer if needed
        config.usePhysicalPixels = true

        val environment = getEnvironment(config)
        val environmentalImplementations = EnvironmentalImplementations(
            filePickerImpl = FilePickerJsImpl(),
        )

        TeaApplication(Game(environment, environmentalImplementations), config)
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

        if (platform in setOf(Platform.WEB, Platform.WEB_ANDROID, Platform.WEB_IOS, Platform.WEB_IPAD)) {
            JsInterop.log("disableBrowserScroll")
            JsInterop.disableBrowserScroll()
            setFavicon("assets/graphics/app_icon_4.png")
        }

        return Environment(platform, null, canvasSize.first(), canvasSize.last())
    }
}
