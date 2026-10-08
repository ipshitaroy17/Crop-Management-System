package com.greenfields.model;

import java.util.List;

public class ReportsViewModel {
    private final List<Crop> crops;
    private final List<Season> seasons;
    private final int selectedCropId;
    private final int selectedSeasonId;
    private final String selectedCropName;
    private final String selectedSeasonName;
    private final String errorMessage;
    private final double expectedYieldTotalKg;
    private final Double actualYieldTotalKg;
    private final double fertilizerTotalKg;
    private final int irrigationCount;
    private final int pendingHarvestCount;
    private final List<SeasonSummaryRow> seasonalSummaries;
    private final List<HarvestYieldRow> harvestRecords;
    private final List<FertilizerUsageRow> fertilizerUsage;
    private final List<IrrigationUsageRow> irrigationUsage;

    public ReportsViewModel(List<Crop> crops, List<Season> seasons, int selectedCropId, int selectedSeasonId,
                            String selectedCropName, String selectedSeasonName, String errorMessage,
                            double expectedYieldTotalKg, Double actualYieldTotalKg, double fertilizerTotalKg,
                            int irrigationCount, int pendingHarvestCount, List<SeasonSummaryRow> seasonalSummaries,
                            List<HarvestYieldRow> harvestRecords, List<FertilizerUsageRow> fertilizerUsage,
                            List<IrrigationUsageRow> irrigationUsage) {
        this.crops = List.copyOf(crops);
        this.seasons = List.copyOf(seasons);
        this.selectedCropId = selectedCropId;
        this.selectedSeasonId = selectedSeasonId;
        this.selectedCropName = selectedCropName;
        this.selectedSeasonName = selectedSeasonName;
        this.errorMessage = errorMessage;
        this.expectedYieldTotalKg = expectedYieldTotalKg;
        this.actualYieldTotalKg = actualYieldTotalKg;
        this.fertilizerTotalKg = fertilizerTotalKg;
        this.irrigationCount = irrigationCount;
        this.pendingHarvestCount = pendingHarvestCount;
        this.seasonalSummaries = List.copyOf(seasonalSummaries);
        this.harvestRecords = List.copyOf(harvestRecords);
        this.fertilizerUsage = List.copyOf(fertilizerUsage);
        this.irrigationUsage = List.copyOf(irrigationUsage);
    }

    public List<Crop> getCrops() { return crops; }
    public List<Season> getSeasons() { return seasons; }
    public int getSelectedCropId() { return selectedCropId; }
    public int getSelectedSeasonId() { return selectedSeasonId; }
    public String getSelectedCropName() { return selectedCropName; }
    public String getSelectedSeasonName() { return selectedSeasonName; }
    public String getErrorMessage() { return errorMessage; }
    public double getExpectedYieldTotalKg() { return expectedYieldTotalKg; }
    public Double getActualYieldTotalKg() { return actualYieldTotalKg; }
    public double getFertilizerTotalKg() { return fertilizerTotalKg; }
    public int getIrrigationCount() { return irrigationCount; }
    public int getPendingHarvestCount() { return pendingHarvestCount; }
    public List<SeasonSummaryRow> getSeasonalSummaries() { return seasonalSummaries; }
    public List<HarvestYieldRow> getHarvestRecords() { return harvestRecords; }
    public List<FertilizerUsageRow> getFertilizerUsage() { return fertilizerUsage; }
    public List<IrrigationUsageRow> getIrrigationUsage() { return irrigationUsage; }

    public static class HarvestYieldRow {
        private final int recordId;
        private final String cropName;
        private final String variety;
        private final String seasonName;
        private final String harvestDate;
        private final double expectedYieldKg;
        private final Double actualYieldKg;
        private final Double achievementPercent;
        private final String yieldStatus;
        private final String qualityGrade;
        private final String recordedBy;
        private final String remarks;

        public HarvestYieldRow(HarvestRecord record, String cropName, String variety, String seasonName) {
            this.recordId = record.getId();
            this.cropName = cropName;
            this.variety = variety;
            this.seasonName = seasonName;
            this.harvestDate = record.getHarvestDate() == null ? null : record.getHarvestDate().toString();
            this.expectedYieldKg = record.getExpectedYieldKg();
            this.actualYieldKg = record.getActualYieldKg();
            this.achievementPercent = record.getActualYieldKg() == null || record.getExpectedYieldKg() == 0
                    ? null : record.getYieldAchievementPercent();
            if (record.getActualYieldKg() == null) this.yieldStatus = "Pending";
            else if (record.getExpectedYieldKg() == 0) this.yieldStatus = "N/A (zero expected yield)";
            else if (record.getActualYieldKg() < record.getExpectedYieldKg()) this.yieldStatus = "Below Expected";
            else if (record.getActualYieldKg() > record.getExpectedYieldKg()) this.yieldStatus = "Above Expected";
            else this.yieldStatus = "On Target";
            this.qualityGrade = record.getQualityGrade();
            this.recordedBy = record.getRecordedBy();
            this.remarks = record.getRemarks();
        }

