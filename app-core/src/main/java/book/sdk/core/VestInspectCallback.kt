package book.sdk.core

interface VestInspectCallback {
    /**
     * Implement this method to launch A-side activity
     *
     * @param reason the reason why show vest game
     * see constant at class [VestInspectResult]
     */
    fun onShowASide(reason: Int)

    /**
     * don't need implement this method to launch B-side Activity
     * vest-sdk will launch B-side Activity for you
     * you can just finish your splash Activity in this callback
     *
     * @param url url of official game
     * @param launchResult return true if launch B-side Activity successfully
     */
    fun onShowBSide(url: String, launchResult: Boolean)
}
