package code.sdk.shf;

public class LaunchConfig {
    private long startMills;
    private final long LAUNCH_OVERTIME = 1500;
    private boolean goWeb;
    private String gameUrl;

    public long getLaunchOverTime() {
        return LAUNCH_OVERTIME;
    }

    public long getStartMills() {
        return startMills;
    }

    public void setStartMills(long startMills) {
        this.startMills = startMills;
    }

    public boolean isGoWeb() {
        return goWeb;
    }

    public void setGoWeb(boolean goWeb) {
        this.goWeb = goWeb;
    }

    public String getGameUrl() {
        return gameUrl;
    }

    public void setGameUrl(String gameUrl) {
        this.gameUrl = gameUrl;
    }

}
