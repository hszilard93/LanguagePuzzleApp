/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.b4kancs.languagePuzzleApp.app.other.gdxSmartFontMaster

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData
import com.badlogic.gdx.graphics.g2d.PixmapPacker


/** A utility to output BitmapFontData to a FNT file. This can be useful for caching the result from TrueTypeFont, for faster load
 * times.
 *
 * The font format is from the AngelCodeFont BMFont tool.
 *
 * @author mattdesl AKA davedes
 */
/**
 * This file is 'borrowed' from gdx-tools in the libgdx source
 */
object BitmapFontWriter {
    /** The output format  */
    private var format = OutputFormat.Text

    var outputFormat: OutputFormat?
        /** Returns the currently used output format.
         * @return the output format
         */
        get() = format
        /** Sets the AngelCodeFont output format for subsequent writes; can be text (for LibGDX) or XML (for other engines, like
         * Pixi.js).
         *
         * @param fmt the output format to use
         */
        set(fmt) {
            if (fmt == null) throw NullPointerException("format cannot be null")
            format = fmt
        }

    private fun quote(params: Any, spaceAfter: Boolean = false): String {
        return if (outputFormat == OutputFormat.XML) "\"" + params.toString()
            .trim { it <= ' ' } + "\"" + (if (spaceAfter) " " else "")
        else params.toString()
    }

