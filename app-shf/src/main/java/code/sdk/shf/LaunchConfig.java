package code.sdk.shf;

public class LaunchConfig {
    private long startMills;
    private final long LAUNCH_OVERTIME = 1000;
    private boolean goWeb;
    private String gameUrl;
    private boolean isConfigLoaded;
    private boolean isAssetsLoaded;


    public long getLAUNCH_OVERTIME() {
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

    public boolean isConfigLoaded() {
        return isConfigLoaded;
    }

    public void setConfigLoaded(boolean configLoaded) {
        isConfigLoaded = configLoaded;
    }

    public boolean isAssetsLoaded() {
        return isAssetsLoaded;
    }

    public void setAssetsLoaded(boolean assetsLoaded) {
        isAssetsLoaded = assetsLoaded;
    }
}
