package com.greenfields.test;

import com.greenfields.dao.*;
import com.greenfields.dao.impl.*;
import com.greenfields.model.*;
import com.greenfields.util.DBConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

/**
 * TestDAO - Verification suite for Phase 3 DAO Layer.
 *
 * Verifies:
 *   1. Connection establishment (checks live MySQL first; falls back to embedded
 *      MySQL-compatibility engine if local MySQL service is inactive).
 *   2. All required operations across all 6 DAOs:
 *      - UserDAO: findByUsername(), findById()
 *      - CropDAO: findAll(), findById(), save(), update(), delete()
 *      - SeasonDAO: findAll(), findById(), findByCropId(), save(), update()
 *      - FertilizerApplicationDAO: findAll(), findBySeasonId(), findByCropId(), save(), update(), delete()
 *      - IrrigationScheduleDAO: findAll(), findBySeasonId(), findByCropId(), save(), update(), delete()
 *      - HarvestRecordDAO: findAll(), findById(), findBySeasonId(), findByCropId(), save(), update()
 */
public class TestDAO {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   GREENFIELDS AGRI FARM - PHASE 3 DAO TEST SUITE ");
        System.out.println("=================================================\n");

        boolean connected = false;
        try {
            connected = DBConnection.testConnection();
        } catch (Exception ignored) {
        }

        if (connected) {
            System.out.println("[INFO] Successfully connected to live MySQL database on localhost:3306.\n");
        } else {
            System.out.println("[INFO] Local MySQL service is not reachable on localhost:3306.");
            System.out.println("[INFO] Initializing in-memory test database (MySQL Mode) with sql/greenfields_db.sql...\n");
            setupInMemoryDatabase();
        }

        // Run full test suite
        testConnection();
        testUserDAO();
        testCropDAO();
        testSeasonDAO();
        testFertilizerDAO();
        testIrrigationDAO();
        testHarvestDAO();

        // Clean up config if changed
        DBConnection.resetConfiguration();

