package edu.b4kancs.languagePuzzleApp.app.lwjgl3.view.ui

import com.badlogic.gdx.files.FileHandle
import edu.b4kancs.languagePuzzleApp.app.view.ui.FilePickerInterface
import java.awt.FileDialog
import java.awt.Frame
import javax.swing.SwingUtilities

class FilePickerDesktopImpl : FilePickerInterface {

    override fun openFileChooser(callback: (FileHandle) -> Unit) {

        // Run on the AWT Event Dispatch Thread
        SwingUtilities.invokeLater {
            // Create a hidden AWT Frame to act as the parent for FileDialog
            val frame = Frame()
            frame.isUndecorated = true
            frame.isVisible = false
            val fileDialog = FileDialog(frame, "Select File", FileDialog.LOAD).apply {
                isMultipleMode = false
                // Optional: Set file filters (extensions)
                // filenames must match case; for more flexibility, additional handling is needed
                // file = "png" // Not directly supported; AWT's FileDialog has limited filtering
                // AWT's FileDialog does not support setting filters directly like JFileChooser
                // To implement filtering, you might need to handle it manually after selection
                // For simplicity, we'll proceed without filters
                directory = System.getProperty("user.home") // Optional: Set default directory
                isVisible = true
            }

            fileDialog.file?.let {
                val selectedFile = FileHandle(fileDialog.directory + java.io.File.separator + fileDialog.file)
                callback(selectedFile)
            }
        }
    }
}
