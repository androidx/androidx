/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.arcore.projected.testapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.TrackingState
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Stress test Activity to provoke update / configure / pause race conditions with HUD output. */
@OptIn(ExperimentalProjectedApi::class)
open class ThreadingStressTestActivity : ComponentActivity() {
    protected open fun usesProjectedScreen(): Boolean = true

    // --- Observable Telemetry & State ---
    private var currentModeTitle by mutableStateOf("Initializing...")
    private var currentLifecycleState by mutableStateOf("RESUMED")
    private var updateCount by mutableLongStateOf(0L)
    private var configCount by mutableLongStateOf(0L)
    private var lifecycleCount by mutableLongStateOf(0L)
    private var errorCount by mutableLongStateOf(0L)
    private var lastError by mutableStateOf("None")
    private var isStressTesting by mutableStateOf(true)

    // Configuration modes and telemetry
    private var isFastSwitchMode by mutableStateOf(false) // Default: Wait for non-0,0 lat/long
    private var runningConfigCount by mutableLongStateOf(0L)
    private var pausedConfigCount by mutableLongStateOf(0L)
    private var validPoseFixCount by mutableLongStateOf(0L)
    private var timeoutCount by mutableLongStateOf(0L)

    // XR Runtime instances
    private var sessionInstance by mutableStateOf<Session?>(null)
    private var geospatialInstance by mutableStateOf<Geospatial?>(null)
    private var arDeviceInstance by mutableStateOf<ArDevice?>(null)

    // Exact testing mode sequence (preserved order to provoke threading bugs)
    private val testModes =
        listOf(
            "6DoF (No Geo)" to GeospatialMode.DISABLED,
            "Low Power Geo" to GeospatialMode.INERTIAL,
            "High Power Geo" to GeospatialMode.SPATIAL,
        )
    private var testModeIndex = 0

    private val sessionLifecycleRegistry = LifecycleRegistry(this)
    private val sessionLifecycleOwner =
        object : LifecycleOwner {
            override val lifecycle: Lifecycle
                get() = sessionLifecycleRegistry
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionLifecycleRegistry.currentState = Lifecycle.State.RESUMED

        // Thread startup parameters (adapted from CL 4192255)
        val useBgThread = intent.getBooleanExtra("debug.jxr.geo.bg_thread", false)
        val delayMs = intent.getIntExtra("debug.jxr.geo.delay_ms", 0)
        val dispatcher = if (useBgThread) Dispatchers.IO else Dispatchers.Main
        Log.i(TAG, "onCreate: useBgThread=$useBgThread, delayMs=$delayMs, dispatcher=$dispatcher")

        initializeSession(dispatcher, delayMs)

        setContent { GlimmerTheme { StressTestDashboard() } }
    }

    private fun initializeSession(dispatcher: CoroutineDispatcher, delayMs: Int) {
        lifecycleScope.launch(dispatcher) {
            delay(delayMs.toLong())

            val sessionContext =
                if (usesProjectedScreen()) {
                    this@ThreadingStressTestActivity
                } else {
                    try {
                        ProjectedContext.createProjectedDeviceContext(
                            this@ThreadingStressTestActivity.applicationContext
                        )
                    } catch (e: Exception) {
                        this@ThreadingStressTestActivity
                    }
                }

            val result =
                Session.create(context = sessionContext, lifecycleOwner = sessionLifecycleOwner)
            if (result !is SessionCreateSuccess) {
                Log.e(TAG, "Session creation failed: $result")
                lastError = "Session create failed: $result"
                return@launch // Quick-out on failure
            }

            val session = result.session
            sessionInstance = session
            try {
                session.configure(
                    Config.Builder()
                        .setGeospatial(GeospatialMode.DISABLED)
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .build()
                )
                configCount++
                runningConfigCount++
            } catch (e: Exception) {
                recordError("Initial config error", e)
            }

            val arDevice = ArDevice.getInstance(session)
            arDeviceInstance = arDevice
            val geospatial = Geospatial.getInstance(session)
            geospatialInstance = geospatial

            Log.i(TAG, "Session created successfully, starting stress loops...")
            startStressLoops(session, arDevice, geospatial)
        }
    }

    private fun startStressLoops(session: Session, arDevice: ArDevice, geospatial: Geospatial) {
        // 1. Unified State Polling Loop (~100 FPS on Default)
        // [Note: Reading StateFlow .value accesses cached in-memory Kotlin state without triggering
        // AIDL calls.]
        lifecycleScope.launch(Dispatchers.Default) { runStatePollingLoop(arDevice, geospatial) }

        // 2. Coordinated Configuration & Lifecycle Stress Loop (Dispatchers.IO)
        lifecycleScope.launch(Dispatchers.IO) {
            runCoordinatedConfigLoop(session, arDevice, geospatial)
        }

        // 3. Secondary Overlapping Fast Configuration Loop (Dispatchers.IO)
        lifecycleScope.launch(Dispatchers.IO) { runOverlappingFastConfigLoop(session) }
    }

