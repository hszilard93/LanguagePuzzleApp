package edu.b4kancs.languagePuzzleApp.app.other.gdxSmartFontMaster

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData
import com.badlogic.gdx.graphics.g2d.PixmapPacker
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException

/*
GdxSmartFontMaster by jrenner is available at https://github.com/jrenner/gdx-smart-font.
The modified version of this library is used in this application to prerender .ttf font files into .fnt files.
This is necessary because TeaVM didn't work with runtime font generation and other solution used to convert font files
produced bad results.
*/

class SmartFontGenerator // size of atlas pages for font pngs
{
    // GETTERS, SETTERS -----------------------
    var forceGeneration: Boolean = false
    /** @see SmartFontGenerator.setGeneratedFontDir
     */
    /** Set directory for storing generated fonts  */
    var generatedFontDir: String = "generated-fonts/"
    /** @see SmartFontGenerator.setReferenceScreenWidth
     */
    /** Set the reference screen width for computing sizes.  If reference width is 1280, and screen width is 1280
     * Then the fontSize paramater will be unaltered when creating a font.  If the screen width is 720, the font size
     * will by scaled down to (720 / 1280) of original size.  */
    var referenceScreenWidth: Int = 1280
    /** @see SmartFontGenerator.setPageSize
     */
    /** Set the width and height of the png files to which the fonts will be saved.
     * In the future it would be nice for page size to be automatically set to the optimal size
     * by the font generator.  In the mean time it must be set manually.  */
    // TODO figure out optimal page size automatically
    var pageSize: Int = 512


    /** Will load font from file. If that fails, font will be generated and saved to file.
     * @param fontFile the actual font (.otf, .ttf)
     * @param fontName the name of the font, i.e. "arial-small", "arial-large", "monospace-10"
     * This will be used for creating the font file names
     * @param fontSize size of font when screen width equals referenceScreenWidth
     */
    fun createFont(fontFile: FileHandle, fontName: String, fontSize: Int, flip: Boolean = false, forceGenerate: Boolean = false): BitmapFont {
        this.forceGeneration = forceGenerate

        var font: BitmapFont? = null
        // if fonts are already generated, just load from file
        val fontPrefs = Gdx.app.getPreferences("org.jrenner.smartfont")
        val displayWidth = fontPrefs.getInteger("display-width", 0)
        val displayHeight = fontPrefs.getInteger("display-height", 0)
        var loaded = false
        if (displayWidth != Gdx.graphics.width || displayHeight != Gdx.graphics.height) {
            Gdx.app.debug(TAG, "Screen size change detected, regenerating fonts")
        }
        else {
            try {
                // try to load from file
                Gdx.app.debug(TAG, "Loading generated font from file cache")
                font = BitmapFont(getFontFile("$fontName.fnt", fontSize))
                loaded = true
            } catch (e: GdxRuntimeException) {
                Gdx.app.error(TAG, e.message)
                Gdx.app.debug(TAG, "Couldn't load pre-generated fonts. Will generate fonts.")
            }
        }
        if (!loaded || forceGeneration) {
            forceGeneration = false
            val width = Gdx.graphics.width.toFloat()
            val ratio = width / referenceScreenWidth // use 1920x1280 as baseline, arbitrary
            val baseSize = 28f // for 28 sized fonts at baseline width above

            // store screen width for detecting screen size change
            // on later startups, which will require font regeneration
            fontPrefs.putInteger("display-width", Gdx.graphics.width)
            fontPrefs.putInteger("display-height", Gdx.graphics.height)
            fontPrefs.flush()

            font = generateFontWriteFiles(fontName, fontFile, fontSize, pageSize, pageSize, flip)
        }
        return font!!
    }

    /** Convenience method for generating a font, and then writing the fnt and png files.
     * Writing a generated font to files allows the possibility of only generating the fonts when they are missing, otherwise
     * loading from a previously generated file.
     * @param fontFile
     * @param fontSize
     */
    private fun generateFontWriteFiles(fontName: String, fontFile: FileHandle, fontSize: Int, pageWidth: Int, pageHeight: Int, flip: Boolean): BitmapFont {
        val generator = FreeTypeFontGenerator(fontFile)

        val packer = PixmapPacker(pageWidth, pageHeight, Pixmap.Format.RGBA8888, 2, false)
        val parameter = FreeTypeFontParameter()
        parameter.size = fontSize
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + "\u0151\u0150\u0171\u0170"
        parameter.flip = flip
        parameter.packer = packer
        val fontData = generator.generateData(parameter)
        val pages = packer.pages
        val texRegions = Array<TextureRegion>()
        for (i in 0 until pages.size) {
            val p = pages[i]
            val tex: Texture = object : Texture(
                PixmapTextureData(p.pixmap, p.pixmap.format, false, false, true)
            ) {
                override fun dispose() {
                    super.dispose()
                    textureData.consumePixmap().dispose()
                }
            }
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            texRegions.add(TextureRegion(tex))
        }
        val font = BitmapFont(fontData as BitmapFontData, texRegions, false)
        saveFontToFile(font, fontSize, fontName, packer)
        generator.dispose()
        packer.dispose()
        return font
    }

    private fun saveFontToFile(font: BitmapFont, fontSize: Int, fontName: String, packer: PixmapPacker) {
        val fontFile = getFontFile("$fontName.fnt", fontSize) // .fnt path
        val pixmapDir = getFontFile(fontName, fontSize) // png dir path
        BitmapFontWriter.outputFormat = BitmapFontWriter.OutputFormat.Text

        val pageRefs = BitmapFontWriter.writePixmaps(packer.pages, pixmapDir, fontName)
        Gdx.app.debug(TAG, String.format("Saving font [%s]: fontfile: %s, pixmapDir: %s\n", fontName, fontFile, pixmapDir))
        // here we must add the png dir to the page refs
        for (i in pageRefs.indices) {
            pageRefs[i] = fontSize.toString() + "_" + fontName + "/" + pageRefs[i]
        }
        BitmapFontWriter.writeFont(font.data, pageRefs, fontFile, BitmapFontWriter.FontInfo(fontName, fontSize), 1, 1)
    }

    private fun getFontFile(filename: String, fontSize: Int): FileHandle {
        return Gdx.files.local(generatedFontDir + fontSize + "_" + filename)
    }

    companion object {
        private const val TAG = "SmartFontGenerator"
    }
}
