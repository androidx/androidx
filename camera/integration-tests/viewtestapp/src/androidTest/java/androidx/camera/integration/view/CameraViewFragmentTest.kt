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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.asFlow
import androidx.lifecycle.testing.TestLifecycleOwner
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
import org.junit.rules.TestRule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraViewFragmentTest {

    @get:Rule
    val useCamera: TestRule = CameraUtil.grantCameraPermissionAndPreTest()

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.RECORD_AUDIO
        )

    @Before
    fun setup() {
        Assume.assumeTrue(CameraUtil.deviceHasCamera())
        CoreAppTestUtil.assumeCompatibleDevice()
        // Clear the device UI and check if there is no dialog or lock screen on the top of the
        // window before start the test.
        CoreAppTestUtil.prepareDeviceUI(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun cameraView_canStream_defaultLifecycle() = runBlocking {
        with(launchFragmentInContainer<CameraViewFragment>()) { assertStreaming() }
    }

    @Test
    fun cameraView_canStream_withActivityLifecycle() = runBlocking {
        with(
            launchFragmentInContainer<CameraViewFragment>(
                fragmentArgs = bundleOf(
                    CameraViewFragment.ARG_LIFECYCLE_TYPE to
                        CameraViewFragment.LIFECYCLE_TYPE_ACTIVITY
                )
            )
        ) { assertStreaming() }
    }

    @Test
    fun cameraView_canStream_withFragmentLifecycle() = runBlocking {
        with(
            launchFragmentInContainer<CameraViewFragment>(
                fragmentArgs = bundleOf(
                    CameraViewFragment.ARG_LIFECYCLE_TYPE to
                        CameraViewFragment.LIFECYCLE_TYPE_FRAGMENT
                )
            )
        ) { assertStreaming() }
    }

    @Test
    fun cameraView_canStream_withFragmentViewLifecycle() = runBlocking {
        with(
            launchFragmentInContainer<CameraViewFragment>(
                fragmentArgs = bundleOf(
                    CameraViewFragment.ARG_LIFECYCLE_TYPE to
                        CameraViewFragment.LIFECYCLE_TYPE_FRAGMENT_VIEW
                )
            )
        ) { assertStreaming() }
    }

    @Test
    fun cameraView_ignoresLifecycleInDestroyedState() {
        // Since launchFragmentInContainer waits for onResume() to complete, CameraView should
        // have measured its view and bound to the lifecycle by this time. This would crash with
        // an IllegalArgumentException prior to applying fix for b/157949175
        launchFragmentInContainer(
            fragmentArgs = bundleOf(
                CameraViewFragment.ARG_LIFECYCLE_TYPE to CameraViewFragment.LIFECYCLE_TYPE_DEBUG
            ),
            instantiate = {
                CameraViewFragment().apply {
                    setDebugLifecycleOwner(TestLifecycleOwner(Lifecycle.State.DESTROYED))
                }
            }
        )
    }
}

private suspend inline fun FragmentScenario<CameraViewFragment>.assertStreaming() {
    val streamState = withFragment {
        (view?.findViewById<CameraView>(R.id.camera)?.previewStreamState)!!
    }.asFlow().first {
        it == PreviewView.StreamState.STREAMING
    }

    assertThat(streamState).isEqualTo(PreviewView.StreamState.STREAMING)
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