    private suspend fun runStatePollingLoop(arDevice: ArDevice, geospatial: Geospatial) {
        while (true) {
            if (!isStressTesting) {
                delay(PAUSE_IDLE_DELAY_MS)
                continue // Quick-out when testing is paused
            }
            try {
                // Accessing cached in-memory state objects (does not generate AIDL traffic)
                val unusedPose = arDevice.state.value.devicePose
                val unusedGeo = geospatial.state.value
                updateCount++
            } catch (e: Exception) {
                recordError("State polling error", e)
            }
            delay(STATE_POLL_INTERVAL_MS) // ~100 FPS
        }
    }

    private suspend fun runCoordinatedConfigLoop(
        session: Session,
        arDevice: ArDevice,
        geospatial: Geospatial,
    ) {
        while (true) {
            if (!isStressTesting) {
                delay(PAUSE_IDLE_DELAY_MS)
                continue
            }
            try {
                // Phase 1: Perform larger batch of configurations while RESUMED (running)
                setLifecycleState(Lifecycle.State.RESUMED, "RESUMED")
                executeConfigBatch(
                    session,
                    arDevice,
                    geospatial,
                    iterations = RESUMED_BATCH_ITERATIONS,
                    isPaused = false,
                )

                // Phase 2: Perform smaller batch of configurations while PAUSED
                setLifecycleState(Lifecycle.State.CREATED, "PAUSED")
                executeConfigBatch(
                    session,
                    arDevice,
                    geospatial,
                    iterations = PAUSED_BATCH_ITERATIONS,
                    isPaused = true,
                )
            } catch (e: Exception) {
                recordError("Coordinator error", e)
            }
        }
    }

    private suspend fun executeConfigBatch(
        session: Session,
        arDevice: ArDevice,
        geospatial: Geospatial,
        iterations: Int,
        isPaused: Boolean,
    ) {
        for (step in 1..iterations) {
            if (!isStressTesting) return // Quick-out if stopped mid-batch

            val (name, geoMode) = testModes[testModeIndex]
            testModeIndex = (testModeIndex + 1) % testModes.size
            currentModeTitle = if (isPaused) "$name (Paused)" else name

            try {
                session.configure(
                    Config.Builder()
                        .setGeospatial(geoMode)
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .build()
                )
                configCount++
                if (isPaused) pausedConfigCount++ else runningConfigCount++
            } catch (e: Exception) {
                recordError(if (isPaused) "Paused config error" else "Running config error", e)
            }

            // Quick-out delay for Fast Switch mode or Paused state
            if (isFastSwitchMode || isPaused) {
                delay(if (isFastSwitchMode) FAST_SWITCH_DELAY_MS else NORMAL_SWITCH_DELAY_MS)
                continue
            }

            // Default Mode 2: Wait up to 10s for XYZ/Geo state change in running state
            waitForStateChangeAfterConfig(arDevice, geospatial, geoMode, name)
        }
    }

    private suspend fun waitForStateChangeAfterConfig(
        arDevice: ArDevice,
        geospatial: Geospatial,
        geoMode: GeospatialMode,
        modeName: String,
    ) {
        val initialTranslation = arDevice.state.value.devicePose.translation
        val initialGeoPose = geospatial.state.value.geospatialPose
        val isGeospatialMode =
            geoMode == GeospatialMode.SPATIAL || geoMode == GeospatialMode.INERTIAL

        val startTime = System.currentTimeMillis()
        while (
            System.currentTimeMillis() - startTime < STATE_CHANGE_TIMEOUT_MS &&
                isStressTesting &&
                !isFastSwitchMode
        ) {
            val currTranslation = arDevice.state.value.devicePose.translation
            val xyzChanged = currTranslation != initialTranslation

            if (!isGeospatialMode) {
                // For 6DoF (No Geo): Wait until local XYZ translation changes from baseline
                if (xyzChanged) {
                    validPoseFixCount++
                    Log.i(TAG, "XYZ position updated after config in $modeName")
                    return // Quick-out upon detecting translation update
                }
            } else {
                // For Geo High or Low: Wait until BOTH XYZ and Lat/Long (or Alt) change from cached
                // state
                val currGeoPose = geospatial.state.value.geospatialPose
                val isNonZero = currGeoPose.latitude != 0.0 || currGeoPose.longitude != 0.0
                val geoChanged =
                    currGeoPose.latitude != initialGeoPose.latitude ||
                        currGeoPose.longitude != initialGeoPose.longitude ||
                        currGeoPose.altitude != initialGeoPose.altitude

                if (xyzChanged && isNonZero && geoChanged) {
                    validPoseFixCount++
                    Log.i(
                        TAG,
                        "XYZ & Geo updated (${currGeoPose.latitude}, ${currGeoPose.longitude}) in $modeName",
                    )
                    return // Quick-out upon verifying new un-cached geospatial sample
                }
            }
            delay(PAUSE_IDLE_DELAY_MS)
        }
        if (isStressTesting && !isFastSwitchMode) {
            timeoutCount++
            val target = if (isGeospatialMode) "XYZ & Lat/Long change" else "XYZ change"
            Log.w(TAG, "Timeout (10s) waiting for $target in $modeName")
        }
    }

