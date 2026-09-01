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

@file:OptIn(ExperimentalRemotePlayerApi::class, ExperimentalRemoteCreationComposeApi::class)

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.WriterEvents
import androidx.compose.remote.creation.compose.capture.captureRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.state.ri
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.core.RemoteDocument
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowTrace

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@Suppress("RestrictedApi")
class RcPlayerRecompositionEventsTest {

    val repeatRule = RepeatRule()
    val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()
    val rule = createComposeRule()

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(repeatRule).around(enableEmbeddedPlayer).around(rule)

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = true
    }

    @After
    fun tearDown() {
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        testLambdaMap.clear()
        lambdaCounter = 0
        ShadowTrace.reset()
    }

    @Test
    @Repeat(5)
    fun testMutableStateOutputsTextAndIsUpdated(): Unit = runBlocking {
        val state = mutableStateOf("Initial")

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = { RemoteText(state.value.rs) },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent { document?.let { doc -> key(doc) { RcPlayer(document = doc) } } }

        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        try {
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.onNodeWithText("Initial", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                state.value = "Updated"
                Snapshot.sendApplyNotifications()
            }

            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 2
            }

            rule.onNodeWithText("Updated", useUnmergedTree = true).assertExists()
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testDerivedStateOfSeconds(): Unit = runBlocking {
        val secondsState = mutableStateOf(0)
        val derivedText = derivedStateOf { "Seconds: ${secondsState.value}" }

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = { RemoteText(derivedText.value.rs) },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent { document?.let { doc -> key(doc) { RcPlayer(document = doc) } } }

        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        try {
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.onNodeWithText("Seconds: 0", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                secondsState.value = 1
                Snapshot.sendApplyNotifications()
            }

            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 2
            }

            rule.onNodeWithText("Seconds: 1", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                secondsState.value = 2
                Snapshot.sendApplyNotifications()
            }

            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 3
            }

            rule.onNodeWithText("Seconds: 2", useUnmergedTree = true).assertExists()
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testLaunchedEffectTriggered(): Unit = runBlocking {
        val triggerState = mutableStateOf(0)

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = {
                    var text by remember { mutableStateOf("Initial") }
                    LaunchedEffect(triggerState.value) {
                        if (triggerState.value > 0) {
                            text = "Updated: ${triggerState.value}"
                        }
                    }
                    RemoteText(text.rs)
                },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent { document?.let { doc -> key(doc) { RcPlayer(document = doc) } } }

        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        try {
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.onNodeWithText("Initial", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                triggerState.value = 1
                Snapshot.sendApplyNotifications()
            }

            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 2
            }

            rule.onNodeWithText("Updated: 1", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                triggerState.value = 2
                Snapshot.sendApplyNotifications()
            }

            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 3
            }

            rule.onNodeWithText("Updated: 2", useUnmergedTree = true).assertExists()
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testCollectAsStateWithLifecycle(): Unit = runBlocking {
        val stateFlow = MutableStateFlow("Initial")
        val lifecycleOwner = SimpleLifecycleOwner()

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = {
                    CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                        val text by stateFlow.collectAsStateWithLifecycle()
                        RemoteText(text.rs)
                    }
                },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent { document?.let { doc -> key(doc) { RcPlayer(document = doc) } } }

        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        try {
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.onNodeWithText("Initial", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                stateFlow.value = "Updated"
                Snapshot.sendApplyNotifications()
            }

            rule.waitUntil(10000) {
                Snapshot.sendApplyNotifications()
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 2
            }

            rule.onNodeWithText("Updated", useUnmergedTree = true).assertExists()
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testLambdaActionOnClick(): Unit = runBlocking {
        var lambdaClicked = false

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = {
                    RemoteBox(modifier = RemoteModifier.testOnClick { lambdaClicked = true }) {}
                },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent {
            document?.let { doc ->
                key(doc) {
                    RcPlayer(
                        document = doc,
                        onNamedAction = { name, value, _ ->
                            if (name == "test_lambda" && value is Int) {
                                testLambdaMap[value]?.invoke()
                            }
                        },
                    )
                }
            }
        }

        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        try {
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.onNode(hasClickAction()).performClick()

            rule.waitUntil(5000) {
                ShadowLooper.idleMainLooper()
                lambdaClicked
            }
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testBooleanDisplayBranchChange(): Unit = runBlocking {
        val displayState = mutableStateOf(true)

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = {
                    if (displayState.value) {
                        RemoteText("True Branch".rs)
                    } else {
                        RemoteText("False Branch".rs)
                    }
                },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent { document?.let { doc -> key(doc) { RcPlayer(document = doc) } } }

        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        // Essential to run on Main thread to align with Compose state changes and avoid deadlocks
        // in Robolectric
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        try {
            // Wait for 1st emission
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.onNodeWithText("True Branch", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                displayState.value = false
                Snapshot.sendApplyNotifications()
            }

            // Wait for 2nd emission
            rule.waitUntil(10000) {
                Snapshot.sendApplyNotifications()
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 2
            }

            rule.onNodeWithText("False Branch", useUnmergedTree = true).assertExists()
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testBooleanDisplayBranchChange_StressTest(): Unit = runBlocking {
        val iterations = 50 // 100 state changes
        val displayState = mutableStateOf(true)

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = {
                    if (displayState.value) {
                        RemoteText("True Branch".rs)
                    } else {
                        RemoteText("False Branch".rs)
                    }
                },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent { document?.let { doc -> key(doc) { RcPlayer(document = doc) } } }

        val emissionCount = AtomicInteger(0)

        val collectorJob = Job()
        // Essential to run on Main thread to align with Compose state changes and avoid deadlocks
        // in Robolectric
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        // Total emissions expected = iterations * 2 (one for true, one for false per iteration)
        // Let's change the loop to be per state change to keep it simple.
        // We start at true.

        try {
            repeat(iterations * 2) { i ->
                val expectedState = i % 2 == 0 // 0 -> true, 1 -> false, 2 -> true...
                val expectedText = if (expectedState) "True Branch" else "False Branch"
                val targetEmission = i + 1 // Starts at 1

                // Wait for emission
                rule.waitUntil(10000) {
                    ShadowLooper.idleMainLooper()
                    val current = emissionCount.get()
                    current >= targetEmission
                }

                // Assert text
                rule.onNodeWithText(expectedText, useUnmergedTree = true).assertExists()

                // Toggle state
                if (i < (iterations * 2) - 1) {
                    val nextState = !expectedState
                    rule.runOnUiThread {
                        displayState.value = nextState
                        Snapshot.sendApplyNotifications()
                    }
                }
            }
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testNoEmissionWhenStateChangesDoNotChangeLayout(): Unit = runBlocking {
        val state = mutableStateOf("Initial")

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = {
                    state.value
                    RemoteText("Same Content".rs)
                },
            )

        val documents = mutableListOf<ByteArray>()
        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        collectorScope.launch {
            flow.collect { bytes ->
                synchronized(documents) { documents.add(bytes) }
                emissionCount.incrementAndGet()
            }
        }

        try {
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.runOnUiThread {
                state.value = "Updated"
                Snapshot.sendApplyNotifications()
            }

            ShadowLooper.idleMainLooper()
            delay(200)
            ShadowLooper.idleMainLooper()

            assertThat(documents).hasSize(1)
            val doc = RemoteDocument(documents[0])
            assertThat(doc.document.displayHierarchy()).contains("Same Content")
        } finally {
            collectorJob.cancel()
        }
    }

    @Test
    @Repeat(5)
    fun testCollectAsStateWithLifecycle_respectsLifecycleStateChanges(): Unit = runBlocking {
        val stateFlow = MutableStateFlow("Initial")
        val lifecycleOwner = SimpleLifecycleOwner()

        val flow =
            captureRemoteDocument(
                context = context,
                creationDisplayInfo = RemoteCreationDisplayInfo(100, 100, 160, 1.0f),
                writerEvents = WriterEvents(),
                coroutineContext = Dispatchers.Main,
                content = {
                    CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                        val text by stateFlow.collectAsStateWithLifecycle()
                        RemoteText(text.rs)
                    }
                },
            )

        var document by mutableStateOf<CoreDocument?>(null)

        rule.setContent { document?.let { doc -> key(doc) { RcPlayer(document = doc) } } }

        val emissionCount = AtomicInteger(0)
        val collectorJob = Job()
        val collectorScope = CoroutineScope(Dispatchers.Main + collectorJob)

        val documents = mutableListOf<ByteArray>()
        collectorScope.launch {
            flow.collect { bytes ->
                synchronized(documents) { documents.add(bytes) }
                val doc =
                    CoreDocument(RemoteClock.SYSTEM).apply {
                        ByteArrayInputStream(bytes).use {
                            initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                        }
                    }
                document = doc
                emissionCount.incrementAndGet()
            }
        }

        try {
            rule.waitUntil(10000) {
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 1
            }

            rule.onNodeWithText("Initial", useUnmergedTree = true).assertExists()

            rule.runOnUiThread {
                lifecycleOwner.setCurrentState(Lifecycle.State.CREATED)
                Snapshot.sendApplyNotifications()
            }
            ShadowLooper.idleMainLooper()

            rule.runOnUiThread {
                stateFlow.value = "Updated While Stopped"
                Snapshot.sendApplyNotifications()
            }
            ShadowLooper.idleMainLooper()

            synchronized(documents) { assertThat(documents).hasSize(1) }

            rule.runOnUiThread {
                lifecycleOwner.setCurrentState(Lifecycle.State.RESUMED)
                Snapshot.sendApplyNotifications()
            }

            rule.waitUntil(10000) {
                Snapshot.sendApplyNotifications()
                ShadowLooper.idleMainLooper()
                emissionCount.get() >= 2
            }

            rule.onNodeWithText("Updated While Stopped", useUnmergedTree = true).assertExists()
        } finally {
            collectorJob.cancel()
        }
    }

    private class SimpleLifecycleOwner : LifecycleOwner {
        private val registry =
            LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle
            get() = registry

        fun setCurrentState(state: Lifecycle.State) {
            registry.currentState = state
        }
    }
}

private var lambdaCounter = 0
private val testLambdaMap = HashMap<Int, () -> Unit>()

private fun RemoteModifier.testOnClick(action: () -> Unit): RemoteModifier {
    val actionId = 123000 + lambdaCounter++
    testLambdaMap[actionId] = action
    return clickable(hostAction("test_lambda".rs, actionId.ri))
}

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Repeat(val times: Int = 1)

class RepeatRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        val repeat =
            description.getAnnotation(Repeat::class.java)
                ?: description.testClass.getAnnotation(Repeat::class.java)
        val times = repeat?.times ?: 1
        return if (times > 1) {
            object : Statement() {
                override fun evaluate() {
                    for (i in 0 until times) {
                        base.evaluate()
                    }
                }
            }
        } else {
            base
        }
    }
}
