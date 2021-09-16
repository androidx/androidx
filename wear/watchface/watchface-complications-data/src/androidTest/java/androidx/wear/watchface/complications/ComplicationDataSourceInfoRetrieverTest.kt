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

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.support.wearable.complications.IPreviewComplicationDataCallback
import android.support.wearable.complications.IProviderInfoService
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

private typealias WireComplicationProviderInfo =
    android.support.wearable.complications.ComplicationProviderInfo

private const val LEFT_COMPLICATION_ID = 101
private const val RIGHT_COMPLICATION_ID = 102

private val watchFaceComponent = ComponentName("test.package", "test.class")
private val dataSource1 = ComponentName("dataSource.app1", "dataSource.class1")
private val dataSource2 = ComponentName("dataSource.app2", "dataSource.class2")
private val dataSource3 = ComponentName("dataSource.app3", "dataSource.class3")

public class TestProviderInfoService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return TestProviderInfoServiceStub().asBinder()
    }
}

public class TestProviderInfoServiceStub : IProviderInfoService.Stub() {
    private val dataSourceIcon1: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    private val dataSourceIcon2: Icon =
        Icon.createWithBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    private val dataSourceInfos = mapOf(
        LEFT_COMPLICATION_ID to ComplicationDataSourceInfo(
            "DataSourceApp1",
            "DataSource1",
            dataSourceIcon1,
            ComplicationType.SHORT_TEXT,
            dataSource1
        ),
        RIGHT_COMPLICATION_ID to ComplicationDataSourceInfo(
            "DataSourceApp2",
            "DataSource2",
            dataSourceIcon2,
            ComplicationType.LONG_TEXT,
            dataSource2
        )
    )
    private val previewData = mapOf(
        dataSource1 to
            ShortTextComplicationData.Builder(
                PlainComplicationText.Builder("Left").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
        dataSource2 to
            LongTextComplicationData.Builder(
                PlainComplicationText.Builder("Right").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
        dataSource3 to
            LongTextComplicationData.Builder(
                PlainComplicationText.Builder("DataSource3").build(),
                ComplicationText.EMPTY
            ).build().asWireComplicationData(),
    )

    override fun getProviderInfos(
        watchFaceComponentName: ComponentName,
        ids: IntArray
    ): Array<WireComplicationProviderInfo?>? {
        if (watchFaceComponentName != watchFaceComponent) {
            return null
        }
        return ArrayList<WireComplicationProviderInfo?>().apply {
            for (id in ids) {
                add(dataSourceInfos[id]?.toWireComplicationProviderInfo())
            }
        }.toTypedArray()
    }

    override fun getApiVersion() = IProviderInfoService.API_VERSION

    override fun requestPreviewComplicationData(
        dataSourceComponent: ComponentName,
        complicationType: Int,
        previewComplicationDataCallback: IPreviewComplicationDataCallback
    ): Boolean {
        previewComplicationDataCallback.updateComplicationData(previewData[dataSourceComponent])
        return true
    }
}

@RunWith(AndroidJUnit4::class)
@MediumTest
public class ComplicationDataSourceInfoRetrieverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val serviceIntent = Intent(context, TestProviderInfoService::class.java)
    private val complicationDataSourceInfoRetriever = ComplicationDataSourceInfoRetriever(
        ApplicationProvider.getApplicationContext<Context>(),
        serviceIntent
    )

    @Test
    public fun retrieveComplicationDataSourceInfo() {
        runBlocking {
            val complicationDataSourceInfos =
                complicationDataSourceInfoRetriever.retrieveComplicationDataSourceInfo(
                    watchFaceComponent,
                    intArrayOf(LEFT_COMPLICATION_ID, RIGHT_COMPLICATION_ID)
                )!!
            assertThat(complicationDataSourceInfos.size).isEqualTo(2)
            assertThat(complicationDataSourceInfos[0].slotId).isEqualTo(LEFT_COMPLICATION_ID)
            assertThat(complicationDataSourceInfos[0].info!!.componentName).isEqualTo(dataSource1)
            assertThat(complicationDataSourceInfos[0].info!!.appName).isEqualTo("DataSourceApp1")
            assertThat(complicationDataSourceInfos[0].info!!.name).isEqualTo("DataSource1")
            assertThat(complicationDataSourceInfos[1].slotId).isEqualTo(RIGHT_COMPLICATION_ID)
            assertThat(complicationDataSourceInfos[1].info!!.componentName).isEqualTo(dataSource2)
            assertThat(complicationDataSourceInfos[1].info!!.appName).isEqualTo("DataSourceApp2")
            assertThat(complicationDataSourceInfos[1].info!!.name).isEqualTo("DataSource2")
            complicationDataSourceInfoRetriever.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Test
    public fun retrievePreviewComplicationData() {
        runBlocking {
            val complicationData =
                complicationDataSourceInfoRetriever.retrievePreviewComplicationData(
                    dataSource1,
                    ComplicationType.SHORT_TEXT
                ) as ShortTextComplicationData
            assertThat(
                complicationData.text.getTextAt(context.resources, Instant.EPOCH)
            ).isEqualTo("Left")
            complicationDataSourceInfoRetriever.close()
        }
    }

    @Test
    public fun closeTwice() {
        assertThat(complicationDataSourceInfoRetriever.closed).isFalse()
        complicationDataSourceInfoRetriever.close()
        assertThat(complicationDataSourceInfoRetriever.closed).isTrue()

        // This should not crash.
        complicationDataSourceInfoRetriever.close()
    }
}