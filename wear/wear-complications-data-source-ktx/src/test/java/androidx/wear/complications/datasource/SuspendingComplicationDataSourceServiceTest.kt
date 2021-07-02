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

package androidx.wear.complications.datasource

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationText
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.PlainComplicationText
import androidx.wear.complications.data.ShortTextComplicationData
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.internal.bytecode.InstrumentationConfiguration

class TestService : SuspendingComplicationDataSourceService() {
    override suspend fun onComplicationRequest(request: ComplicationRequest) =
        ShortTextComplicationData.Builder(
            PlainComplicationText.Builder("Complication").build(),
            ComplicationText.EMPTY
        ).build()

    override fun getPreviewData(type: ComplicationType) =
        ShortTextComplicationData.Builder(
            PlainComplicationText.Builder("Preview").build(),
            ComplicationText.EMPTY
        ).build()
}

/** Needed to prevent Robolectric from instrumenting various classes.  */
class ComplicationsTestRunner(clazz: Class<*>?) : RobolectricTestRunner(clazz) {
    override fun createClassLoaderConfig(method: FrameworkMethod): InstrumentationConfiguration {
        return InstrumentationConfiguration.Builder(super.createClassLoaderConfig(method))
            .doNotInstrumentPackage("android.support.wearable.complications")
            .doNotInstrumentPackage("android.support.wearable.watchface")
            .doNotInstrumentPackage("androidx.wear.complications")
            .doNotInstrumentPackage("androidx.wear.complications.datasource")
            .doNotInstrumentPackage("androidx.wear.watchface")
            .build()
    }
}

/** Tests for {@link ComplicationDataSourceService}. */
@RunWith(ComplicationsTestRunner::class)
@DoNotInstrument
public class SuspendingComplicationDataSourceServiceTest {
    @Test
    public fun onComplicationRequest() {
        val testService = TestService()
        lateinit var result: ComplicationData

        testService.onComplicationRequest(
            ComplicationRequest(123, ComplicationType.SMALL_IMAGE),
            object : ComplicationDataSourceService.ComplicationRequestListener {
                override fun onComplicationData(complicationData: ComplicationData?) {
                    result = complicationData!!
                }
            }
        )

        assertThat(
            (result as ShortTextComplicationData).text.getTextAt(
                ApplicationProvider.getApplicationContext<Context>().resources,
                0
            )
        ).isEqualTo("Complication")
    }

    @Test
    public fun getPreviewData() {
        val testService = TestService()

        assertThat(
            testService.getPreviewData(ComplicationType.SMALL_IMAGE).text.getTextAt(
                ApplicationProvider.getApplicationContext<Context>().resources,
                0
            )
        ).isEqualTo("Preview")
    }
}