    private suspend fun runOverlappingFastConfigLoop(session: Session) {
        val overlappingModes =
            listOf(GeospatialMode.SPATIAL, GeospatialMode.INERTIAL, GeospatialMode.DISABLED)
        var index = 0
        while (true) {
            // Quick-out when not in fast switch mode
            if (!isStressTesting || !isFastSwitchMode || sessionInstance == null) {
                delay(OVERLAPPING_LOOP_DELAY_MS)
                continue
            }
            try {
                val geoMode = overlappingModes[index]
                index = (index + 1) % overlappingModes.size
                session.configure(
                    Config.Builder()
                        .setGeospatial(geoMode)
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .build()
                )
                configCount++
                if (currentLifecycleState == "RESUMED") runningConfigCount++
                else pausedConfigCount++
            } catch (e: Exception) {
                recordError("Overlapping config error", e)
            }
            delay(OVERLAPPING_LOOP_DELAY_MS)
        }
    }

    private suspend fun setLifecycleState(targetState: Lifecycle.State, stateName: String) {
        if (currentLifecycleState == stateName) return // Quick-out if unchanged
        withContext(Dispatchers.Main) {
            sessionLifecycleRegistry.currentState = targetState
            currentLifecycleState = stateName
        }
        lifecycleCount++
    }

    private fun recordError(context: String, e: Throwable) {
        errorCount++
        Log.w(TAG, "$context: ${e.message}", e)
        lastError = "$context: ${e.javaClass.simpleName}: ${e.message}"
    }

    // --- Declarative Jetpack Compose UI ---

