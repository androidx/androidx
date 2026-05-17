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

package androidx.xr.scenecore.testing

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.os.Looper
import android.os.ParcelFileDescriptor
import androidx.lifecycle.LifecycleOwner
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.SoundEffect
import androidx.xr.scenecore.SoundEffectPool
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SoundEffectPoolTesterTest {
    @Rule @JvmField val testRule = SceneCoreTestRule()

    private val activity =
        Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java)
            .create()
            .start()
            .get()
    private lateinit var session: Session
    private lateinit var context: Context

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        val result =
            Session.create(activity, testDispatcher, lifecycleOwner = activity as LifecycleOwner)
        assertThat(result).isInstanceOf(SessionCreateSuccess::class.java)
        session = (result as SessionCreateSuccess).session
        context = activity
    }

    @Test
    fun equalsAndHashCode_behaveCorrectly() {
        val pool = SoundEffectPool.create(session, 10)
        val tester1 = SoundEffectPoolTester.create(pool)
        val tester2 = SoundEffectPoolTester.create(pool)

        assertThat(tester1).isEqualTo(tester2)
        assertThat(tester1.hashCode()).isEqualTo(tester2.hashCode())
    }

    @Test
    fun isResourceLoaded_returnsCorrectValue() {
        val pool = SoundEffectPool.create(session, 10)
        val tester = testRule.createTester(pool)
        val resId = 123

        assertThat(tester.isResourceLoaded(resId)).isFalse()

        val soundEffect = pool.load(context, resId)
        assertThat(tester.isResourceLoaded(resId)).isTrue()

        pool.unload(soundEffect)
        assertThat(tester.isResourceLoaded(resId)).isFalse()
    }

    @Test
    fun isAssetLoaded_returnsCorrectValue() {
        val pool = SoundEffectPool.create(session, 10)
        val tester = testRule.createTester(pool)

        val tempFile = File.createTempFile("test", ".wav")
        val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val afd = AssetFileDescriptor(pfd, 0, -1)

        try {
            assertThat(tester.isAssetLoaded(afd)).isFalse()

            val soundEffect = pool.load(afd)
            assertThat(tester.isAssetLoaded(afd)).isTrue()

            pool.unload(soundEffect)
            assertThat(tester.isAssetLoaded(afd)).isFalse()
        } finally {
            afd.close()
            tempFile.delete()
        }
    }

    @Test
    fun isLoaded_returnsFalseAfterClose() {
        val pool = SoundEffectPool.create(session, 10)
        val tester = testRule.createTester(pool)
        val resId = 123

        pool.load(context, resId)

        try {
            assertThat(tester.isResourceLoaded(resId)).isTrue()
        } finally {
            pool.close()
        }
        assertThat(tester.isResourceLoaded(resId)).isFalse()
    }

    @Test
    fun triggerLoadCompleteListener_triggersListenerInPool() {
        val pool = SoundEffectPool.create(session, 10)
        val tester = testRule.createTester(pool)
        val resId = 123
        val soundEffect = pool.load(context, resId)

        var capturedSoundEffect: SoundEffect? = null
        var capturedSuccess: Boolean? = null
        pool.addLoadCompleteListener { sound, success ->
            capturedSoundEffect = sound
            capturedSuccess = success
        }

        tester.triggerLoadCompleteListener(soundEffect, true)
        shadowOf(Looper.getMainLooper()).idle()

        // SoundEffect doesn't implement equals, so we compare the id.
        assertThat(capturedSoundEffect?.id).isEqualTo(soundEffect.id)
        assertThat(capturedSuccess).isTrue()
    }
}
