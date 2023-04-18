/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.ambient

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.wearable.compat.WearableActivityController
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AmbientLifecycleObserverImplTest {
    private lateinit var scenario: ActivityScenario<AmbientLifecycleObserverTestActivity>

    @Before
    fun setUp() {
        scenario = AmbientTestActivityUtil.launchActivity(
            AmbientLifecycleObserverTestActivity::class.java)
    }

    private fun resetState(controller: WearableActivityController) {
        controller.mCreateCalled = false
        controller.mDestroyCalled = false
        controller.mPauseCalled = false
        controller.mResumeCalled = false
        controller.mStopCalled = false
    }

    @Test
    fun testEnterAmbientCallback() {
        scenario.onActivity { activity ->
            WearableActivityController.getLastInstance().enterAmbient()
            assertTrue(activity.enterAmbientCalled)
            assertFalse(activity.exitAmbientCalled)
            assertFalse(activity.updateAmbientCalled)

            assertNotNull(activity.enterAmbientArgs)

            // Nothing in the bundle, both should be false.
            assertFalse(activity.enterAmbientArgs!!.burnInProtectionRequired)
            assertFalse(activity.enterAmbientArgs!!.deviceHasLowBitAmbient)
        }
    }

    @Test
    fun testEnterAmbientCallbackWithArgs() {
        scenario.onActivity { activity ->
            val bundle = Bundle()
            bundle.putBoolean(WearableActivityController.EXTRA_LOWBIT_AMBIENT, true)
            bundle.putBoolean(WearableActivityController.EXTRA_BURN_IN_PROTECTION, true)

            WearableActivityController.getLastInstance().enterAmbient(bundle)

            assertTrue(activity.enterAmbientArgs!!.burnInProtectionRequired)
            assertTrue(activity.enterAmbientArgs!!.deviceHasLowBitAmbient)
        }
    }

    @Test
    fun testExitAmbientCallback() {
        scenario.onActivity { activity ->
            WearableActivityController.getLastInstance().exitAmbient()
            assertFalse(activity.enterAmbientCalled)
            assertTrue(activity.exitAmbientCalled)
            assertFalse(activity.updateAmbientCalled)
        }
    }

    @Test
    fun testUpdateAmbientCallback() {
        scenario.onActivity { activity ->
            WearableActivityController.getLastInstance().updateAmbient()
            assertFalse(activity.enterAmbientCalled)
            assertFalse(activity.exitAmbientCalled)
            assertTrue(activity.updateAmbientCalled)
        }
    }

    @Test
    fun onCreateCanPassThrough() {
        // Default after launch is that the activity is running.
        val controller = WearableActivityController.getLastInstance()
        assertTrue(controller.mCreateCalled)
        assertFalse(controller.mDestroyCalled)
        assertFalse(controller.mPauseCalled)
        assertTrue(controller.mResumeCalled)
        assertFalse(controller.mStopCalled)
    }

    @Test
    fun onPauseCanPassThrough() {
        val controller = WearableActivityController.getLastInstance()
        resetState(controller)

        // Note: STARTED is when the app is paused; RUNNING is when it's actually running.
        scenario.moveToState(Lifecycle.State.STARTED)

        assertFalse(controller.mCreateCalled)
        assertFalse(controller.mDestroyCalled)
        assertTrue(controller.mPauseCalled)
        assertFalse(controller.mResumeCalled)
        assertFalse(controller.mStopCalled)
    }

    @Test
    fun onStopCanPassThrough() {
        val controller = WearableActivityController.getLastInstance()
        resetState(controller)

        scenario.moveToState(Lifecycle.State.CREATED)

        assertFalse(controller.mCreateCalled)
        assertFalse(controller.mDestroyCalled)
        assertTrue(controller.mPauseCalled)
        assertFalse(controller.mResumeCalled)
        assertTrue(controller.mStopCalled)
    }

    @Test
    fun onDestroyCanPassThrough() {
        val controller = WearableActivityController.getLastInstance()
        resetState(controller)

        scenario.moveToState(Lifecycle.State.DESTROYED)

        assertFalse(controller.mCreateCalled)
        assertTrue(controller.mDestroyCalled)
        assertTrue(controller.mPauseCalled)
        assertFalse(controller.mResumeCalled)
        assertTrue(controller.mStopCalled)
    }

    @Test
    fun canQueryInAmbient() {
        scenario.onActivity { activity ->
            val controller = WearableActivityController.getLastInstance()
            assertFalse(activity.observer.isAmbient)
            controller.isAmbient = true
            assertTrue(activity.observer.isAmbient)
        }
    }
}
