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

package androidx.wear.watchface.client.test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.client.WatchFaceClientExperimental
import androidx.wear.watchface.client.WatchFaceMetadataClient
import androidx.wear.watchface.control.WatchFaceControlService
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
import androidx.wear.watchface.samples.EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@OptIn(WatchFaceClientExperimental::class)
public class WatchFaceMetadataServiceTest {
    private val exampleWatchFaceComponentName = ComponentName(
        "androidx.wear.watchface.samples.test",
        "androidx.wear.watchface.samples.ExampleCanvasAnalogWatchFaceService"
    )

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val service = runBlocking {
        WatchFaceMetadataClient.createWatchFaceMetadataClientImpl(
            context,
            Intent(context, WatchFaceControlTestService::class.java).apply {
                action = WatchFaceControlService.ACTION_WATCHFACE_CONTROL_SERVICE
            },
            exampleWatchFaceComponentName
        )
    }

    @Before
    public fun setUp() {
        WatchFaceControlTestService.apiVersionOverride = null
    }

    @Test
    public fun getSchema() {
        val schema = service.getUserStyleSchema()

        Truth.assertThat(schema.userStyleSettings.size).isEqualTo(4)
        Truth.assertThat(schema.userStyleSettings[0].id.value).isEqualTo(
            "color_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[1].id.value).isEqualTo(
            "draw_hour_pips_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[2].id.value).isEqualTo(
            "watch_hand_length_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[3].id.value).isEqualTo(
            "complications_style_setting"
        )
    }

    @Test
    public fun getSchema_oldApi() {
        WatchFaceControlTestService.apiVersionOverride = 1
        val schema = service.getUserStyleSchema()

        Truth.assertThat(schema.userStyleSettings.size).isEqualTo(4)
        Truth.assertThat(schema.userStyleSettings[0].id.value).isEqualTo(
            "color_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[1].id.value).isEqualTo(
            "draw_hour_pips_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[2].id.value).isEqualTo(
            "watch_hand_length_style_setting"
        )
        Truth.assertThat(schema.userStyleSettings[3].id.value).isEqualTo(
            "complications_style_setting"
        )
    }

    @Test
    public fun getComplicationSlotMetadataMap() {
        val complicationSlotMetadataMap = service.getComplicationSlotMetadataMap()
        Truth.assertThat(complicationSlotMetadataMap.size).isEqualTo(2)

        val leftComplicationMetadata = complicationSlotMetadataMap[
            EXAMPLE_CANVAS_WATCHFACE_LEFT_COMPLICATION_ID
        ]!!
        Truth.assertThat(
            leftComplicationMetadata.bounds!!.perComplicationTypeBounds[ComplicationType.SHORT_TEXT]
        ).isEqualTo(RectF(0.2f, 0.4f, 0.4f, 0.6f))
        Truth.assertThat(leftComplicationMetadata.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        Truth.assertThat(
            leftComplicationMetadata.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(SystemDataSources.DATA_SOURCE_DAY_OF_WEEK)
        Truth.assertThat(leftComplicationMetadata.defaultDataSourceType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        Truth.assertThat(leftComplicationMetadata.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

        val rightComplicationMetadata = complicationSlotMetadataMap[
            EXAMPLE_CANVAS_WATCHFACE_RIGHT_COMPLICATION_ID
        ]!!
        Truth.assertThat(
            rightComplicationMetadata.bounds!!.perComplicationTypeBounds[
                ComplicationType.SHORT_TEXT
            ]
        ).isEqualTo(RectF(0.6f, 0.4f, 0.8f, 0.6f))
        Truth.assertThat(rightComplicationMetadata.boundsType)
            .isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        Truth.assertThat(
            rightComplicationMetadata.defaultDataSourcePolicy.systemDataSourceFallback
        ).isEqualTo(SystemDataSources.DATA_SOURCE_STEP_COUNT)
        Truth.assertThat(rightComplicationMetadata.defaultDataSourceType).isEqualTo(
            ComplicationType.SHORT_TEXT
        )
        Truth.assertThat(rightComplicationMetadata.supportedTypes).containsExactly(
            ComplicationType.RANGED_VALUE,
            ComplicationType.LONG_TEXT,
            ComplicationType.SHORT_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )
    }
}
