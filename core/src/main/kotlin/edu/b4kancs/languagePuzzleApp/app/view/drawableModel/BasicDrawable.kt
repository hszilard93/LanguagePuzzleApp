package edu.b4kancs.languagePuzzleApp.app.view.drawableModel

import com.badlogic.gdx.scenes.scene2d.utils.Drawable

const val DEFAULT_PADDING = 10f

abstract class DrawablePaddingImpl(
    var topPadding: Float = DEFAULT_PADDING,
    var bottomPadding: Float = DEFAULT_PADDING,
    var leftPadding: Float = DEFAULT_PADDING,
    var rightPadding: Float = DEFAULT_PADDING
) : Drawable {
    override fun getTopHeight(): Float = topPadding

    override fun setTopHeight(topHeight: Float) {
        topPadding = topHeight
    }

    override fun getBottomHeight(): Float = bottomPadding

    override fun setBottomHeight(bottomHeight: Float) {
        bottomPadding = bottomHeight
    }

    override fun getLeftWidth(): Float = leftPadding

    override fun setLeftWidth(leftWidth: Float) {
        leftPadding = leftWidth
    }

    override fun getRightWidth(): Float = rightPadding

    override fun setRightWidth(rightWidth: Float) {
        rightPadding = rightWidth
    }
}
