# WalkingPad MC21 Pro Controller & Analytics

A modern, high-performance Android replacement for KS Fit engineered specifically for the KingSmith WalkingPad MC21, delivering real-time Bluetooth Low Energy (FTMS) speed control, goal-oriented workout routines, interval training, Room-backed telemetry logging, and interactive performance analytics.

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural and operational decisions confirmed during Phase 1 clarification are locked into this plan:

- **Confirmed Decision 1 (Connectivity)**: Real BLE connection exclusively communicating directly with the WalkingPad MC21 via Bluetooth SIG Fitness Machine Service (`0x1826`), Fitness Machine Control Point (`0x2AD9`), Treadmill Data (`0x2ACD`), and Status notifications (`0x2ADA`) with KingSmith bit-13 step extensions.
- **Confirmed Decision 2 (Control Modes)**: Full control suite including a responsive speed wheel/stepper (0.5 – 10.0 km/h in 0.1/0.5 km/h increments), instant quick-speed presets (Walk 3.0, Brisk 4.5, Jog 6.0, Run 8.0 km/h), target goals (time, distance, calories), and structured interval training with automated speed ramps.
- **Confirmed Decision 3 (Analytics & Export)**: Detailed per-second workout telemetry stored locally in Room database, rendered in interactive custom Jetpack Compose charts (speed profile, step cadence, calorie burn), alongside personal records tracking and one-tap CSV/JSON data export.

---

## 1. Overview & Core Concept

- **What It Does**: Connects directly over Bluetooth Low Energy to the WalkingPad MC21, replacing the proprietary KS Fit application. Provides zero-latency speed adjustments, safety emergency stop, live dashboard displaying speed, elapsed time, distance, step count, and estimated calories, automated interval routines, and comprehensive historical analytics with interactive charts.
- **Target Audience / Persona**: WalkingPad MC21 owners seeking a clean, telemetry-rich, responsive fitness companion free from third-party account walls, intrusive telemetry, or laggy cloud dependencies.
- **Key Value**: Low-latency, reliable FTMS Bluetooth control, structured interval routines that automatically adjust belt speed, and private on-device workout logs with rich visual analytics and CSV/JSON exports.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Device Discovery & Connection Flow**:
   - The app scans for nearby Bluetooth devices advertising the WalkingPad MC21 service or name.
   - Shows connection status with signal strength (RSSI), battery/status indicators, and instant reconnect memory.
2. **Real-Time Workout & Speed Control Flow**:
   - A central athletic speed gauge and large digital readout with haptic/tactile feedback.
   - Quick speed buttons (+0.1, -0.1, +0.5, -0.5, and preset chips: 2.0, 3.5, 5.0, 6.5, 8.0 km/h).
   - High-contrast prominent Start, Pause, Resume, and red emergency Stop triggers.
   - Live metrics bar displaying distance (km), active time (MM:SS), step count, cadence (SPM), and estimated kcal.
3. **Structured Workout & Interval Mode**:
   - Configure target goals: Distance (e.g. 3.0 km), Time (e.g. 30 min), or Calories (e.g. 200 kcal).
   - Pre-configured and customizable HIIT / Interval routines (e.g., Warm-up 3.0 km/h -> Sprint 6.5 km/h -> Recover 3.5 km/h -> Cool-down 2.5 km/h) with automatic belt speed commands and countdown audio cues.
4. **History & Analytics Hub**:
   - Summary cards displaying total distance, lifetime workouts, total steps, and streak.
   - Interactive Compose telemetry charts displaying speed curves over time, pace consistency, and calorie accumulation.
   - Detailed workout review sheets with ability to export raw session data to CSV or JSON.

### Visual Identity & Theme
- **Aesthetic Direction**: Precision athletic performance cockpit. Deep carbon slate surface (`#0D1117`) paired with vibrant electric volt accents (`#00E676` / `#00F5D4`), hyper-legible numerical displays, and crisp status cards.
- **Color Palette**:
  - `Background`: Deep Carbon (`#0A0E14`) and Surface (`#141923`)
  - `Primary / Accent`: Electric Volt Green (`#00E676`) for active state and target speeds
  - `Secondary Accent`: Cyan Horizon (`#00D2FF`) for distance and cadence metrics
  - `Emergency / Warning`: Hot Coral Red (`#FF3B30`) for immediate stop and disconnect alerts
  - `Card / Elevation`: Elevated Dark Surface (`#1A2230`) with subtle outline borders
