package dev.cerus.mapads.screen;

public class AdScreen {

    private final String id;
    private int screenId;
    private String transition;
    private int fixedTime;
    private double fixedPrice;
    private boolean noDefaultImage;

    public AdScreen(final String id, final int screenId, final String transition, final int fixedTime, final double fixedPrice, final boolean noDefaultImage) {
        this.id = id;
        this.screenId = screenId;
        this.transition = transition;
        this.fixedTime = fixedTime;
        this.fixedPrice = fixedPrice;
        this.noDefaultImage = noDefaultImage;
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

    public int getFixedTime() {
        return this.fixedTime;
    }

    public void setFixedTime(final int fixedTime) {
        this.fixedTime = fixedTime;
    }

    public double getFixedPrice() {
        return this.fixedPrice;
    }

    public void setFixedPrice(final double fixedPrice) {
        this.fixedPrice = fixedPrice;
    }

    public boolean isNoDefaultImage() {
        return this.noDefaultImage;
    }

    public void setNoDefaultImage(final boolean noDefaultImage) {
        this.noDefaultImage = noDefaultImage;
    }

}
