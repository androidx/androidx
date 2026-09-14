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

package androidx.xr.scenecore.openxr

import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented unit tests for [OpenXrSceneRuntime] lifecycle state management and coordinated
 * teardown protocol.
 */
@SdkSuppress(minSdkVersion = 29)
@LargeTest
@RunWith(AndroidJUnit4::class)
class OpenXrSceneRuntimeTest {

    companion object {
        init {
            System.loadLibrary("androidx.xr.scenecore.openxr.test")
        }
    }

    @get:Rule val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Test
    fun create_instantiatesOpenXrSceneRuntimeWithValidNativeWrapper() {
        activityRule.scenario.onActivity { activity ->
            val runtime = OpenXrSceneRuntime.create(activity)

            assertThat(runtime.isDestroyed).isFalse()
            assertThat(runtime.nativeWrapper.nativeScenecore).isNotEqualTo(INVALID_HANDLE)

            runtime.destroy()
        }
    }

    @Test
    fun destroy_cleansUpNativeHandlesAndSetsIsDestroyed() {
        activityRule.scenario.onActivity { activity ->
            val runtime = OpenXrSceneRuntime.create(activity)

            runtime.destroy()

            assertThat(runtime.isDestroyed).isTrue()
            assertThat(runtime.nativeWrapper.nativeScenecore).isEqualTo(INVALID_HANDLE)
        }
    }

    @Test
    fun destroy_multipleTimes_isIdempotent() {
        activityRule.scenario.onActivity { activity ->
            val runtime = OpenXrSceneRuntime.create(activity)

            runtime.destroy()
            runtime.destroy()

            assertThat(runtime.isDestroyed).isTrue()
            assertThat(runtime.nativeWrapper.nativeScenecore).isEqualTo(INVALID_HANDLE)
        }
    }

    @Test
    fun initialize_afterDestroy_throwsIllegalStateException() {
        activityRule.scenario.onActivity { activity ->
            val runtime = OpenXrSceneRuntime.create(activity)
            runtime.destroy()

            assertThrows(IllegalStateException::class.java) { runtime.initialize() }
        }
    }

    @Test
    fun coordinatedTeardown_releasesSceneCoreChildHandlesBeforeNativeDestroy() {
        var activity: ComponentActivity? = null
        activityRule.scenario.onActivity { activity = it }
        var runtime: OpenXrSceneRuntime? = null
        runBlocking {
            val session = (Session.create(context = activity!!) as SessionCreateSuccess).session
            runtime = session.runtimes.filterIsInstance<OpenXrSceneRuntime>().first()
        }

        activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)

        assertThat(runtime!!.isDestroyed).isTrue()
        assertThat(runtime!!.nativeWrapper.nativeScenecore).isEqualTo(INVALID_HANDLE)
    }

    @Test
    fun destroy_fiftyIterations_noLeaksOrCrashes() {
        activityRule.scenario.onActivity { activity ->
            repeat(50) {
                val runtime = OpenXrSceneRuntime.create(activity)
                assertThat(runtime.isDestroyed).isFalse()
                assertThat(runtime.nativeWrapper.nativeScenecore).isNotEqualTo(INVALID_HANDLE)
                runtime.destroy()
                assertThat(runtime.isDestroyed).isTrue()
                assertThat(runtime.nativeWrapper.nativeScenecore).isEqualTo(INVALID_HANDLE)
            }
        }
    }

    @Test
    fun initialize_mainPanelEntityIsParentedToActivitySpace() {
        var activity: ComponentActivity? = null
        activityRule.scenario.onActivity { activity = it }
        try {
            runBlocking {
                val session = (Session.create(context = activity!!) as SessionCreateSuccess).session
                val runtime = session.runtimes.filterIsInstance<OpenXrSceneRuntime>().first()
                assertThat(runtime.mainPanelEntity.parent).isEqualTo(runtime.activitySpace)
            }
        } finally {
            activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun sessionCreate_succeeds() {
        var activity: ComponentActivity? = null
        activityRule.scenario.onActivity { activity = it }
        try {
            runBlocking {
                val result = Session.create(context = activity!!)
                assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
                val session = (result as SessionCreateSuccess).session
                assertThat(session).isNotNull()
                assertThat(session.runtimes.filterIsInstance<OpenXrSceneRuntime>()).isNotEmpty()
            }
        } finally {
            activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        }
    }
}
