package code.sdk.ui

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import code.sdk.common.ScreenUtil.dp2px
import code.sdk.common.ScreenUtil.getScreenSize
import code.sdk.core.Constant
import code.sdk.core.Constant.HoverMenuDockType
import code.sdk.core.Constant.HoverMenuState
import code.sdk.drawable.Drawables
import code.sdk.util.ImageUtil
import kotlin.math.abs

class FunctionMenu @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    RelativeLayout(context, attrs), OnTouchListener {

   private val TAG = FunctionMenu::class.java.simpleName
    private val THRESHOLD_DRAGGING = 2 // px
    private val MENU_BUTTON_SIZE = dp2px(40f)

    @HoverMenuState
    private var mMenuState = Constant.STATE_DOCKED

    @HoverMenuDockType
    private var mMenuDockType = Constant.DOCK_LEFT
    private lateinit var mMenuLayout: View
    private lateinit var mMenuButton: ImageView
    private lateinit var mMenuPlaceholder: View
    private lateinit var mMenuRefreshButton: View
    private lateinit var mMenuCloseButton: View

    private var mListener: OnMenuClickListener? = null
    private var mScreenWidth = 0
    private var mScreenHeight = 0
    private var mTouchStartX = 0f
    private var mTouchStartY = 0f

    init {
        init(context)
        setOnTouchListener(this)
    }

    private fun init(context: Context) {
        val view = createView(context)
        mMenuLayout = view
        addView(view)
        updateMenuByState()
    }

    private fun createView(context: Context): View {
        val linearLayout = LinearLayout(context)
        linearLayout.layoutParams =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, MENU_BUTTON_SIZE)
        linearLayout.background = ImageUtil.base64ToDrawable(resources, Drawables.MENU_EXPANDED_BG)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        mMenuButton = createImageView(context, Drawables.MENU_SHRINKED)
        linearLayout.addView(mMenuButton)
        mMenuPlaceholder = View(context)
        mMenuPlaceholder.layoutParams = LayoutParams(dp2px(10f), 0)
        linearLayout.addView(mMenuPlaceholder)
        mMenuRefreshButton = createImageView(context, Drawables.MENU_REFRESH)
        mMenuRefreshButton.setOnClickListener { view: View? -> onRefreshClick(view) }
        linearLayout.addView(mMenuRefreshButton)
        mMenuCloseButton = createImageView(context, Drawables.MENU_CLOSE)
        mMenuCloseButton.setOnClickListener { view: View? -> onCloseClick(view) }
        linearLayout.addView(mMenuCloseButton)
        return linearLayout
    }

    private fun createImageView(context: Context, base64: String): ImageView {
        val imageView = ImageView(context)
        imageView.setImageBitmap(ImageUtil.base64ToBitmap(base64))
        imageView.layoutParams = LinearLayout.LayoutParams(MENU_BUTTON_SIZE, MENU_BUTTON_SIZE)
        return imageView
    }

    fun setMenuListener(mListener: OnMenuClickListener?) {
        this.mListener = mListener
    }

    fun show() {
        val activity = context as Activity
        val decorView = activity.window.decorView
        val contentLayout = decorView.findViewById<FrameLayout>(android.R.id.content)
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        contentLayout.addView(this, layoutParams)

        //ObfuscationStub3.inject();
        resetPositionDelayed()
    }

    fun hide() {
        val activity = context as Activity
        val decorView = activity.window.decorView
        val contentLayout = decorView.findViewById<FrameLayout>(android.R.id.content)
        contentLayout.removeView(this)
    }

    private fun updateMenuByState() {
        when (mMenuState) {
            Constant.STATE_DOCKED, Constant.STATE_DRAGGING -> {
                scaleX = 1f
                mMenuLayout.setBackgroundResource(0)
                mMenuButton.setImageBitmap(ImageUtil.base64ToBitmap(Drawables.MENU_SHRINKED))
                mMenuPlaceholder.visibility = GONE
                mMenuRefreshButton.visibility = GONE
                mMenuCloseButton.visibility = GONE
            }

            Constant.STATE_EXPANDED -> {
                scaleX = (if (Constant.DOCK_LEFT == mMenuDockType) 1 else -1).toFloat()
                mMenuLayout.background =
                    ImageUtil.base64ToDrawable(resources, Drawables.MENU_EXPANDED_BG)
                mMenuButton.setImageBitmap(ImageUtil.base64ToBitmap(Drawables.MENU_EXPANDED))
                mMenuPlaceholder.visibility = INVISIBLE
                mMenuRefreshButton.visibility = VISIBLE
                mMenuCloseButton.visibility = VISIBLE
            }
        }
    }

    fun resetPositionDelayed() {
        post {
            initScreenSize()
            resetXByDockType()
            y = (mScreenHeight * 0.35f).toInt().toFloat()
        }
    }

    private fun initScreenSize() {
        val screenSize = getScreenSize()
        mScreenWidth = screenSize[0]
        mScreenHeight = screenSize[1]
    }

    private fun resetXByDockType() {
        when (mMenuDockType) {
            Constant.DOCK_LEFT -> x = 0f
            Constant.DOCK_RIGHT -> post { x = (mScreenWidth - width).toFloat() }
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mTouchStartX = x
                mTouchStartY = y
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - mTouchStartX
                val deltaY = y - mTouchStartY
                if (abs(deltaX) > THRESHOLD_DRAGGING
                    && abs(deltaY) > THRESHOLD_DRAGGING
                ) {
                    mMenuState = Constant.STATE_DRAGGING
                    updateMenuByState()
                    setX(x - MENU_BUTTON_SIZE)
                    setY(y - MENU_BUTTON_SIZE)
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                if (Constant.STATE_DOCKED == mMenuState) {
                    mMenuState = Constant.STATE_EXPANDED
                } else if (Constant.STATE_DRAGGING == mMenuState) {
                    mMenuState = Constant.STATE_DOCKED
                } else if (Constant.STATE_EXPANDED == mMenuState) {
                    mMenuState = Constant.STATE_DOCKED
                }
                updateMenuByState()
                mMenuDockType = if (x <= mScreenWidth / 2.0f) {
                    Constant.DOCK_LEFT
                } else {
                    Constant.DOCK_RIGHT
                }
                resetXByDockType()
                mTouchStartY = 0f
                mTouchStartX = mTouchStartY
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                mMenuState = Constant.STATE_DOCKED
                updateMenuByState()
                mTouchStartY = 0f
                mTouchStartX = mTouchStartY
                false
            }

            else -> false
        }
    }

    override fun setX(x: Float) {
        var xx = x
        if (xx < 0) {
            xx = 0f
        }
        super.setX(xx)
    }

    override fun setY(y: Float) {
        var yy = y
        if (yy < 0) {
            yy = 0f
        }
        super.setY(yy)
    }

    fun onRefreshClick(view: View?) {
        mListener?.onRefresh()

    }

    fun onCloseClick(view: View?) {
        mListener?.onClose()
    }

    interface OnMenuClickListener {
        fun onRefresh()
        fun onClose()
    }
}
