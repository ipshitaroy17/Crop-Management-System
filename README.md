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
- **Reports & Crop History:** Filtered seasonal production, crop performance, fertilizer usage, irrigation records, and read-only crop histories across seasons.

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
│           ├── model/                  ← Domain and report view models
│           ├── dao/                    ← DAO interfaces
│           │   └── impl/               ← JDBC DAO implementations
│           ├── service/                ← DAO-backed report aggregation
│           ├── servlet/                ← Controllers (login, dashboard, CRUD, reports, history)
│           ├── test/                   ← Verification suites (DAO, auth, dashboard, modules)
│           └── util/                   ← Database utilities (DBConnection)
├── webapp/
│   ├── WEB-INF/
│   │   └── web.xml                     ← Deployment descriptor
│   └── jsp/                            ← Login, dashboard, CRUD, reports, and history views
├── .gitignore                          ← Git exclusion rules
├── PROJECT_STATUS.md                   ← Development phases and progress tracker
└── README.md                           ← Project documentation
```

---

## 8. MySQL Setup Instructions

The original `sql/greenfields_db.sql` is a destructive reset script: it begins with `DROP DATABASE IF EXISTS greenfields_db` and also drops all six tables. **Never run it against a database containing data.** It has not been made part of application startup or the Docker image. It also contains development/demo user seed records; those are intentionally absent from production initialization.

For a new, empty Railway database, select the database named by `MYSQLDATABASE` and run `sql/greenfields_railway_init.sql`. It creates the six required tables only, with no `DROP`, `TRUNCATE`, or data-seed statements. If a table already exists, stop and inspect rather than dropping or replacing it. The application does not run schema migrations automatically.

The application expects the six existing tables (`users`, `crops`, `seasons`, `fertilizer_applications`, `irrigation_schedules`, and `harvest_records`). The production schema contains no administrator account or credentials. After the application is deployed and its Railway MySQL variables are connected, create the first administrator using the hidden-prompt helper from an interactive Railway SSH session:

```bash
railway ssh -s <APP_SERVICE> -- java -cp '/usr/local/tomcat/webapps/greenfields/WEB-INF/classes:/usr/local/tomcat/webapps/greenfields/WEB-INF/lib/*' com.greenfields.tools.AdminAccountBootstrap
```

The helper refuses an existing username, uses prepared statements, requires a unique 16-100 character password entered without echo, and does not print the password or exception details. The current application schema and login code store/compare passwords as plaintext. Therefore this only protects the credential from source, shell history, and logs; it is **not secure at rest**. Do not use a password used anywhere else, and do not expose the app publicly with a real account until password hashing is implemented and existing credentials are migrated.

To initialize a dedicated empty local database (destructive; only do this if you intend to reset it), run:
   ```bash
   mysql -u root -p < sql/greenfields_db.sql
   ```

---

## 9. Configuring `db.properties`

For local development, copy `src/db.properties.example` to `src/db.properties`:
   ```bash
   cp src/db.properties.example src/db.properties
   ```
Then set your local MySQL connection values in that ignored file:
   ```properties
   db.url=jdbc:mysql://localhost:3306/greenfields_db?useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
   db.username=root
   db.password=your_mysql_password
   ```
   (`src/db.properties` is excluded by `.gitignore`.)

In production, `DBConnection` uses Railway's `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, and `MYSQLDATABASE` variables when present. The local `db.properties` fallback is retained for development and is not copied into the Docker image. Never place credentials in Git, Docker build arguments, image layers, or logs.

## 11. Railway Deployment (Docker)

The repository-root `Dockerfile` compiles production Java sources with Java 21, packages the existing JSP webapp and MySQL Connector/J into `greenfields.war`, and runs it on Tomcat 10.1.60. The container adjusts Tomcat's HTTP connector to Railway's injected `PORT` (with 8080 as a local default). Railway's health check is `/greenfields/login`.

1. Create a Railway project with a new application service connected to this GitHub repository and add a separate MySQL service. Initialize only a newly created empty database with `sql/greenfields_railway_init.sql`; do not import the reset SQL file into Railway.
2. In the application service's Variables panel, add Railway variable references to the MySQL service for `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, and `MYSQLDATABASE`. Use Railway's reference picker so the password remains a secret variable. `PORT` is supplied by Railway; do not set a different fixed port.
3. Deploy the application service from the repository root. Railway detects the root `Dockerfile`; the health check waits for `/greenfields/login` to return successfully.
4. Deploy, initialize the first administrator through the interactive helper above, then open `https://<your-railway-domain>/greenfields/login` and test authentication and the data-backed modules. Because the current schema stores plaintext passwords, use a unique temporary password until password hashing is implemented.

Local checks can be run with `docker build -t greenfields .` and `docker run --rm -p 8080:8080 -e PORT=8080 -e MYSQLHOST=... -e MYSQLPORT=... -e MYSQLUSER=... -e MYSQLPASSWORD=... -e MYSQLDATABASE=... greenfields`. Never paste a real password into shell history; use a local ignored env file or your shell's secure environment mechanism instead. Railway deployment and a live URL require a Railway account, service variables, and explicit deployment from the dashboard or authenticated CLI.

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
6. **Harvest & Yield Management (servlet + H2 persistence):**
   ```powershell
   java -cp "out;lib\*" com.greenfields.test.TestHarvestServlet
   ```
7. **Reports & Crop History (existing JDBC DAOs + H2 MySQL mode):**
   ```powershell
   java -cp "out;lib\*" com.greenfields.test.TestReportsServlet
   ```

Phase 10 reports are read-only and use existing DAOs/tables; filter IDs and crop-season relationships are checked server-side. Yield achievement uses `HarvestRecord.getYieldAchievementPercent()` only where actual yield exists and expected yield is non-zero. A NULL actual yield is Pending, while zero expected yield displays N/A. Fertilizer quantity is summed in the existing kilogram unit; irrigation is summarized by schedule count and recorded details. Crop History combines crop and season information with fertilizer, irrigation, and harvest records.

Fertilizer, irrigation, harvest, and reports suites use H2 in MySQL compatibility mode for database integration; request tests use test data. The harvest suite verifies pending NULL yield, zero expected yield, and safe handling of foreign-key errors. Only admins can mutate crop/input/harvest records; Reports and Crop History are read-only. The DAO suite falls back to H2 when local MySQL is unavailable. A successful H2 run does not establish live MySQL or Tomcat/JSP browser behavior; those require the configured MySQL service and a Tomcat 10.1+ deployment.

Phase 10 verification: 27 passed. Phase 3–9 regression totals: DAO 33, authentication 16, crop CRUD 11, dashboard 17, fertilizer 22, irrigation 27, and harvest/yield 35 passed. MySQL and Tomcat were unavailable for live database or browser deployment testing.

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
- **Phase 9: Harvest & Yield Management** — ✅ COMPLETE
- **Phase 10: Reports & Crop History** — ✅ COMPLETE
- **Phase 11: Season Management Module** — ⏳ NOT STARTED
- **Phase 12: Greenfields Theme Styling & Faculty Presentation** — ⏳ NOT STARTED
