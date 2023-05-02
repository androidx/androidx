/*
 * Copyright 2023 The Android Open Source Project
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

import android.os.Build
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.allowUnsafeIntentLaunch
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.Column
import androidx.glance.text.Text
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import junit.framework.TestCase.fail
import kotlin.test.assertIs
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@RequiresApi(Build.VERSION_CODES.S)
class StrictModeTest {
    @get:Rule
    val mHostRule = AppWidgetHostRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext!!
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var previousPolicy: StrictMode.VmPolicy

    @Before
    fun setUp() {
        previousPolicy = StrictMode.getVmPolicy()
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyListener(executor) {
                    Log.e("StrictModeTest", "Logging violation:")
                    Log.e("StrictModeTest", "$it")
                    Log.e("StrictModeTest", "Stack trace: ${it.stackTrace}", it.cause)
                    fail("Received violation: $it")
                }.build()
        )
    }

    @After
    fun cleanUp() {
        executor.shutdown()
        StrictMode.setVmPolicy(previousPolicy)
    }

    @Test
    fun actionRunCallback() {
        TestGlanceAppWidget.uiDefinition = {
            Column {
                Text(
                    "text1",
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<CallbackTest>(
                            actionParametersOf(CallbackTest.key to 1)
                        )
                    )
                )
                Text(
                    "text2",
                    modifier = GlanceModifier.clickable(
                        actionRunCallback<CallbackTest>(
                            actionParametersOf(CallbackTest.key to 2)
                        )
                    )
                )
            }
        }

        mHostRule.startHost()

        CallbackTest.received.set(emptyList())
        CallbackTest.latch = CountDownLatch(2)
        mHostRule.onHostView { root ->
            checkNotNull(
                root.findChild<TextView> { it.text.toString() == "text1" }?.parent as? View
            )
                .performClick()
            checkNotNull(
                root.findChild<TextView> { it.text.toString() == "text2" }?.parent as? View
            )
                .performClick()
        }
        Truth.assertThat(CallbackTest.latch.await(5, TimeUnit.SECONDS)).isTrue()
        Truth.assertThat(CallbackTest.received.get()).containsExactly(1, 2)
    }

    @Test
    fun lazyColumn_actionRunCallback() {
        TestGlanceAppWidget.uiDefinition = {
            LazyColumn {
                item {
                    Text(
                        "Text",
                        modifier = GlanceModifier.clickable(
                            actionRunCallback<CallbackTest>(
                                actionParametersOf(CallbackTest.key to 1)
                            )
                        )
                    )
                    Button(
                        "Button",
                        onClick = actionRunCallback<CallbackTest>(
                            actionParametersOf(CallbackTest.key to 2)
                        )

                    )
                }
            }
        }

        mHostRule.startHost()

        CallbackTest.received.set(emptyList())
        CallbackTest.latch = CountDownLatch(2)
        mHostRule.waitForListViewChildren { list ->
            val row = list.getUnboxedListItem<FrameLayout>(0)
            val (rowItem0, _) = row.notGoneChildren.toList()
            // All items with actions are wrapped in FrameLayout
            assertIs<FrameLayout>(rowItem0)
            Truth.assertThat(rowItem0.hasOnClickListeners()).isTrue()
            // We must allow unsafe intent launches here because the AppWidgetHost will always
            // launch PendingIntents that are read from a RemoteViews parcel.
            allowUnsafeIntentLaunch { rowItem0.performClick() }
        }
        mHostRule.waitForListViewChildren { list ->
            val row = list.getUnboxedListItem<FrameLayout>(0)
            val (_, rowItem1) = row.notGoneChildren.toList()
            // S+ buttons are implemented using native buttons.
            assertIs<Button>(rowItem1)
            Truth.assertThat(rowItem1.hasOnClickListeners()).isTrue()
            allowUnsafeIntentLaunch { rowItem1.performClick() }
        }

        Truth.assertThat(CallbackTest.latch.await(5, TimeUnit.SECONDS)).isTrue()
        Truth.assertThat(CallbackTest.received.get()).containsExactly(1, 2)
    }
}