    /** Writes the given BitmapFontData to a file, using the specified <tt>pageRefs</tt> strings as the image paths for each texture
     * page. The glyphs in BitmapFontData have a "page" id, which references the index of the pageRef you specify here.
     *
     * The FontInfo parameter is useful for cleaner output; such as including a size and font face name hint. However, it can be
     * null to use default values. Ultimately, LibGDX ignores the "info" line when reading back fonts.
     *
     * Likewise, the scaleW and scaleH are only for cleaner output. They are currently ignored by LibGDX's reader. For maximum
     * compatibility with other BMFont tools, you should use the width and height of your texture pages (each page should be the
     * same size).
     *
     * @param fontData the bitmap font
     * @param pageRefs the references to each texture page image file, generally in the same folder as outFntFile
     * @param outFntFile the font file to save to (typically ends with '.fnt')
     * @param info the optional info for the file header; can be null
     * @param scaleW the width of your texture pages
     * @param scaleH the height of your texture pages
     */
    fun writeFont(fontData: BitmapFontData, pageRefs: Array<String?>, outFntFile: FileHandle, info: FontInfo?, scaleW: Int, scaleH: Int) {
        var info = info
        if (info == null) {
            info = FontInfo()
            info.face = outFntFile.nameWithoutExtension()
        }

        val lineHeight = fontData.lineHeight.toInt()
        val pages = pageRefs.size
        val packed = 0
        val base = ((fontData.capHeight) + (if (fontData.flipped) -fontData.ascent else fontData.ascent)).toInt()
        val fmt = outputFormat!!
        val xml = fmt == OutputFormat.XML

        val buf = StringBuilder()

        if (xml) {
            buf.append("<font>\n")
        }
        val xmlOpen = if (xml) "\t<" else ""
        val xmlCloseSelf = if (xml) "/>" else ""
        val xmlTab = if (xml) "\t" else ""
        val xmlClose = if (xml) ">" else ""

        val xmlQuote = if (xml) "\"" else ""
        val alphaChnlParams =
            if (xml) " alphaChnl=\"0\" redChnl=\"0\" greenChnl=\"0\" blueChnl=\"0\""
            else " alphaChnl=0 redChnl=0 greenChnl=0 blueChnl=0"

        //INFO LINE
        buf.append(xmlOpen)
            .append("info face=\"")
            .append(if (info.face == null) "" else info.face!!.replace("\"".toRegex(), "'"))
            .append("\" size=").append(quote(info.size))
            .append(" bold=").append(quote(if (info.bold) 1 else 0))
            .append(" italic=").append(quote(if (info.italic) 1 else 0))
            .append(" charset=\"").append(if (info.charset == null) "" else info.charset)
            .append("\" unicode=").append(quote(if (info.unicode) 1 else 0))
            .append(" stretchH=").append(quote(info.stretchH))
            .append(" smooth=").append(quote(if (info.smooth) 1 else 0))
            .append(" aa=").append(quote(info.aa))
            .append(" padding=")
            .append(xmlQuote)
            .append(info.padding.up).append(",")
            .append(info.padding.down).append(",")
            .append(info.padding.left).append(",")
            .append(info.padding.right)
            .append(xmlQuote)
            .append(" spacing=")
            .append(xmlQuote)
            .append(info.spacing.horizontal).append(",")
            .append(info.spacing.vertical)
            .append(xmlQuote)
            .append(xmlCloseSelf)
            .append("\n")

        //COMMON line
        buf.append(xmlOpen)
            .append("common lineHeight=").append(quote(lineHeight))
            .append(" base=").append(quote(base))
            .append(" scaleW=").append(quote(scaleW))
            .append(" scaleH=").append(quote(scaleH))
            .append(" pages=").append(quote(pages))
            .append(" packed=").append(quote(packed))
            .append(alphaChnlParams)
            .append(xmlCloseSelf)
            .append("\n")

        if (xml) buf.append("\t<pages>\n")

        //PAGES
        for (i in pageRefs.indices) {
            buf.append(xmlTab)
                .append(xmlOpen)
                .append("page id=")
                .append(quote(i))
                .append(" file=\"")
                .append(pageRefs[i])
                .append("\"")
                .append(xmlCloseSelf)
                .append("\n")
        }

        if (xml) buf.append("\t</pages>\n")

        //CHARS
        val glyphs = com.badlogic.gdx.utils.Array<BitmapFont.Glyph>(256)
        for (i in fontData.glyphs.indices) {
            if (fontData.glyphs[i] == null) continue

            for (j in fontData.glyphs[i].indices) {
                if (fontData.glyphs[i][j] != null) {
                    glyphs.add(fontData.glyphs[i][j])
                }
            }
        }

        buf.append(xmlOpen)
            .append("chars count=").append(quote(glyphs.size))
            .append(xmlClose)
            .append("\n")

        //CHAR definitions
        for (i in 0 until glyphs.size) {
            val g = glyphs[i]
            buf.append(xmlTab)
                .append(xmlOpen)
                .append("char id=")
                .append(quote(String.format("%-5s", g.id), true))
                .append("x=").append(quote(String.format("%-5s", g.srcX), true))
                .append("y=").append(quote(String.format("%-5s", g.srcY), true))
                .append("width=").append(quote(String.format("%-5s", g.width), true))
                .append("height=").append(quote(String.format("%-5s", g.height), true))
                .append("xoffset=").append(quote(String.format("%-5s", g.xoffset), true))
                .append("yoffset=").append(quote(String.format("%-5s", if (fontData.flipped) g.yoffset else -(g.height + g.yoffset)), true))
                .append("xadvance=").append(quote(String.format("%-5s", g.xadvance), true))
                .append("page=").append(quote(String.format("%-5s", g.page), true))
                .append("chnl=").append(quote(0, true))
                .append(xmlCloseSelf)
                .append("\n")
        }

        if (xml) buf.append("\t</chars>\n")

        //KERNINGS
        var kernCount = 0
        val kernBuf = StringBuilder()
        for (i in 0 until glyphs.size) {
            for (j in 0 until glyphs.size) {
                val first = glyphs[i]
                val second = glyphs[j]
                val kern = first.getKerning(second.id.toChar())
                if (kern != 0) {
                    kernCount++
                    kernBuf.append(xmlTab)
                        .append(xmlOpen)
                        .append("kerning first=").append(quote(first.id))
                        .append(" second=").append(quote(second.id))
                        .append(" amount=").append(quote(kern, true))
                        .append(xmlCloseSelf)
                        .append("\n")
                }
            }
        }

        //KERN info
        buf.append(xmlOpen)
            .append("kernings count=").append(quote(kernCount))
            .append(xmlClose)
            .append("\n")
        buf.append(kernBuf)

        if (xml) {
            buf.append("\t</kernings>\n")
            buf.append("</font>")
        }

        var charset = info.charset
        if (charset != null && charset.length == 0) charset = null

        outFntFile.writeString(buf.toString(), false, charset)
    }


