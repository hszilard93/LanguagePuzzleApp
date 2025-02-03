package edu.b4kancs.languagePuzzleApp.app.view.screens.menu

import ktx.inject.Context
import ktx.log.logger
import edu.b4kancs.languagePuzzleApp.app.Game

class TKMenuScreen(
    context: Context,
    game: Game
) : AbstractExerciseMenuScreen(context, game, fileListPath) { // Pass fileListPath to superclass

    companion object {
        val logger = logger<TKMenuScreen>()
        private const val fileListPath = "tasks/tk_task_list.txt"

    }

    override val screenLogger = logger // Provide logger instance for AbstractMenuScreen
    override val tasksWebPath = "tasks/tk"
    override val tasksDesktopPath = "assets/tasks/tk"
}
