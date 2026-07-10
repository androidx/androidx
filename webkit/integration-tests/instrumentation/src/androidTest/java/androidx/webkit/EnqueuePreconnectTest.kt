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

package androidx.webkit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.webkit.test.common.WebkitUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Test for [Profile.enqueuePreconnect]. */
@MediumTest
@RunWith(AndroidJUnit4::class)
class EnqueuePreconnectTest {
    private lateinit var defaultProfile: Profile

    @Before
    fun setUp() {
        defaultProfile =
            WebkitUtils.onMainThreadSync<Profile> {
                ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME)
            }
    }

    @Test
    fun doesNotCrash() {
        WebkitUtils.checkFeature(WebViewFeature.ENQUEUE_PRECONNECT)
        WebkitUtils.onMainThreadSync { defaultProfile.enqueuePreconnect(EXAMPLE_URL) }
    }

    @Test
    fun throws_whenFeatureDisabledOrUnsupported() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.ENQUEUE_PRECONNECT)) {
            WebkitUtils.onMainThreadSync {
                Assert.assertThrows(UnsupportedOperationException::class.java) {
                    defaultProfile.enqueuePreconnect(EXAMPLE_URL)
                }
            }
        }
    }

    companion object {
        private const val EXAMPLE_URL = "https://www.example.com"
    }
}