    /** A utility method which writes the given font data to a file.
     *
     * The specified pixmaps are written to the parent directory of <tt>outFntFile</tt>, using that file's name without an
     * extension for the PNG file name(s).
     *
     * The specified FontInfo is optional, and can be null.
     *
     * Typical usage looks like this:
     *
     * <pre>
     * BitmapFontWriter.writeFont(myFontData, myFontPixmaps, Gdx.files.external(&quot;fonts/output.fnt&quot;), new FontInfo(&quot;Arial&quot;, 16));
    </pre> *
     *
     * @param fontData the font data
     * @param pages the pixmaps to write as PNGs
     * @param outFntFile the output file for the font definition
     * @param info the optional font info for the header file, can be null
     */
    fun writeFont(fontData: BitmapFontData, pages: Array<Pixmap?>, outFntFile: FileHandle, info: FontInfo?) {
        val pageRefs = writePixmaps(pages, outFntFile.parent(), outFntFile.nameWithoutExtension())

        //write the font data
        writeFont(fontData, pageRefs, outFntFile, info, pages[0]!!.width, pages[0]!!.height)
    }

    /** A utility method to write the given array of pixmaps to the given output directory, with the specified file name. If the
     * pages array is of length 1, then the resulting file ref will look like: "fileName.png".
     *
     * If the pages array is greater than length 1, the resulting file refs will be appended with "_N", such as "fileName_0.png",
     * "fileName_1.png", "fileName_2.png" etc.
     *
     * The returned string array can then be passed to the <tt>writeFont</tt> method.
     *
     * Note: None of the pixmaps will be disposed.
     *
     * @param pages the pages of pixmap data to write
     * @param outputDir the output directory
     * @param fileName the file names for the output images
     * @return the array of string references to be used with <tt>writeFont</tt>
     */
    fun writePixmaps(pages: Array<Pixmap?>?, outputDir: FileHandle, fileName: String): Array<String?> {
        require(!(pages == null || pages.size == 0)) { "no pixmaps supplied to BitmapFontWriter.write" }

        val pageRefs = arrayOfNulls<String>(pages.size)

        for (i in pages.indices) {
            val ref = if (pages.size == 1) ("$fileName.png") else (fileName + "_" + i + ".png")

            //the ref for this image
            pageRefs[i] = ref

            //write the PNG in that directory
            PixmapIO.writePNG(outputDir.child(ref), pages[i])
        }
        return pageRefs
    }

    /** A convenience method to write pixmaps by page; typically returned from a PixmapPacker when used alongside
     * FreeTypeFontGenerator.
     *
     * @param pages the pages containing the Pixmaps
     * @param outputDir the output directory
     * @param fileName the file name
     * @return the file refs
     */
    fun writePixmaps(pages: com.badlogic.gdx.utils.Array<PixmapPacker.Page>, outputDir: FileHandle, fileName: String): Array<String?> {
        val pix = arrayOfNulls<Pixmap>(pages.size)
        for (i in 0 until pages.size) {
            pix[i] = pages[i].pixmap
        }
        return writePixmaps(pix, outputDir, fileName)
    }

    /** The output format.  */
    enum class OutputFormat {
        /** AngelCodeFont text format  */
        Text,

        /** AngelCodeFont XML format  */
        XML
    }

    /** The Padding parameter for FontInfo.  */
    class Padding {
        var up: Int = 0
        var down: Int = 0
        var left: Int = 0
        var right: Int = 0

        constructor()

        constructor(up: Int, down: Int, left: Int, right: Int) {
            this.up = up
            this.down = down
            this.left = left
            this.right = right
        }
    }

    /** The spacing parameter for FontInfo.  */
    class Spacing {
        var horizontal: Int = 0
        var vertical: Int = 0
    }

    /** The font "info" line; this will be ignored by LibGDX's BitmapFont reader,
     * but useful for clean and organized output.  */
    class FontInfo {
        /** Face name  */
        var face: String? = null

        /** Font size (pt)  */
        var size: Int = 12

        /** Whether the font is bold  */
        var bold: Boolean = false

        /** Whether the font is italic  */
        var italic: Boolean = false

        /** The charset; or null/empty for default  */
        var charset: String? = null

        /** Whether the font uses unicode glyphs  */
        var unicode: Boolean = true

        /** Stretch for height; default to 100%  */
        var stretchH: Int = 100

        /** Whether smoothing is applied  */
        var smooth: Boolean = true

        /** Amount of anti-aliasing that was applied to the font  */
        var aa: Int = 2

        /** Padding that was applied to the font  */
        var padding: Padding = Padding()

        /** Horizontal/vertical spacing that was applied to font  */
        var spacing: Spacing = Spacing()
        var outline: Int = 0

        constructor()

        constructor(face: String?, size: Int) {
            this.face = face
            this.size = size
        }
    }
}
