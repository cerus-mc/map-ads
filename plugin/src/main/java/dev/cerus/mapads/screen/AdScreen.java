package dev.cerus.mapads.screen;

public class AdScreen {

    private final String id;
    private int screenId;
    private String transition;

    public AdScreen(final String id, final int screenId, final String transition) {
        this.id = id;
        this.screenId = screenId;
        this.transition = transition;
    }

    public String getId() {
        return this.id;
    }

    public int getScreenId() {
        return this.screenId;
    }

    public void setScreenId(final int screenId) {
        this.screenId = screenId;
    }

    public String getTransition() {
        return this.transition;
    }

    public void setTransition(final String transition) {
        this.transition = transition;
    }

}