    @Composable
    private fun StressTestDashboard() {
        val geo = geospatialInstance
        val dev = arDeviceInstance

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(GlimmerTheme.colors.surface)
                    .padding(
                        top = if (usesProjectedScreen()) 70.dp else 130.dp,
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    )
        ) {
            Column(modifier = Modifier.align(Alignment.TopStart)) {
                if (geo != null && dev != null) {
                    ActiveDashboardContent(geo, dev)
                } else {
                    HudModeTitleText("Initializing Stress Test...")
                    Spacer(modifier = Modifier.height(8.dp))
                    HudDataText("Waiting for ARCore session initialization...")
                    if (lastError != "None") {
                        Spacer(modifier = Modifier.height(10.dp))
                        HudDataText("Error: $lastError", color = Color.Yellow)
                    }
                }
            }
        }
    }

    @Composable
    private fun ActiveDashboardContent(geospatial: Geospatial, arDevice: ArDevice) {
        val geospatialState by geospatial.state.collectAsState()
        val arDeviceState by arDevice.state.collectAsState()

        val geoStr = getGeospatialStateMessage(geospatialState)
        val devStr = getTrackingStateMessage(arDeviceState.trackingState)

        HudModeTitleText("Mode: $currentModeTitle | [$currentLifecycleState]")
        Spacer(modifier = Modifier.height(4.dp))
        HudDataText("State -> Geo: $geoStr | Device: $devStr")

        Spacer(modifier = Modifier.height(12.dp))
        HudTitleText("— REALTIME POSE & GEOSPATIAL —")

        val translation = arDeviceState.devicePose.translation
        val rotation = arDeviceState.devicePose.rotation
        HudDataText(
            "Local XYZ: %.3f, %.3f, %.3f".format(translation.x, translation.y, translation.z)
        )
        HudDataText(
            "Local Quat: %.3f, %.3f, %.3f, %.3f"
                .format(rotation.x, rotation.y, rotation.z, rotation.w)
        )

        val geoPose = geospatialState.geospatialPose
        HudDataText(
            "Lat/Lon: %.6f, %.6f | Alt: %.1fm"
                .format(geoPose.latitude, geoPose.longitude, geoPose.altitude)
        )
        HudDataText(
            "Acc(H/V/Yaw): %.1fm / %.1fm / %.1f°"
                .format(
                    geospatialState.horizontalAccuracy,
                    geospatialState.verticalAccuracy,
                    geospatialState.orientationYawAccuracy,
                )
        )

        Spacer(modifier = Modifier.height(16.dp))
        HudTitleText("— THREADING & LOCK STRESS METRICS —")
        HudDataText("State Polling (~100 FPS): $updateCount", color = Color.Green)
        HudDataText(
            "Config Cycles: $configCount (Running: $runningConfigCount | Paused: $pausedConfigCount)",
            color = Color.Cyan,
        )
        if (!isFastSwitchMode) {
            HudDataText(
                "State Changes (XYZ/Geo Updated): $validPoseFixCount | Timeouts (10s): $timeoutCount",
                color = Color.Cyan,
            )
        }
        HudDataText("Lifecycle Toggles: $lifecycleCount", color = Color.Magenta)
        HudDataText(
            "Error Count: $errorCount",
            color = if (errorCount > 0) Color.Red else Color.Gray,
        )
        if (lastError != "None") {
            HudDataText("Last Error: $lastError", color = Color.Yellow)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = { isStressTesting = !isStressTesting }) {
            Text(
                if (isStressTesting) "Pause Stress Loops" else "Resume Stress Loops",
                fontSize = 16.sp,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { isFastSwitchMode = !isFastSwitchMode }) {
            Text(
                if (isFastSwitchMode) "Mode: Fast Config Switch"
                else "Mode: Wait for XYZ/Geo Change (10s Timeout)",
                fontSize = 15.sp,
            )
        }
    }

    @Composable
    private fun HudTitleText(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            modifier = modifier,
        )
    }

    @Composable
    private fun HudModeTitleText(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            color = GlimmerTheme.colors.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            modifier = modifier,
        )
    }

    @Composable
    private fun HudDataText(text: String, modifier: Modifier = Modifier, color: Color? = null) {
        if (color != null) {
            Text(
                text = text,
                color = color,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                modifier = modifier,
            )
        } else {
            Text(text = text, fontSize = 16.sp, lineHeight = 20.sp, modifier = modifier)
        }
    }

    private fun getTrackingStateMessage(trackingState: TrackingState?): String {
        return when (trackingState) {
            TrackingState.TRACKING -> "TRACKING"
            TrackingState.PAUSED -> "PAUSED"
            TrackingState.STOPPED -> "STOPPED"
            else -> "UNKNOWN"
        }
    }

    private fun getGeospatialStateMessage(state: Geospatial.State?): String {
        return when (state?.geospatialTrackingState) {
            Geospatial.GeospatialTrackingState.RUNNING -> "RUNNING"
            Geospatial.GeospatialTrackingState.NOT_RUNNING -> "NOT_RUNNING"
            Geospatial.GeospatialTrackingState.ERROR_INTERNAL -> "ERROR_INTERNAL"
            Geospatial.GeospatialTrackingState.ERROR_NOT_AUTHORIZED -> "ERROR_NOT_AUTHORIZED"
            Geospatial.GeospatialTrackingState.ERROR_RESOURCE_EXHAUSTED ->
                "ERROR_RESOURCE_EXHAUSTED"
            Geospatial.GeospatialTrackingState.PAUSED -> "PAUSED"
            else -> "UNKNOWN"
        }
    }

    override fun onDestroy() {
        sessionLifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    companion object {
        private const val TAG = "ThreadingStressTestActivity"
        private const val STATE_POLL_INTERVAL_MS = 10L // ~100 FPS combined pose/geo polling
        private const val PAUSE_IDLE_DELAY_MS = 50L // Quick-out idle delay when testing is paused
        private const val FAST_SWITCH_DELAY_MS =
            15L // Delay between config toggles in Fast Switch mode
        private const val NORMAL_SWITCH_DELAY_MS = 150L // Delay after paused configuration step
        private const val OVERLAPPING_LOOP_DELAY_MS =
            20L // Pacing for overlapping config stress worker
        private const val STATE_CHANGE_TIMEOUT_MS =
            10_000L // Max duration waiting for pose/lat/long changes
        private const val RESUMED_BATCH_ITERATIONS = 6 // Config steps per RESUMED lifecycle phase
        private const val PAUSED_BATCH_ITERATIONS = 2 // Config steps per PAUSED lifecycle phase
    }
}
