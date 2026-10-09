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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.SensorBuilder
import org.robolectric.shadows.SensorEventBuilder
import org.robolectric.shadows.ShadowSensorManager

/** Sensor system variables in the embedded player, driven through Robolectric's sensor manager. */
@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerSensorTest {

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    private lateinit var sensorManager: ShadowSensorManager
    private lateinit var accelerometer: Sensor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sensorManager = shadowOf(context.getSystemService(SensorManager::class.java))
        accelerometer = SensorBuilder.newBuilder().setType(Sensor.TYPE_ACCELEROMETER).build()
        sensorManager.addSensor(accelerometer)
    }

    @Test
    fun accelerometerReadingDrivesTheDocument() {
        // A marker whose x position is the accelerometer's x reading. Like the View player,
        // sensors are subscribed for variables read by expressions.
        val document = buildDocument {
            painter.setColor(AndroidColor.RED).commit()
            writer.drawCircle(accelerationX(), 50f, 6f)
        }
        play(document)
        assertThat(sensorManager.listeners).isNotEmpty()

        sendAcceleration(70f)
        rule.waitForIdle()

        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        assertRed(bitmap, 70, 50)
        assertNotRed(bitmap, 6, 50)
    }

    @Test
    fun documentsWithoutSensorVariablesRegisterNoListener() {
        play(
            buildDocument {
                painter.setColor(AndroidColor.RED).commit()
                writer.drawCircle(50f, 50f, 6f)
            }
        )

        assertThat(sensorManager.listeners).isEmpty()
    }

    @Test
    fun listenerIsUnregisteredWhenThePlayerLeavesComposition() {
        val document = buildDocument { writer.drawCircle(accelerationX(), 50f, 6f) }
        var show by mutableStateOf(true)
        rule.setContent {
            if (show) Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) }
        }
        rule.waitForIdle()
        assertThat(sensorManager.listeners).isNotEmpty()

        show = false
        rule.waitForIdle()

        assertThat(sensorManager.listeners).isEmpty()
    }

    @Test
    fun explicitFeatureSetWithoutSensorFeatureRegistersNoListener() {
        val document = buildDocument { writer.drawCircle(accelerationX(), 50f, 6f) }

        rule.renderDocument(document, RcPlayerFeatureSet.Empty)

        assertThat(sensorManager.listeners).isEmpty()
    }

    @Test
    fun defaultFeaturesIncludeSensorsForEveryProfile() {
        // Sensors are a host capability, not operations of a profile.
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_BASELINE).features)
            .contains(SensorFeature)
        assertThat(RcPlayerFeatures.forProfiles(RcProfiles.PROFILE_ANDROIDX).features)
            .contains(SensorFeature)
    }

    private fun RemoteComposeContextAndroid.accelerationX(): Float =
        writer.floatExpression(RemoteContext.FLOAT_ACCELERATION_X, 0f, AnimatedFloatExpression.ADD)

    private fun play(document: CoreDocument) {
        rule.setContent { Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) } }
        rule.onRoot().captureToImage()
    }

    private fun sendAcceleration(x: Float) {
        sensorManager.sendSensorEventToListeners(
            SensorEventBuilder.newBuilder(accelerometer, floatArrayOf(x, 0f, 0f)).build()
        )
    }
}
