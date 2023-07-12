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

package androidx.wear.watchface.complications

import android.content.ComponentName
import android.content.Context
import android.support.wearable.complications.IPreviewComplicationDataCallback
import android.support.wearable.complications.IProviderInfoService
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.PhotoImageComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import com.google.common.truth.Truth.assertThat
import kotlin.jvm.java
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito

@RunWith(SharedRobolectricTestRunner::class)
public class ComplicationDataSourceInfoRetrieverTest {
    private val mockService = Mockito.mock(IProviderInfoService::class.java)
    private val mockBinder = Mockito.mock(android.os.IBinder::class.java)
    private val complicationDataSourceInfoRetriever =
        ComplicationDataSourceInfoRetriever(mockService)
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationData() {
        kotlinx.coroutines.runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(1)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)

            val testData: ComplicationData =
                LongTextComplicationData.Builder(
                        PlainComplicationText.Builder("Test Text").build(),
                        ComplicationText.Companion.EMPTY
                    )
                    .build()

            Mockito.doAnswer {
                    val callback = it.arguments[2] as IPreviewComplicationDataCallback
                    callback.updateComplicationData(testData.asWireComplicationData())
                    true
                }
                .`when`(mockService)
                .requestPreviewComplicationData(
                    eq(component),
                    eq(type.toWireComplicationType()),
                    any()
                )

