# TECHNICAL IMPLEMENTATION PLAN: TO-DO LIST ANDROID APP

## OBJECTIVE
Provide strict, atomic, and sequential instructions to generate a production-ready Android application cloning the specified "Lista de Tareas by Splend Apps" interface and integrating bidirectionally with the Google Tasks API. 

## SECTION 1: ARCHITECTURE AND TECH STACK

**Architectural Pattern:** MVVM + Clean Architecture + Offline-First Sync.
*   **Presentation Layer:** Jetpack Compose, ViewModels, StateFlow.
*   **Domain Layer:** Use Cases (Interactors), Repository Interfaces, Domain Models.
*   **Data Layer:** Room (Local SSOT), Retrofit/Google Client (Remote), Repository Implementations, WorkManager (Sync).

**Tech Stack & Dependencies (To be defined in `build.gradle.kts`):**
*   **Language:** Kotlin.
*   **UI Toolkit:** Jetpack Compose (Material 3).
*   **Async/Reactive:** Kotlin Coroutines, Kotlin Flow.
*   **Local DB:** Room Database.
*   **Dependency Injection:** Dagger-Hilt.
*   **Network/API:** Retrofit2 with Gson converter / Google API Client Library for Kotlin.
*   **Authentication:** Android Credential Manager API (Google Sign-In) + Google Play Services Auth.
*   **Background Tasks:** WorkManager (CoroutineWorker).
*   **Speech-to-Text:** Android `SpeechRecognizer` Intent.
*   **Ads:** Google Mobile Ads SDK (AdMob) - For the persistent bottom banner.

## SECTION 2: GOOGLE TASKS API & AUTHENTICATION INTEGRATION

### 2.1 Authentication Flow
1.  Use `androidx.credentials:credentials` to prompt Google Sign-In.
2.  Request OAuth 2.0 Scope: `https://www.googleapis.com/auth/tasks`.
3.  Obtain Server Auth Code, exchange for Access Token via backend or Google Auth library. Store Access/Refresh tokens securely in EncryptedSharedPreferences.

### 2.2 Resource Mapping
**Remote -> Local Mapping Strategy:**
*   **Google `TaskList` (API) <-> `CategoryEntity` (Room)**
    *   API `id` (String) -> `remoteId`
    *   API `title` (String) -> `name`
*   **Google `Task` (API) <-> `TaskEntity` (Room)**
    *   API `id` (String) -> `remoteId`
    *   API `title` (String) -> `title`
    *   API `notes` (String) -> `description`
    *   API `due` (RFC 3339 String) -> `dueDate` (Long UTC)
    *   API `status` ("needsAction" / "completed") -> `isCompleted` (Boolean)
    *   API `updated` (RFC 3339 String) -> `lastUpdated` (Long UTC)

### 2.3 Offline-First Synchronization Strategy
*   **Single Source of Truth (SSOT):** The UI reads *only* from the Room Database using `Flow`.
*   **Sync States:** Every local entity requires a `SyncState` enum: `SYNCED`, `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`.
*   **WorkManager Integration:**
    *   On any local mutation (Create/Edit/Delete), update Room and set the corresponding `PENDING_*` state.
    *   Enqueue a `OneTimeWorkRequest` to `SyncWorker`.
    *   `SyncWorker` reads pending changes, executes API calls, and updates `SyncState` to `SYNCED` upon success.
    *   Periodic `PeriodicWorkRequest` polls for remote changes (resolving conflicts by comparing `updated` timestamps: Last-Write-Wins).

## SECTION 3: DATA MODELING & LOCAL DATABASE (ROOM)

```kotlin
// Data Models to generate in: com.app.tasks.data.local.entity

enum class SyncState { SYNCED, PENDING_CREATE, PENDING_UPDATE, PENDING_DELETE }

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val remoteId: String? = null, // Null if pending creation
    val name: String,
    val isDefault: Boolean = false,
    val syncState: SyncState = SyncState.PENDING_CREATE
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(entity = CategoryEntity::class, parentColumns = ["localId"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["categoryId"])]
)
data class TaskEntity(
    @PrimaryKey val localId: String = UUID.randomUUID().toString(),
    val remoteId: String? = null,
    val categoryId: String,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null, // Epoch millis
    val hasTime: Boolean = false,
    val isCompleted: Boolean = false,
    val repeatRule: String? = null, // e.g., "FREQ=DAILY"
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.PENDING_CREATE
)
```

## SECTION 4: MODULE BREAKDOWN & AI EXECUTION INSTRUCTIONS

### PHASE 1: Project Setup & Core Configuration
1.  **Generate `build.gradle.kts` (app):** Include Compose UI, Room, Hilt, WorkManager, Retrofit, Credential Manager, and AdMob dependencies. Enable `kapt` or `ksp` for Room and Hilt.
2.  **Generate `AndroidManifest.xml`:** Add `<uses-permission android:name="android.permission.INTERNET"/>` and `<uses-permission android:name="android.permission.RECORD_AUDIO"/>`. Register `AdMob` app ID in `<meta-data>`.
3.  **Generate `TaskApplication.kt`:** Create application class, annotate with `@HiltAndroidApp`.

### PHASE 2: Data Layer (Room & API)
1.  **Generate DAOs (`TaskDao.kt`, `CategoryDao.kt`):** 
    *   `CategoryDao`: Write `getCategoryWithTaskCount()` returning `Flow<List<CategoryWithCount>>`.
    *   `TaskDao`: Write `getAllTasks()`, `getTasksByCategory(id)`, `getPendingSyncTasks()`. Write functions to group tasks by completion and due date.
