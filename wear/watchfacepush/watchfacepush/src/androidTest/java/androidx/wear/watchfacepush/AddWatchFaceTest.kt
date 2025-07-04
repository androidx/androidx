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
import androidx.wear.watchfacepush.*
import androidx.wear.watchfacepush.WatchFacePushManager.AddWatchFaceException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddWatchFaceTest {
    private var context: Context = ApplicationProvider.getApplicationContext()
    private var wfp = WatchFacePushManagerFactory.createWatchFacePushManager(context)

    @Before
    fun setup() {
        // Sets up the test environment by installing a sample watch face.
        // This ensures the test environment is consistent before each test.
        setup(context, SAMPLE_WATCHFACE)
    }

    @Test
    @RequiresWatch
    fun addWatchFace() {
        runBlocking {
            assertThat(wfp.listWatchFaces().installedWatchFaceDetails).isEmpty()
            readWatchFace(context, VALID_APK).use { pipe ->
                wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
            }
            assertThat(wfp.listWatchFaces().installedWatchFaceDetails).isNotEmpty()
        }
    }

    @Test
    @RequiresWatch
    fun addWatchface_onInvalidValidationToken() {
        val exception =
            assertThrows(AddWatchFaceException::class.java) {
                runBlocking {
                    readWatchFace(context, VALID_APK).use { pipe ->
                        wfp.addWatchFace(pipe.readFd, INVALID_TOKEN)
                    }
                }
            }

        assertThat(exception.errorCode)
            .isEqualTo(AddWatchFaceException.ERROR_INVALID_VALIDATION_TOKEN)
        assertThat(exception).hasMessageThat().contains("token")
    }

    @Test
    @RequiresWatch
    fun addWatchface_onTooManySlots() {
        val exception =
            assertThrows(AddWatchFaceException::class.java) {
                runBlocking {

                    // Add first watch face
                    readWatchFace(context, VALID_APK).use { pipe ->
                        wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
                    }

                    // Attempt to add a second watch face, exceeding the slot limit.
                    readWatchFace(context, VALID_APK).use { pipe ->
                        wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
                    }
                }
            }

        assertThat(exception.errorCode).isEqualTo(AddWatchFaceException.ERROR_SLOT_LIMIT_REACHED)
        assertThat(exception).hasMessageThat().contains("limit")
    }

    @Test
    @RequiresWatch
    fun addWatchface_onMalformedApk() {
        val exception =
            assertThrows(AddWatchFaceException::class.java) {
                runBlocking {
                    readWatchFace(context, MALFORMED_APK).use { pipe ->
                        wfp.addWatchFace(pipe.readFd, VALID_TOKEN)
                    }
                }
            }

        assertThat(exception.errorCode)
            .isEqualTo(AddWatchFaceException.ERROR_MALFORMED_WATCHFACE_APK)
        assertThat(exception).hasMessageThat().contains("APK")
    }

    @Test
    @RequiresWatch
    fun addWatchface_onUnexpectedContent() {
        val exception =
            assertThrows(AddWatchFaceException::class.java) {
                runBlocking {
                    readWatchFace(context, UNSECURE_APK).use { pipe ->
                        wfp.addWatchFace(pipe.readFd, UNSECURE_TOKEN)
                    }
                }
            }

        assertThat(exception).hasMessageThat().contains("Unexpected content")
        assertThat(exception.errorCode).isEqualTo(AddWatchFaceException.ERROR_UNEXPECTED_CONTENT)
    }

    @Test
    @RequiresWatch
    fun addWatchface_onInvalidPackageName() {
        val exception =
            assertThrows(AddWatchFaceException::class.java) {
                runBlocking {
                    readWatchFace(context, INVALID_PACKAGE_NAME_APK).use { pipe ->
                        wfp.addWatchFace(
                            pipe.readFd,
                            "XPdIIO1kCKDsA+lYKq2Mw5bjC1VEAK2kqUApZgA87Eg=:MS4wLjA=",
                        )
                    }
                }
            }

        assertThat(exception.errorCode).isEqualTo(AddWatchFaceException.ERROR_INVALID_PACKAGE_NAME)
        assertThat(exception).hasMessageThat().contains("package name")
    }
}
