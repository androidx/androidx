/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.watchfacepush.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.wear.watchfacepush.*
import androidx.wear.watchfacepush.WatchFacePushManager.IsWatchFaceActiveException
import androidx.wear.watchfacepush.WatchFacePushManager.SetWatchFaceAsActiveException
import androidx.wear.watchfacepush.WatchFacePushManager.WatchFaceDetails
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IsWatchFaceActiveTest {
    private var context: Context = ApplicationProvider.getApplicationContext()
    private var wfp = WatchFacePushManagerFactory.createWatchFacePushManager(context)

    @Rule
    @JvmField
    var mRuntimePermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant("com.google.wear.permission.SET_PUSHED_WATCH_FACE_AS_ACTIVE")

    @Before
    fun setup() {
        setup(context, SAMPLE_WATCHFACE)
    }

    @Test
    @RequiresWatch
    fun isWatchFaceActive_onNonActive() {
        assertThat(
                runBlocking {
                    readWatchFace(context, VALID_APK).use { pipe ->
                        wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
                    }
                    wfp.isWatchFaceActive(SAMPLE_WATCHFACE)
                }
            )
            .isFalse()
    }

    @Test
    @RequiresWatch
    fun isWatchFaceActive_onActive() {
        assertThat(
                runBlocking {
                    val details: WatchFaceDetails =
                        readWatchFace(context, VALID_APK).use { pipe ->
                            wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
                        }
                    wfp.setWatchFaceAsActive(details.slotId)
                    wfp.isWatchFaceActive(SAMPLE_WATCHFACE)
                }
            )
            .isTrue()
    }

    @Test
    @RequiresWatch
    fun isWatchFaceActive_onNotInstalledPackage() {
        val exception =
            assertThrows(IsWatchFaceActiveException::class.java) {
                runBlocking { wfp.isWatchFaceActive("weird package name") }
            }

        assertThat(exception).isNotNull()
        assertThat(exception.errorCode)
            .isEqualTo(IsWatchFaceActiveException.Companion.ERROR_INVALID_PACKAGE_NAME)
        assertThat(exception.message).contains("package name")
    }

    @Test
    @RequiresWatch
    fun isWatchFaceActive_setActive_badSlot() {
        val exception =
            assertThrows(SetWatchFaceAsActiveException::class.java) {
                runBlocking { wfp.setWatchFaceAsActive("an invalid slot") }
            }

        assertThat(exception.errorCode)
            .isEqualTo(SetWatchFaceAsActiveException.Companion.ERROR_INVALID_SLOT_ID)
        assertThat(exception).hasMessageThat().contains("slot ID")
    }
}
