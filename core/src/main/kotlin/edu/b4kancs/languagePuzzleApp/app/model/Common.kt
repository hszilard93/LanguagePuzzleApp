package edu.b4kancs.languagePuzzleApp.app.model

import com.badlogic.gdx.graphics.Color
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat

enum class CustomColors(val value: Color) {
    SUBJECT_GREEN(Color(0x4bad5bff)),
    OBJECT_YELLOW( Color(250.toRGBFloat(), 184.toRGBFloat(), 45.toRGBFloat(), 1f)),
    ADVERB_PURPLE(Color(163.toRGBFloat(), 135.toRGBFloat(), 189.toRGBFloat(), 1f)),
    OFF_WHITE(Color(230.toRGBFloat(), 230.toRGBFloat(), 230.toRGBFloat(), 1f));
}
