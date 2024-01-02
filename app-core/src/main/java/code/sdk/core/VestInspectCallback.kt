package code.sdk.core

interface VestInspectCallback {
    /**
     * Implement this method to launch vest game
     *
     * @param reason the reason why show vest game
     * see constant at class [VestGameReason]
     */
    fun onShowVestGame(reason: Int)

    /**
     * This is just an notification of launching official game
     * don't need implementation
     *
     * @param url url of official game, open url by method
     * VestSDK.gotoGameActivity(context, url);
     */
    fun onShowOfficialGame(url: String)
}
