package edu.b4kancs.languagePuzzleApp.app.view.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import ktx.log.Logger

data class PuzzleFontHolder(val baseFont: BitmapFont, val tabFont: BitmapFont)

data class TaskFontHolder(val descriptionFont: BitmapFont, val counterFont: BitmapFont)

data class HudFontHolder(val font: BitmapFont)

data class MenuFontHolder(val font: BitmapFont)

data class UIFontHolder(val font: BitmapFont)

private const val TASK_DESC_DEFAULT_FONT_SIZE = 24
private const val MENU_DEFAULT_FONT_SIZE = 24
private const val MANUAL_DEFAULT_FONT_SIZE = 32
private const val UI_DEFAULT_FONT_SIZE = 22
private const val PUZZLE_BASE_FONT_SIZE = 40
private const val PUZZLE_TAB_FONT_SIZE = (PUZZLE_BASE_FONT_SIZE * 0.75f).toInt()
private const val MIN_FONT_SIZE = 16

// Caching the FreeTypeFontGenerator objects to save on IO
private val fontGeneratorMapByFileName = mutableMapOf<String, FreeTypeFontGenerator>()

private val logger = Logger("FontManager")

fun loadFreeTypeFont(fileName: String, fontSize: Int, flipFont: Boolean = false): BitmapFont {
    var typeFontGenerator = fontGeneratorMapByFileName.getOrDefault(fileName, null)
    if (typeFontGenerator == null) {
        typeFontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/$fileName"))
        fontGeneratorMapByFileName[fileName] = typeFontGenerator
    }

    val typeFontParameter = FreeTypeFontParameter().apply {
        size = maxOf(fontSize, MIN_FONT_SIZE)   // The app crashes if the font size becomes too small.
        characters = FreeTypeFontGenerator.DEFAULT_CHARS + "őŐűŰ–„“”…"
        flip = flipFont
    }

    val font = typeFontGenerator.generateFont(typeFontParameter)
    font.region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    return font
}

fun loadTaskDescriptionFont(multiplier: Float = 1f): BitmapFont {
    val normalizedMultiplier = if (multiplier > 1f) {
        multiplier * (1f - (multiplier / 8))
    } else {
        multiplier
    }
    return loadFreeTypeFont("PlaywriteGBS.ttf", (TASK_DESC_DEFAULT_FONT_SIZE * normalizedMultiplier).toInt())
//    return loadFreeTypeFont("libre-baskerville.regular.ttf", (TASK_DESC_DEFAULT_FONT_SIZE * scale).toInt())
}

fun loadTaskCounterFont(multiplier: Float = 1f): BitmapFont {
    return loadFreeTypeFont("PlaywriteGBS.ttf", (TASK_DESC_DEFAULT_FONT_SIZE * 1.25f * multiplier).toInt())
}

fun loadMenuFont(multiplier: Float = 1f): BitmapFont {
    return loadFreeTypeFont("Roboto-Regular.ttf", (MENU_DEFAULT_FONT_SIZE * normalizeMultiplier(multiplier).toInt()))
}

fun loadManualFont(multiplier: Float = 1f): BitmapFont {
    return loadFreeTypeFont("Roboto-Regular.ttf", (MANUAL_DEFAULT_FONT_SIZE * multiplier.toInt()))
}

fun loadUIFont(multiplier: Float = 1f): BitmapFont {
    val normalizedMultiplier = if (multiplier > 1f) {
        multiplier * (1f - (multiplier / 7))
    } else {
        multiplier
    }
    return loadFreeTypeFont("Roboto-Regular.ttf", (UI_DEFAULT_FONT_SIZE * normalizedMultiplier).toInt())
}

fun loadPuzzleBaseFont(multiplier: Float = 1f): BitmapFont {
    return loadFreeTypeFont("libre-baskerville.regular.ttf", PUZZLE_BASE_FONT_SIZE, true)
}

fun loadPuzzleTabFont(multiplier: Float = 1f): BitmapFont {
    return loadFreeTypeFont("libre-baskerville.regular.ttf", PUZZLE_TAB_FONT_SIZE, true)
}

private fun normalizeMultiplier(multiplier: Float): Float {
    return if (multiplier > 1f) {
        multiplier * (1f - (multiplier / 8))
    } else {
        multiplier
    }
}
