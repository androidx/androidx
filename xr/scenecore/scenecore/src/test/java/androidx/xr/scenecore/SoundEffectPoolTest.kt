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

package androidx.xr.scenecore

import android.content.res.AssetFileDescriptor
import androidx.activity.ComponentActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.runtime.SoundEffect as RtSoundEffect
import androidx.xr.scenecore.testing.FakeSoundEffectPool
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundEffectPoolTest {

    private val activity =
        Robolectric.buildActivity(ComponentActivity::class.java).create().start().get()
    private lateinit var session: Session

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result = Session.create(activity, testDispatcher)

        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)

        session = (result as SessionCreateSuccess).session
    }

    @Test
    fun create_createsRuntimeSoundEffectPool() {
        val soundEffectPool = SoundEffectPool.create(session, 1)

        assertThat(soundEffectPool.rtSoundEffectPool).isInstanceOf(FakeSoundEffectPool::class.java)
    }

    @Test
    fun load_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val resId = 123

        val soundEffect = soundEffectPool.load(activity, resId)

        val fakePool = soundEffectPool.rtSoundEffectPool as FakeSoundEffectPool
        assertThat(fakePool.loadedResId).isEqualTo(resId)
        assertThat(soundEffect.id).isEqualTo(resId)
    }

    @Test
    fun load_withAfd_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val afd = mock<AssetFileDescriptor>()

        val soundEffect = soundEffectPool.load(afd)

        val fakePool = soundEffectPool.rtSoundEffectPool as FakeSoundEffectPool
        assertThat(fakePool.loadedAfd).isEqualTo(afd)
        assertThat(soundEffect).isNotNull()
    }

    @Test
    fun unload_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        val soundEffect = soundEffectPool.load(activity, 123)

        soundEffectPool.unload(soundEffect)

        val fakePool = soundEffectPool.rtSoundEffectPool as FakeSoundEffectPool
        assertThat(fakePool.unloadedSoundEffect?.id).isEqualTo(soundEffect.id)
    }

    @Test
    fun release_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)

        soundEffectPool.release()

        val fakePool = soundEffectPool.rtSoundEffectPool as FakeSoundEffectPool
        assertThat(fakePool.released).isTrue()
    }

    @Test
    fun setOnLoadCompleteListener_callsRuntime() {
        val soundEffectPool = SoundEffectPool.create(session, 1)
        var callbackCalled = false
        var loadedSoundEffect: SoundEffect? = null
        var loadedSuccess = false

        val listener =
            SoundEffectPool.LoadCompleteListener { soundEffect, success ->
                callbackCalled = true
                loadedSoundEffect = soundEffect
                loadedSuccess = success
            }

        soundEffectPool.setOnLoadCompleteListener(listener)

        val fakePool = soundEffectPool.rtSoundEffectPool as FakeSoundEffectPool
        assertThat(fakePool.loadCompleteListener).isNotNull()

        // Trigger the listener via the fake
        val rtSoundEffect = RtSoundEffect(123)
        fakePool.notifyLoadComplete(rtSoundEffect, true)

        assertThat(callbackCalled).isTrue()
        assertThat(loadedSoundEffect?.id).isEqualTo(123)
        assertThat(loadedSuccess).isTrue()
    }
}