- **Typography & Hierarchy**: Bold athletic monospace/tabular numerals for speed, cadence, and timers to prevent layout jitter; clean geometric sans-serif for labels and navigation.
- **Interactive Feedback**: M3 ripples, subtle spring animations on speed adjustments, and prominent touch targets adhering to 48dp accessibility standards.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native FTMS (Fitness Machine Service) Profile Implementation**
  - *Chosen Approach*: Implement standard Bluetooth SIG FTMS UUIDs (`0x1826`, `0x2ACD`, `0x2AD9`, `0x2ADA`) with the KingSmith vendor bit-13 step count decoder and sequence timing for CCCD subscriptions.
  - *Why*: The MC21 natively speaks standard FTMS, ensuring clean control packet exchange without fragile reverse-engineered cloud bridges.
  - *Alternatives Considered*: KS Fit cloud API emulation (rejected: unreliable, requires internet and third-party login).
- **Decision 2: Room SQLite for Private, Resilient Local Storage**
  - *Chosen Approach*: Store completed workouts and per-minute telemetry points locally using Android Room with reactive Kotlin `Flow` queries.
  - *Why*: Workouts are logged immediately and reliably without network lag, keeping health metrics private on the user's phone with zero login barriers.
  - *Alternatives Considered*: Cloud sync (not requested by user; local-first satisfies immediate tracking, zero-latency retrieval, and easy CSV export).
- **Decision 3: Dedicated Foreground Service for Workout Continuity**
  - *Chosen Approach*: Support background connection monitoring so the belt doesn't disconnect or lose telemetry if the user switches to music or video apps while walking.
  - *Why*: WalkingPad workouts typically happen while the user listens to podcasts or watches media; background BLE stability is essential.

---

## 4. Technical Architecture & Data Strategy

### Architecture & System Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI                              │
│  ┌───────────────────┐  ┌───────────────────┐  ┌────────────────────┐  │
│  │ Dashboard / Speed │  │ Interval Programs │  │ History & Analytics│  │
│  │   Control Screen  │  │ & Target Workout  │  │  & Data Export     │  │
│  └─────────┬─────────┘  └─────────┬─────────┘  └──────────┬─────────┘  │
└────────────┼──────────────────────┼───────────────────────┼────────────┘
             ▼                      ▼                       ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        WalkingPadViewModel                             │
│  - Real-time speed state & command dispatch                            │
│  - Active workout session timer & telemetry accumulator               │
│  - Interval program state machine                                      │
└────────────┬──────────────────────────────────────────────┬────────────┘
             ▼                                              ▼
┌───────────────────────────────┐              ┌─────────────────────────┐
│     WalkingPadBleManager      │              │    WorkoutRepository    │
│  - BLE Scanner & BluetoothGatt│              │  - Room WorkoutDao      │
│  - FTMS 0x1826 / 0x2ACD / 2AD9│              │  - Session & Telemetry  │
│  - Bit-13 Step Decoder        │              │  - CSV / JSON Exporter  │
└────────────┬──────────────────┘              └────────────┬────────────┘
             ▼                                              ▼
┌───────────────────────────────┐              ┌─────────────────────────┐
│ Physical WalkingPad MC21 BLE  │              │ Room SQLite Database    │
└───────────────────────────────┘              └─────────────────────────┘
```

### Data Model & Entities
1. **WorkoutSession**:
   - `id: Long` (Auto-increment PK)
   - `startTime: Long`, `endTime: Long`, `durationSeconds: Int`
   - `totalDistanceMeters: Float`
   - `totalSteps: Int`
   - `caloriesBurned: Int`
   - `avgSpeedKmh: Float`, `maxSpeedKmh: Float`
   - `mode: String` (MANUAL, INTERVAL, TARGET_GOAL)
   - `notes: String?`
2. **WorkoutTelemetrySample**:
   - `id: Long`, `sessionId: Long` (FK)
   - `timestampMs: Long`
   - `speedKmh: Float`
   - `cadenceSpm: Int`
   - `cumulativeDistanceMeters: Float`
   - `instantaneousCalories: Float`
3. **IntervalProgram**:
   - `id: Long`, `title: String`, `description: String`
   - `stagesJson: String` (serialized list of Stage: speed, durationSeconds, name)

### Bluetooth Communication Sequence
1. Scan for devices advertising Service `0x1826` or matching KingSmith/MC21 name.
2. Connect GATT, request MTU 512, discover services.
3. Enable notifications on Treadmill Data (`0x2ACD`) and Status (`0x2ADA`) descriptors.
4. Send "Request Control" opcode `[0x00]` to Control Point (`0x2AD9`).
5. Dispatch speed commands `[0x02, LSB, MSB]` and start/stop commands `[0x07]` / `[0x08, 0x01]`.
