/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.view

import androidx.camera.testing.CameraUtil
import androidx.camera.testing.CoreAppTestUtil
import androidx.camera.view.CameraView
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.asFlow
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraViewFragmentTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO)

    @Before
    fun setup() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        CoreAppTestUtil.clearDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun cameraView_canStream() = runBlocking {
        with(launchFragmentInContainer<CameraViewFragment>()) {
            val streamState = withFragment {
                (view?.findViewById<CameraView>(R.id.camera)?.previewStreamState)!!
            }.asFlow().first {
                it == PreviewView.StreamState.STREAMING
            }

            assertThat(streamState).isEqualTo(PreviewView.StreamState.STREAMING)
        }
    }
}

// Adapted from ActivityScenario.withActivity extension function
private inline fun <reified F : Fragment, T : Any> FragmentScenario<F>.withFragment(
    crossinline block: F.() -> T
): T {
    lateinit var value: T
    var err: Throwable? = null
    onFragment { fragment ->
        try {
            value = block(fragment)
        } catch (t: Throwable) {
            err = t
        }
    }
    err?.let { throw it }
    return value
}