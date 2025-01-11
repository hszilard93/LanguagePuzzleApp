package edu.b4kancs.languagePuzzleApp.app.view.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter

data class PuzzleFontHolder(val baseFont: BitmapFont, val tabFont: BitmapFont)

data class TaskFontHolder(val descriptionFont: BitmapFont, val counterFont: BitmapFont)

data class HudFontHolder(val font: BitmapFont)

data class MenuFontHolder(val font: BitmapFont)

data class UIFontHolder(val font: BitmapFont)

private const val TASK_DESC_DEFAULT_FONT_SIZE = 24
private const val MENU_DEFAULT_FONT_SIZE = 24
private const val UI_DEFAULT_FONT_SIZE = 22
private const val PUZZLE_BASE_FONT_SIZE = 40
private const val PUZZLE_TAB_FONT_SIZE = (PUZZLE_BASE_FONT_SIZE * 0.75f).toInt()

// Caching the FreeTypeFontGenerator objects to save on IO
private val fontGeneratorMapByFileName = mutableMapOf<String, FreeTypeFontGenerator>()

fun loadFreeTypeFont(fileName: String, fontSize: Int, flipFont: Boolean = false): BitmapFont {

    var typeFontGenerator = fontGeneratorMapByFileName.getOrDefault(fileName, null)
    if (typeFontGenerator == null) {
        typeFontGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/$fileName"))
        fontGeneratorMapByFileName[fileName] = typeFontGenerator
    }

    val typeFontParameter = FreeTypeFontParameter().apply {
        size = fontSize
        characters = FreeTypeFontGenerator.DEFAULT_CHARS + "őŐűŰ"
        flip = flipFont
    }
    val font = typeFontGenerator.generateFont(typeFontParameter)
    return font
}

fun loadTaskDescriptionFont(): BitmapFont {
    return loadFreeTypeFont("PlaywriteGBS.ttf", TASK_DESC_DEFAULT_FONT_SIZE)
//    return loadFreeTypeFont("libre-baskerville.regular.ttf", (TASK_DESC_DEFAULT_FONT_SIZE * scale).toInt())
}

fun loadTaskCounterFont(): BitmapFont {
    return loadFreeTypeFont("PlaywriteGBS.ttf", (TASK_DESC_DEFAULT_FONT_SIZE * 1.25f).toInt())
}

fun loadMenuFont(): BitmapFont {
    return loadFreeTypeFont("Roboto-Regular.ttf", MENU_DEFAULT_FONT_SIZE)
}

fun loadUIFont(): BitmapFont {
    return loadFreeTypeFont("Roboto-Regular.ttf", UI_DEFAULT_FONT_SIZE)
}

fun loadPuzzleBaseFont(): BitmapFont {
    return loadFreeTypeFont("libre-baskerville.regular.ttf", PUZZLE_BASE_FONT_SIZE, true)
}

fun loadPuzzleTabFont(): BitmapFont {
    return loadFreeTypeFont("libre-baskerville.regular.ttf", PUZZLE_TAB_FONT_SIZE, true)
}
