package com.greenfields.model;

public class HarvestRecordView {
    private final HarvestRecord record;
    private final String cropName;
    private final String seasonName;

    public HarvestRecordView(HarvestRecord record, String cropName, String seasonName) {
        this.record = record;
        this.cropName = cropName;
        this.seasonName = seasonName;
    }

    public HarvestRecord getRecord() { return record; }
    public String getCropName() { return cropName; }
    public String getSeasonName() { return seasonName; }

    public Double getAchievementPercent() {
        if (record.getActualYieldKg() == null || record.getExpectedYieldKg() == 0) return null;
        return record.getYieldAchievementPercent();
    }

    public String getYieldStatus() {
        if (record.getActualYieldKg() == null) return "Pending";
        if (record.getExpectedYieldKg() == 0) return "N/A (zero expected yield)";
        int comparison = Double.compare(record.getActualYieldKg(), record.getExpectedYieldKg());
        if (comparison < 0) return "Below Expected";
        if (comparison > 0) return "Above Expected";
        return "On Target";
    }
}
