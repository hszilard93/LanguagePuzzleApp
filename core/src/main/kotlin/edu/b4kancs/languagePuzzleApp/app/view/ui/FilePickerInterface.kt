package edu.b4kancs.languagePuzzleApp.app.view.ui

import com.badlogic.gdx.files.FileHandle

interface FilePickerInterface {

    fun openFileChooser(callback: (FileHandle) -> Unit)
}
