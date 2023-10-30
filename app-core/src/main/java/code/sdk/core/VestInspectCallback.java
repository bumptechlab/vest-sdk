package code.sdk.core;

public interface VestInspectCallback {

    /**
     * Implement this method to launch vest game
     *
     * @param reason the reason why show vest game
     *               see constant at class {@link VestGameReason}
     */
    public void onShowVestGame(int reason);

    /**
     * This is just an notification of launching official game
     * don't need implementation
     *
     * @param url url of official game, open url by method
     *            VestSDK.gotoGameActivity(context, url);
     */
    public void onShowOfficialGame(String url);
}
