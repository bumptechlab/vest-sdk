package code.sdk.core

import androidx.annotation.StringDef

object Constant {
   private val TAG = Constant::class.java.simpleName

    /* target country iso code (Alpha-2 lowcase) */
    const val TARGET_COUNTRY_INDIA = "in"
    const val TARGET_COUNTRY_BRAZIL = "br"
    const val TARGET_COUNTRY_INDONESIA = "id"
    const val TARGET_COUNTRY_VIETNAM = "vn"
    const val DELAY_FOR_DEVICE_ID_ASYNC = 1500L
    const val SELF = 200
    const val OTHER_CODE = 1

    /* url query params */
    const val QUERY_PARAM_ORIENTATION = "orientation"
    const val QUERY_PARAM_HOVER_MENU = "hoverMenu"
    const val QUERY_PARAM_NAV_BAR = "navBar"
    const val QUERY_PARAM_SAFE_CUTOUT = "safeCutout"

    /* screen orientation */
    const val LANDSCAPE = "landscape"
    const val PORTRAIT = "portrait"
    const val UNSPECIFIED = "unspecified"

    /* hover menu */
    const val DOCK_LEFT = "dockLeft"
    const val DOCK_RIGHT = "dockRight"
    const val STATE_DOCKED = "stateDocked"
    const val STATE_DRAGGING = "stateDragging"
    const val STATE_EXPANDED = "stateExpanded"

    @StringDef(PORTRAIT, LANDSCAPE, UNSPECIFIED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ScreenOrientation

    @StringDef(DOCK_LEFT, DOCK_RIGHT)
    @Retention(AnnotationRetention.SOURCE)
    annotation class HoverMenuDockType

    @StringDef(STATE_DOCKED, STATE_DRAGGING, STATE_EXPANDED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class HoverMenuState
}
