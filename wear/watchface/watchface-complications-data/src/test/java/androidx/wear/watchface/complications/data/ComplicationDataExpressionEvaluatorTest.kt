/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.watchface.complications.data

import android.support.wearable.complications.ComplicationData as WireComplicationData
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import java.util.function.Consumer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowLooper.runUiThreadTasks

@RunWith(SharedRobolectricTestRunner::class)
class ComplicationDataExpressionEvaluatorTest {
    private val listener = mock<Consumer<WireComplicationData>>()

    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
    }

    @Test
    fun data_notInitialized_setNull() {
        val evaluator = ComplicationDataExpressionEvaluator(
            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("text").build(),
                contentDescription = PlainComplicationText.Builder("description").build(),
            ).build().asWireComplicationData(),
        )

        assertThat(evaluator.data.value).isNull()
    }

    @Test
    fun data_initialized_setToUnevaluated() {
        val evaluator = ComplicationDataExpressionEvaluator(UNEVALUATED_DATA)
        evaluator.init()

        assertThat(evaluator.data.value).isEqualTo(UNEVALUATED_DATA)
    }

    @Test
    fun compat_notInitialized_listenerNotInvoked() {
        ComplicationDataExpressionEvaluator.Compat(
            UNEVALUATED_DATA,
            ContextCompat.getMainExecutor(getApplicationContext()),
            listener,
        ).use {
            runUiThreadTasks()

            verify(listener, never()).accept(any())
        }
    }

    @Test
    fun compat_noExpression_listenerInvokedWithData() {
        ComplicationDataExpressionEvaluator.Compat(
            UNEVALUATED_DATA,
            ContextCompat.getMainExecutor(getApplicationContext()),
            listener,
        ).use { evaluator ->
            evaluator.init()
            runUiThreadTasks()

            verify(listener, times(1)).accept(UNEVALUATED_DATA)
        }
    }

    private companion object {
        val UNEVALUATED_DATA: WireComplicationData = ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("text").build(),
            contentDescription = PlainComplicationText.Builder("description").build(),
        ).build().asWireComplicationData()
    }
}
