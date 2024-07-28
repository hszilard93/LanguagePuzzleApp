package edu.b4kancs.languagePuzzleApp.app.teavm

import com.badlogic.gdx.Files.FileType
import com.github.xpenatan.gdx.backends.teavm.config.AssetFileHandle
import java.io.File
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuildConfiguration
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuilder
import com.github.xpenatan.gdx.backends.teavm.gen.SkipClass
import org.teavm.vm.TeaVMOptimizationLevel
import java.io.IOException

/** Builds the TeaVM/HTML application. */
//@SkipClass
//object TeaVMBuilder {
//    @JvmStatic fun main(arguments: Array<String>) {
//        val teaBuildConfiguration = TeaBuildConfiguration().apply {
//            assetsPath.add(AssetFileHandle.createCopyHandle(File("../assets"), FileType.Local))
//            webappPath = File("build/dist").canonicalPath
//            // Register any extra classpath assets here:
//            // additionalAssetsClasspathFiles += "edu/b4kancs/puzli/app/asset.extension"
//        }
//
//        // Register any classes or packages that require reflection here:
//        // TeaReflectionSupplier.addReflectionClass("edu.b4kancs.puzli.app.reflect")
//
//        val tool = TeaBuilder.config(teaBuildConfiguration)
//        tool.mainClass = "edu.b4kancs.puzli.app.teavm.TeaVMLauncher"
//        TeaBuilder.build(tool)
//    }
//}

@SkipClass
object TeaVMBuilder {
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val teaBuildConfiguration = TeaBuildConfiguration()
        val assetFileHandle = AssetFileHandle.createCopyHandle(File("../assets"), FileType.Classpath)
        teaBuildConfiguration.assetsPath.add(assetFileHandle)
        teaBuildConfiguration.webappPath = File("build/dist").canonicalPath

        // Register any extra classpath assets here:
        // teaBuildConfiguration.additionalAssetsClasspathFiles.add("com/b4kancs/libgdxtest/asset.extension");

        // Register any classes or packages that require reflection here:
        // TeaReflectionSupplier.addReflectionClass("com.b4kancs.libgdxtest.reflect");
        val tool = TeaBuilder.config(teaBuildConfiguration)
        tool.mainClass = TeaVMLauncher::class.java.name
        // For many (or most) applications, using the highest optimization won't add much to build time.
        // If your builds take too long, and runtime performance doesn't matter, you can change FULL to SIMPLE .
        tool.optimizationLevel = TeaVMOptimizationLevel.SIMPLE
        TeaBuilder.build(tool)
    }
}
