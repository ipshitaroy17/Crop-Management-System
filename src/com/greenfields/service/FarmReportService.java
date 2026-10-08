package com.greenfields.service;

import com.greenfields.dao.CropDAO;
import com.greenfields.dao.FertilizerApplicationDAO;
import com.greenfields.dao.HarvestRecordDAO;
import com.greenfields.dao.IrrigationScheduleDAO;
import com.greenfields.dao.SeasonDAO;
import com.greenfields.model.Crop;
import com.greenfields.model.CropHistoryViewModel;
import com.greenfields.model.FertilizerApplication;
import com.greenfields.model.HarvestRecord;
import com.greenfields.model.IrrigationSchedule;
import com.greenfields.model.ReportsViewModel;
import com.greenfields.model.Season;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FarmReportService {
    private final CropDAO cropDAO;
    private final SeasonDAO seasonDAO;
    private final FertilizerApplicationDAO fertilizerDAO;
    private final IrrigationScheduleDAO irrigationDAO;
    private final HarvestRecordDAO harvestDAO;

    public FarmReportService(CropDAO cropDAO, SeasonDAO seasonDAO, FertilizerApplicationDAO fertilizerDAO,
                             IrrigationScheduleDAO irrigationDAO, HarvestRecordDAO harvestDAO) {
        this.cropDAO = cropDAO;
        this.seasonDAO = seasonDAO;
        this.fertilizerDAO = fertilizerDAO;
        this.irrigationDAO = irrigationDAO;
        this.harvestDAO = harvestDAO;
    }

    public ReportsViewModel buildReport(List<Crop> crops, List<Season> seasons, int cropId, int seasonId) {
        Map<Integer, Crop> cropsById = indexCrops(crops);
        Map<Integer, Season> seasonsById = indexSeasons(seasons);
        Crop selectedCrop = cropId == 0 ? null : cropsById.get(cropId);
        Season selectedSeason = seasonId == 0 ? null : seasonsById.get(seasonId);
        if (cropId != 0 && selectedCrop == null) throw new IllegalArgumentException("The selected crop does not exist.");
        if (seasonId != 0 && selectedSeason == null) throw new IllegalArgumentException("The selected season does not exist.");
        if (selectedCrop != null && selectedSeason != null && selectedSeason.getCropId() != cropId) {
            throw new IllegalArgumentException("The selected season does not belong to the selected crop.");
        }

        List<HarvestRecord> harvests = filterHarvests(cropId, seasonId);
        List<FertilizerApplication> fertilizers = filterFertilizers(cropId, seasonId);
        List<IrrigationSchedule> irrigation = filterIrrigation(cropId, seasonId);
        List<ReportsViewModel.HarvestYieldRow> harvestRows = harvests.stream()
                .map(record -> harvestRow(record, seasonsById, cropsById)).toList();
        List<ReportsViewModel.FertilizerUsageRow> fertilizerRows = fertilizers.stream()
                .map(record -> fertilizerRow(record, seasonsById, cropsById)).toList();
        List<ReportsViewModel.IrrigationUsageRow> irrigationRows = irrigation.stream()
                .map(record -> irrigationRow(record, seasonsById, cropsById)).toList();

        double expected = harvests.stream().mapToDouble(HarvestRecord::getExpectedYieldKg).sum();
        double actualRecorded = harvests.stream().filter(HarvestRecord::isHarvestComplete)
                .mapToDouble(record -> record.getActualYieldKg()).sum();
        Double actualTotal = harvests.stream().anyMatch(HarvestRecord::isHarvestComplete) ? actualRecorded : null;
        double fertilizerTotal = fertilizers.stream().mapToDouble(FertilizerApplication::getQuantityKg).sum();
        int pending = (int) harvests.stream().filter(record -> !record.isHarvestComplete()).count();

        List<ReportsViewModel.SeasonSummaryRow> summaries = buildSeasonSummaries(
                harvests, selectedSeason, seasonsById, cropsById);
        return new ReportsViewModel(crops, seasons, cropId, seasonId,
                selectedCrop == null ? null : selectedCrop.getLabel(),
                selectedSeason == null ? null : selectedSeason.getSeasonName(), null,
                expected, actualTotal, fertilizerTotal, irrigation.size(), pending,
                summaries, harvestRows, fertilizerRows, irrigationRows);
    }

    public CropHistoryViewModel buildCropHistory(List<Crop> crops, int cropId) {
        Crop crop = crops.stream().filter(item -> item.getId() == cropId).findFirst().orElse(null);
        if (crop == null) return new CropHistoryViewModel(crops, null, "The selected crop does not exist.",
                List.of(), List.of(), List.of(), List.of());

        List<Season> seasons = seasonDAO.findByCropId(cropId);
        Map<Integer, Season> seasonsById = indexSeasons(seasons);
        Map<Integer, Crop> cropsById = Map.of(cropId, crop);
        List<ReportsViewModel.FertilizerUsageRow> fertilizers = fertilizerDAO.findByCropId(cropId).stream()
                .map(item -> fertilizerRow(item, seasonsById, cropsById)).toList();
        List<ReportsViewModel.IrrigationUsageRow> irrigation = irrigationDAO.findByCropId(cropId).stream()
                .map(item -> irrigationRow(item, seasonsById, cropsById)).toList();
        List<ReportsViewModel.HarvestYieldRow> harvests = harvestDAO.findByCropId(cropId).stream()
                .map(item -> harvestRow(item, seasonsById, cropsById)).toList();
        return new CropHistoryViewModel(crops, crop, null, seasons, fertilizers, irrigation, harvests);
    }

    private List<HarvestRecord> filterHarvests(int cropId, int seasonId) {
        List<HarvestRecord> rows = seasonId > 0 ? harvestDAO.findBySeasonId(seasonId)
                : cropId > 0 ? harvestDAO.findByCropId(cropId) : harvestDAO.findAll();
        if (cropId > 0 && seasonId > 0) rows = rows.stream().filter(row -> row.getSeasonId() == seasonId).toList();
        return rows;
    }

    private List<FertilizerApplication> filterFertilizers(int cropId, int seasonId) {
        List<FertilizerApplication> rows = seasonId > 0 ? fertilizerDAO.findBySeasonId(seasonId)
                : cropId > 0 ? fertilizerDAO.findByCropId(cropId) : fertilizerDAO.findAll();
        if (cropId > 0 && seasonId > 0) rows = rows.stream().filter(row -> row.getSeasonId() == seasonId).toList();
        return rows;
    }

    private List<IrrigationSchedule> filterIrrigation(int cropId, int seasonId) {
        List<IrrigationSchedule> rows = seasonId > 0 ? irrigationDAO.findBySeasonId(seasonId)
                : cropId > 0 ? irrigationDAO.findByCropId(cropId) : irrigationDAO.findAll();
        if (cropId > 0 && seasonId > 0) rows = rows.stream().filter(row -> row.getSeasonId() == seasonId).toList();
        return rows;
    }

    private List<ReportsViewModel.SeasonSummaryRow> buildSeasonSummaries(List<HarvestRecord> harvests,
            Season selectedSeason, Map<Integer, Season> seasonsById, Map<Integer, Crop> cropsById) {
        Map<Integer, List<HarvestRecord>> grouped = new LinkedHashMap<>();
        if (selectedSeason != null) grouped.put(selectedSeason.getId(), new ArrayList<>());
        harvests.forEach(row -> grouped.computeIfAbsent(row.getSeasonId(), ignored -> new ArrayList<>()).add(row));
        return grouped.entrySet().stream().map(entry -> {
            Season season = seasonsById.get(entry.getKey());
            if (season == null) return null;
            List<HarvestRecord> rows = entry.getValue();
            double expected = rows.stream().mapToDouble(HarvestRecord::getExpectedYieldKg).sum();
            boolean hasActual = rows.stream().anyMatch(HarvestRecord::isHarvestComplete);
            Double actual = hasActual ? rows.stream().filter(HarvestRecord::isHarvestComplete)
                    .mapToDouble(row -> row.getActualYieldKg()).sum() : null;
            int pending = (int) rows.stream().filter(row -> !row.isHarvestComplete()).count();
            Crop crop = cropsById.get(season.getCropId());
            return new ReportsViewModel.SeasonSummaryRow(season,
                    crop == null ? "Unavailable crop" : crop.getLabel(), expected, actual, rows.size(), pending);
        }).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(ReportsViewModel.SeasonSummaryRow::getSeasonName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))).toList();
    }

    private ReportsViewModel.HarvestYieldRow harvestRow(HarvestRecord record,
            Map<Integer, Season> seasonsById, Map<Integer, Crop> cropsById) {
        Season season = seasonsById.get(record.getSeasonId());
        Crop crop = season == null ? null : cropsById.get(season.getCropId());
        return new ReportsViewModel.HarvestYieldRow(record,
                crop == null ? "Unavailable crop" : crop.getCropName(),
                crop == null ? "" : crop.getVariety(),
                season == null ? "Unavailable season" : season.getSeasonName());
    }

    private ReportsViewModel.FertilizerUsageRow fertilizerRow(FertilizerApplication record,
            Map<Integer, Season> seasonsById, Map<Integer, Crop> cropsById) {
        Season season = seasonsById.get(record.getSeasonId());
        Crop crop = season == null ? null : cropsById.get(season.getCropId());
        return new ReportsViewModel.FertilizerUsageRow(record,
                crop == null ? "Unavailable crop" : crop.getCropName(),
                season == null ? "Unavailable season" : season.getSeasonName());
    }

    private ReportsViewModel.IrrigationUsageRow irrigationRow(IrrigationSchedule record,
            Map<Integer, Season> seasonsById, Map<Integer, Crop> cropsById) {
        Season season = seasonsById.get(record.getSeasonId());
        Crop crop = season == null ? null : cropsById.get(season.getCropId());
        return new ReportsViewModel.IrrigationUsageRow(record,
                crop == null ? "Unavailable crop" : crop.getCropName(),
                season == null ? "Unavailable season" : season.getSeasonName());
    }

    private static Map<Integer, Crop> indexCrops(List<Crop> crops) {
        Map<Integer, Crop> map = new LinkedHashMap<>();
        crops.forEach(crop -> map.put(crop.getId(), crop));
        return map;
    }

    private static Map<Integer, Season> indexSeasons(List<Season> seasons) {
        Map<Integer, Season> map = new LinkedHashMap<>();
        seasons.forEach(season -> map.put(season.getId(), season));
        return map;
    }
}
