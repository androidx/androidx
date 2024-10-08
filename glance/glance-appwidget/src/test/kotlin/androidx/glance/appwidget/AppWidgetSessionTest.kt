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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.widget.TextView
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionModifier
import androidx.glance.action.LambdaAction
import androidx.glance.layout.EmittableBox
import androidx.glance.state.ConfigManager
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.EmittableText
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppWidgetSessionTest {

    private val id = AppWidgetId(123)
    private val widget = TestWidget {}
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val defaultOptions =
        optionsBundleOf(listOf(DpSize(100.dp, 50.dp), DpSize(50.dp, 100.dp)))
    private val testState = TestGlanceState()
    private val session = AppWidgetSession(widget, id, defaultOptions, testState)

    @Before
    fun setUp() {
        val appWidgetManager =
            Shadows.shadowOf(
                context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
            )
        appWidgetManager.addBoundWidget(id.appWidgetId, AppWidgetProviderInfo())
    }

    @Test
    fun createRootEmittable() = runTest { assertIs<RemoteViewsRoot>(session.createRootEmittable()) }

    @Test
    fun provideGlanceRunsGlance() = runTest {
        runTestingComposition(session.provideGlance(context))
        assertThat(widget.provideGlanceCalled.get()).isTrue()
    }

    @Test
    fun provideGlanceEmitsIgnoreResultForNullContent() = runTest {
        // Create a widget that never calls provideContent, which means the session never produces
        // a valid result.
        val widget =
            object : GlanceAppWidget() {
                override suspend fun provideGlance(context: Context, id: GlanceId) {}
            }
        val session = AppWidgetSession(widget, id, defaultOptions, testState)
        val root =
            runCompositionUntil(
                { state, _ -> state == Recomposer.State.Idle },
                session.provideGlance(context)
            )
        assertThat(root.shouldIgnoreResult()).isTrue()
    }

    @Ignore // b/369179257
    @Test
    fun processEmittableTree() = runTest {
        val root =
            RemoteViewsRoot(maxDepth = 1).apply {
                children += EmittableText().apply { text = "hello" }
            }

        session.processEmittableTree(context, root)
        context.applyRemoteViews(session.lastRemoteViews.value!!).let {
            val text = assertIs<TextView>(it)
            assertThat(text.text).isEqualTo("hello")
        }
    }

    @Test
    fun processEmittableTree_ignoresResult() = runTest {
        val root = RemoteViewsRoot(maxDepth = 1).apply { children += EmittableIgnoreResult() }

        session.processEmittableTree(context, root)
        assertThat(session.lastRemoteViews.value).isNull()
    }

    @Test
    fun processEvent_unknownAction() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { session.processEvent(context, Any()) }
        }
    }

    @Test
    fun processEvent_updateGlance() = runTest {
        session.processEvent(context, AppWidgetSession.UpdateGlanceState)
        assertThat(testState.getValueCalls).containsExactly(id.toSessionKey())
    }

    @Test
    fun updateGlance() = runTest {
        session.updateGlance()
        session.receiveEvents(context) { this@runTest.launch { session.close() } }
        assertThat(testState.getValueCalls).containsExactly(id.toSessionKey())
    }

    @Test
    fun processEvent_runLambda() = runTest {
        var didRunFirst = false
        var didRunSecond = false
        session.processEmittableTree(
            context,
            RemoteViewsRoot(1).apply {
                children +=
                    EmittableBox().apply {
                        modifier =
                            GlanceModifier.then(
                                ActionModifier(LambdaAction("123") { didRunFirst = true })
                            )
                    }
                children +=
                    EmittableBox().apply {
                        modifier =
                            GlanceModifier.then(
                                ActionModifier(LambdaAction("123") { didRunSecond = true })
                            )
                    }
            }
        )
        session.processEvent(context, AppWidgetSession.RunLambda("123+0"))
        assertTrue(didRunFirst)
        assertFalse(didRunSecond)

        didRunFirst = false
        session.processEvent(context, AppWidgetSession.RunLambda("123+1"))
        assertTrue(didRunSecond)
        assertFalse(didRunFirst)
    }

    @Test
    fun runLambda() = runTest {
        var didRunFirst = false
        var didRunSecond = false
        session.processEmittableTree(
            context,
            RemoteViewsRoot(1).apply {
                children +=
                    EmittableBox().apply {
                        modifier =
                            GlanceModifier.then(
                                ActionModifier(LambdaAction("123") { didRunFirst = true })
                            )
                    }
                children +=
                    EmittableBox().apply {
                        modifier =
                            GlanceModifier.then(
                                ActionModifier(
                                    LambdaAction("123") {
                                        didRunSecond = true
                                        this@runTest.launch { session.close() }
                                    }
                                )
                            )
                    }
            }
        )

        session.runLambda("123+0")
        session.runLambda("123+1")
        session.receiveEvents(context) {}
        assertTrue(didRunFirst)
        assertTrue(didRunSecond)
    }

    @Test
    fun onCompositionError_throws_whenErrorUiLayoutNotSet() = runTest {
        // GlanceAppWidget.onCompositionError rethrows error when widget.errorUiLayout == 0
        val throwable = Exception("error")
        var caught: Throwable? = null
        try {
            session.onCompositionError(context, throwable)
        } catch (t: Throwable) {
            caught = t
        }
        assertThat(caught).isEqualTo(throwable)
    }

    @Test
    fun onCompositionError_noThrow_whenErrorUiLayoutIsSet() = runTest {
        val throwable = Exception("error")
        var caught: Throwable? = null
        widget.errorUiLayout = R.layout.glance_error_layout
        try {
            session.onCompositionError(context, throwable)
        } catch (t: Throwable) {
            caught = t
        }
        assertThat(caught).isEqualTo(null)
    }

    @Test
    fun waitForReadyResumesWhenEventIsReceived() = runTest {
        launch {
            session.waitForReady().join()
            session.close()
        }
        session.receiveEvents(context) {}
    }

    @Test
    fun waitForReadyResumesWhenSessionIsClosed() = runTest {
        launch { session.waitForReady().join() }
        // Advance until waitForReady suspends.
        this.testScheduler.advanceUntilIdle()
        session.close()
    }

    private class TestGlanceState : ConfigManager {

        val getValueCalls = mutableListOf<String>()

        @Suppress("UNCHECKED_CAST")
        override suspend fun <T> getValue(
            context: Context,
            definition: GlanceStateDefinition<T>,
            fileKey: String
        ): T {
            assertIs<PreferencesGlanceStateDefinition>(definition)
            getValueCalls.add(fileKey)
            return definition
                .getDataStore(context, fileKey)
                .also { definition.getLocation(context, fileKey).delete() }
                .data
                .first() as T
        }

        override suspend fun <T> updateValue(
            context: Context,
            definition: GlanceStateDefinition<T>,
            fileKey: String,
            updateBlock: suspend (T) -> T
        ): T {
            TODO("Not yet implemented")
        }

        override suspend fun deleteStore(
            context: Context,
            definition: GlanceStateDefinition<*>,
            fileKey: String
        ) {
            TODO("Not yet implemented")
        }
    }
}
