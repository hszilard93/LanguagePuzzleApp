package edu.b4kancs.languagePuzzleApp.app.teavm.platform.ui

import JsInterop
import com.badlogic.gdx.files.FileHandle
import edu.b4kancs.languagePuzzleApp.app.platform.services.FilePickerInterface

class FilePickerJsImpl : FilePickerInterface {

    override fun openFileChooser(callback: (FileHandle) -> Unit) {
        JsInterop.log("openFileChooser")
        return
    }
}
