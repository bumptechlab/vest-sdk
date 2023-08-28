package code.sdk.core;

public interface VestInspectCallback {

    /**
     * Implement this method to launch vest game
     */
    public void onShowVestGame();

    /**
     * This is just an notification of launching official game
     * don't need implementation
     */
    public void onShowOfficialGame(String url);
}
