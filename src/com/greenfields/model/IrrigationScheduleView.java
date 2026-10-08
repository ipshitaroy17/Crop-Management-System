package com.greenfields.model;

public class IrrigationScheduleView {
    private final IrrigationSchedule schedule;
    private final String cropName;
    private final String seasonName;

    public IrrigationScheduleView(IrrigationSchedule schedule, String cropName, String seasonName) {
        this.schedule = schedule;
        this.cropName = cropName;
        this.seasonName = seasonName;
    }

    public IrrigationSchedule getSchedule() { return schedule; }
    public String getCropName() { return cropName; }
    public String getSeasonName() { return seasonName; }
}
