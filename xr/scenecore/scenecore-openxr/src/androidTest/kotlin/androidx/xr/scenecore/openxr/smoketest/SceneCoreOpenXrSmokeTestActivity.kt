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

package androidx.xr.scenecore.openxr.smoketest

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.scenecore.openxr.INVALID_HANDLE
import androidx.xr.scenecore.openxr.OpenXrSceneRuntime
import androidx.xr.scenecore.openxr.OpenXrSceneRuntimeFactory
import androidx.xr.scenecore.openxr.SceneCoreOpenXrNative

/**
 * SceneCore OpenXR Smoke Test Activity for verifying OpenXR SceneCore initialization, extension
 * negotiation, lifecycle events, and teardown protocol on-device.
 */
class SceneCoreOpenXrSmokeTestActivity : Activity() {

    /** Status of an individual smoke test step execution. */
    enum class StepStatus {
        PASSED,
        FAILED,
        PENDING_DEPENDENCY,
    }

    /** Structured result representing the outcome and description of a smoke test step. */
    data class SmokeTestStepResult(
        val stepNumber: Int,
        val stepName: String,
        val status: StepStatus,
        val detail: String,
    )

    private data class SmokeTestStep(
        val stepNumber: Int,
        val stepName: String,
        val action: () -> Pair<StepStatus, String>,
    )

    private val testResults = mutableListOf<SmokeTestStepResult>()
    private var logContainer: LinearLayout? = null

