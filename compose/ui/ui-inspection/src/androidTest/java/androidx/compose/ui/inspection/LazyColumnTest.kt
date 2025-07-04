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

package androidx.compose.ui.inspection

import android.view.inspector.WindowInspector.getGlobalWindowViews
import androidx.compose.ui.inspection.rules.JvmtiRule
import androidx.compose.ui.inspection.testdata.LazyColumnTestActivity
import androidx.compose.ui.inspection.util.createAllParametersChecks
import androidx.compose.ui.inspection.util.filter
import androidx.compose.ui.inspection.util.nodes
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToIndex
import androidx.inspection.testing.InspectorTester
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode.Flags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class LazyColumnTest {
    private val rule = createAndroidComposeRule<LazyColumnTestActivity>()

    @get:Rule val chain = RuleChain.outerRule(JvmtiRule()).around(rule)!!

    private lateinit var inspectorTester: InspectorTester
    private var rootId = 0L
    private var generation = 1

    @Before
    fun before() {
        runBlocking {
            inspectorTester = InspectorTester(inspectorId = "layoutinspector.compose.inspection")
        }
    }

    @After
    fun after() {
        inspectorTester.dispose()
    }

    @Test
    fun unknownLocation(): Unit = runBlocking {
        rootId = getGlobalWindowViews().map { it.uniqueDrawingId }.single()

        // Scrolling to index 30 is known to create some extra composables that are used to display
        // rows after a scroll operation. These would have been marked with unknown location.
        rule.onNode(hasScrollAction()).performScrollToIndex(30)
        generation++
        val checks = createAllParametersChecks(inspectorTester, rootId, generation)
        val texts = checks.composableResponse.filter("Text")

        // Verify that all composables with unknown location have been filtered out.
        // Do that by checking the text parameter value of all the Text composables.
        // Note: A composable with unknown location does not have any parameters i.e. the text
        // parameter would be empty.
        val expectedTextValues = mutableSetOf<String>()
        for (index in 30..41) {
            expectedTextValues.add("Hello number: $index")
        }
        assertThat(texts.size).isAtLeast(10)
        for (text in texts) {
            assertThat(checks.parameterTextValue(text)).isIn(expectedTextValues)
        }
    }

    @Test
    fun rowsInOrder(): Unit = runBlocking {
        rootId = getGlobalWindowViews().map { it.uniqueDrawingId }.single().toLong()
        textComponentsInOrder(0)
        rule.onNode(hasScrollAction()).performScrollToIndex(30)
        textComponentsInOrder(30)
        rule.onNode(hasScrollAction()).performScrollToIndex(85)
        textComponentsInOrder(85)
        rule.onNode(hasScrollAction()).performScrollToIndex(15)
        textComponentsInOrder(15)
    }

    @Test
    fun testDrawModifierNodes() = runBlocking {
        generation++
        rootId = getGlobalWindowViews().map { it.uniqueDrawingId }.single().toLong()
        val checks = createAllParametersChecks(inspectorTester, rootId, generation)
        val withChildDrawModifiers =
            checks.composableResponse
                .nodes()
                .filter { (it.flags and Flags.SYSTEM_CREATED_VALUE) == 0 }
                .filter { (it.flags and Flags.HAS_CHILD_DRAW_MODIFIER_VALUE) != 0 }
        assertThat(withChildDrawModifiers.size).isAtLeast(10)

        // If this fails: Check for changes in SubcomposeLayout.
        // We want to skip the draw modifier flag for the composable that os drawing on top of
        // all the elements in LazyColumn.
        checks.assertNoNode(withChildDrawModifiers, "LazyColumn")

        // All nodes with draw modifiers should be Text nodes:
        withChildDrawModifiers.forEach { checks.assertNode(it, "Text") }

        checks.assertTextNode(withChildDrawModifiers[0], "Hello number: 0")
        checks.assertTextNode(withChildDrawModifiers[1], "Hello number: 1")
    }

    private suspend fun textComponentsInOrder(startIndex: Int): Int {
        generation++
        val checks = createAllParametersChecks(inspectorTester, rootId, generation)
        val texts = checks.composableResponse.filter("Text")
        assertThat(texts.size).isAtLeast(10)

        var index = startIndex
        var top = texts[0].bounds.layout.y
        texts.forEach { text ->
            assertThat(text.bounds.layout.y).isEqualTo(top)
            checks.assertTextNode(text, "Hello number: $index")
            top += text.bounds.layout.h
            index++
        }
        return startIndex + texts.size
    }
}
