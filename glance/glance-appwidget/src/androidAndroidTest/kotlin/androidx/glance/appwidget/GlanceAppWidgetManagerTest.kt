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

package androidx.glance.appwidget

import androidx.glance.text.Text
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class GlanceAppWidgetManagerTest {
    @get:Rule
    val mHostRule = AppWidgetHostRule()

    @Before
    fun setUp() {
        // Reset the size mode to the default
        TestGlanceAppWidget.sizeMode = SizeMode.Single
    }

    @Test
    fun noAppWidget() {
        mHostRule.onHostActivity { activity ->
            val manager = GlanceAppWidgetManager(activity)
            runBlocking {
                assertThat(manager.getGlanceIds(TestGlanceAppWidget::class.java)).isEmpty()
            }
        }
    }

    @Test
    fun withAppWidget() {
        TestGlanceAppWidget.uiDefinition = {
            Text("Something")
        }

        mHostRule.startHost()

        mHostRule.onHostActivity { activity ->
            val manager = GlanceAppWidgetManager(activity)

            runBlocking {
                val glanceIds = manager.getGlanceIds(TestGlanceAppWidget::class.java)
                assertThat(glanceIds).hasSize(1)
                val glanceId = manager.getGlanceIdBy((glanceIds[0] as AppWidgetId).appWidgetId)
                assertThat(glanceId).isEqualTo(glanceIds[0])
                val sizes = manager.getAppWidgetSizes(glanceIds[0])
                assertThat(sizes).containsExactly(
                    mHostRule.portraitSize,
                    mHostRule.landscapeSize
                )
            }
        }
    }

    @Test
    fun cleanReceivers() {
        mHostRule.onHostActivity { activity ->
            val manager = GlanceAppWidgetManager(activity)

            runBlocking {
                manager.updateReceiver(DummyGlanceAppWidgetReceiver(), TestGlanceAppWidget)
                assertThat(manager.listKnownReceivers()).containsExactly(
                    DummyGlanceAppWidgetReceiver::class.java.canonicalName,
                    TestGlanceAppWidgetReceiver::class.java.canonicalName
                )

                manager.cleanReceivers()
                assertThat(manager.listKnownReceivers()).containsExactly(
                    TestGlanceAppWidgetReceiver::class.java.canonicalName
                )
            }
        }
    }
}

private class DummyGlanceAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TestGlanceAppWidget
}