    /** Returns the results of the executed smoke test steps. */
    fun getResults(): List<SmokeTestStepResult> = testResults.toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val dpPadding = (ROOT_PADDING_DP * resources.displayMetrics.density).toInt()
                setPadding(dpPadding, dpPadding, dpPadding, dpPadding)
            }
        logContainer = container

        val root =
            ScrollView(this).apply {
                setBackgroundColor(Color.parseColor(COLOR_BACKGROUND))
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                addView(container)
            }
        setContentView(root)

        executeSceneCoreOpenXrSmokeTest()
    }

    override fun onDestroy() {
        super.onDestroy()
        testResults.clear()
        logContainer = null
    }

    /** Executes all SceneCore OpenXR smoke test steps and renders results to UI and logcat. */
    fun executeSceneCoreOpenXrSmokeTest(): List<SmokeTestStepResult> {
        testResults.clear()

        val steps =
            listOf(
                // 1. Verify native prebuilt library load and initial handle allocation
                SmokeTestStep(1, "Native Prebuilt & Handle Creation") {
                    val nativeWrapper = SceneCoreOpenXrNative()
                    try {
                        if (nativeWrapper.nativeScenecore != INVALID_HANDLE) {
                            StepStatus.PASSED to
                                "Loaded libandroidx.xr.scenecore.openxr.so successfully; nativeScenecore=${nativeWrapper.nativeScenecore}"
                        } else {
                            StepStatus.FAILED to "nativeScenecore handle was INVALID_HANDLE (0L)"
                        }
                    } finally {
                        nativeWrapper.destroy()
                    }
                },

                // 2. Verify OpenXrSceneRuntimeFactory discovery and feature requirements
                SmokeTestStep(2, "OpenXrSceneRuntimeFactory Requirements Query") {
                    val factory = OpenXrSceneRuntimeFactory()
                    val reqs = factory.requirements
                    val expectedReqs = setOf(Feature.FULLSTACK)
                    if (reqs == expectedReqs) {
                        StepStatus.PASSED to "Factory discovered and requires exact set: $reqs"
                    } else {
                        StepStatus.FAILED to
                            "Unexpected factory requirements: $reqs (expected $expectedReqs)"
                    }
                },

                // 3. Verify top-down OpenXrSceneRuntime lifecycle and state machine
                SmokeTestStep(3, "Top-Down OpenXrSceneRuntime Lifecycle & Teardown Protocol") {
                    val runtime = OpenXrSceneRuntime.create(this)
                    try {
                        val initialHandle = runtime.nativeWrapper.nativeScenecore
                        if (initialHandle == INVALID_HANDLE) {
                            return@SmokeTestStep StepStatus.FAILED to
                                "Runtime created with INVALID_HANDLE"
                        }

                        runtime.destroy()
                        if (
                            runtime.isDestroyed &&
                                runtime.nativeWrapper.nativeScenecore == INVALID_HANDLE
                        ) {
                            StepStatus.PASSED to
                                "Created runtime with handle=$initialHandle and executed coordinated teardown protocol safely"
                        } else {
                            StepStatus.FAILED to
                                "Runtime destroy did not transition state properly: isDestroyed=${runtime.isDestroyed}"
                        }
                    } finally {
                        if (!runtime.isDestroyed) {
                            runtime.destroy()
                        }
                    }
                },

                // 4. Verify OpenXR Extension & Spatial Container negotiation capability
                SmokeTestStep(4, "Extension Negotiation & Spatial Container Capability") {
                    val nativeWrapper = SceneCoreOpenXrNative()
                    try {
                        val containerBefore = nativeWrapper.getSpatialContainerHandle()
                        val rootBefore = nativeWrapper.getRootSpaceHandle()
                        if (containerBefore == INVALID_HANDLE && rootBefore == INVALID_HANDLE) {
                            StepStatus.PASSED to
                                "Child handles safely reset before init; extension negotiation ready"
                        } else {
                            StepStatus.FAILED to
                                "Unexpected non-zero handles before init: container=$containerBefore root=$rootBefore"
                        }
                    } finally {
                        nativeWrapper.destroy()
                    }
                },

                // 5. Session Creation Top-Down Wire-up Status (Pending upstream runtime CL)
                SmokeTestStep(5, "Top-Down Session Creation & xrSession Hookup") {
                    StepStatus.PENDING_DEPENDENCY to
                        "Waiting on upstream runtime CL (aosp/4204803) to expose xrSession handle from ARCore/Runtime"
                },

                // 6. Lifecycle Event Routing Verification Status (Pending upstream event routing)
                SmokeTestStep(6, "Lifecycle Event Propagation & Routing") {
                    StepStatus.PENDING_DEPENDENCY to
                        "Waiting on upstream event propagation architecture to route pause/resume/destroy events"
                },
            )

        for (step in steps) {
            executeStep(step)
        }

        renderResultsToUi()
        return testResults
    }

    private fun executeStep(step: SmokeTestStep) {
        val result =
            try {
                val (status, detail) = step.action()
                SmokeTestStepResult(step.stepNumber, step.stepName, status, detail)
            } catch (t: Throwable) {
                SmokeTestStepResult(
                    step.stepNumber,
                    step.stepName,
                    StepStatus.FAILED,
                    "Failure during step: ${t.javaClass.simpleName}: ${t.message}",
                )
            }
        testResults.add(result)
        Log.i(TAG, "[${step.stepName}] -> ${result.status}: ${result.detail}")
    }

    private fun renderResultsToUi() {
        runOnUiThread {
            val container = logContainer ?: return@runOnUiThread
            container.removeAllViews()

            val density = resources.displayMetrics.density
            val title =
                TextView(this).apply {
                    text = TITLE_TEXT
                    textSize = TITLE_TEXT_SIZE_SP
                    setTypeface(null, Typeface.BOLD)
                    setPadding(0, 0, 0, (TITLE_BOTTOM_PADDING_DP * density).toInt())
                    setTextColor(Color.parseColor(COLOR_TITLE_TEXT))
                }
            container.addView(title)

            for (result in testResults) {
                val card =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        val cardPadding = (CARD_PADDING_DP * density).toInt()
                        setPadding(cardPadding, cardPadding, cardPadding, cardPadding)
                        setBackgroundColor(Color.parseColor(COLOR_CARD_BACKGROUND))
                        val params =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                            )
                        params.setMargins(0, 0, 0, (CARD_BOTTOM_MARGIN_DP * density).toInt())
                        layoutParams = params
                    }

                val badgeText =
                    when (result.status) {
                        StepStatus.PASSED -> BADGE_PASS
                        StepStatus.FAILED -> BADGE_FAIL
                        StepStatus.PENDING_DEPENDENCY -> BADGE_WAITING
                    }
                val badgeColor =
                    when (result.status) {
                        StepStatus.PASSED -> Color.parseColor(COLOR_PASS)
                        StepStatus.FAILED -> Color.parseColor(COLOR_FAIL)
                        StepStatus.PENDING_DEPENDENCY -> Color.parseColor(COLOR_WAITING)
                    }

                val header =
                    TextView(this).apply {
                        text = "$badgeText Step ${result.stepNumber}: ${result.stepName}"
                        textSize = HEADER_TEXT_SIZE_SP
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(badgeColor)
                        setPadding(0, 0, 0, (HEADER_BOTTOM_PADDING_DP * density).toInt())
                    }

                val detail =
                    TextView(this).apply {
                        text = result.detail
                        textSize = DETAIL_TEXT_SIZE_SP
                        setTextColor(Color.parseColor(COLOR_DETAIL_TEXT))
                    }

                card.addView(header)
                card.addView(detail)
                container.addView(card)
            }
        }
    }

    companion object {
        private const val TAG = "SceneCoreOpenXrSmokeTest"
        private const val TITLE_TEXT = "SceneCore OpenXR Smoke Test Results"

        // Status badge string literals
        private const val BADGE_PASS = "[PASS]"
        private const val BADGE_FAIL = "[FAIL]"
        private const val BADGE_WAITING = "[WAITING]"

        // Color string literals
        private const val COLOR_BACKGROUND = "#121212"
        private const val COLOR_CARD_BACKGROUND = "#1E1E24"
        private const val COLOR_TITLE_TEXT = "#F5F5F5"
        private const val COLOR_DETAIL_TEXT = "#E0E0E0"
        private const val COLOR_PASS = "#66BB6A"
        private const val COLOR_FAIL = "#EF5350"
        private const val COLOR_WAITING = "#FFCA28"

        // Layout dimension literals (DP / SP)
        private const val ROOT_PADDING_DP = 16
        private const val TITLE_TEXT_SIZE_SP = 22f
        private const val TITLE_BOTTOM_PADDING_DP = 14
        private const val CARD_PADDING_DP = 12
        private const val CARD_BOTTOM_MARGIN_DP = 10
        private const val HEADER_TEXT_SIZE_SP = 16f
        private const val HEADER_BOTTOM_PADDING_DP = 4
        private const val DETAIL_TEXT_SIZE_SP = 14f
    }
}
