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
import android.graphics.Color
import android.graphics.Rect
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.complications.SystemDataSources
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.RenderParameters
import androidx.wear.watchface.client.ComplicationSlotState
import androidx.wear.watchface.client.WatchFaceId
import androidx.wear.watchface.client.asApiEditorState
import androidx.wear.watchface.control.data.ComplicationRenderParams
import androidx.wear.watchface.control.data.HeadlessWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.control.data.WatchFaceRenderParams
import androidx.wear.watchface.ComplicationSlotBoundsType
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.UserStyleData
import androidx.wear.watchface.style.WatchFaceLayer
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.Base64

/** Tests that we can deserialize golden resources correctly to ensure backwards compatibility. */
@RunWith(AndroidJUnit4::class)
@MediumTest
public class SerializationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun <T> loadGoldenResource(resourceId: Int, readFromParcel: (p: Parcel) -> T): T {
        val resource = context.resources.openRawResource(resourceId)
        val bytes = Base64.getDecoder().decode(String(resource.readBytes()))
        val p = Parcel.obtain()
        p.unmarshall(bytes, 0, bytes.size)
        p.setDataPosition(0)
        val result = readFromParcel(p)
        p.recycle()
        return result
    }

    @Test
    public fun complicationRenderParams() {
        val deserialized = loadGoldenResource(R.raw.complication_render_params) {
            ComplicationRenderParams.CREATOR.createFromParcel(it)
        }

        assertThat(deserialized.complicationSlotId).isEqualTo(123)
        assertThat(RenderParameters(deserialized.renderParametersWireFormat))
            .isEqualTo(
                RenderParameters(
                    DrawMode.AMBIENT,
                    WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                    null,
                )
            )
        assertThat(deserialized.calendarTimeMillis).isEqualTo(123456789)
        assertThat(UserStyleData(deserialized.userStyle!!)).isEqualTo(
            UserStyleData(mapOf("COLOR_STYLE_SETTING" to "GREEN_STYLE".encodeToByteArray()))
        )

        val complication =
            deserialized.complicationData!!.toApiComplicationData() as LongTextComplicationData
        assertThat(complication.text.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("Example")
        assertThat(complication.title!!.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("complication")
    }

    @Test
    public fun headlessWatchFaceInstanceParams() {
        val deserialized = loadGoldenResource(R.raw.headless_watchface_instance_params) {
            HeadlessWatchFaceInstanceParams.CREATOR.createFromParcel(it)
        }

        assertThat(deserialized.watchFaceName).isEqualTo(ComponentName("package", "class"))
        assertThat(deserialized.deviceConfig.hasBurnInProtection).isFalse()
        assertThat(deserialized.deviceConfig.hasLowBitAmbient).isTrue()
        assertThat(deserialized.deviceConfig.analogPreviewReferenceTimeMillis).isEqualTo(10)
        assertThat(deserialized.deviceConfig.digitalPreviewReferenceTimeMillis).isEqualTo(20)
        assertThat(deserialized.width).isEqualTo(640)
        assertThat(deserialized.height).isEqualTo(480)
    }

    @Test
    public fun wallpaperInteractiveWatchFaceInstanceParams() {
        val deserialized =
            loadGoldenResource(R.raw.wallpaper_interactive_watchface_instance_params) {
                WallpaperInteractiveWatchFaceInstanceParams.CREATOR.createFromParcel(it)
            }

        assertThat(deserialized.instanceId).isEqualTo("interactiveInstanceId")
        assertThat(deserialized.idAndComplicationDataWireFormats!!.size).isEqualTo(1)
        assertThat(deserialized.idAndComplicationDataWireFormats!![0].id).isEqualTo(1)

        val complication =
            deserialized.idAndComplicationDataWireFormats!![0].complicationData
                .toApiComplicationData() as LongTextComplicationData
        assertThat(complication.text.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("Test Text")
        assertThat(complication.title!!.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("Test Title")

        assertThat(deserialized.deviceConfig.hasBurnInProtection).isTrue()
        assertThat(deserialized.deviceConfig.hasLowBitAmbient).isTrue()
        assertThat(deserialized.deviceConfig.analogPreviewReferenceTimeMillis).isEqualTo(100)
        assertThat(deserialized.deviceConfig.digitalPreviewReferenceTimeMillis).isEqualTo(200)
        assertThat(deserialized.watchUiState.inAmbientMode).isFalse()
        assertThat(deserialized.watchUiState.interruptionFilter).isEqualTo(0)
        assertThat(UserStyleData(deserialized.userStyle)).isEqualTo(
            UserStyleData(
                mapOf(
                    "COLOR_STYLE_SETTING" to "GREEN_STYLE".encodeToByteArray(),
                    "WATCH_HAND_STYLE" to "GOTHIC".encodeToByteArray()
                )
            )
        )
    }

    @Test
    public fun watchFaceRenderParams() {
        val deserialized = loadGoldenResource(R.raw.watch_face_render_params) {
            WatchFaceRenderParams.CREATOR.createFromParcel(it)
        }

        assertThat(RenderParameters(deserialized.renderParametersWireFormat)).isEqualTo(
            RenderParameters(
                DrawMode.INTERACTIVE,
                WatchFaceLayer.ALL_WATCH_FACE_LAYERS,
                RenderParameters.HighlightLayer(
                    RenderParameters.HighlightedElement.AllComplicationSlots,
                    Color.RED,
                    Color.argb(128, 0, 0, 0)
                )
            )
        )
        assertThat(deserialized.calendarTimeMillis).isEqualTo(123456789)
        assertThat(UserStyleData(deserialized.userStyle!!)).isEqualTo(
            UserStyleData(
                mapOf(
                    "COLOR_STYLE_SETTING" to "RED".encodeToByteArray(),
                    "NUM_COMPLICATIONS" to "TWO".encodeToByteArray(),
                )
            )
        )

        assertThat(deserialized.idAndComplicationDatumWireFormats!!.size).isEqualTo(2)
        assertThat(deserialized.idAndComplicationDatumWireFormats!![0].id).isEqualTo(1)
        val complicationA =
            deserialized.idAndComplicationDatumWireFormats!![0].complicationData
                .toApiComplicationData() as LongTextComplicationData
        assertThat(complicationA.text.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("A Text")
        assertThat(complicationA.title!!.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("A Title")

        assertThat(deserialized.idAndComplicationDatumWireFormats!![1].id).isEqualTo(2)
        val complicationB =
            deserialized.idAndComplicationDatumWireFormats!![1].complicationData
                .toApiComplicationData() as ShortTextComplicationData
        assertThat(complicationB.text.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("B Text")
        assertThat(complicationB.title!!.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("B Title")
    }

    @Test
    public fun complicationState() {
        val deserialized = loadGoldenResource(R.raw.complication_state) {
            ComplicationSlotState(ComplicationStateWireFormat.CREATOR.createFromParcel(it))
        }

        assertThat(deserialized.bounds).isEqualTo(Rect(1, 2, 3, 4))
        assertThat(deserialized.boundsType).isEqualTo(ComplicationSlotBoundsType.ROUND_RECT)
        assertThat(deserialized.supportedTypes).containsExactly(
            ComplicationType.LONG_TEXT, ComplicationType.SHORT_TEXT
        )
        assertThat(deserialized.defaultDataSourcePolicy.systemDataSourceFallback).isEqualTo(
            SystemDataSources.DATA_SOURCE_DAY_AND_DATE
        )
        assertThat(deserialized.defaultDataSourcePolicy.primaryDataSource).isEqualTo(
            ComponentName("a", "b")
        )
        assertThat(deserialized.defaultDataSourcePolicy.secondaryDataSource).isNull()
        assertThat(deserialized.currentType).isEqualTo(ComplicationType.SHORT_TEXT)
        assertThat(deserialized.isEnabled).isTrue()
        assertThat(deserialized.isInitiallyEnabled).isFalse()
        assertThat(deserialized.fixedComplicationDataSource).isFalse()
        assertThat(deserialized.complicationConfigExtras.getInt("keyA")).isEqualTo(100)
    }

    @Test
    public fun editorState() {
        val deserialized = loadGoldenResource(R.raw.editor_state) {
            EditorStateWireFormat.CREATOR.createFromParcel(it).asApiEditorState()
        }

        assertThat(deserialized.watchFaceId).isEqualTo(WatchFaceId("id-1"))
        assertThat(deserialized.userStyle).isEqualTo(
            UserStyleData(
                mapOf(
                    "COLOR_STYLE_SETTING" to "BLUE".encodeToByteArray(),
                    "NUM_COMPLICATIONS" to "THREE".encodeToByteArray(),
                )
            )
        )
        assertThat(deserialized.shouldCommitChanges).isTrue()

        val complicationA = deserialized.previewComplicationsData[10] as ShortTextComplicationData
        val complicationB = deserialized.previewComplicationsData[20] as LongTextComplicationData
        assertThat(complicationA.text.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("Mon")
        assertThat(complicationA.title!!.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("23rd")
        assertThat(complicationB.text.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("Example")
        assertThat(complicationB.title!!.getTextAt(context.resources, Instant.EPOCH))
            .isEqualTo("complication")
    }

    @Test
    public fun userStyleSchema() {
        // TODO(b/187498135): Implement a golden test. This is harder than it sounds because the raw
        // bytes of  a serialized Parcel is not portable and can differ between architectures even
        // on the same API level.  The tests above possibly only pass on current bots by luck.
    }
}
