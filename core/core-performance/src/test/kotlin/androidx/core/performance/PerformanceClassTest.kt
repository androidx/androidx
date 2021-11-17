/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.performance

import android.os.Build.VERSION_CODES.R
import android.os.Build.VERSION_CODES.S
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemProperties

/** Unit tests for [PerformanceClass]. */
@RunWith(RobolectricTestRunner::class)
class PerformanceClassTest {

    private val pc = PerformanceClass()

    @Test
    @Config(maxSdk = R)
    fun getPerformanceClass_sdk30() {
        assertThat(pc.getPerformanceClass()).isEqualTo(0)
    }

    @Test
    // Note this test is not actually running because robolectric does not support sdk31 yet
    @Ignore("b/206673076")
    @Config(minSdk = S)
    fun getPerformanceClass_sdk31() {
        // TODO(b/205732671): Use ShadowBuild.setMediaPerformanceClass when available
        ShadowSystemProperties.override("ro.odm.build.media_performance_class", "31")
        assertThat(pc.getPerformanceClass()).isEqualTo(31)
    }
}