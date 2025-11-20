/*
 * Copyright 2025 The Android Open Source Project
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
@file:RequiresApi(Build.VERSION_CODES.Q)

package androidx.glance.appwidget.multiprocess.test

import android.Manifest
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.await
import androidx.core.view.children
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.multiprocess.test.MultiProcessTests.Companion.timeout
import androidx.glance.appwidget.multiprocess.test.TestWidget.Companion.setContent
import androidx.glance.text.Text
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.multiprocess.RemoteWorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.FileInputStream
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * This class tests that multi-process glance widgets are able to correctly run in a non-default
 * process.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class MultiProcessTests {
    @get:Rule
    val activityRule: ActivityScenarioRule<TestActivity> =
        ActivityScenarioRule(TestActivity::class.java)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext!!
    private val app = context.applicationContext as TestApplication
    private val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation

    @Before
    fun before() {
        checkCustomProcess()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        waitForBroadcastIdle()
    }

    @After
    fun after() {
        AppWidgetHost(context, 1234).apply { appWidgetIds.forEach { deleteAppWidgetId(it) } }
        runBlocking { RemoteWorkManager.getInstance(context).cancelAllWork().await() }
        uiAutomation.dropShellPermissionIdentity()
    }

    private fun bindWidget(): AppWidgetHostView {
        uiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        var hostView: AppWidgetHostView? = null
        activityRule.scenario.onActivity { activity ->
            val host = AppWidgetHost(activity, 1234)
            val appWidgetId = host.allocateAppWidgetId()
            val appWidgetManager = AppWidgetManager.getInstance(activity)
            val bound =
                appWidgetManager.bindAppWidgetIdIfAllowed(
                    appWidgetId,
                    ComponentName(activity, TestWidgetReceiver::class.java),
                )
            assertWithMessage("Failed to bind").that(bound).isTrue()
            Log.v("MultiProcessTests", "Bound widget $appWidgetId")
            host.startListening()
            hostView =
                host.createView(
                    activity,
                    appWidgetId,
                    appWidgetManager.getAppWidgetInfo(appWidgetId),
                )
            activity.setContentView(hostView)
        }
        return hostView!!
    }

    @Test
    fun text() = runBlocking {
        setContent { Text("Hello World") }

        val textView = bindWidget().waitForChildren().findByType<TextView>()
        assertThat(textView.text.toString()).isEqualTo("Hello World")
    }

    @Test
    fun list() = runBlocking {
        setContent { LazyColumn { items(10, { it.toLong() }) { Text("$it") } } }

        val list = bindWidget().waitForChildren().findByType<ListView>()
        val adapter = list.waitForAdapter()
        assertThat(adapter.count).isEqualTo(10)
        for (i in 0 until 10) {
            val unboxedItem = (adapter.getView(i, null, list) as ViewGroup).findByType<TextView>()
            assertThat(unboxedItem.text.toString()).isEqualTo("$i")
        }
    }

    @Test
    fun lambda() =
        runBlocking<Unit> {
            val state = MutableStateFlow(0)
            setContent {
                Text(text = "Hello World", modifier = GlanceModifier.clickable { state.value = 1 })
            }

            val textView = bindWidget().waitForChildren().findByType<TextView>()
            app.mainExecutor.execute {
                // Click listener is registered on parent view
                assertThat((textView.parent as View).performClick()).isTrue()
            }
            val result = withTimeoutOrNull(timeout) { state.first { it == 1 } }
            assertWithMessage("Did not receive the state change within $timeout")
                .that(result)
                .isNotNull()
        }

    @Test
    fun runAction() =
        runBlocking<Unit> {
            val state = app.actionFlow.apply { value = 0 }
            setContent {
                Text(
                    text = "Hello World",
                    modifier = GlanceModifier.clickable(actionRunCallback<TestAction>()),
                )
            }

            val textView = bindWidget().waitForChildren().findByType<TextView>()
            app.mainExecutor.execute {
                // Click listener is registered on parent view
                assertThat((textView.parent as View).performClick()).isTrue()
            }
            val result = withTimeoutOrNull(timeout) { state.first { it == 1 } }
            assertWithMessage("Did not receive the state change within $timeout")
                .that(result)
                .isNotNull()
        }

    companion object {
        val timeout = 5.minutes
    }
}

class TestAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        checkCustomProcess()
        (context.applicationContext as TestApplication).actionFlow.value = 1
    }
}

/** Does a depth-first search for a view of type T. */
private inline fun <reified T : View> View.findByType(): T =
    assertNotNull(findByType(T::class), "Could not find View of type ${T::class} in $this")

private fun <T : View> View.findByType(clazz: KClass<T>): T? =
    when {
        clazz.isInstance(this) -> clazz.cast(this)
        this is ViewGroup -> children.firstNotNullOfOrNull { it.findByType(clazz) }
        else -> null
    }

/** Waits for the AppWidgetHostView to have children and returns the unboxed host view. */
private suspend fun AppWidgetHostView.waitForChildren(): View {
    fun test(): Boolean = findViewById<View>(androidx.glance.appwidget.R.id.rootView) != null
    if (test()) return (getChildAt(0) as ViewGroup).getChildAt(0)

    val channel = Channel<Unit>(Channel.CONFLATED)
    val onDraw = {
        if (test()) {
            assertTrue(channel.trySend(Unit).isSuccess, "failed to send on channel")
        }
    }
    post { viewTreeObserver.addOnDrawListener(onDraw) }
    val result = withTimeoutOrNull(timeout) { channel.receive() }
    assertWithMessage("Did not receive children within $timeout").that(result).isNotNull()
    post { viewTreeObserver.removeOnDrawListener(onDraw) }
    return (getChildAt(0) as ViewGroup).getChildAt(0)
}

/** Waits for the ListView to have an adapter. */
private suspend fun ListView.waitForAdapter(): Adapter {
    val adapter =
        withTimeoutOrNull(timeout) {
            while (adapter == null) {
                delay(500.milliseconds)
            }
            adapter!!
        }
    assertWithMessage("Did not find list adapter within $timeout").that(adapter).isNotNull()
    return adapter!!
}

fun waitForBroadcastIdle() {
    // Default timeout set per observation with FTL devices in b/283484546
    val cmd: String =
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            // wait for pending broadcasts until this point to be completed for UDC+
            "am wait-for-broadcast-barrier"
        } else {
            // wait for broadcast queues to be idle. This is less preferred approach as it can
            // technically take forever.
            "am wait-for-broadcast-idle"
        }
    Log.v("MultiProcessTests", runShellCommand("timeout ${timeout.inWholeSeconds} $cmd"))
}

/** Run a command and retrieve the output as a string. */
fun runShellCommand(command: String): String {
    return InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand(command)
        .use { FileInputStream(it.fileDescriptor).reader().readText() }
}
