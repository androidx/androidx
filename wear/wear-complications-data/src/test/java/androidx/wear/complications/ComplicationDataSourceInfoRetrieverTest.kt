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

package androidx.wear.complications

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.IBinder
import android.support.wearable.complications.IPreviewComplicationDataCallback
import android.support.wearable.complications.IProviderInfoService
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.LongTextComplicationData
import androidx.wear.complications.data.PlainComplicationText
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer

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

@RunWith(SharedRobolectricTestRunner::class)
public class ComplicationDataSourceInfoRetrieverTest {
    private val mockService = Mockito.mock(IProviderInfoService::class.java)
    private val mockBinder = Mockito.mock(IBinder::class.java)
    private val complicationDataSourceInfoRetriever =
        ComplicationDataSourceInfoRetriever(mockService)

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationData() {
        runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(1)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)

            val testData: ComplicationData = LongTextComplicationData.Builder(
                PlainComplicationText.Builder("Test Text").build(),
                ComplicationText.EMPTY
            ).build()

            doAnswer {
                val callback = it.arguments[2] as IPreviewComplicationDataCallback
                callback.updateComplicationData(testData.asWireComplicationData())
                true
            }.`when`(mockService).requestPreviewComplicationData(
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
                (previewData as LongTextComplicationData).text.getTextAt(
                    ApplicationProvider.getApplicationContext<Context>().resources, 0
                )
            ).isEqualTo("Test Text")
        }
    }

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationData_DataSourceReturnsNull() {
        runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(1)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)

            doAnswer {
                val callback = it.arguments[2] as IPreviewComplicationDataCallback
                callback.updateComplicationData(null)
                true
            }.`when`(mockService).requestPreviewComplicationData(
                eq(component),
                eq(type.toWireComplicationType()),
                any()
            )

            assertThat(
                complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                    component,
                    type
                )
            ).isNull()
        }
    }

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationDataApiNotSupported() {
        runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(0)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)

            assertThat(
                complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                    component,
                    type
                )
            ).isNull()
        }
    }

    @Test
    @Suppress("NewApi") // retrievePreviewComplicationData
    public fun retrievePreviewComplicationDataApiReturnsFalse() {
        runBlocking {
            val component = ComponentName("dataSource.package", "dataSource.class")
            val type = ComplicationType.LONG_TEXT
            Mockito.`when`(mockService.apiVersion).thenReturn(1)
            Mockito.`when`(mockService.asBinder()).thenReturn(mockBinder)
            doAnswer {
                false
            }.`when`(mockService).requestPreviewComplicationData(
                eq(component),
                eq(type.toWireComplicationType()),
                any()
            )

            assertThat(
                complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                    component,
                    type
                )
            ).isNull()
        }
    }

    @Test
    public fun complicationDataSourceInfo_NullComponentName() {
        val complicationDataSourceInfo = ComplicationDataSourceInfo(
            "appName",
            "name",
            Icon.createWithContentUri("icon"),
            ComplicationType.SHORT_TEXT,
            componentName = null
        )
        assertThat(complicationDataSourceInfo.componentName).isNull()
    }
}