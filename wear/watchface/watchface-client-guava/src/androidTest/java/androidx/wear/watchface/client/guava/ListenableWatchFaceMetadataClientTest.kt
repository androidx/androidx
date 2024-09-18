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

package androidx.wear.watchface.client.guava

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.client.ListenableWatchFaceMetadataClient
import androidx.wear.watchface.client.WatchFaceMetadataClient
import androidx.wear.watchface.control.WatchFaceControlService
import com.google.common.truth.Truth
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith

private const val TIMEOUT_MS = 500L

@RunWith(AndroidJUnit4::class)
@MediumTest
@RequiresApi(Build.VERSION_CODES.O_MR1)
public class ListenableWatchFaceMetadataClientTest {
    private val exampleWatchFaceComponentName =
        ComponentName(
            "androidx.wear.watchface.samples.test",
            "androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService"
        )

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @Suppress("Deprecation") // userStyleSettings
    public fun getSchema() {
        val listenableFuture =
            ListenableWatchFaceMetadataClient.createImpl(
                context,
                Intent(context, WatchFaceControlTestService::class.java).apply {
                    action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
                },
                exampleWatchFaceComponentName,
                object : WatchFaceMetadataClient.Companion.ParserProvider() {
                    override fun getParser(
                        context: Context,
                        watchFaceName: ComponentName
                    ): XmlResourceParser? = null
                }
            )

        val watchFaceMetadataClient = listenableFuture.get(TIMEOUT_MS, TimeUnit.MILLISECONDS)
        val schema = watchFaceMetadataClient.getUserStyleSchema()

        Truth.assertThat(schema.userStyleSettings.size).isEqualTo(5)
        Truth.assertThat(schema.userStyleSettings[0].id.value).isEqualTo("color_style_setting")
        Truth.assertThat(schema.userStyleSettings[1].id.value)
            .isEqualTo("draw_hour_pips_style_setting")
        Truth.assertThat(schema.userStyleSettings[2].id.value)
            .isEqualTo("watch_hand_length_style_setting")
        Truth.assertThat(schema.userStyleSettings[3].id.value)
            .isEqualTo("complications_style_setting")
        Truth.assertThat(schema.userStyleSettings[4].id.value)
            .isEqualTo("hours_draw_freq_style_setting")
    }
}
