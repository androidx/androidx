/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.tooling

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalComposeRuntimeApi::class)
@MediumTest
@RunWith(Parameterized::class)
class UiErrorTraceTests(private val lookahead: Boolean) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "lookahead={0}")
        fun init() = listOf(false, true)
    }

    private lateinit var exceptionHandler: (Throwable) -> Unit

    @OptIn(ExperimentalTestApi::class)
    @get:Rule
    val rule =
        createAndroidComposeRule<ComponentActivity>(
            CoroutineExceptionHandler { _, e -> exceptionHandler.invoke(e) }
        )

    @Before
    fun setUp() {
        Composer.setDiagnosticStackTraceEnabled(true)
    }

    @After
    fun tearDown() {
        Composer.setDiagnosticStackTraceEnabled(false)
    }

    @Test
    fun initialLayoutMeasure() {
        val traceContext =
            rule.testContent { Layout(measurePolicy = { _, _ -> throwTestException() }) }

        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "Layout" }
    }

    @Test
    fun initialLayoutPlace() {
        val traceContext =
            rule.testContent {
                Layout(measurePolicy = { _, _ -> layout(10, 10) { throwTestException() } })
            }

        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "Layout" }
    }

    @Test
    fun initialModifierDraw() {
        val traceContext =
            rule.testContent {
                val drawModifier = Modifier.drawBehind { throwTestException() }
                Box(Modifier.size(10.dp).then(drawModifier))
            }

        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "Box" }
    }

    @Test
    fun initialModifierMeasure() {
        val traceContext =
            rule.testContent {
                val modifier = Modifier.layout { _, _ -> throwTestException() }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "Box" }
    }

    @Test
    fun initialModifierLayout() {
        val traceContext =
            rule.testContent {
                val modifier = Modifier.layout { _, _ -> layout(10, 10) { throwTestException() } }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "Box" }
    }

    @Test
    fun recomposeModifierLayoutPlace() {
        assumeFalse(lookahead) // todo figure out how to bail out of main pass in lookahead

        var state by mutableStateOf(false)
        val traceContext =
            rule.testContent {
                val modifier =
                    if (state) {
                        Modifier.layout { _, _ -> layout(10, 10) { throwTestException() } }
                    } else {
                        Modifier
                    }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertNull(traceContext.trace, "No initial crash expected")

        state = true
        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "Box" }
    }

    @Test
    fun recomposeModifierLayoutMeasure() {
        assumeFalse(lookahead) // todo figure out how to bail out of main pass in lookahead

        var state by mutableStateOf(false)
        val context =
            rule.testContent {
                val modifier =
                    if (state) {
                        Modifier.layout { _, _ -> throwTestException() }
                    } else {
                        Modifier
                    }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertNull(context.trace, "No initial crash expected")

        state = true
        rule.waitForIdle()

        assertFirstContentFrame(context) { it.name == "Box" }
    }

    @Test
    fun recomposeModifierDraw() {
        var state by mutableStateOf(false)
        val context =
            rule.testContent {
                val modifier =
                    if (state) {
                        Modifier.drawBehind { throwTestException() }
                    } else {
                        Modifier
                    }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertNull(context.trace, "No initial crash expected")

        state = true
        rule.waitForIdle()

        assertFirstContentFrame(context) { it.name == "Box" }
    }

    @Test
    fun modifierReplace() {
        assumeFalse(lookahead) // todo figure out how to bail out of main pass in lookahead

        var state by mutableStateOf(false)
        val context =
            rule.testContent {
                val modifier =
                    Modifier.layout { m, c ->
                        val p = m.measure(c)
                        layout(p.width, p.height) {
                            if (state) throwTestException()
                            p.place(0, 0)
                        }
                    }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertNull(context.trace, "No initial crash expected")

        state = true
        rule.waitForIdle()

        assertFirstContentFrame(context) { it.name == "Box" }
    }

    @Test
    fun modifierRemeasure() {
        assumeFalse(lookahead) // todo figure out how to bail out of main pass in lookahead

        var state by mutableStateOf(false)
        val context =
            rule.testContent {
                val modifier =
                    Modifier.layout { m, c ->
                        if (state) throwTestException()
                        val p = m.measure(c)
                        layout(p.width, p.height) { p.place(0, 0) }
                    }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertNull(context.trace, "No initial crash expected")

        state = true
        rule.waitForIdle()

        assertFirstContentFrame(context) { it.name == "Box" }
    }

    @Test
    fun modifierRedraw() {
        var state by mutableStateOf(false)
        val context =
            rule.testContent {
                val modifier = Modifier.drawBehind { if (state) throwTestException() }
                Box(Modifier.size(10.dp).then(modifier))
            }

        rule.waitForIdle()

        assertNull(context.trace, "No initial crash expected")

        state = true
        rule.waitForIdle()

        assertFirstContentFrame(context) { it.name == "Box" }
    }

    @Test
    fun initialSubcomposeLayoutMeasure() {
        val traceContext =
            rule.testContent {
                SubcomposeLayout { c ->
                    val p = subcompose(Unit) { throwTestException() }.map { it.measure(c) }
                    layout(10, 10) { p.forEach { it.place(0, 0) } }
                }
            }

        assertFirstContentFrame(traceContext) { it.name == "SubcomposeLayout" }
        assertTrace(traceContext) {
            it.first().name == "<lambda>" && it.first().file == CurrentTestFile
        }
    }

    @Test
    fun initialSubcomposeLayoutPlace() {
        val traceContext =
            rule.testContent {
                SubcomposeLayout { c ->
                    layout(10, 10) {
                        val p = subcompose(Unit) { throwTestException() }.map { it.measure(c) }
                        p.forEach { it.place(0, 0) }
                    }
                }
            }

        assertFirstContentFrame(traceContext) { it.name == "SubcomposeLayout" }
        assertTrace(traceContext) {
            it.first().name == "<lambda>" && it.first().file == CurrentTestFile
        }
    }

    @Test
    fun recomposeSubcomposeLayout() {
        assumeFalse(lookahead) // todo figure out how to bail out of main pass in lookahead

        var state by mutableStateOf(false)
        val traceContext =
            rule.testContent {
                SubcomposeLayout { c ->
                    val value = state
                    val p =
                        subcompose(Unit) {
                                // technically not exact recomposition, but we want to throw it on
                                // measure path
                                // recompositions + subcompose layout are tested in runtime
                                if (value) throwTestException()
                                Box(Modifier.size(10.dp))
                            }
                            .map { it.measure(c) }

                    layout(10, 10) { p.forEach { it.place(0, 0) } }
                }
            }

        assertNull(traceContext.trace, "No initial crash expected")

        state = true
        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "SubcomposeLayout" }
        assertTrace(traceContext) {
            it.first().name == "<lambda>" && it.first().file == CurrentTestFile
        }
    }

    @Test()
    fun launchedEffect() {
        val traceContext = rule.testContent { LaunchedEffect(Unit) { throwTestException() } }

        assertTrace(traceContext) {
            it[0].name == "remember" &&
                it[0].line == "<unknown line>" &&
                it[1].name == "LaunchedEffect" &&
                it[2].name == "<lambda>" &&
                it[2].file == CurrentTestFile
        }
    }

    @Test()
    fun launchedEffectLaunch() {
        val traceContext =
            rule.testContent { LaunchedEffect(Unit) { launch { throwTestException() } } }

        assertTrace(traceContext) {
            it[0].name == "remember" &&
                it[0].line == "<unknown line>" &&
                it[1].name == "LaunchedEffect" &&
                it[2].name == "<lambda>" &&
                it[2].file == CurrentTestFile
        }
    }

    @Test
    fun rememberCoroutine() {
        val traceContext =
            rule.testContent {
                val scope = rememberCoroutineScope()
                DisposableEffect(Unit) {
                    scope.launch { throwTestException() }
                    onDispose { scope.cancel() }
                }
            }

        assertTrace(traceContext) {
            it[0].name == "remember" &&
                it[0].line == "<unknown line>" &&
                it[1].name == "rememberCoroutineScope" &&
                it[2].name == "<lambda>" &&
                it[2].file == CurrentTestFile
        }
    }

    @Test
    fun layerCrash() {
        var shouldCrash by mutableStateOf(false)
        val traceContext =
            rule.testContent {
                Box(
                    Modifier.graphicsLayer {
                        alpha = if (shouldCrash) 0f else 1f
                        if (shouldCrash) throwTestException()
                    }
                )
            }

        shouldCrash = true
        Snapshot.sendApplyNotifications()
        rule.waitForIdle()

        assertFirstContentFrame(traceContext) { it.name == "Box" }
    }

    private fun AndroidComposeTestRule<*, *>.testContent(
        content: @Composable () -> Unit
    ): TestTraceContext {
        val traceContext = TestTraceContext()
        // init view
        setContent {}
        val view =
            activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) as ComposeView
        exceptionHandler = { e: Throwable ->
            if (traceContext.trace == null && e is TestException) {
                traceContext.trace = e.composeStackTrace()
            } else {
                throw e
            }
        }

        findViewRootForTest(view)!!.setUncaughtExceptionHandler(
            object : RootForTest.UncaughtExceptionHandler {
                override fun onUncaughtException(t: Throwable) {
                    exceptionHandler.invoke(t)
                }
            }
        )
        view.setContent {
            if (!lookahead) {
                content()
            } else {
                LookaheadScope { content() }
            }
        }
        waitForIdle()
        return traceContext
    }

    private fun assertFirstContentFrame(
        traceContext: TestTraceContext,
        assertion: (TraceFrame) -> Boolean,
    ) {
        assertTrace(traceContext) { t ->
            val lambdaFrame = t.indexOfLast { it.file == CurrentTestFile && it.name == "<lambda>" }
            val offset = if (lookahead) 6 else 2
            assertion(t[lambdaFrame - offset])
        }
    }
}

private class TestTraceContext {
    var trace: List<TraceFrame>? = null
}

private fun findViewRootForTest(view: View): ViewRootForTest? {
    if (view is ViewRootForTest) {
        return view
    }

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val composeView = findViewRootForTest(view.getChildAt(i))
            if (composeView != null) {
                return composeView
            }
        }
    }
    return null
}

