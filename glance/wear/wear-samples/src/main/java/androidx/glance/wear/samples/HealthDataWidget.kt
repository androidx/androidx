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

package androidx.glance.wear.samples

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions
import android.os.Build
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.glance.wear.ExperimentalGlanceWearApi
import androidx.glance.wear.GlanceWearWidget
import androidx.glance.wear.GlanceWearWidgetService
import androidx.glance.wear.WearWidgetBrush
import androidx.glance.wear.WearWidgetData
import androidx.glance.wear.WearWidgetDocument
import androidx.glance.wear.color
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.health.DataTypes

class HealthDataWidgetService : GlanceWearWidgetService() {
    override val widget: GlanceWearWidget = HealthDataWidget()
}

private class HealthDataWidget : GlanceWearWidget() {
    override suspend fun provideWidgetData(
        context: Context,
        params: WearWidgetParams,
    ): WearWidgetData =
        WearWidgetDocument(background = WearWidgetBrush.color(Color.Red.rc)) {
            HealthDataWidgetContent()
        }
}

@OptIn(ExperimentalGlanceWearApi::class)
@RemoteComposable
@Composable
private fun HealthDataWidgetContent() {
    val context = LocalContext.current
    val hasHeartRatePermission = remember {
        ContextCompat.checkSelfPermission(context, getHeartRatePermission()) ==
            PackageManager.PERMISSION_GRANTED
    }
    val hasActivityRecognitionPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED
    }

    val isHeartRateValid =
        DataTypes.heartRateAccuracy
            .eq(DataTypes.HEART_RATE_ACCURACY_LOW)
            .or(DataTypes.heartRateAccuracy.eq(DataTypes.HEART_RATE_ACCURACY_MEDIUM))
            .or(DataTypes.heartRateAccuracy.eq(DataTypes.HEART_RATE_ACCURACY_HIGH))

    val heartRateStr = isHeartRateValid.select(DataTypes.heartRateBpm.toRemoteString(), "--".rs)

    RemoteColumn(
        modifier = RemoteModifier.fillMaxSize(),
        horizontalAlignment = RemoteAlignment.CenterHorizontally,
        verticalArrangement = RemoteArrangement.Center,
    ) {
        RemoteText(
            text =
                if (hasHeartRatePermission) {
                    "Heart Rate: ".rs + heartRateStr
                } else {
                    NO_HEART_RATE_PERMISSION_STR
                }
        )
        RemoteText(
            text =
                if (hasActivityRecognitionPermission) {
                    "Steps: ".rs + DataTypes.dailySteps.toRemoteString()
                } else {
                    NO_ACTIVITY_RECOGNITION_PERMISSION_STR
                }
        )
        RemoteText(
            text =
                if (hasActivityRecognitionPermission) {
                    "Calories: ".rs + DataTypes.dailyCalories.toRemoteString()
                } else {
                    NO_ACTIVITY_RECOGNITION_PERMISSION_STR
                }
        )
        RemoteText(
            text =
                if (hasActivityRecognitionPermission) {
                    "Distance: ".rs + DataTypes.dailyDistanceMeters.toRemoteString()
                } else {
                    NO_ACTIVITY_RECOGNITION_PERMISSION_STR
                }
        )
        RemoteText(
            text =
                if (hasActivityRecognitionPermission) {
                    "Floors: ".rs + DataTypes.dailyFloors.toRemoteString()
                } else {
                    NO_ACTIVITY_RECOGNITION_PERMISSION_STR
                }
        )
    }
}

private val NO_HEART_RATE_PERMISSION_STR = "No HR Permission".rs
private val NO_ACTIVITY_RECOGNITION_PERMISSION_STR = "No Activity Permission".rs

private fun getHeartRatePermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        HealthPermissions.READ_HEART_RATE
    } else {
        Manifest.permission.BODY_SENSORS
    }
