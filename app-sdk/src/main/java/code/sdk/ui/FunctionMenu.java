package code.sdk.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import code.sdk.R;
import code.sdk.core.Constant;
import code.sdk.common.ScreenUtil;
import code.sdk.drawable.Drawables;
import code.sdk.util.ImageUtil;


public class FunctionMenu extends RelativeLayout implements View.OnTouchListener {
    public static final String TAG = FunctionMenu.class.getSimpleName();

    private final int THRESHOLD_DRAGGING = 2; // px
    private final int MENU_BUTTON_SIZE = ScreenUtil.dp2px(40);

    @Constant.HoverMenuState
    private String mMenuState = Constant.STATE_DOCKED;
    @Constant.HoverMenuDockType
    private String mMenuDockType = Constant.DOCK_LEFT;

    private View mMenuLayout;
    private ImageView mMenuButton;
    private View mMenuPlaceholder;
    private View mMenuRefreshButton;
    private View mMenuCloseButton;

    private OnMenuClickListener mListener;
    private int mScreenWidth;
    private int mScreenHeight;

    private float mTouchStartX;
    private float mTouchStartY;


    public FunctionMenu(@NonNull Context context) {
        this(context, null);
    }

    public FunctionMenu(@NonNull Context context, @Nullable AttributeSet attrs) {

        super(context, attrs);
        init(context);
        setOnTouchListener(this);
    }

    private void init(Context context) {
        View view = createView(context);
        mMenuLayout = view;
        addView(view);
        //ObfuscationStub2.inject();

        updateMenuByState();
    }


    private View createView(Context context) {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, MENU_BUTTON_SIZE));
        linearLayout.setBackground(ImageUtil.base64ToDrawable(getResources(), Drawables.MENU_EXPANDED_BG));
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        mMenuButton = createImageView(context, Drawables.MENU_SHRINKED);
        linearLayout.addView(mMenuButton);

        mMenuPlaceholder = new View(context);
        mMenuPlaceholder.setLayoutParams(new LayoutParams(ScreenUtil.dp2px(10), 0));
        linearLayout.addView(mMenuPlaceholder);

        mMenuRefreshButton = createImageView(context, Drawables.MENU_REFRESH);
        mMenuRefreshButton.setOnClickListener(this::onRefreshClick);
        linearLayout.addView(mMenuRefreshButton);

        mMenuCloseButton = createImageView(context, Drawables.MENU_CLOSE);
        mMenuCloseButton.setOnClickListener(this::onCloseClick);
        linearLayout.addView(mMenuCloseButton);

        return linearLayout;
    }

    private ImageView createImageView(Context context, String base64) {
        ImageView imageView = new ImageView(context);
        imageView.setImageBitmap(ImageUtil.base64ToBitmap(base64));
        imageView.setLayoutParams(new LinearLayout.LayoutParams(MENU_BUTTON_SIZE, MENU_BUTTON_SIZE));
        return imageView;
    }

    public void setMenuListener(OnMenuClickListener mListener) {
        this.mListener = mListener;
    }

    public void show() {
        Activity activity = (Activity) getContext();
        View decorView = activity.getWindow().getDecorView();
        FrameLayout contentLayout = decorView.findViewById(android.R.id.content);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentLayout.addView(this, layoutParams);

        //ObfuscationStub3.inject();

        resetPositionDelayed();
    }

    public void hide() {
        Activity activity = (Activity) getContext();
        View decorView = activity.getWindow().getDecorView();
        FrameLayout contentLayout = decorView.findViewById(android.R.id.content);
        contentLayout.removeView(this);
    }

    private void updateMenuByState() {
        switch (mMenuState) {
            case Constant.STATE_DOCKED:
            case Constant.STATE_DRAGGING:
                setScaleX(1);
                mMenuLayout.setBackgroundResource(0);
                mMenuButton.setImageBitmap(ImageUtil.base64ToBitmap(Drawables.MENU_SHRINKED));
                mMenuPlaceholder.setVisibility(GONE);
                mMenuRefreshButton.setVisibility(GONE);
                mMenuCloseButton.setVisibility(GONE);
                //ObfuscationStub4.inject();
                break;
            case Constant.STATE_EXPANDED:
                setScaleX(Constant.DOCK_LEFT.equals(mMenuDockType) ? 1 : -1);
                mMenuLayout.setBackground(ImageUtil.base64ToDrawable(getResources(), Drawables.MENU_EXPANDED_BG));
                mMenuButton.setImageBitmap(ImageUtil.base64ToBitmap(Drawables.MENU_EXPANDED));
                mMenuPlaceholder.setVisibility(INVISIBLE);
                mMenuRefreshButton.setVisibility(VISIBLE);
                mMenuCloseButton.setVisibility(VISIBLE);
                //ObfuscationStub5.inject();
                break;
        }
    }

    public void resetPositionDelayed() {
        post(() -> {
            //ObfuscationStub6.inject();
            initScreenSize();
            resetXByDockType();
            setY((int) (mScreenHeight * 0.35f));
        });
    }

    private void initScreenSize() {
        //ObfuscationStub7.inject();

        int[] screenSize = ScreenUtil.getScreenSize();
        mScreenWidth = screenSize[0];
        mScreenHeight = screenSize[1];
    }

    private void resetXByDockType() {
        //ObfuscationStub8.inject();
        switch (mMenuDockType) {
            case Constant.DOCK_LEFT:
                setX(0);
                break;
            case Constant.DOCK_RIGHT:
                post(() -> setX(mScreenWidth - getWidth()));
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouchStartX = x;
                mTouchStartY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaX = x - mTouchStartX;
                float deltaY = y - mTouchStartY;
                if (Math.abs(deltaX) > THRESHOLD_DRAGGING
                        && Math.abs(deltaY) > THRESHOLD_DRAGGING) {
                    mMenuState = Constant.STATE_DRAGGING;
                    updateMenuByState();

                    setX(x - MENU_BUTTON_SIZE);
                    setY(y - MENU_BUTTON_SIZE);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (Constant.STATE_DOCKED.equals(mMenuState)) {
                    mMenuState = Constant.STATE_EXPANDED;
                } else if (Constant.STATE_DRAGGING.equals(mMenuState)) {
                    mMenuState = Constant.STATE_DOCKED;
                } else if (Constant.STATE_EXPANDED.equals(mMenuState)) {
                    mMenuState = Constant.STATE_DOCKED;
                }
                updateMenuByState();

                if (x <= mScreenWidth / 2.0f) {
                    mMenuDockType = Constant.DOCK_LEFT;
                } else {
                    mMenuDockType = Constant.DOCK_RIGHT;
                }
                resetXByDockType();

                mTouchStartX = mTouchStartY = 0;
                return true;
            case MotionEvent.ACTION_CANCEL:
                mMenuState = Constant.STATE_DOCKED;
                updateMenuByState();

                mTouchStartX = mTouchStartY = 0;
                return false;
            default:
                return false;
        }
    }

    @Override
    public void setX(float x) {
        if (x < 0) {
            x = 0;
        }
        super.setX(x);
    }

    @Override
    public void setY(float y) {
        if (y < 0) {
            y = 0;
        }
        super.setY(y);
    }

    public void onRefreshClick(View view) {
        //ObfuscationStub0.inject();

        if (mListener != null) {
            mListener.onRefresh();
        }
    }

    public void onCloseClick(View view) {
        //ObfuscationStub1.inject();

        if (mListener != null) {
            mListener.onClose();
        }
    }

    public interface OnMenuClickListener {
        void onRefresh();

        void onClose();
    }
}