2.  **Generate `TaskDatabase.kt`:** RoomDatabase implementation including `TaskEntity` and `CategoryEntity`.
3.  **Generate `GoogleTasksApi.kt` (Retrofit Interface):**
    *   `@GET("tasks/v1/users/@me/lists") suspend fun getLists()`
    *   `@POST("tasks/v1/users/@me/lists/{tasklist}/tasks") suspend fun insertTask(...)`
    *   Include endpoints for Update, Delete, and List Tasks.
4.  **Generate Repositories (`TaskRepositoryImpl.kt`):** Implement domain interfaces. Functions must map Domain models to Entities, save to Room, and trigger `SyncWorker`.

### PHASE 3: Background Synchronization Engine
1.  **Generate `SyncWorker.kt` (extends `CoroutineWorker`):**
    *   Inject `TaskDao`, `CategoryDao`, `GoogleTasksApi`.
    *   **Push:** Query DAOs for `PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE`. Iterate and call respective API endpoints. Update Room `remoteId` and set `syncState = SYNCED`.
    *   **Pull:** Fetch remote lists and tasks. Compare `lastUpdated`. Upsert into Room.

### PHASE 4: Domain & Presentation (ViewModels)
1.  **Generate `MainViewModel.kt`:**
    *   Expose `StateFlow<List<CategoryWithCount>>` for the Drawer.
    *   Expose `StateFlow<Map<String, List<Task>>>` (Grouped by "Overdue", "Today", "No Date").
    *   Implement function `onSearch(query: String)`.
    *   Implement function `toggleTaskCompletion(taskId)`.
2.  **Generate `TaskFormViewModel.kt` (Shared for Create/Edit):**
    *   Expose `StateFlow<TaskFormState>` containing title, date, time, selectedCategoryId, isBatchMode, isRepeat.
    *   Functions: `saveTask()`, `deleteTask()`, `updateTitle(String)`, `onVoiceInput(String)`.

### PHASE 5: Jetpack Compose UI Implementation
*Target Screen Structure:*

1.  **Generate `MainActivity.kt` & Navigation:**
    *   Use `ModalNavigationDrawer` as the root container.
    *   Use `NavHost` for screens: `TaskList`, `TaskCreate`, `TaskEdit`, `Settings`.

2.  **Generate `AppDrawer.kt` (Navigation Drawer):**
    *   **Header:** Text "Lista de Tareas by Splend Apps".
    *   **List:** Iterate categories. Item layout: Text (name) + `Badge` (count of uncompleted tasks).
    *   **Footer:** Row with "Editar" (TextButton), "Configuración" (TextButton), and a `Switch` or `IconButton` for Theme (Sun/Moon icon).

3.  **Generate `TaskListScreen.kt`:**
    *   **TopAppBar:** Blue background. `IconButton(Icons.Default.Menu)` for drawer. Title reflects current filter (e.g., "Tareas / Todas"). `IconButton(Icons.Default.Search)`.
    *   **Content:** `LazyColumn`. Implement sections using `stickyHeader` or distinct list items. Section headers: "Atrasado" (Red text color), "Sin fecha" (Gray text color).
    *   **TaskItem (Card):** Dark blue background, rounded corners. Row layout: `Checkbox`, `Text` (multiline title). Below title: `Icon` (Calendar) + `Text` (Formatted Date/Time "EEE, dd MMM, hh:mm a"). Top Right corner: `Text` (Category tag).
    *   **FAB:** `FloatingActionButton` (rounded square) with `Icons.Default.Add`.
    *   **Bottom Banner:** Anchor `AndroidView` for AdMob `AdView` at the bottom of the screen.

4.  **Generate `TaskCreateScreen.kt` ("Nueva tarea"):**
    *   **TopBar:** Back button, Title "Nueva tarea".
    *   **Section 1 ("¿Qué hay que hacer?"):** `OutlinedTextField` with placeholder. `trailingIcon` = `Icons.Default.Mic`. When Mic is clicked, launch `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())` with `RecognizerIntent.ACTION_RECOGNIZE_SPEECH`. Add a `Row` below with a `Switch` for "Añadir en lote".
    *   **Section 2 ("Notificación"):** Clickable `Row`. Default text "No configurado" + Calendar Icon. On click, show `DatePickerDialog` and `TimePickerDialog`. Include repeat frequency dropdown.
    *   **Section 3 ("Lista de tareas"):** `ExposedDropdownMenuBox` showing current categories. Adjacent `IconButton` (Plus) to trigger a popup dialog to create a new list locally.
    *   **FAB:** `FloatingActionButton` in bottom right with `Icons.Default.Check` (Saves to Room, triggers worker, pops backstack).

5.  **Generate `TaskEditScreen.kt`:**
    *   **TopBar:** Back button, `Icons.Default.Share`, `Icons.Default.Delete`.
    *   **Section 1:** Text field populated with task title. Mic icon for speech. `Row` with `Checkbox` labeled "¿Tarea terminada?".
    *   **Section 2:** Two distinct clickable boxes: Date (Calendar icon) and Time (Clock icon). Time box includes an 'X' `IconButton` to clear the time. `Switch` for "Repetir" with loop icon.
    *   **Section 3:** Dropdown to change the assigned category.
    *   **FAB:** Save changes (Check icon).