/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.health.connect.client.contracts

import android.content.Context
import android.health.connect.HealthConnectManager
import android.os.Build
import androidx.health.platform.client.service.HealthDataServiceConstants
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@MediumTest
class ExerciseRouteRequestContractTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun requestExerciseRoute_createIntent_emptySessionIdThrowsIAE() {
        assertThrows(IllegalArgumentException::class.java) {
            ExerciseRouteRequestContract().createIntent(context, "")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun requestExerciseRoute_createIntent_hasPlatformIntentAction() {
        val intent = ExerciseRouteRequestContract().createIntent(context, "sessionId")
        assertThat(intent.action).isEqualTo(HealthConnectManager.ACTION_REQUEST_EXERCISE_ROUTE)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun requestExerciseRoute_createIntent_hasApkIntentAction() {
        val intent = ExerciseRouteRequestContract().createIntent(context, "sessionId")
        assertThat(intent.action).isEqualTo(HealthDataServiceConstants.ACTION_REQUEST_ROUTE)
    }
}
