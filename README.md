# GreenFields Agri Farm Crop Monitoring System

An academic web application developed for monitoring agricultural production, crop lifecycles, resource management, and yield analysis at Greenfields Agri Farm.

---

## 1. Project Description & Problem Statement

### Problem Statement
Traditional farm management relies heavily on manual ledgers and fragmented paper records to track planting cycles, input applications (fertilizers, irrigation), and seasonal outputs. This leads to:
- Inaccurate expected vs. actual yield comparisons.
- Lack of accountability in resource distribution and water scheduling.
- Difficulty in assessing historical performance across consecutive farming seasons.

### Solution
The **GreenFields Agri Farm Crop Monitoring System** provides a centralized, web-based platform tailored for farm supervisors and administrators. It tracks crops (Paddy, Maize, Tomato), manages field seasons, logs input applications, organizes watering schedules, and evaluates harvest yields with built-in analytics.

---

## 2. Key Features

- **Session-Based Authentication:** Secure login and role tracking (`admin`, `viewer`) with automatic session termination and route protection.
- **Crop Master Catalog:** Management of crop profiles, varieties, growth duration, and botanical categories.
- **Season Management:** Field block allocation, acreage tracking, and planting-to-harvest scheduling.
- **Fertilizer Application Logging:** Tracking of chemical, organic, and bio-fertilizer dosages by stage and applicator.
- **Irrigation Scheduling:** Multi-method watering schedules (Drip, Sprinkler, Flood, Manual) with overdue detection.
- **Harvest & Yield Analysis:** Direct side-by-side comparison of `expected_yield_kg` vs. `actual_yield_kg` with achievement percentage calculations.
- **Historical Production Reporting:** Cross-season aggregations and performance auditing.

---

## 3. Technology Stack

- **Language:** Java 21 (LTS)
- **Web Layer:** Java Servlets (Jakarta EE 6.0), JSP, JSTL
- **Frontend:** HTML5, CSS3 (GreenFields Botanical Theme), Vanilla JavaScript
- **Database:** MySQL 8.0+
- **Persistence:** JDBC (Java Database Connectivity) with PreparedStatement & Connection management
- **Servlet Container / Server:** Apache Tomcat 10.1+ (supports Jakarta EE 10 / Servlet 6.0)

---

## 4. Architecture

The application strictly implements the classical **MVC (Model-View-Controller)** pattern across a **3-Tier Enterprise Web Architecture**:

```
┌─────────────────────────────────────────────────────────────┐
│                 PRESENTATION LAYER (View)                   │
│          JSP Pages + HTML5 / CSS3 / JavaScript              │
└──────────────────────────────┬──────────────────────────────┘
                               │ HTTP Requests / Responses
┌──────────────────────────────▼──────────────────────────────┐
│                 CONTROLLER LAYER (Servlet)                  │
│       Java Servlets + AuthFilter (Session Management)       │
└──────────────────────────────┬──────────────────────────────┘
                               │ Java Method Invocations
┌──────────────────────────────▼──────────────────────────────┐
│                  DATA ACCESS LAYER (DAO)                    │
│        DAO Interfaces & Implementations (Pure JDBC)         │
└──────────────────────────────┬──────────────────────────────┘
                               │ SQL Queries / ResultSets
┌──────────────────────────────▼──────────────────────────────┐
│                       DATABASE LAYER                        │
│                MySQL Database (greenfields_db)              │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Database Structure & Relational Schema

The schema consists of 6 normalized tables with foreign keys and cascade rules:

```
users (standalone authentication)

crops
  └── seasons
        ├── fertilizer_applications
        ├── irrigation_schedules
        └── harvest_records (stores both expected_yield_kg & actual_yield_kg)