private fun throwTestException(): Nothing = throw TestException()

private class TestException : RuntimeException("Expected test error")

private fun TestException.composeStackTrace(): List<TraceFrame>? =
    suppressedExceptions
        .singleOrNull { it.javaClass.simpleName == "DiagnosticComposeException" }
        ?.message
        ?.traceFrames()

private class TraceFrame(val name: String, val line: String, val file: String) {
    override fun toString() = "$name($file:$line)"
}

private fun String.traceFrames(): List<TraceFrame> =
    substringAfter("Composition stack when thrown:\n")
        .lines()
        .filter { it.isNotEmpty() }
        .map {
            val trace = it.removePrefix("\tat ")
            val name = trace.substringBefore('(')
            val line = trace.substringAfter(':').substringBefore(')')
            val file = trace.substringAfter('(').substringBefore(':')
            TraceFrame(name, line, file)
        }

private fun assertTrace(traceContext: TestTraceContext, assertion: (List<TraceFrame>) -> Boolean) {
    val trace =
        traceContext.trace ?: throw AssertionError("No composition traces have been collected")
    val result = assertion(trace)
    if (!result)
        throw AssertionError("Trace does not match expectations:\n${trace.joinToString(",\n")}")
}

private const val CurrentTestFile = "UiErrorTraceTests.kt"
