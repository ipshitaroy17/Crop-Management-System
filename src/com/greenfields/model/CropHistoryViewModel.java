package com.greenfields.model;

import java.util.List;

public class CropHistoryViewModel {
    private final List<Crop> crops;
    private final Crop crop;
    private final String errorMessage;
    private final List<Season> seasons;
    private final List<ReportsViewModel.FertilizerUsageRow> fertilizerApplications;
    private final List<ReportsViewModel.IrrigationUsageRow> irrigationSchedules;
    private final List<ReportsViewModel.HarvestYieldRow> harvestRecords;

    public CropHistoryViewModel(List<Crop> crops, Crop crop, String errorMessage, List<Season> seasons,
                                List<ReportsViewModel.FertilizerUsageRow> fertilizerApplications,
                                List<ReportsViewModel.IrrigationUsageRow> irrigationSchedules,
                                List<ReportsViewModel.HarvestYieldRow> harvestRecords) {
        this.crops = List.copyOf(crops);
        this.crop = crop;
        this.errorMessage = errorMessage;
        this.seasons = List.copyOf(seasons);
        this.fertilizerApplications = List.copyOf(fertilizerApplications);
        this.irrigationSchedules = List.copyOf(irrigationSchedules);
        this.harvestRecords = List.copyOf(harvestRecords);
    }

    public List<Crop> getCrops() { return crops; }
    public Crop getCrop() { return crop; }
    public String getErrorMessage() { return errorMessage; }
    public List<Season> getSeasons() { return seasons; }
    public List<ReportsViewModel.FertilizerUsageRow> getFertilizerApplications() { return fertilizerApplications; }
    public List<ReportsViewModel.IrrigationUsageRow> getIrrigationSchedules() { return irrigationSchedules; }
    public List<ReportsViewModel.HarvestYieldRow> getHarvestRecords() { return harvestRecords; }
}
