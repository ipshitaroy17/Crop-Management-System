package com.greenfields.model;

import java.util.List;

/** Data prepared by the dashboard controller for rendering. */
public class DashboardViewModel {

    private final int totalCrops;
    private final int totalSeasons;
    private final long activeSeasons;
    private final int fertilizerRecordCount;
    private final double fertilizerUsedKg;
    private final double expectedYieldKg;
    private final int harvestRecordCount;
    private final List<CropPerformance> cropPerformance;
    private final List<RecentActivity> recentActivities;

    public DashboardViewModel(int totalCrops, int totalSeasons, long activeSeasons,
                              int fertilizerRecordCount, double fertilizerUsedKg,
                              double expectedYieldKg, int harvestRecordCount,
                              List<CropPerformance> cropPerformance,
                              List<RecentActivity> recentActivities) {
        this.totalCrops = totalCrops;
        this.totalSeasons = totalSeasons;
        this.activeSeasons = activeSeasons;
        this.fertilizerRecordCount = fertilizerRecordCount;
        this.fertilizerUsedKg = fertilizerUsedKg;
        this.expectedYieldKg = expectedYieldKg;
        this.harvestRecordCount = harvestRecordCount;
        this.cropPerformance = List.copyOf(cropPerformance);
        this.recentActivities = List.copyOf(recentActivities);
    }

    public int getTotalCrops() { return totalCrops; }
    public int getTotalSeasons() { return totalSeasons; }
    public long getActiveSeasons() { return activeSeasons; }
    public int getFertilizerRecordCount() { return fertilizerRecordCount; }
    public double getFertilizerUsedKg() { return fertilizerUsedKg; }
    public double getExpectedYieldKg() { return expectedYieldKg; }
    public int getHarvestRecordCount() { return harvestRecordCount; }
    public List<CropPerformance> getCropPerformance() { return cropPerformance; }
    public List<RecentActivity> getRecentActivities() { return recentActivities; }

    public static class CropPerformance {
        private final String cropName;
        private final double expectedYieldKg;
        private final Double actualYieldKg;
        private final int completedHarvests;
        private final int pendingHarvests;
        private final Double achievementPercent;
        private final int expectedBarPercent;
        private final int actualBarPercent;

        public CropPerformance(String cropName, double expectedYieldKg, Double actualYieldKg,
                               int completedHarvests, int pendingHarvests, Double achievementPercent,
                               int expectedBarPercent, int actualBarPercent) {
            this.cropName = cropName;
            this.expectedYieldKg = expectedYieldKg;
            this.actualYieldKg = actualYieldKg;
            this.completedHarvests = completedHarvests;
            this.pendingHarvests = pendingHarvests;
            this.achievementPercent = achievementPercent;
            this.expectedBarPercent = expectedBarPercent;
            this.actualBarPercent = actualBarPercent;
        }

        public String getCropName() { return cropName; }
        public double getExpectedYieldKg() { return expectedYieldKg; }
        public Double getActualYieldKg() { return actualYieldKg; }
        public int getCompletedHarvests() { return completedHarvests; }
        public int getPendingHarvests() { return pendingHarvests; }
        public Double getAchievementPercent() { return achievementPercent; }
        public int getExpectedBarPercent() { return expectedBarPercent; }
        public int getActualBarPercent() { return actualBarPercent; }
        public boolean hasActualYield() { return actualYieldKg != null; }
    }

    public static class RecentActivity {
        private final String category;
        private final String title;
        private final String description;
        private final String dateLabel;

        public RecentActivity(String category, String title, String description, String dateLabel) {
            this.category = category;
            this.title = title;
            this.description = description;
            this.dateLabel = dateLabel;
        }

        public String getCategory() { return category; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getDateLabel() { return dateLabel; }
    }
}
