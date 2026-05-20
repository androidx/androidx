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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore

import android.content.res.AssetFileDescriptor
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.testing.SceneCoreTestRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundEffectPoolTest {

    @get:Rule val scenecoreTestRule = SceneCoreTestRule()

    private lateinit var activity: ComponentActivity
    private lateinit var session: Session

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @Test
    fun create_createsRuntimeSoundEffectPool() {
        val soundEffectPool = SoundEffectPool.create(session, 1)

        assertThat(soundEffectPool.rtSoundEffectPool).isNotNull()
    }

    @Test
    fun load_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val soundEffectPoolTester = scenecoreTestRule.createTester(soundEffectPool)
        val resId = 123

        val soundEffect = soundEffectPool.load(activity, resId)

        assertThat(soundEffectPoolTester.isResourceLoaded(resId)).isTrue()
        assertThat(soundEffect.id).isEqualTo(resId)
    }

    @Test
    fun load_withAfd_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val soundEffectPoolTester = scenecoreTestRule.createTester(soundEffectPool)
        val afd = mock<AssetFileDescriptor>()

        val soundEffect = soundEffectPool.load(afd)

        assertThat(soundEffectPoolTester.isAssetLoaded(afd)).isTrue()
        assertThat(soundEffect).isNotNull()
    }

    @Test
    fun unload_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val soundEffectPoolTester = scenecoreTestRule.createTester(soundEffectPool)
        val resId = 123
        val soundEffect = soundEffectPool.load(activity, resId)
        assertThat(soundEffectPoolTester.isResourceLoaded(resId)).isTrue()

        soundEffectPool.unload(soundEffect)

        assertThat(soundEffectPoolTester.isResourceLoaded(resId)).isFalse()
    }

    @Test
    fun release_clearsAllResourcesFromTester() {
        val soundEffectPool = SoundEffectPool.create(session, 2)
        val soundEffectPoolTester = scenecoreTestRule.createTester(soundEffectPool)
        val resId = 123
        val afd = mock<AssetFileDescriptor>()

        // 1. Load both types of resources.
        soundEffectPool.load(activity, resId)
        soundEffectPool.load(afd)

        // 2. Verify both are currently loaded.
        assertThat(soundEffectPoolTester.isResourceLoaded(resId)).isTrue()
        assertThat(soundEffectPoolTester.isAssetLoaded(afd)).isTrue()

        // 3. Perform release.
        soundEffectPool.release()

        // 4. Verify both are cleared from the tester's perspective.
        // This indirectly confirms that the underlying fake has been released.
        assertThat(soundEffectPoolTester.isResourceLoaded(resId)).isFalse()
        assertThat(soundEffectPoolTester.isAssetLoaded(afd)).isFalse()
    }

    @Test
    fun addLoadCompleteListener_receivesCallback() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val tester = scenecoreTestRule.createTester(soundEffectPool)
        var callbackCalled = false
        var loadedSoundEffect: SoundEffect? = null
        var loadedSuccess = false

        val listener =
            SoundEffectPool.LoadCompleteListener { soundEffect, success ->
                callbackCalled = true
                loadedSoundEffect = soundEffect
                loadedSuccess = success
            }

        soundEffectPool.addLoadCompleteListener(listener)

        // Trigger the listener via the tester
        val soundEffect = SoundEffect(123)
        tester.triggerLoadCompleteListener(soundEffect, true)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(callbackCalled).isTrue()
        assertThat(loadedSoundEffect?.id).isEqualTo(123)
        assertThat(loadedSuccess).isTrue()
    }
}
