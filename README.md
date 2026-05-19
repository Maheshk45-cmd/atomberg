# AtomQuest Goal Portal - Technical Architecture & Logic

This document provides a deep dive into the business logic, data models, and component architecture for both the Backend and Frontend of the AtomQuest Goal Portal.

---

## 🏗️ Backend Logic & Architecture

The backend is built as a RESTful API using Spring Boot, focusing on robust business rules, transactional integrity, and strict role-based access control.

### 1. Entity Relationships (Data Model)
*   **User:** Contains employee details, `Role` (`ADMIN`, `MANAGER`, `EMPLOYEE`), and a self-referential `manager_id` to build the reporting hierarchy.
*   **Cycle:** Represents a performance period (e.g., H1-2026). Goals are strictly tied to an active cycle.
*   **ThrustArea:** High-level strategic categories (e.g., *Innovation*, *Customer Success*) that all goals must align with.
*   **GoalSheet:** The core aggregate root for an employee's goals during a specific `Cycle`. Tracks the `status` (`DRAFT`, `SUBMITTED`, `APPROVED`, `RETURNED`) and `locked` boolean.
*   **Goal:** Belongs to a `GoalSheet` and a `ThrustArea`. Tracks `targetValue`, `weightage` (%), and `uom` (Unit of Measurement).
*   **AuditLog:** Tracks any administrative forced edits on locked goals to ensure compliance.

### 2. Core Business Rules (`GoalSheetService`)
*   **Initialization Flow:** When an employee requests their dashboard, the system looks for the active `Cycle`. If a `GoalSheet` doesn't exist for this cycle, it automatically initializes one in the `DRAFT` state.
*   **Validation Constraints:**
    *   An employee can have a maximum of **8 goals** per sheet.
    *   To transition a sheet from `DRAFT` to `SUBMITTED`, the total `weightage` of all goals must equal exactly **100%**.
*   **State Machine Transitions:**
    *   `DRAFT` ➔ `SUBMITTED`: Employee finalizes goals. This triggers a WebSocket notification sent directly to their Manager's active session.
    *   `SUBMITTED` ➔ `APPROVED`: Manager approves. The sheet is marked `locked = true`, preventing further edits by the employee.
    *   `SUBMITTED` ➔ `RETURNED`: Manager rejects. Employee can edit and resubmit.
*   **Administrative Overrides:** Admins can edit goals even after they are locked (`APPROVED`). This triggers a mandatory insert into the `AuditLog` capturing the old value, new value, and the admin's typed reason.

### 3. Authentication Flow (`SecurityConfig` & JWT)
*   **Login (`AuthController`):** Validates credentials using `AuthenticationManager`. Upon success, `JwtTokenProvider` issues a signed JWT containing the user's UUID and Role.
*   **Validation (`JwtAuthenticationFilter`):** Intercepts all `/api/**` requests (except `/login`), validates the Bearer token signature, and populates the `SecurityContextHolder`.
*   **Method Security:** Spring Security's `@PreAuthorize("hasRole('...')")` is used heavily on Controller methods to enforce that only Managers can approve sheets, only Employees can submit them, etc.

---

## 💻 Frontend Logic & Architecture

The frontend is a React SPA (Single Page Application) built with Vite. It relies on Redux for global state and Axios for secure API communication.

### 1. State Management (Redux)
*   **`authSlice`:** Manages the user's authentication state. Stores the JWT `token`, `role`, `userId`, and `name`.
*   **Persistence:** The token and user details are synced with `localStorage` to survive page reloads. 

### 2. API Communication (Axios Interceptors)
*   Located in `src/services/api.js`.
*   **Request Interceptor:** Automatically retrieves the JWT from `localStorage` and attaches it as a `Bearer` token to the `Authorization` header of every outgoing request.
*   **Response Interceptor:** Catches `401 Unauthorized` responses globally. If a token expires, it automatically clears storage and forces the user back to the login screen.

### 3. Component Architecture & Role-Based Routing
Routing is handled dynamically; when a user logs in, the app renders the appropriate dashboard based on their Redux `role`:

*   **`LoginPage.jsx`:** Captures email/password, dispatches the login API call, and saves the resulting token to Redux.
*   **`EmployeeDashboard.jsx`:**
    *   *Logic:* On mount (`useEffect`), it fetches the active `GoalSheet`, `ThrustAreas`, and `Goals`.
    *   *UI Calculation:* Sums up goal weightages to display a progress bar (must hit 100%). Hides the "+ Add Goal" and "Submit" buttons if the sheet is `APPROVED` or `SUBMITTED`.
    *   *Modals:* Contains forms for Adding Goals and Logging Achievements for specific quarters.
*   **`ManagerDashboard.jsx`:**
    *   *Logic:* Fetches all `GoalSheets` where the `manager_id` matches the current logged-in user.
    *   *UI:* Displays a team roster. Allows the manager to click into an employee's sheet, edit goals inline (if `SUBMITTED`), and Approve or Return the sheet.
    *   *WebSockets:* Opens a connection to `/topic/manager/{managerId}` to listen for live "Goal Submitted" notifications from employees.
*   **`AdminDashboard.jsx`:**
    *   *Logic:* Has global visibility over all `Users`, `Cycles`, and `GoalSheets` across the organization.
    *   *UI:* Allows forced edits on locked goals. A prompt requires the Admin to enter a "Reason" which is sent in the API request params for the backend Audit Log.

### 4. UI/UX Details & "AI" SMART Checker
*   **AI SMART Checker:** In the Employee "Add Goal" modal, a debounced React function analyzes the goal title text in real-time. If the title lacks numbers (Measurable) or timeframes (Time-bound like "Q2" or "Month"), it dynamically renders UI suggestions (e.g., *"Try adding 'by Q2' to make it time-bound"*).
*   **Ant Design:** Forms, Tables, Modals, and notification messages heavily rely on `antd` for robust, pre-built behaviors.
