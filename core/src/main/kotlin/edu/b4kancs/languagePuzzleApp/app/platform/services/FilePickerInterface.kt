package edu.b4kancs.languagePuzzleApp.app.platform.services

import com.badlogic.gdx.files.FileHandle

interface FilePickerInterface {

    fun openFileChooser(callback: (FileHandle) -> Unit)
}
