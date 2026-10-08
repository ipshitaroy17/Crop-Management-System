# Project Status: Greenfields Agri Farm Crop Monitoring System

**Current Milestone:** Checkpoint 1 (Phases 1–4 Completed)  
**Last Updated:** Phase 4 Verification Complete  

---

## 📌 Phase Progress Overview

- [x] **Phase 1 — Database & DDL Initialization** — `COMPLETE`
- [x] **Phase 2 — Java 21 Domain Models & OOP Architecture** — `COMPLETE`
- [x] **Phase 3 — JDBC DAO Layer & Data Persistence** — `COMPLETE`
- [x] **Phase 4 — Authentication, Session Management & Servlets** — `COMPLETE`
- [ ] **Phase 5 — Crop Management CRUD Module** — `NEXT`
- [ ] **Phase 6 — Dashboard Controller & Metrics Integration**
- [ ] **Phase 7 — Season Management Module**
- [ ] **Phase 8 — Fertilizer Application Module**
- [ ] **Phase 9 — Irrigation Schedule Module**
- [ ] **Phase 10 — Harvest & Yield Analysis Module**
- [ ] **Phase 11 — Seasonal Production Reports**
- [ ] **Phase 12 — Greenfields Theme Styling & Faculty Presentation**

---

## 🏛️ Core Architectural Decisions Made

1. **Strict 3-Tier MVC Pattern:**
   - **View (JSP):** Renders HTML views using standard EL and scriptlets/JSTL.
   - **Controller (Servlets):** Manages HTTP request parameters, session lifecycle, and view dispatching.
   - **Model & Persistence (DAO + JDBC):** Domain models encapsulate business entities; DAO implementations handle direct JDBC operations.
   - **No heavy external frameworks:** Built intentionally with pure Servlets and JDBC to satisfy academic course requirements.

2. **Database & Yield Representation:**
   - Both `expected_yield_kg` and `actual_yield_kg` are stored side-by-side in `harvest_records`.
   - Rationale: Avoids redundant table JOINs for yield variance queries, keeping SQL beginner-friendly and clear during viva examinations.

3. **OOP Concepts Grounded in Real Needs:**
   - `BaseEntity`: Single base class eliminating duplicate `id` field across all 6 models and defining `describe()`.
   - `Reportable` interface: Contract for classes that generate report summaries (`Season`, `HarvestRecord`).
   - `Schedulable` interface: Contract for time-sensitive scheduled events (`IrrigationSchedule.isOverdue()`).
   - `DatabaseException`: Custom unchecked runtime exception wrapping low-level `SQLException`.

4. **Centralized Configuration & Credential Safety:**
   - Database credentials are read dynamically from `src/db.properties` at runtime.
   - `src/db.properties` is explicitly ignored by Git to prevent committing local passwords.
   - A sanitized template `src/db.properties.example` is committed for setup.

5. **Security & Session Strategy:**
   - Session authentication managed via standard `HttpSession`.
   - `AuthFilter` intercepts protected routes (`/*`), whitelists public assets (`/login`, `/logout`, `/css/*`), and prevents unauthenticated access.
   - 30-minute session timeout configured with `http-only` cookie settings in `web.xml`.

6. **Target Environment Compatibility:**
   - Java 21 LTS with Jakarta EE 6.0 (`jakarta.servlet.*`).
   - Ready for deployment on Apache Tomcat 10.1+.
