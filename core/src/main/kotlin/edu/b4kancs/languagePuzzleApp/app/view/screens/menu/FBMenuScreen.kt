package edu.b4kancs.languagePuzzleApp.app.view.screens.menu

import edu.b4kancs.languagePuzzleApp.app.Game
import ktx.inject.Context
import ktx.log.logger

class FBMenuScreen(
    context: Context,
    game: Game
) : AbstractExerciseMenuScreen(context, game, FILE_LIST_PATH) { // Pass fileListPath to superclass

    companion object {
        val logger = logger<FBMenuScreen>()
        private const val FILE_LIST_PATH = "tasks/fb_task_list.txt"
    }

    override val screenLogger = logger // Provide logger instance for AbstractMenuScreen
    override val tasksWebPath = "tasks/fb"
    override val tasksDesktopPath = "assets/tasks/fb"
    override val menuButtonWidth: Float = 600f
}