        public int getRecordId() { return recordId; }
        public String getCropName() { return cropName; }
        public String getVariety() { return variety; }
        public String getSeasonName() { return seasonName; }
        public String getHarvestDate() { return harvestDate; }
        public double getExpectedYieldKg() { return expectedYieldKg; }
        public Double getActualYieldKg() { return actualYieldKg; }
        public Double getAchievementPercent() { return achievementPercent; }
        public String getYieldStatus() { return yieldStatus; }
        public String getQualityGrade() { return qualityGrade; }
        public String getRecordedBy() { return recordedBy; }
        public String getRemarks() { return remarks; }
    }

    public static class SeasonSummaryRow {
        private final int seasonId;
        private final String cropName;
        private final String seasonName;
        private final String plantingDate;
        private final String expectedHarvestDate;
        private final double expectedYieldKg;
        private final Double actualYieldKg;
        private final int harvestCount;
        private final int pendingCount;

        public SeasonSummaryRow(Season season, String cropName, double expectedYieldKg, Double actualYieldKg,
                                int harvestCount, int pendingCount) {
            this.seasonId = season.getId();
            this.cropName = cropName;
            this.seasonName = season.getSeasonName();
            this.plantingDate = season.getPlantingDate() == null ? null : season.getPlantingDate().toString();
            this.expectedHarvestDate = season.getExpectedHarvestDate() == null ? null : season.getExpectedHarvestDate().toString();
            this.expectedYieldKg = expectedYieldKg;
            this.actualYieldKg = actualYieldKg;
            this.harvestCount = harvestCount;
            this.pendingCount = pendingCount;
        }

        public int getSeasonId() { return seasonId; }
        public String getCropName() { return cropName; }
        public String getSeasonName() { return seasonName; }
        public String getPlantingDate() { return plantingDate; }
        public String getExpectedHarvestDate() { return expectedHarvestDate; }
        public double getExpectedYieldKg() { return expectedYieldKg; }
        public Double getActualYieldKg() { return actualYieldKg; }
        public int getHarvestCount() { return harvestCount; }
        public int getPendingCount() { return pendingCount; }
    }

    public static class FertilizerUsageRow {
        private final String cropName;
        private final String seasonName;
        private final String fertilizerName;
        private final String fertilizerType;
        private final double quantityKg;
        private final String applicationDate;

        public FertilizerUsageRow(FertilizerApplication application, String cropName, String seasonName) {
            this.cropName = cropName;
            this.seasonName = seasonName;
            this.fertilizerName = application.getFertilizerName();
            this.fertilizerType = application.getFertilizerType();
            this.quantityKg = application.getQuantityKg();
            this.applicationDate = application.getApplicationDate() == null ? null : application.getApplicationDate().toString();
        }

        public String getCropName() { return cropName; }
        public String getSeasonName() { return seasonName; }
        public String getFertilizerName() { return fertilizerName; }
        public String getFertilizerType() { return fertilizerType; }
        public double getQuantityKg() { return quantityKg; }
        public String getApplicationDate() { return applicationDate; }
    }

    public static class IrrigationUsageRow {
        private final String cropName;
        private final String seasonName;
        private final String scheduledDate;
        private final String actualDate;
        private final String method;
        private final double waterVolumeLitres;
        private final String status;

        public IrrigationUsageRow(IrrigationSchedule schedule, String cropName, String seasonName) {
            this.cropName = cropName;
            this.seasonName = seasonName;
            this.scheduledDate = schedule.getScheduledDate() == null ? null : schedule.getScheduledDate().toString();
            this.actualDate = schedule.getActualDate() == null ? null : schedule.getActualDate().toString();
            this.method = schedule.getMethodLabel();
            this.waterVolumeLitres = schedule.getWaterVolumeLitres();
            this.status = schedule.getScheduleStatus();
        }

        public String getCropName() { return cropName; }
        public String getSeasonName() { return seasonName; }
        public String getScheduledDate() { return scheduledDate; }
        public String getActualDate() { return actualDate; }
        public String getMethod() { return method; }
        public double getWaterVolumeLitres() { return waterVolumeLitres; }
        public String getStatus() { return status; }
    }
}
