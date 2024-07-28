package edu.b4kancs.languagePuzzleApp.app.view.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.utils.viewport.Viewport
import com.ray3k.tenpatch.TenPatchDrawable
import edu.b4kancs.languagePuzzleApp.app.GameCamera
import edu.b4kancs.languagePuzzleApp.app.HudCamera
import edu.b4kancs.languagePuzzleApp.app.misc
import edu.b4kancs.languagePuzzleApp.app.model.GameModel
import edu.b4kancs.languagePuzzleApp.app.model.PuzzlePiece
import edu.b4kancs.languagePuzzleApp.app.view.drawableModel.PuzzlePieceDrawable
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRGBFloat
import edu.b4kancs.languagePuzzleApp.app.view.utils.toRadians
import ktx.app.KtxScreen
import ktx.assets.Asset
import ktx.assets.load
import ktx.collections.gdxArrayOf
import ktx.graphics.use
import ktx.inject.Context
import ktx.log.logger
import space.earlygrey.shapedrawer.JoinType
import space.earlygrey.shapedrawer.ShapeDrawer


class GameScreen(
    private val context: Context,
    private val batch: Batch,
    private val assetManager: AssetManager,
//    private val skin: Skin,
    private val gameCamera: GameCamera,
    private val hudCamera: HudCamera,
    private val viewport: Viewport,
    private val gameModel: GameModel
) : KtxScreen, BaseInputProcessor() {

    companion object {
        val logger = logger<GameScreen>()
    }

    private lateinit var image: Asset<Texture>
    private lateinit var localDrawer: ShapeDrawer
    private lateinit var font: BitmapFont
    private val puzzleDrawable = PuzzlePieceDrawable(context.inject(), PuzzlePiece())
    private lateinit var frameBuffer: FrameBuffer
    private lateinit var frameRegion: TextureRegion
    private val base: NinePatch
    private val blank: Texture
    private var baseWidth = 300f

    init {
//        base = skin.get("puzle_base_test_3_10P", TenPatchDrawable::class.java)
        val ninePatchTexture = Texture(Gdx.files.internal("puzzle_base_test_3.png"))
        base = NinePatch(ninePatchTexture, 32, 32, 32, 32)
        blank = Texture(Gdx.files.internal("puzzle_blank_test_3.png"))
    }

    override fun show() {
        logger.debug { "show" }

        Gdx.input.inputProcessor = this

        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, viewport.screenWidth, viewport.screenHeight, false)
        frameRegion = TextureRegion(frameBuffer.colorBufferTexture)
        frameRegion.flip(false, true)

        image = assetManager.load<Texture>("graphics/logo.png")
        font = BitmapFont(Gdx.files.internal("hud_font.fnt"))

        if (!context.contains<ShapeDrawer>()) {
            val drawer = createDrawer().apply {
                isDefaultSnap = false
            }
            localDrawer = drawer
            context.bindSingleton<ShapeDrawer>(drawer)
        }
        else {
            localDrawer = context.inject()
        }

        assetManager.finishLoading()
        super.show()
    }

    override fun render(delta: Float) {
        logger.misc { "render delta=$delta" }

        // Set the background color
        Gdx.gl.glClearColor(180.toRGBFloat(), 255.toRGBFloat(), 180.toRGBFloat(), 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

//        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
//        val maskX = ((Gdx.graphics.width / 2) - (100f / 2))
//        val maskY = ((Gdx.graphics.height / 2) - (100f / 2))
//        Gdx.gl.glScissor(100, 150, 100, 100)

//        batch.use {
//            gameCamera.update()
//            batch.projectionMatrix = gameCamera.combined
//            if (System.currentTimeMillis() / 1000 % 2 == 0L) {
//                batch.color = Color.RED
//            }
//            else {
//                batch.color = Color.WHITE
//            }
//            batch.draw(image.asset, 200f, 200f)
//            drawShape()
//            puzzleDrawable.draw(batch, 400f, 600f, 0f, 0f)

//            batch.setBlendFunction(GL20.GL_DST_ALPHA, GL20.GL_SRC_ALPHA)

//            renderHud()
//        }

        frameBuffer.use {
            batch.use {
                baseWidth += 0.5f
                base.draw(batch, 100f, 100f, baseWidth, 300f)
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ZERO)
                batch.draw(blank, 100f, 200f)
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            }
        }

        batch.use {
            gameCamera.update()
            batch.projectionMatrix = gameCamera.combined
            it.draw(frameRegion, 0f, 0f)
            renderHud()
        }
//        stage.act()
//        stage.draw()
    }

    override fun resize(newWidth: Int, newHeight: Int) {
        logger.debug { "resize newWidth=$newWidth newHeight=$newHeight" }

        val viewport = context.inject<Viewport>()
        viewport.update(newWidth, newHeight)

        frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, viewport.screenWidth, viewport.screenHeight, false)
        frameRegion = TextureRegion(frameBuffer.colorBufferTexture)
        frameRegion.flip(false, true)