            val previewData =
                complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                    component,
                    type
                )!!
            assertThat(previewData.type).isEqualTo(type)
            assertThat(
                    (previewData as LongTextComplicationData)
                        .text
                        .getTextAt(
                            ApplicationProvider.getApplicationContext<Context>().resources,
                            java.time.Instant.EPOCH
                        )
                )
                .isEqualTo("Test Text")
        }
    }

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationData_DataSourceReturnsNull() {
        kotlinx.coroutines.runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(1)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)

            Mockito.doAnswer {
                    val callback = it.arguments[2] as IPreviewComplicationDataCallback
                    callback.updateComplicationData(null)
                    true
                }
                .`when`(mockService)
                .requestPreviewComplicationData(
                    eq(component),
                    eq(type.toWireComplicationType()),
                    any()
                )

            assertThat(
                    complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                        component,
                        type
                    )
                )
                .isNull()
        }
    }

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationDataApiNotSupported() {
        kotlinx.coroutines.runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(0)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)

            assertThat(
                    complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                        component,
                        type
                    )
                )
                .isNull()
        }
    }

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationDataApiReturnsFalse() {
        kotlinx.coroutines.runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(1)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)
            Mockito.doAnswer { false }
                .`when`(mockService)
                .requestPreviewComplicationData(
                    eq(component),
                    eq(type.toWireComplicationType()),
                    any()
                )

            assertThat(
                    complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                        component,
                        type
                    )
                )
                .isNull()
        }
    }

    @Test
    public fun complicationDataSourceInfo_NullComponentName() {
        val complicationDataSourceInfo =
            ComplicationDataSourceInfo(
                "appName",
                "name",
                android.graphics.drawable.Icon.createWithContentUri("icon"),
                ComplicationType.SHORT_TEXT,
                componentName = null
            )
        assertThat(complicationDataSourceInfo.componentName).isNull()
        assertThat(complicationDataSourceInfo.toString())
            .isEqualTo(
                "ComplicationDataSourceInfo(appName=appName, name=name, type=SHORT_TEXT" +
                    ", icon=Icon(typ=URI uri=icon), componentName=null)"
            )
    }

    @Test
    public fun createShortTextFallbackPreviewData() {
        val icon = android.graphics.drawable.Icon.createWithContentUri("icon")
        val shortTextPreviewData =
            ComplicationDataSourceInfo(
                    "applicationName",
                    "complicationName",
                    icon,
                    ComplicationType.SHORT_TEXT,
                    componentName = null
                )
                .fallbackPreviewData as ShortTextComplicationData
        assertThat(shortTextPreviewData.text.getTextAt(resources, java.time.Instant.EPOCH))
            .isEqualTo("complic")
        assertThat(
                shortTextPreviewData.contentDescription!!.getTextAt(
                    resources,
                    java.time.Instant.EPOCH
                )
            )
            .isEqualTo("complicationName")
        assertThat(shortTextPreviewData.monochromaticImage!!.image).isEqualTo(icon)
    }

    @Test
    public fun createLongTextFallbackPreviewData() {
        val icon = android.graphics.drawable.Icon.createWithContentUri("icon")
        val longTextPreviewData =
            ComplicationDataSourceInfo(
                    "applicationName",
                    "complicationName",
                    icon,
                    ComplicationType.LONG_TEXT,
                    componentName = null
                )
                .fallbackPreviewData as LongTextComplicationData
        assertThat(longTextPreviewData.text.getTextAt(resources, java.time.Instant.EPOCH))
            .isEqualTo("complicationName")
        assertThat(
                longTextPreviewData.contentDescription!!.getTextAt(
                    resources,
                    java.time.Instant.EPOCH
                )
            )
            .isEqualTo("complicationName")
        assertThat(longTextPreviewData.monochromaticImage!!.image).isEqualTo(icon)
    }

    @Test
    public fun createSmallImageFallbackPreviewData() {
        val icon = android.graphics.drawable.Icon.createWithContentUri("icon")
        val smallImagePreviewData =
            ComplicationDataSourceInfo(
                    "applicationName",
                    "complicationName",
                    icon,
                    ComplicationType.SMALL_IMAGE,
                    componentName = null
                )
                .fallbackPreviewData as SmallImageComplicationData
        assertThat(smallImagePreviewData.smallImage.image).isEqualTo(icon)
        assertThat(
                smallImagePreviewData.contentDescription!!.getTextAt(
                    resources,
                    java.time.Instant.EPOCH
                )
            )
            .isEqualTo("complicationName")
    }

    @Test
    public fun createPhotoImageFallbackPreviewData() {
        val icon = android.graphics.drawable.Icon.createWithContentUri("icon")
        val photoImagePreviewData =
            ComplicationDataSourceInfo(
                    "applicationName",
                    "complicationName",
                    icon,
                    ComplicationType.PHOTO_IMAGE,
                    componentName = null
                )
                .fallbackPreviewData as PhotoImageComplicationData
        assertThat(photoImagePreviewData.photoImage).isEqualTo(icon)
        assertThat(
                photoImagePreviewData.contentDescription!!.getTextAt(
                    resources,
                    java.time.Instant.EPOCH
                )
            )
            .isEqualTo("complicationName")
    }

    @Test
    public fun createMonochromaticImageFallbackPreviewData() {
        val icon = android.graphics.drawable.Icon.createWithContentUri("icon")
        val monochromaticImagePreviewData =
            ComplicationDataSourceInfo(
                    "applicationName",
                    "complicationName",
                    icon,
                    ComplicationType.MONOCHROMATIC_IMAGE,
                    componentName = null
                )
                .fallbackPreviewData as MonochromaticImageComplicationData
        assertThat(monochromaticImagePreviewData.monochromaticImage.image).isEqualTo(icon)
        assertThat(
                monochromaticImagePreviewData.contentDescription!!.getTextAt(
                    resources,
                    java.time.Instant.EPOCH
                )
            )
            .isEqualTo("complicationName")
    }

    @Test
    public fun createRangedValueFallbackPreviewData() {
        val icon = android.graphics.drawable.Icon.createWithContentUri("icon")
        val rangedValuePreviewData =
            ComplicationDataSourceInfo(
                    "applicationName",
                    "complicationName",
                    icon,
                    ComplicationType.RANGED_VALUE,
                    componentName = null
                )
                .fallbackPreviewData as RangedValueComplicationData
        assertThat(rangedValuePreviewData.min).isEqualTo(0.0f)
        assertThat(rangedValuePreviewData.max).isEqualTo(100.0f)
        assertThat(rangedValuePreviewData.value).isEqualTo(42.0f)
        assertThat(rangedValuePreviewData.text!!.getTextAt(resources, java.time.Instant.EPOCH))
            .isEqualTo("complicationName")
        assertThat(rangedValuePreviewData.monochromaticImage!!.image).isEqualTo(icon)
        assertThat(
                rangedValuePreviewData.contentDescription!!.getTextAt(
                    resources,
                    java.time.Instant.EPOCH
                )
            )
            .isEqualTo("complicationName")
    }

    @Test
    public fun complicationDataSourceInfo_equals() {
        val icon = android.graphics.drawable.Icon.createWithContentUri("icon")
        val icon2 = android.graphics.drawable.Icon.createWithContentUri("icon")
        val a =
            ComplicationDataSourceInfo(
                "applicationName",
                "complicationName",
                icon,
                ComplicationType.RANGED_VALUE,
                componentName = null
            )
        val b =
            ComplicationDataSourceInfo(
                "applicationName",
                "complicationName",
                icon2,
                ComplicationType.RANGED_VALUE,
                componentName = null
            )
        val c =
            ComplicationDataSourceInfo(
                "applicationName2",
                "complicationName2",
                icon,
                ComplicationType.RANGED_VALUE,
                componentName = null
            )

        // Test two identical ComplicationDataSourceInfo with different references.
        assertThat(a).isEqualTo(b)

        // Test two ComplicationDataSourceInfos with different contents.
        assertThat(a).isNotEqualTo(c)
    }
}
