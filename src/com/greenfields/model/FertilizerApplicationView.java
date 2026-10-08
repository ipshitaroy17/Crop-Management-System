package com.greenfields.model;

public class FertilizerApplicationView {
    private final FertilizerApplication application;
    private final String cropName;
    private final String seasonName;

    public FertilizerApplicationView(FertilizerApplication application, String cropName, String seasonName) {
        this.application = application;
        this.cropName = cropName;
        this.seasonName = seasonName;
    }

    public FertilizerApplication getApplication() { return application; }
    public String getCropName() { return cropName; }
    public String getSeasonName() { return seasonName; }
}