//        super.resize(newWidth, newHeight)
    }

    private fun createDrawer(): ShapeDrawer {
        logger.debug { "createDrawer" }

        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.drawPixel(0, 0)
        val texture = Texture(pixmap)
        pixmap.dispose()
        val textureRegion = TextureRegion(texture, 0, 0, 1, 1)
        return ShapeDrawer(batch, textureRegion)
    }

    private fun drawShape() {
//        drawer.line(0f, 0f, 100f, 100f, 10f)
        localDrawer.setColor(Color.BLUE)
        localDrawer.path(gdxArrayOf(Vector2(200f, 200f), Vector2(350f, 500f), Vector2(700f, 500f)), 10f, JoinType.SMOOTH, true)

        localDrawer.setColor(Color.GREEN)
        localDrawer.polygon(floatArrayOf(400f, 200f, 600f, 200f, 500f, 400f), 4f, JoinType.POINTY)
        localDrawer.setColor(Color.YELLOW)
        localDrawer.arc(250f, 250f, 100f, 0f, 90f.toRadians(), 5f, false)
        localDrawer.setColor(Color.PINK)
        localDrawer.arc(350f, 350f, 100f, 0f, 90f.toRadians(), 5f, true)
    }

    private fun renderHud() {
        logger.misc { "renderHud" }

        // Use the HUD camera to render the text
        hudCamera.update()
        batch.projectionMatrix = hudCamera.combined
        font.draw(batch, "Upper left, FPS=" + Gdx.graphics.framesPerSecond, 0f, hudCamera.viewportHeight)
        font.draw(batch, "Lower left", 0f, font.lineHeight)

        if (gameModel.isDebugModeOn) {
            // Render the mouse position interpreted as real pixel coordinates within the viewport ("0, 0" is the viewports bottom left corner)
            val viewport = context.inject<Viewport>()
            val screenHeight = Gdx.graphics.height.toFloat()
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = Gdx.input.y.toFloat()
            val mouseViewportX = mouseX - viewport.leftGutterWidth
            val mouseViewportY = screenHeight - mouseY - viewport.bottomGutterHeight
            val renderVector = viewport.unproject(Vector2(mouseX, mouseY))
            if (viewport.screenWidth >= 200) {  // Catches errors when the viewport is too small or not visible (e.g. window is minimized)
                val renderX = (renderVector.x + 10f).coerceIn(10f, viewport.screenWidth - 100f)
                val renderY = (renderVector.y + 10f).coerceIn(10f, viewport.screenHeight - 10f)
                font.draw(batch, "$mouseViewportX, $mouseViewportY", renderX, renderY + 10f)
            }
        }
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        logger.debug { "touchDown button=$button" }

        if (button == Input.Buttons.RIGHT) {
            gameModel.isDebugModeOn = !gameModel.isDebugModeOn
            logger.info { "isDebugModeOn = ${gameModel.isDebugModeOn}" }
        }
        return true
    }

    override fun keyDown(keycode: Int): Boolean {
        logger.debug { "keyDown keycode=$keycode" }
        return true
    }
}
