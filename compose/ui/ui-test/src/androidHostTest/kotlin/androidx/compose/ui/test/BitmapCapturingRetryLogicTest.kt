/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import android.view.PixelCopy
import androidx.compose.testutils.expectError
import androidx.compose.ui.test.android.PixelCopyException
import androidx.compose.ui.test.android.runWithRetryWhenNoData
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BitmapCapturingRetryLogicTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun pixelCopyRequest_succeeded_noRetries() {
        var attempt = 0

        expectError<PixelCopyException>(false) {
            runWithRetryWhenNoData {
                try {
                    // success
                } finally {
                    attempt++
                }
            }
        }

        assertThat(attempt).isEqualTo(1)
    }

    @Test
    fun pixelCopyRequest_succeeded_afterRetry_whenNoData() {
        var attempt = 0

        expectError<PixelCopyException>(false) {
            runWithRetryWhenNoData {
                try {
                    if (attempt == 0) {
                        throw PixelCopyException(PixelCopy.ERROR_SOURCE_NO_DATA)
                    } else {
                        // success
                    }
                } finally {
                    attempt++
                }
            }
        }
    }

    @Test
    fun pixelCopyRequest_retry_whenNoData() {
        var attempt = 0

        expectError<PixelCopyException> {
            runWithRetryWhenNoData {
                try {
                    throw PixelCopyException(PixelCopy.ERROR_SOURCE_NO_DATA)
                } finally {
                    attempt++
                }
            }
        }
        assertThat(attempt).isEqualTo(3)
    }

    @Test
    fun pixelCopyRequest_error_rethrow() {
        expectError<PixelCopyException> {
            runWithRetryWhenNoData { throw PixelCopyException(PixelCopy.ERROR_UNKNOWN) }
        }
    }

    @Test
    fun pixelCopyRequest_error_rethrow_afterRetry() {
        var attempt = 0

        expectError<PixelCopyException> {
            runWithRetryWhenNoData {
                try {
                    if (attempt == 0) {
                        throw PixelCopyException(PixelCopy.ERROR_SOURCE_NO_DATA)
                    } else {
                        throw PixelCopyException(PixelCopy.ERROR_UNKNOWN)
                    }
                } finally {
                    attempt++
                }
            }
        }
        assertThat(attempt).isEqualTo(2)
    }
}
