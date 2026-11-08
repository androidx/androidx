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

package androidx.xr.scenecore.integration.tests.e2e

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.scene
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Base class for activities that run a single SceneCore scenario on launch, driven by the
 * out-of-process end-to-end tests in `:xr:scenecore:integration-tests:release-tests`.
 *
 * These activities exist so that the tests can exercise SceneCore inside the R8-optimized release
 * build of this app, verifying that library keep rules (e.g. for JNI-bound classes) are correct.
 *
 * The outcome of [runScenario] is reported through a single [TextView] whose text starts with
 * [STATUS_SUCCESS] or [STATUS_FAILURE], which the tests observe via UiAutomator.
 */
abstract class ScenarioActivity : ComponentActivity() {

    private lateinit var statusView: TextView

    /** Runs the scenario under test. Throwing an exception reports a failure. */
    protected abstract suspend fun runScenario(session: Session)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusView = TextView(this).apply { text = STATUS_RUNNING }
        setContentView(statusView)

        lifecycleScope.launch {
            val status =
                try {
                    val sessionResult = Session.create(context = this@ScenarioActivity)
                    check(sessionResult is SessionCreateSuccess) {
                        "Session creation failed: $sessionResult"
                    }
                    val session = sessionResult.session
                    session.scene.keyEntity = session.scene.mainPanelEntity
                    runScenario(session)
                    STATUS_SUCCESS
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Log.e(javaClass.simpleName, "Scenario failed", e)
                    "$STATUS_FAILURE: $e"
                }
            statusView.text = status
        }
    }

    companion object {
        const val STATUS_RUNNING: String = "RUNNING"
        const val STATUS_SUCCESS: String = "SUCCESS"
        const val STATUS_FAILURE: String = "FAILURE"
    }
}
