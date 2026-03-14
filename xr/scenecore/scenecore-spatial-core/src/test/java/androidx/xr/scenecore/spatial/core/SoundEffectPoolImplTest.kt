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

package androidx.xr.scenecore.spatial.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.xr.scenecore.runtime.SoundEffect
import androidx.xr.scenecore.runtime.SoundEffectPool
import androidx.xr.scenecore.runtime.SoundPoolExtensionsWrapper
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SoundEffectPoolImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockSoundPoolExtensions = mock<SoundPoolExtensionsWrapper>()

    @Test
    fun load_returnsSoundEffectWithId() {
        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, null)
        val soundEffect = soundEffectPool.load(context, 123)

        assertThat(soundEffect.id).isEqualTo(1)
    }

    @Test
    fun setOnLoadCompleteListener_callsListener() {
        val soundEffectPool = SoundEffectPoolImpl(1, mockSoundPoolExtensions, null)
        var callbackCalled = false
        var loadedSoundEffect: SoundEffect? = null
        var loadedSuccess = false
        val resId = 123

        val testExecutor = TestExecutor()
        val loadCompleteListener =
            SoundEffectPool.LoadCompleteListener { soundEffect, success ->
                callbackCalled = true
                loadedSoundEffect = soundEffect
                loadedSuccess = success
            }

        soundEffectPool.setOnLoadCompleteListener(testExecutor, loadCompleteListener)

        val soundEffect = soundEffectPool.load(context, resId)

        val soundPool = soundEffectPool.soundPool
        val shadowSoundPool = Shadows.shadowOf(soundPool)

        shadowSoundPool.notifyResourceLoaded(resId, true)

        assertThat(callbackCalled).isTrue()
        assertThat(loadedSoundEffect?.id).isEqualTo(soundEffect.id)
        assertThat(loadedSuccess).isTrue()
        assertThat(testExecutor.lastExecuted()).isNotNull()
    }
}

class TestExecutor : Executor {
    private var mLastExecuted: Runnable? = null

    override fun execute(command: Runnable) {
        command.run()
        mLastExecuted = command
    }

    fun lastExecuted(): Runnable? {
        return mLastExecuted
    }
}