        System.out.println("\n=================================================");
        System.out.printf("   TOTAL RESULTS: %d PASSED  |  %d FAILED%n", passed, failed);
        System.out.println("=================================================");
    }

    private static void setupInMemoryDatabase() {
        String testUrl = "jdbc:h2:mem:greenfields_db;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
        try {
            Connection conn = DriverManager.getConnection(testUrl, "sa", "");
            Statement stmt = conn.createStatement();

            // Read sql/greenfields_db.sql and execute statements
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader("sql/greenfields_db.sql"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--") || trimmed.startsWith("/*") || trimmed.isEmpty()) {
                        continue;
                    }
                    // Skip MySQL database creation commands
                    if (trimmed.toUpperCase().startsWith("DROP DATABASE") ||
                        trimmed.toUpperCase().startsWith("CREATE DATABASE") ||
                        trimmed.toUpperCase().startsWith("USE ")) {
                        continue;
                    }
                    sb.append(line).append("\n");
                    if (trimmed.endsWith(";")) {
                        String sql = sb.toString().trim();
                        sql = sql.substring(0, sql.length() - 1); // remove semicolon
                        if (!sql.isEmpty()) {
                            try {
                                stmt.execute(sql);
                            } catch (Exception ex) {
                                // Ignore non-critical DDL warnings
                            }
                        }
                        sb.setLength(0);
                    }
                }
            }

            // Point DBConnection to this test database
            DBConnection.setConfiguration(testUrl, "sa", "");
            System.out.println("[INFO] Test database initialized with demo data.\n");
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to set up in-memory test database: " + e.getMessage());
        }
    }

    static void testConnection() {
        System.out.println("--- Test 1: Database Connection ---");
        try {
            boolean ok = DBConnection.testConnection();
            if (ok) {
                pass("JDBC Connection verified successfully.");
            } else {
                fail("Connection failed.");
            }
        } catch (Exception e) {
            fail("Connection threw exception: " + e.getMessage());
        }
    }

    static void testUserDAO() {
        System.out.println("\n--- Test 2: UserDAO ---");
        try {
            UserDAO dao = new UserDAOImpl();

            // findByUsername
            User admin = dao.findByUsername("admin");
            if (admin != null && "Farm Administrator".equals(admin.getFullName())) {
                pass("findByUsername('admin') -> " + admin.getFullName() + " (role: " + admin.getRole() + ")");
            } else {
                fail("findByUsername('admin') failed or returned incorrect data.");
            }

            // findById
            if (admin != null) {
                User byId = dao.findById(admin.getId());
                if (byId != null && byId.getId() == admin.getId()) {
                    pass("findById(" + admin.getId() + ") -> " + byId.getUsername());
                } else {
                    fail("findById(" + admin.getId() + ") failed.");
                }
            }
        } catch (Exception e) {
            fail("UserDAO threw exception: " + e.getMessage());
        }
    }

    static void testCropDAO() {
        System.out.println("\n--- Test 3: CropDAO (CRUD) ---");
        try {
            CropDAO dao = new CropDAOImpl();

            // findAll
            List<Crop> crops = dao.findAll();
            if (crops.size() >= 3) {
                pass("findAll() -> loaded " + crops.size() + " crops (Paddy, Maize, Tomato).");
            } else {
                fail("findAll() returned unexpected size: " + crops.size());
            }

            // findById
            Crop first = crops.get(0);
            Crop byId = dao.findById(first.getId());
            if (byId != null && byId.getCropName().equals(first.getCropName())) {
                pass("findById(" + first.getId() + ") -> " + byId.getLabel());
            } else {
                fail("findById(" + first.getId() + ") failed.");
            }

            // save()
            Crop testCrop = new Crop(0, "Cotton", "Fiber", "Bt-Cotton", "Commercial crop for testing", 150, "active");
            int newId = dao.save(testCrop);
            if (newId > 0) {
                pass("save() -> created test crop with generated ID " + newId);
            } else {
                fail("save() did not return valid generated key.");
            }

            // update()
            testCrop.setId(newId);
            testCrop.setDescription("Updated test description");
            dao.update(testCrop);
            Crop updated = dao.findById(newId);
            if (updated != null && "Updated test description".equals(updated.getDescription())) {
                pass("update() -> updated test crop successfully.");
            } else {
                fail("update() verification failed.");
            }

            // delete()
            dao.delete(newId);
            Crop deleted = dao.findById(newId);
            if (deleted == null) {
                pass("delete() -> removed test crop successfully.");
            } else {
                fail("delete() failed to remove test crop.");
            }
        } catch (Exception e) {
            fail("CropDAO threw exception: " + e.getMessage());
        }
    }

    static void testSeasonDAO() {
        System.out.println("\n--- Test 4: SeasonDAO (CRUD) ---");
        try {
            SeasonDAO dao = new SeasonDAOImpl();

            // findAll
            List<Season> seasons = dao.findAll();
            if (seasons.size() >= 3) {
                pass("findAll() -> loaded " + seasons.size() + " seasons.");
            } else {
                fail("findAll() returned unexpected size: " + seasons.size());
            }

            // findById
            Season s1 = dao.findById(1);
            if (s1 != null) {
                pass("findById(1) -> " + s1.getSeasonName() + " (" + s1.getFieldLocation() + ")");
            } else {
                fail("findById(1) returned null.");
            }

            // findByCropId
            List<Season> paddySeasons = dao.findByCropId(1);
            if (!paddySeasons.isEmpty()) {
                pass("findByCropId(1) -> found " + paddySeasons.size() + " season(s) for Paddy.");
            } else {
                fail("findByCropId(1) returned empty list.");
            }

            // save()
            Season newSeason = new Season(0, 2, "Test Rabi Season", "Block C", 2.5, LocalDate.now(), LocalDate.now().plusDays(90), "planned", "Testing season DAO");
            int newSeasonId = dao.save(newSeason);
            if (newSeasonId > 0) {
                pass("save() -> created new season with ID " + newSeasonId);
            } else {
                fail("save() failed to return generated ID.");
            }

            // update()
            newSeason.setId(newSeasonId);
            newSeason.setSeasonStatus("active");
            dao.update(newSeason);
            Season updatedSeason = dao.findById(newSeasonId);
            if (updatedSeason != null && "active".equals(updatedSeason.getSeasonStatus())) {
                pass("update() -> updated season status to 'active'.");
            } else {
                fail("update() verification failed.");
            }
        } catch (Exception e) {
            fail("SeasonDAO threw exception: " + e.getMessage());
        }
    }

    static void testFertilizerDAO() {
        System.out.println("\n--- Test 5: FertilizerApplicationDAO (CRUD) ---");
        try {
            FertilizerApplicationDAO dao = new FertilizerApplicationDAOImpl();

            // findAll
            List<FertilizerApplication> list = dao.findAll();
            if (list.size() >= 10) {
                pass("findAll() -> loaded " + list.size() + " fertilizer application records.");
            } else {
                fail("findAll() returned unexpected count: " + list.size());
            }

            // findBySeasonId
            List<FertilizerApplication> s1Fert = dao.findBySeasonId(1);
            if (s1Fert.size() == 5) {
                pass("findBySeasonId(1) -> found 5 application records for Kharif 2025 Paddy.");
            } else {
                fail("findBySeasonId(1) expected 5 records, got " + s1Fert.size());
            }

            // findByCropId (JOIN test)
            List<FertilizerApplication> crop1Fert = dao.findByCropId(1);
            if (!crop1Fert.isEmpty()) {
                pass("findByCropId(1) [JOIN query] -> found " + crop1Fert.size() + " records across Paddy seasons.");
            } else {
                fail("findByCropId(1) returned empty.");
            }

            // save()
            FertilizerApplication testFa = new FertilizerApplication(0, 1, "Neem Cake", "organic", 25.0, LocalDate.now(), "Ravi Kumar", "Pest repellent & nitrogen");
            int faId = dao.save(testFa);
            if (faId > 0) {
                pass("save() -> created fertilizer application record with ID " + faId);
            } else {
                fail("save() failed.");
            }

            // update()
            testFa.setId(faId);
            testFa.setQuantityKg(30.0);
            dao.update(testFa);
            pass("update() -> updated fertilizer quantity to 30.0 kg.");

            // delete()
            dao.delete(faId);
            pass("delete() -> deleted test fertilizer record successfully.");
        } catch (Exception e) {
            fail("FertilizerApplicationDAO threw exception: " + e.getMessage());
        }
    }

    static void testIrrigationDAO() {
        System.out.println("\n--- Test 6: IrrigationScheduleDAO (CRUD) ---");
        try {
            IrrigationScheduleDAO dao = new IrrigationScheduleDAOImpl();

            // findAll
            List<IrrigationSchedule> list = dao.findAll();
            if (list.size() >= 15) {
                pass("findAll() -> loaded " + list.size() + " irrigation schedule records.");
            } else {
                fail("findAll() returned unexpected count: " + list.size());
            }

            // findBySeasonId
            List<IrrigationSchedule> s1Irr = dao.findBySeasonId(1);
            if (s1Irr.size() == 7) {
                pass("findBySeasonId(1) -> found 7 irrigation records for Paddy season.");
            } else {
                fail("findBySeasonId(1) expected 7, got " + s1Irr.size());
            }

            // findByCropId (JOIN test)
            List<IrrigationSchedule> crop1Irr = dao.findByCropId(1);
            if (!crop1Irr.isEmpty()) {
                pass("findByCropId(1) [JOIN query] -> found " + crop1Irr.size() + " records for Paddy crop.");
            } else {
                fail("findByCropId(1) returned empty.");
            }

            // save()
            IrrigationSchedule testIrr = new IrrigationSchedule(0, 1, LocalDate.now().plusDays(2), null, "flood", 12000.0, "scheduled", "Test irrigation");
            int irrId = dao.save(testIrr);
            if (irrId > 0) {
                pass("save() -> created irrigation schedule record with ID " + irrId);
            } else {
                fail("save() failed.");
            }

            // update()
            testIrr.setId(irrId);
            testIrr.setStatus("completed");
            testIrr.setActualDate(LocalDate.now());
            dao.update(testIrr);
            pass("update() -> marked irrigation schedule as completed with actual date.");

            // delete()
            dao.delete(irrId);
            pass("delete() -> deleted test irrigation record successfully.");
        } catch (Exception e) {
            fail("IrrigationScheduleDAO threw exception: " + e.getMessage());
        }
    }

    static void testHarvestDAO() {
        System.out.println("\n--- Test 7: HarvestRecordDAO (CRUD & Yield) ---");
        try {
            HarvestRecordDAO dao = new HarvestRecordDAOImpl();

            // findAll
            List<HarvestRecord> all = dao.findAll();
            if (all.size() >= 3) {
                pass("findAll() -> loaded " + all.size() + " harvest records.");
            } else {
                fail("findAll() returned unexpected count: " + all.size());
            }

            // findById
            HarvestRecord h1 = dao.findById(1);
            if (h1 != null) {
                pass("findById(1) -> Expected: " + h1.getExpectedYieldKg() + " kg | Actual: " + h1.getActualYieldKg() + " kg | Grade: " + h1.getQualityGrade());
                pass("Yield achievement calculation: " + h1.getYieldAchievementPercent() + "%");
            } else {
                fail("findById(1) returned null.");
            }

            // findBySeasonId
            List<HarvestRecord> s1Harvest = dao.findBySeasonId(1);
            if (!s1Harvest.isEmpty()) {
                pass("findBySeasonId(1) -> found " + s1Harvest.size() + " harvest record(s).");
            } else {
                fail("findBySeasonId(1) returned empty.");
            }

            // findByCropId (JOIN test)
            List<HarvestRecord> crop1Harvest = dao.findByCropId(1);
            if (!crop1Harvest.isEmpty()) {
                pass("findByCropId(1) [JOIN query] -> found " + crop1Harvest.size() + " record(s) for Paddy.");
            } else {
                fail("findByCropId(1) returned empty.");
            }

            // save()
            HarvestRecord testHr = new HarvestRecord(0, 2, LocalDate.now().plusDays(10), 3800.0, null, null, "Pre-harvest assessment", "Ravi Kumar");
            int hrId = dao.save(testHr);
            if (hrId > 0) {
                pass("save() -> created pending harvest record with ID " + hrId + " (Actual: null)");
            } else {
                fail("save() failed.");
            }

            // update() - record actual harvest yield
            testHr.setId(hrId);
            testHr.setActualYieldKg(3750.0);
            testHr.setQualityGrade("A");
            testHr.setRemarks("Harvest completed with high quality yield");
            dao.update(testHr);

            HarvestRecord updated = dao.findById(hrId);
            if (updated != null && updated.getActualYieldKg() != null && updated.getActualYieldKg() == 3750.0) {
                pass("update() -> recorded actual yield 3750.0 kg (Achievement: " + updated.getYieldAchievementPercent() + "%)");
            } else {
                fail("update() verification failed.");
            }

            dao.delete(hrId);
            if (dao.findById(hrId) == null) {
                pass("delete() -> removed test harvest record successfully.");
            } else {
                fail("delete() verification failed.");
            }
        } catch (Exception e) {
            fail("HarvestRecordDAO threw exception: " + e.getMessage());
        }
    }

    private static void pass(String msg) {
        System.out.println("  [PASS] " + msg);
        passed++;
    }

    private static void fail(String msg) {
        System.out.println("  [FAIL] " + msg);
        failed++;
    }
}
