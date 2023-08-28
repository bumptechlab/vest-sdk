package code.sdk.core;

import androidx.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Constant {
    public static final String TAG = Constant.class.getSimpleName();

    /* target country iso code (Alpha-2 lowcase) */
    public static final String TARGET_COUNTRY_INDIA = "in";
    public static final String TARGET_COUNTRY_BRAZIL = "br";
    public static final String TARGET_COUNTRY_INDONESIA = "id";
    public static final String TARGET_COUNTRY_VIETNAM = "vn";

    public static final long DELAY_FOR_DEVICE_ID_ASYNC = 1500L;


    /* url query params */
    public static final String QUERY_PARAM_ORIENTATION = "orientation";
    public static final String QUERY_PARAM_HOVER_MENU = "hoverMenu";
    public static final String QUERY_PARAM_NAV_BAR = "navBar";
    public static final String QUERY_PARAM_SAFE_CUTOUT = "safeCutout";

    /* screen orientation */
    public static final String LANDSCAPE = "landscape";
    public static final String PORTRAIT = "portrait";
    public static final String UNSPECIFIED = "unspecified";

    @StringDef({PORTRAIT, LANDSCAPE, UNSPECIFIED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScreenOrientation {
    }

    /* hover menu */
    public static final String DOCK_LEFT = "dockLeft";
    public static final String DOCK_RIGHT = "dockRight";

    @StringDef({DOCK_LEFT, DOCK_RIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HoverMenuDockType {
    }

    public static final String STATE_DOCKED = "stateDocked";
    public static final String STATE_DRAGGING = "stateDragging";
    public static final String STATE_EXPANDED = "stateExpanded";

    @StringDef({STATE_DOCKED, STATE_DRAGGING, STATE_EXPANDED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HoverMenuState {
    }

}
