package edu.b4kancs.languagePuzzleApp.app

import com.badlogic.gdx.Gdx
import edu.b4kancs.languagePuzzleApp.app.model.LOG_LEVEL_MISC
import ktx.log.Logger
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun Logger.misc(message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (Gdx.app.logLevel >= LOG_LEVEL_MISC) Gdx.app.debug("MISC", buildMessage(message()))
}