```

### Table Summary:
1. **`users`**: Login credentials, full name, and roles (`admin`, `viewer`).
2. **`crops`**: Botanical master data (Paddy IR-64, Maize Sweet Corn, Tomato Roma VF).
3. **`seasons`**: Farming blocks linked to a crop, area in acres, planting date, and status.
4. **`fertilizer_applications`**: Fertilizer name, type (`chemical`, `organic`, `bio`), quantity, and applicator.
5. **`irrigation_schedules`**: Scheduled dates, actual dates, method (`drip`, `sprinkler`, `flood`, `manual`), volume, and status.
6. **`harvest_records`**: Harvest dates, `expected_yield_kg`, `actual_yield_kg`, quality grade (`A`, `B`, `C`, `reject`), and remarks.

---

## 6. Java & OOP Concepts Demonstrated

| OOP / Java Concept | Implementation in Greenfields | Viva Explanation |
|---|---|---|
| **Encapsulation** | All model classes (`User`, `Crop`, `Season`, etc.) | Private fields, validation logic, and public getter/setter methods. |
| **Inheritance** | `BaseEntity` abstract class | Common `id` primary key and shared entity behavior inherited by all 6 models. |
| **Polymorphism** | `BaseEntity.describe()` & DAO interfaces | Subclasses provide distinct implementations; servlets depend on DAO interfaces rather than concrete implementations. |
| **Interfaces** | `Reportable`, `Schedulable`, DAO interfaces | Contract definitions (`generateSummary()`, `isOverdue()`) implemented by relevant domain models. |
| **Abstraction** | Abstract classes & interface boundaries | Callers do not know internal SQL query construction or database connection specifics. |
| **Exception Handling** | Custom `DatabaseException` | Unchecked exception wrapping low-level `SQLException` to keep controller signatures clean. |
| **Collections API** | `List<Crop>`, `List<Season>`, `Map` | Standard type-safe collections used across all DAO data retrieval methods. |
| **Stream API** | Model methods & Dashboard aggregation | Filtering active seasons, calculating sums, and determining overdue statuses. |
| **JDBC Best Practices** | `PreparedStatement`, `try-with-resources` | Complete protection against SQL injection and automated resource closure. |

---

## 7. Project Folder Structure

```
greenfields/
├── lib/                                ← External JAR dependencies
│   ├── mysql-connector-j-8.4.0.jar
│   ├── jakarta.servlet-api-6.0.0.jar
│   └── h2-2.3.232.jar                  ← In-memory test engine (MySQL mode)
├── sql/
│   └── greenfields_db.sql              ← Complete DDL + demo data script
├── src/
│   ├── db.properties.example           ← Database configuration template
│   └── com/
│       └── greenfields/
│           ├── exception/              ← Custom exceptions (DatabaseException)
│           ├── filter/                 ← Security filters (AuthFilter)
│           ├── interfaces/             ← Business interfaces (Reportable, Schedulable)
│           ├── model/                  ← Domain models (BaseEntity, Crop, User, etc.)
│           ├── dao/                    ← DAO interfaces
│           │   └── impl/               ← JDBC DAO implementations
│           ├── servlet/                ← Controllers (LoginServlet, DashboardServlet, etc.)
│           ├── test/                   ← Verification suites (DAO, auth, dashboard, CRUD modules)
│           └── util/                   ← Database utilities (DBConnection)
├── webapp/
│   ├── WEB-INF/
│   │   └── web.xml                     ← Deployment descriptor
│   └── jsp/                            ← View templates (login.jsp, dashboard.jsp)
├── .gitignore                          ← Git exclusion rules
├── PROJECT_STATUS.md                   ← Development phases and progress tracker
└── README.md                           ← Project documentation
```

---

## 8. MySQL Setup Instructions

1. Start your local MySQL service (via MySQL Workbench, XAMPP, or Windows Services).
2. Open terminal or MySQL client and run the database initialization script:
   ```bash
   mysql -u root -p < sql/greenfields_db.sql
   ```
3. Verify that the `greenfields_db` database is created with all 6 tables populated with demo records for Paddy, Maize, and Tomato.

---

## 9. Configuring `db.properties`

1. Copy `src/db.properties.example` to `src/db.properties`:
   ```bash
   cp src/db.properties.example src/db.properties
   ```
2. Update the credentials with your local MySQL password:
   ```properties
   db.url=jdbc:mysql://localhost:3306/greenfields_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
   db.username=root
   db.password=your_mysql_password
   ```
   *(Note: `src/db.properties` is excluded by `.gitignore` so your personal password is never committed.)*

---

## 10. How to Compile & Test the Project

### Compiling Source Code
```powershell
Get-ChildItem -Recurse src -Filter "*.java" | Select-Object -ExpandProperty FullName | Out-File -Encoding ASCII sources.txt
javac --release 21 -cp "lib\*" -d out "@sources.txt"
Copy-Item src\db.properties out\ -Force
```

### Running Automated Test Suites
1. **DAO Layer Verification:**
   ```powershell
   java -cp "out;lib\*" com.greenfields.test.TestDAO
   ```
2. **Authentication & Session Verification:**
   ```powershell
   java -cp "out;lib\*" com.greenfields.test.TestAuth
   ```
3. **Fertilizer Management (servlet + H2 persistence):**
   ```powershell
   java -cp "out;lib\*" com.greenfields.test.TestFertilizerServlet
   ```
4. **Irrigation Management (servlet + H2 persistence):**
   ```powershell
   java -cp "out;lib\*" com.greenfields.test.TestIrrigationServlet
   ```
5. **Crop CRUD and dashboard regression:**
   ```powershell
   java -cp "out;lib\*" com.greenfields.test.TestCropServlet
   java -cp "out;lib\*" com.greenfields.test.TestDashboardServlet
   ```

The fertilizer and irrigation servlet suites use in-memory test DAOs for request/authorization behavior and H2 in MySQL compatibility mode for JDBC persistence and foreign-key checks. The DAO suite also falls back to H2 when local MySQL is unavailable. A successful H2 run does not establish live MySQL or Tomcat/JSP deployment behavior; those require the configured MySQL service and a Tomcat 10.1+ deployment.

---

## 11. Project Phase Tracker

- **Phase 1: Database Setup & DDL** — ✅ COMPLETE
- **Phase 2: Java Model Classes & OOP** — ✅ COMPLETE
- **Phase 3: JDBC DAO Layer** — ✅ COMPLETE
- **Phase 4: Authentication & Session Management** — ✅ COMPLETE
- **Phase 5: Crop Management CRUD** — ✅ COMPLETE
- **Phase 6: Dashboard Controller & Metrics Integration** — ✅ COMPLETE
- **Phase 7: Fertilizer Management** — ✅ COMPLETE
- **Phase 8: Irrigation Management** — ✅ COMPLETE
- **Phase 9: Season Management Module** — ⏳ NEXT
