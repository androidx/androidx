/*
 * Copyright 2026 The Android Open Source Project
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
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.action
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ActionCallbackBroadcastReceiver
import androidx.glance.appwidget.updatestrategy.BroadcastRequest
import androidx.glance.appwidget.updatestrategy.UpdateStrategy
import androidx.glance.layout.Box
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild

@OptIn(ExperimentalGlanceApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class UpdateStrategyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        val appWidgetManager =
            Shadows.shadowOf(
                context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
            )
        val info =
            AppWidgetProviderInfo().apply {
                provider = ComponentName(context, TestReceiver::class.java)
            }
        appWidgetManager.addBoundWidget(UPDATE_ID, info)
        appWidgetManager.addBoundWidget(LAMBDA_ID, info)
        // Reset the state shared with classes that Glance creates by reflection.
        RecordingCallback.ranFor = CompletableDeferred()
        WorkerRequestingReceiver.askedForWorker = false
    }

    // Verifies GlanceAppWidget.updateStrategy defaults to DEFAULT (which matches SESSION).
    @Test
    fun defaultStrategy() {
        val widget = StrategyWidget(null) {}
        assertThat(widget.updateStrategy).isSameInstanceAs(UpdateStrategy.DEFAULT)
        assertThat(UpdateStrategy.DEFAULT).isSameInstanceAs(UpdateStrategy.SESSION)
    }

    // Verifies IMMEDIATE handles update broadcasts via goAsync without launching WorkManager.
    @Test
    fun immediate_handlesBroadcastWithGoAsync() = runTest {
        var askedForWorker = false
        val widget = StrategyWidget(UpdateStrategy.IMMEDIATE) { Text("Hello") }
        val receiver =
            object : GlanceAppWidgetReceiver() {
                override val glanceAppWidget: GlanceAppWidget = widget
                override val coroutineContext = StandardTestDispatcher(testScheduler)

                override fun shouldLaunchAsyncRequestWorker(): Boolean {
                    askedForWorker = true
                    return true
                }
            }

        sendUpdateBroadcast(receiver)
        advanceUntilIdle()

        // IMMEDIATE holds the broadcast with goAsync and ignores shouldLaunchAsyncRequestWorker.
        assertThat(Shadows.shadowOf(receiver).wentAsync()).isTrue()
        assertThat(askedForWorker).isFalse()
        assertThat(widget.provideGlanceCount.get()).isEqualTo(1)
    }

    // Verifies IMMEDIATE still updates on coroutineContext without goAsync when goAsync is broken.
    @Config(sdk = [34])
    @Test
    fun immediate_handlesBroadcastWithoutGoAsyncWhenItIsBroken() = runTest {
        ShadowBuild.setManufacturer("vivo")
        val widget = StrategyWidget(UpdateStrategy.IMMEDIATE) { Text("Hello") }
        val receiver =
            object : GlanceAppWidgetReceiver() {
                override val glanceAppWidget: GlanceAppWidget = widget
                override val coroutineContext = StandardTestDispatcher(testScheduler)
            }

        sendUpdateBroadcast(receiver)
        advanceUntilIdle()

        // The update still runs in the receiver's coroutineContext, but without goAsync.
        assertThat(Shadows.shadowOf(receiver).wentAsync()).isFalse()
        assertThat(widget.provideGlanceCount.get()).isEqualTo(1)
    }

    // Verifies createIntent records the widget receiver class name on callback broadcast intents.
    @Test
    fun runCallbackIntent_containsWidgetReceiver() {
        val receiver = ComponentName(context, TestReceiver::class.java)
        val intent =
            ActionCallbackBroadcastReceiver.createIntent(
                glanceComponents = GlanceComponents.getDefault(context),
                appWidgetId = UPDATE_ID,
                callbackClass = RecordingCallback::class.java,
                parameters = actionParametersOf(),
                receiver = receiver,
            )
        assertThat(intent.getStringExtra(ActionCallbackBroadcastReceiver.ExtraReceiverClassName))
            .isEqualTo(receiver.className)
    }

    // Verifies bound-widget callback broadcasts route to the receiver's updateStrategy and owner.
    @Test
    fun runCallback_forBoundWidget_isHandledByStrategyOfItsReceiver() {
        val strategy = mock<UpdateStrategy>()
        SharedStrategyReceiver.strategy = strategy

        val callbackReceiver =
            sendRunCallbackBroadcast(actionReceiver = SharedStrategyReceiver::class.java)

        val request = runCallbackHandledBy(strategy)
        assertThat(request.receiver).isSameInstanceAs(callbackReceiver)
        assertThat(request.widgetOwner).isInstanceOf(SharedStrategyReceiver::class.java)
        assertThat(request.glanceId).isEqualTo(AppWidgetId(UPDATE_ID))
    }

    // Verifies unmanaged-session callback broadcasts fall back to the session's widget strategy.
    @Test
    fun runCallback_forUnmanagedSession_isHandledByStrategyOfItsWidget() = runTest {
        val strategy = mock<UpdateStrategy>()
        registerUnmanagedSession(
            AppWidgetSession(StrategyWidget(strategy) {}, AppWidgetId(UPDATE_ID))
        )

        // GlanceAppWidget.runComposition creates its actions for UnmanagedSessionReceiver, which
        // is not a GlanceAppWidgetReceiver.
        sendRunCallbackBroadcast(actionReceiver = UnmanagedSessionReceiver::class.java)

        val request = runCallbackHandledBy(strategy)
        assertThat(request.widgetOwner).isNull()
        assertThat(request.glanceId).isEqualTo(AppWidgetId(UPDATE_ID))
    }

    // Verifies callback broadcasts execute via goAsync when no widget or session is found.
    @Test
    fun runCallback_withoutWidget_runsWithGoAsync() = runTest {
        // No widget receiver and no unmanaged session, like an intent from an older version.
        val callbackReceiver = sendRunCallbackBroadcast(actionReceiver = null)

        assertThat(Shadows.shadowOf(callbackReceiver).wentAsync()).isTrue()
        assertThat(RecordingCallback.ranFor.await()).isEqualTo(AppWidgetId(UPDATE_ID))
    }

    // Verifies no-widget callback broadcasts still execute without goAsync when goAsync is broken.
    @Config(sdk = [34])
    @Test
    fun runCallback_withoutWidget_runsWithoutGoAsyncWhenItIsBroken() = runTest {
        ShadowBuild.setManufacturer("vivo")
        val callbackReceiver = sendRunCallbackBroadcast(actionReceiver = null)

        assertThat(Shadows.shadowOf(callbackReceiver).wentAsync()).isFalse()
        assertThat(RecordingCallback.ranFor.await()).isEqualTo(AppWidgetId(UPDATE_ID))
    }

    // Verifies SESSION handles ActionCallback broadcasts directly, ignoring WorkManager overrides.
    @Test
    fun session_runCallbackIgnoresShouldLaunchAsyncRequestWorker() = runTest {
        val callbackReceiver =
            sendRunCallbackBroadcast(actionReceiver = WorkerRequestingReceiver::class.java)

        // Only broadcasts received by the widget receiver itself can ask for the worker.
        assertThat(WorkerRequestingReceiver.askedForWorker).isFalse()
        assertThat(Shadows.shadowOf(callbackReceiver).wentAsync()).isTrue()
        assertThat(RecordingCallback.ranFor.await()).isEqualTo(AppWidgetId(UPDATE_ID))
    }

    // Verifies IMMEDIATE update composes once, pushes RemoteViews to AppWidgetManager, and
    // finishes.
    @Test
    fun immediate_updatePushesAndEnds() = runTest {
        val widget = StrategyWidget(UpdateStrategy.IMMEDIATE) { Text("Hello") }

        widget.update(context, AppWidgetId(UPDATE_ID))

        assertThat(widget.provideGlanceCount.get()).isEqualTo(1)
        assertThat(textsShownFor(UPDATE_ID)).containsExactly("Hello")
    }

    // Verifies IMMEDIATE executes composition on the caller's CoroutineDispatcher.
    @Test
    fun immediate_runsOnCallersDispatcher() = runTest {
        var compositionThread: String? = null
        val widget =
            StrategyWidget(UpdateStrategy.IMMEDIATE) {
                SideEffect { compositionThread = Thread.currentThread().name }
                Text("Hello")
            }
        val executor = Executors.newSingleThreadExecutor { Thread(it, "glance-test-thread") }
        try {
            withContext(executor.asCoroutineDispatcher()) {
                widget.update(context, AppWidgetId(UPDATE_ID))
            }
        } finally {
            executor.shutdown()
        }

        // The thread name may have a coroutine debug suffix.
        assertThat(compositionThread).startsWith("glance-test-thread")
        assertThat(textsShownFor(UPDATE_ID)).containsExactly("Hello")
    }

    // Verifies IMMEDIATE invokes the target lambda action when triggerAction is called.
    @Test
    fun immediate_triggerActionRunsLambda() = runTest {
        val clicks = AtomicInteger(0)
        val widget =
            StrategyWidget(UpdateStrategy.IMMEDIATE) {
                Box(
                    modifier =
                        GlanceModifier.clickable(action("click") { clicks.incrementAndGet() })
                ) {
                    Text("Click me")
                }
            }

        // No session is running, so the composition runs again to find the lambda.
        widget.triggerAction(context, LAMBDA_ID, "click+0")

        assertThat(clicks.get()).isEqualTo(1)
    }

    // Verifies IMMEDIATE recomposes and pushes updated RemoteViews after a triggered state change.
    @Test
    fun immediate_triggerActionUpdatesStateAndPushes() = runTest {
        val text = mutableStateOf("before")
        val widget =
            StrategyWidget(UpdateStrategy.IMMEDIATE) {
                Box(modifier = GlanceModifier.clickable(action("click") { text.value = "after" })) {
                    Text(text.value)
                }
            }

        widget.triggerAction(context, LAMBDA_ID, "click+0")

        assertThat(textsShownFor(LAMBDA_ID)).containsExactly("after")
    }

    // Verifies IMMEDIATE pushes the first composition result and cancels ongoing LaunchedEffects.
    @Test
    fun immediate_doesNotWaitForEffects() = runTest {
        var effectCancelled = false
        val widget =
            StrategyWidget(UpdateStrategy.IMMEDIATE) {
                LaunchedEffect(Unit) {
                    try {
                        awaitCancellation()
                    } finally {
                        effectCancelled = true
                    }
                }
                Text("loading")
            }

        widget.update(context, AppWidgetId(UPDATE_ID))

        assertThat(textsShownFor(UPDATE_ID)).containsExactly("loading")
        assertThat(effectCancelled).isTrue()
    }

    // Verifies IMMEDIATE recomposes and publishes RemoteViews for new widget size options.
    @Test
    fun immediate_resizeComposesWithNewOptions() = runTest {
        val widget =
            StrategyWidget(UpdateStrategy.IMMEDIATE, sizeMode = SizeMode.Exact) {
                Text("${LocalSize.current}")
            }

        widget.resize(context, UPDATE_ID, optionsBundleOf(listOf(DpSize(100.dp, 50.dp))))

        assertThat(textsShownFor(UPDATE_ID)).containsExactly("100.0.dp x 50.0.dp")
    }

    // Verifies deleting an IMMEDIATE widget still invokes its onDelete callback.
    @Test
    fun immediate_deleteCallsOnDelete() = runTest {
        val widget = StrategyWidget(UpdateStrategy.IMMEDIATE) {}

        widget.deleted(context, UPDATE_ID)

        assertThat(widget.deletedIds).containsExactly(AppWidgetId(UPDATE_ID))
    }

    // Verifies IMMEDIATE pushes errorUiLayout and suppresses the error when composition fails.
    @Test
    fun immediate_compositionErrorShowsErrorLayout() = runTest {
        val widget =
            StrategyWidget(UpdateStrategy.IMMEDIATE, errorUiLayout = R.layout.glance_error_layout) {
                error("composition failed")
            }

        widget.update(context, AppWidgetId(UPDATE_ID))

        assertThat(textsShownFor(UPDATE_ID))
            .containsExactly(context.getString(R.string.glance_error_layout_text_v2))
    }

    // Verifies IMMEDIATE rethrows composition errors to the caller when errorUiLayout is 0.
    @Test
    fun immediate_compositionErrorWithoutErrorLayoutIsThrown() = runTest {
        val widget =
            StrategyWidget(UpdateStrategy.IMMEDIATE, errorUiLayout = 0) {
                error("composition failed")
            }

        val thrown =
            assertFailsWith<IllegalStateException> {
                widget.update(context, AppWidgetId(UPDATE_ID))
            }

        assertThat(thrown).hasMessageThat().isEqualTo("composition failed")
    }

    // Verifies IMMEDIATE updates do not register or displace active UnmanagedSessionReceiver
    // sessions.
    @Test
    fun immediate_doesNotReplaceUnmanagedSession() = runTest {
        val widget = StrategyWidget(UpdateStrategy.IMMEDIATE) { Text("Hello") }
        val unmanagedSession = AppWidgetSession(widget, AppWidgetId(UPDATE_ID))
        val registration = registerUnmanagedSession(unmanagedSession)

        widget.update(context, AppWidgetId(UPDATE_ID))

        // Registering another session for the same widget would have ended this registration.
        assertThat(registration.isActive).isTrue()
        assertThat(UnmanagedSessionReceiver.getSession(UPDATE_ID))
            .isSameInstanceAs(unmanagedSession)
    }

    /** Delivers an APPWIDGET_UPDATE broadcast for [UPDATE_ID] to [receiver]. */
    private fun sendUpdateBroadcast(receiver: GlanceAppWidgetReceiver) {
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(AppWidgetManager.ACTION_APPWIDGET_UPDATE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        context.sendBroadcast(
            Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(UPDATE_ID))
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    /**
     * Delivers a click on a [RecordingCallback] action of [UPDATE_ID] to a new
     * [ActionCallbackBroadcastReceiver], and returns that receiver.
     *
     * @param actionReceiver the receiver that the widget's actions were created for (see
     *   [TranslationContext.actionBroadcastReceiver]), or null if the intent does not have one
     */
    private fun sendRunCallbackBroadcast(
        actionReceiver: Class<out BroadcastReceiver>?
    ): ActionCallbackBroadcastReceiver {
        val callbackReceiver = ActionCallbackBroadcastReceiver()
        ContextCompat.registerReceiver(
            context,
            callbackReceiver,
            IntentFilter(RUN_CALLBACK_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        context.sendBroadcast(
            ActionCallbackBroadcastReceiver.createIntent(
                    glanceComponents = GlanceComponents.getDefault(context),
                    appWidgetId = UPDATE_ID,
                    callbackClass = RecordingCallback::class.java,
                    parameters = actionParametersOf(),
                    receiver = actionReceiver?.let { ComponentName(context, it) },
                )
                // Address callbackReceiver by action instead of by component. With the component
                // set, Robolectric also delivers the broadcast to a receiver of its own.
                .setComponent(null)
                .setAction(RUN_CALLBACK_ACTION)
        )
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        return callbackReceiver
    }

    /** Returns the RunCallback request that [strategy], a mock, was asked to handle. */
    private fun runCallbackHandledBy(strategy: UpdateStrategy): BroadcastRequest.RunCallback {
        val request = argumentCaptor<BroadcastRequest>()
        verify(strategy).handleBroadcast(any(), request.capture())
        return request.firstValue as BroadcastRequest.RunCallback
    }

    /**
     * Registers [session] as the unmanaged session of [UPDATE_ID] until the test ends, as
     * GlanceAppWidget.runComposition does while its flow is collected.
     */
    private fun TestScope.registerUnmanagedSession(session: AppWidgetSession): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            UnmanagedSessionReceiver.registerSession(UPDATE_ID, session)
        }

    /** Returns the texts of the views last pushed to [AppWidgetManager] for [appWidgetId]. */
    private fun textsShownFor(appWidgetId: Int): List<String> =
        Shadows.shadowOf(AppWidgetManager.getInstance(context))
            .getViewFor(appWidgetId)
            .findTextViews()
            .map { it.text.toString() }

    private class StrategyWidget(
        private val strategy: UpdateStrategy?,
        override val sizeMode: SizeMode = SizeMode.Single,
        @LayoutRes errorUiLayout: Int = 0,
        val ui: @Composable () -> Unit,
    ) : GlanceAppWidget(errorUiLayout) {
        override val updateStrategy: UpdateStrategy
            get() = strategy ?: super.updateStrategy

        override val stateDefinition: GlanceStateDefinition<*>? = null
        val provideGlanceCount = AtomicInteger(0)
        val deletedIds = mutableListOf<GlanceId>()

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            provideGlanceCount.incrementAndGet()
            provideContent(ui)
        }

        override suspend fun onDelete(context: Context, glanceId: GlanceId) {
            deletedIds += glanceId
        }
    }

    class TestReceiver : GlanceAppWidgetReceiver() {
        override val glanceAppWidget: GlanceAppWidget =
            object : GlanceAppWidget() {
                override suspend fun provideGlance(context: Context, id: GlanceId) {}
            }
    }

    /**
     * Glance creates this receiver by reflection, so tests set the [strategy] of its widget here.
     */
    class SharedStrategyReceiver : GlanceAppWidgetReceiver() {
        override val glanceAppWidget: GlanceAppWidget = StrategyWidget(strategy) {}

        companion object {
            lateinit var strategy: UpdateStrategy
        }
    }

    /** The receiver of an [UpdateStrategy.SESSION] widget that asks for the AsyncRequestWorker. */
    class WorkerRequestingReceiver : GlanceAppWidgetReceiver() {
        override val glanceAppWidget: GlanceAppWidget = StrategyWidget(UpdateStrategy.SESSION) {}

        override fun shouldLaunchAsyncRequestWorker(): Boolean {
            askedForWorker = true
            return true
        }

        companion object {
            var askedForWorker = false
        }
    }
}

/**
 * Completes [ranFor] with the widget that it runs for. Glance creates callbacks by reflection from
 * their canonical name, so this is a top-level class that reports through shared state.
 */
class RecordingCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        ranFor.complete(glanceId)
    }

    companion object {
        var ranFor = CompletableDeferred<GlanceId>()
    }
}

private fun View.findTextViews(): List<TextView> =
    when (this) {
        is TextView -> listOf(this)
        is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).findTextViews() }
        else -> emptyList()
    }

private const val UPDATE_ID = 7001
private const val LAMBDA_ID = 7002
private const val RUN_CALLBACK_ACTION = "androidx.glance.appwidget.test.RUN_CALLBACK"
