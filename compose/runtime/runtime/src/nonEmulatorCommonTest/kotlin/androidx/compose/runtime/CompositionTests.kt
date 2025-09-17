/*
 * Copyright 2019 The Android Open Source Project
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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.compose.runtime.mock.Contact
import androidx.compose.runtime.mock.ContactModel
import androidx.compose.runtime.mock.Edit
import androidx.compose.runtime.mock.EmptyApplier
import androidx.compose.runtime.mock.InlineLinear
import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.MockViewValidator
import androidx.compose.runtime.mock.Point
import androidx.compose.runtime.mock.Points
import androidx.compose.runtime.mock.Repeated
import androidx.compose.runtime.mock.Report
import androidx.compose.runtime.mock.ReportsReport
import androidx.compose.runtime.mock.ReportsTo
import androidx.compose.runtime.mock.SelectContact
import androidx.compose.runtime.mock.TestMonotonicFrameClock
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.View
import androidx.compose.runtime.mock.ViewApplier
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.contact
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.frameDelayMillis
import androidx.compose.runtime.mock.revalidate
import androidx.compose.runtime.mock.skip
import androidx.compose.runtime.mock.validate
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.CoroutineContext
import kotlin.math.min
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.test.IgnoreJsTarget
import kotlinx.test.IgnoreWasmTarget

@Composable fun Container(content: @Composable () -> Unit) = content()

@Stable
@OptIn(InternalComposeApi::class)
@Suppress("unused")
class CompositionTests {
    @Test
    fun simple() = compositionTest {
        compose { Text("Hello!") }

        validate { Text("Hello!") }
    }

    @Test
    fun simpleChanges() = compositionTest {
        var name by mutableStateOf("Bob")
        compose { Text("Hello $name") }

        validate { Text("Hello $name") }

        name = "Robert"

        expectChanges()

        validate { Text("Hello $name") }
    }

    @Test
    fun testComposeAModel() = compositionTest {
        val model = testModel()
        compose { SelectContact(model) }

        validate {
            Linear {
                Linear {
                    Text("Filter:")
                    Edit("")
                }
                Linear {
                    Text(value = "Contacts:")
                    Linear {
                        contact(bob)
                        contact(jon)
                        contact(steve)
                    }
                }
            }
        }
    }

    @Test
    fun testRecomposeWithoutChanges() = compositionTest {
        val model = testModel()
        compose { SelectContact(model) }

        expectNoChanges()

        validate { SelectContact(model) }
    }

    @Test
    fun testInsertAContact() = compositionTest {
        val model = testModel(mutableListOf(bob, jon))
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            SelectContact(model)
        }

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear {
                        contact(bob)
                        contact(jon)
                    }
                }
            }
        }

        model.add(steve, after = bob)
        scope?.invalidate()
        expectChanges()

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear {
                        contact(bob)
                        contact(steve)
                        contact(jon)
                    }
                }
            }
        }
    }

    @Test
    fun testMoveAContact() = compositionTest {
        val model = testModel(mutableListOf(bob, steve, jon))
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            SelectContact(model)
        }

        model.move(steve, after = jon)
        scope?.invalidate()
        expectChanges()

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear {
                        contact(bob)
                        contact(jon)
                        contact(steve)
                    }
                }
            }
        }
    }

    @Test
    fun testSeveralArbitraryMoves() = compositionTest {
        val random = Random(1337)
        val list = mutableStateListOf(*List(10) { it }.toTypedArray())

        var position by mutableIntStateOf(-1)

        compose {
            for (item in list) {
                key(item) { Text("Item $item") }
            }
        }

        fun validate() {
            validate {
                for (item in list) {
                    Text("Item $item")
                }
            }
        }

        validate()

        repeat(10) {
            position = it
            list.shuffle(random)
            expectChanges()
            validate()
            list.first()
        }

        position = -1
        list.shuffle(random)
        expectChanges()
        validate()
    }

    @Test
    fun testChangeTheFilter() = compositionTest {
        val model = testModel(mutableListOf(bob, steve, jon))
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            SelectContact(model)
        }

        model.filter = "Jon"
        scope?.invalidate()
        expectChanges()

        validate {
            Linear {
                skip()
                Linear {
                    skip()
                    Linear { contact(jon) }
                }
            }
        }
    }

    @Test
    fun testConditionalNode() = compositionTest {
        val condition = mutableStateOf(false)

        compose {
            InlineLinear {
                Text("A")
                Wrap {
                    if (condition.value) {
                        InlineLinear { Text("B") }
                    }
                }
            }
        }

        validate {
            Linear {
                Text("A")
                if (condition.value) {
                    Linear { Text("B") }
                }
            }
        }

        repeat(10) {
            condition.value = !condition.value
            advance()
            revalidate()
        }
    }

    @Test
    fun testComposeCompositionWithMultipleRoots() = compositionTest {
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois)

        compose { ReportsReport(reports) }

        validate { ReportsReport(reports) }
    }

    @Test
    fun testMoveCompositionWithMultipleRoots() = compositionTest {
        var reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois)
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            ReportsReport(reports)
        }

        reports = listOf(jim_reports_to_sally, clark_reports_to_lois, rob_reports_to_alice)
        scope?.invalidate()
        expectChanges()

        validate { ReportsReport(reports) }
    }

    @Test
    fun testReplace() = compositionTest {
        var includeA = true
        var scope: RecomposeScope? = null

        @Composable
        fun Composition() {
            scope = currentRecomposeScope
            Text("Before")
            if (includeA) {
                Linear { Text("A") }
            } else {
                Edit("B")
            }
            Text("After")
        }

        fun MockViewValidator.Composition() {
            Text("Before")
            if (includeA) {
                Linear { Text("A") }
            } else {
                Edit("B")
            }
            Text("After")
        }

        compose { Composition() }

        validate { this.Composition() }

        includeA = false
        scope?.invalidate()
        expectChanges()
        validate { this.Composition() }
        includeA = true
        scope?.invalidate()
        expectChanges()
        validate { this.Composition() }
        scope?.invalidate()
        expectNoChanges()
    }

    @Test
    fun testInsertWithMultipleRoots() = compositionTest {
        var chars = listOf('a', 'b', 'c')
        var scope: RecomposeScope? = null

        @Composable
        fun TextOf(c: Char) {
            Text(c.toString())
        }

        fun MockViewValidator.TextOf(c: Char) {
            Text(c.toString())
        }

        @Composable
        fun Chars(chars: Iterable<Char>) {
            Repeated(of = chars) { c -> TextOf(c) }
        }

        fun MockViewValidator.validateChars(chars: Iterable<Char>) {
            Repeated(of = chars) { c -> this.TextOf(c) }
        }

        compose {
            scope = currentRecomposeScope
            Chars(chars)
            Chars(chars)
            Chars(chars)
        }

        validate {
            validateChars(chars)
            validateChars(chars)
            validateChars(chars)
        }

        chars = listOf('a', 'b', 'x', 'c')
        scope?.invalidate()
        expectChanges()

        validate {
            validateChars(chars)
            validateChars(chars)
            validateChars(chars)
        }
    }

    @Test
    fun testSimpleSkipping() = compositionTest {
        val points = listOf(Point(1, 2), Point(2, 3))
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            Points(points)
        }

        validate { Points(points) }

        scope?.invalidate()
        expectNoChanges()
    }

    @Test
    fun testMovingMemoization() = compositionTest {
        var points = listOf(Point(1, 2), Point(2, 3), Point(4, 5), Point(6, 7))
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            Points(points)
        }

        validate { Points(points) }

        points = listOf(Point(1, 2), Point(4, 5), Point(2, 3), Point(6, 7))
        scope?.invalidate()
        expectChanges()

        validate { Points(points) }
    }

    @Test
    fun testComponent() = compositionTest {
        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                Text(report.from)
                Text("reports to")
                Text(report.to)
            } else {
                Text("no report to report")
            }
        }

        @Composable
        fun ReportsReport(reports: Iterable<Report>) {
            Linear { Repeated(of = reports) { report -> Reporter(report) } }
        }

        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois)
        compose { ReportsReport(reports) }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
            }
        }

        expectNoChanges()
    }

    @Test
    fun testComposeTwoAttributeComponent() = compositionTest {
        @Composable
        fun Two2(first: Int = 1, second: Int = 2) {
            Linear { Text("$first $second") }
        }

        fun MockViewValidator.two(first: Int, second: Int) {
            Linear { Text("$first $second") }
        }

        compose { Two2(41, 42) }

        validate { two(41, 42) }
    }

    @Test
    fun testComposeThreeAttributeComponent() = compositionTest {
        @Composable
        fun Three3(first: Int = 1, second: Int = 2, third: Int = 3) {
            Linear { Text("$first $second $third") }
        }

        fun MockViewValidator.Three(first: Int, second: Int, third: Int) {
            Linear { Text("$first $second $third") }
        }

        compose { Three3(41, 42, 43) }

        validate { Three(41, 42, 43) }
    }

    @Test
    fun testComposeFourOrMoreAttributeComponent() = compositionTest {
        @Composable
        fun Four4(first: Int = 1, second: Int = 2, third: Int = 3, fourth: Int = 4) {
            Linear { Text("$first $second $third $fourth") }
        }

        fun MockViewValidator.Four(first: Int, second: Int, third: Int, fourth: Int) {
            Linear { Text("$first $second $third $fourth") }
        }

        compose { Four4(41, 42, 43, 44) }

        validate { Four(41, 42, 43, 44) }
    }

    @Test
    fun testSkippingACall() = compositionTest {
        @Composable
        fun Show(value: Int) {
            Linear { Text("$value") }
            Linear { Text("value") }
        }

        fun MockViewValidator.Show(value: Int) {
            Linear { Text("$value") }
            Linear { Text("value") }
        }

        @Composable
        fun Test(showThree: Boolean) {
            Show(1)
            Show(2)
            if (showThree) {
                Show(3)
            }
        }

        var showThree = false

        var scope: RecomposeScope? = null

        @Composable
        fun Test() {
            scope = currentRecomposeScope
            Test(showThree)
        }

        fun MockViewValidator.Test(showThree: Boolean) {
            this.Show(1)
            this.Show(2)
            if (showThree) {
                this.Show(3)
            }
        }

        fun validate() {
            validate { this.Test(showThree) }
        }

        compose { Test() }

        validate()

        showThree = true
        scope?.invalidate()
        expectChanges()
        validate()
    }

    @Test
    fun testSkippingNestedLambda() = compositionTest {
        val data = mutableIntStateOf(0)

        itemRendererCalls = 0
        scrollingListCalls = 0
        compose { TestSkippingContent(data = data) }

        data.intValue++
        advance()

        data.intValue++
        advance()

        data.intValue++
        advance()

        assertTrue(itemRendererCalls < scrollingListCalls)
    }

    @Test
    fun testComponentWithVarConstructorParameter() = compositionTest {
        @Composable
        fun One(first: Int) {
            Text("$first")
        }

        fun MockViewValidator.One(first: Int) {
            Text("$first")
        }

        @Composable
        fun CallOne(value: Int) {
            One(first = value)
        }

        var value = 42
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            CallOne(value)
        }

        validate { this.One(42) }

        value = 43
        scope?.invalidate()
        expectChanges()

        validate { this.One(43) }
    }

    @Test
    fun testComponentWithValConstructorParameter() = compositionTest {
        @Composable
        fun One(first: Int) {
            Text("$first")
        }

        fun MockViewValidator.One(first: Int) {
            Text("$first")
        }

        @Composable
        fun CallOne(value: Int) {
            One(first = value)
        }

        var value = 42
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            CallOne(value)
        }

        validate { this.One(42) }

        value = 43
        scope?.invalidate()
        expectChanges()

        validate { this.One(43) }

        scope?.invalidate()
        expectNoChanges()
    }

    @Test
    fun testComposePartOfTree() = compositionTest {
        var loisScope: RecomposeScope? = null

        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                if (report.from == "Lois" || report.to == "Lois") loisScope = currentRecomposeScope
                Text(report.from)
                Text("reports to")
                Text(report.to)
            } else {
                Text("no report to report")
            }
        }

        @Composable
        fun ReportsReport(reports: Iterable<Report>) {
            Linear { Repeated(of = reports) { report -> Reporter(report) } }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois, r)
        compose { ReportsReport(reports) }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }

        expectNoChanges()

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Compose only the Lois report
        loisScope?.invalidate()

        expectChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }
    }

    @Test
    fun testRecomposeWithReplace() = compositionTest {
        var loisScope: RecomposeScope? = null
        var key = 0

        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                if (report.from == "Lois" || report.to == "Lois") loisScope = currentRecomposeScope
                key(key) {
                    Text(report.from)
                    Text("reports to")
                    Text(report.to)
                }
            } else {
                Text("no report to report")
            }
        }

        @Composable
        fun ReportsReport(reports: Iterable<Report>) {
            Linear { Repeated(of = reports) { report -> Reporter(report) } }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois, r)
        compose { ReportsReport(reports) }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }

        expectNoChanges()

        // Demote Perry
        r.from = "Perry"
        r.to = "Lois"

        // Cause a new group to be generated in the component
        key = 2

        // Compose only the Lois report
        loisScope?.invalidate()

        expectChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }
    }

    @Test
    fun testInvalidationAfterRemoval() = compositionTest {
        var loisScope: RecomposeScope? = null
        val key = 0

        @Composable
        fun Reporter(report: Report? = null) {
            if (report != null) {
                val scope = currentRecomposeScope
                if (report.from == "Lois" || report.to == "Lois") loisScope = scope
                key(key) {
                    Text(report.from)
                    Text("reports to")
                    Text(report.to)
                }
            } else {
                Text("no report to report")
            }
        }

        @Composable
        fun ReportsReport(reports: Iterable<Report>, include: (report: Report) -> Boolean) {
            Linear {
                Repeated(of = reports) { report ->
                    if (include(report)) {
                        Reporter(report)
                    }
                }
            }
        }

        val r = Report("Lois", "Perry")
        val reports = listOf(jim_reports_to_sally, rob_reports_to_alice, clark_reports_to_lois, r)
        val all: (report: Report) -> Boolean = { true }
        val notLois: (report: Report) -> Boolean = { it.from != "Lois" && it.to != "Lois" }

        var filter = all
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            ReportsReport(reports, filter)
        }

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
                ReportsTo(clark_reports_to_lois)
                ReportsTo(r)
            }
        }

        filter = notLois
        scope?.invalidate()
        expectChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
            }
        }

        // Invalidate Lois which is now removed.
        loisScope?.invalidate()
        expectNoChanges()

        validate {
            Linear {
                ReportsTo(jim_reports_to_sally)
                ReportsTo(rob_reports_to_alice)
            }
        }
    }

    // remember()

    @Test
    fun testSimpleRemember() = compositionTest {
        var count = 0
        var scope: RecomposeScope? = null

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(value: Int) {
            scope = currentRecomposeScope
            val w = remember { Wrapper(value) }
            Text("value = ${w.value}")
        }

        fun MockViewValidator.Test(value: Int) {
            Text("value = $value")
        }

        compose { Test(1) }

        validate { this.Test(1) }

        assertEquals(1, count)

        scope?.invalidate()
        expectNoChanges()

        // Expect the previous instance to be remembered
        assertEquals(1, count)
    }

    @Test
    fun testRememberOneParameter() = compositionTest {
        var count = 0

        class Wrapper(val value: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(value: Int) {
            val w = remember(value) { Wrapper(value) }
            Text("value = ${w.value}")
        }

        fun MockViewValidator.Test(value: Int) {
            Text("value = $value")
        }

        var value = 1
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            Test(value)
        }

        validate { this.Test(1) }

        value = 2
        scope?.invalidate()
        expectChanges()

        validate { this.Test(2) }

        scope?.invalidate()
        expectNoChanges()

        validate { this.Test(2) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberTwoParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int) {
            val w = remember(a, b) { Wrapper(a, b) }
            Text("a = ${w.a} b = ${w.b}")
        }

        fun MockViewValidator.Test(a: Int, b: Int) {
            Text("a = $a b = $b")
        }

        var p1 = 1
        var p2 = 2
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            Test(p1, p2)
        }

        validate { this.Test(1, 2) }

        p1 = 2
        p2 = 3
        scope?.invalidate()
        expectChanges()

        validate { this.Test(2, 3) }

        scope?.invalidate()
        expectNoChanges()

        validate { this.Test(2, 3) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberThreeParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int, c: Int) {
            val w = remember(a, b, c) { Wrapper(a, b, c) }
            Text("a = ${w.a} b = ${w.b} c = ${w.c}")
        }

        fun MockViewValidator.Test(a: Int, b: Int, c: Int) {
            Text("a = $a b = $b c = $c")
        }

        var p3 = 3
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            Test(1, 2, p3)
        }

        validate { this.Test(1, 2, 3) }

        p3 = 4
        scope?.invalidate()
        expectChanges()

        validate { this.Test(1, 2, 4) }

        scope?.invalidate()
        expectNoChanges()

        validate { this.Test(1, 2, 4) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberFourParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int, val d: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int, c: Int, d: Int) {
            val w = remember(a, b, c, d) { Wrapper(a, b, c, d) }
            Text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d}")
        }

        fun MockViewValidator.Test(a: Int, b: Int, c: Int, d: Int) {
            Text("a = $a b = $b c = $c d = $d")
        }

        var p3 = 3
        var p4 = 4
        var scope: RecomposeScope? = null

        compose {
            scope = currentRecomposeScope
            Test(1, 2, p3, p4)
        }

        validate { this.Test(1, 2, 3, 4) }

        p3 = 4
        p4 = 5
        scope?.invalidate()
        expectChanges()

        validate { this.Test(1, 2, 4, 5) }

        scope?.invalidate()
        expectNoChanges()

        validate { this.Test(1, 2, 4, 5) }

        assertEquals(2, count)
    }

    @Test
    fun testRememberFiveParameters() = compositionTest {
        var count = 0

        class Wrapper(val a: Int, val b: Int, val c: Int, val d: Int, val e: Int) {
            init {
                count++
            }
        }

        @Composable
        fun Test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            val w = remember(a, b, c, d, e) { Wrapper(a, b, c, d, e) }
            Text("a = ${w.a} b = ${w.b} c = ${w.c} d = ${w.d} e = ${w.e}")
        }

        fun MockViewValidator.Test(a: Int, b: Int, c: Int, d: Int, e: Int) {
            Text("a = $a b = $b c = $c d = $d e = $e")
        }

        var lastParameter = 5
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            Test(1, 2, 3, 4, lastParameter)
        }

        validate { this.Test(1, 2, 3, 4, 5) }

        lastParameter = 6
        scope?.invalidate()

        expectChanges()

        validate { this.Test(1, 2, 3, 4, 6) }

        expectNoChanges()

        validate { this.Test(1, 2, 3, 4, 6) }

        assertEquals(2, count)
    }

    @Test
    fun testInsertGroupInContainer() = compositionTest {
        val values = mutableStateListOf(0)

        @Composable
        fun Composition() {
            Linear {
                for (value in values) {
                    Text("$value")
                }
            }
        }

        fun MockViewValidator.Composition() {
            Linear { for (value in values) Text("$value") }
        }

        compose { Composition() }

        validate { this.Composition() }

        for (i in 1..10) {
            values.add(i)
            expectChanges()
            validate { this.Composition() }
        }
    }

    // b/148273328
    @Test
    fun testInsertInGroups() = compositionTest {
        var threeVisible by mutableStateOf(false)

        @Composable
        fun Composition() {
            Linear {
                Text("one")
                Text("two")
                if (threeVisible) {
                    Text("three")
                    Text("four")
                }
                Linear { Text("five") }
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Text("one")
                Text("two")
                if (threeVisible) {
                    Text("three")
                    Text("four")
                }
                Linear { Text("five") }
            }
        }

        compose { Composition() }
        validate { this.Composition() }

        threeVisible = true
        expectChanges()

        validate { this.Composition() }
    }

    @Test
    fun testStartJoin() = compositionTest {
        var text by mutableStateOf("Starting")

        @Composable
        fun Composition() {
            Linear { Text(text) }
        }

        fun MockViewValidator.Composition() {
            Linear { Text(text) }
        }

        compose { Composition() }

        validate { this.Composition() }

        text = "Ending"

        expectChanges()

        validate { this.Composition() }
    }

    @Test
    fun testInvalidateJoin_End() = compositionTest {
        var text by mutableStateOf("Starting")
        var includeNested by mutableStateOf(true)

        @Composable
        fun Composition() {
            Linear {
                Text(text)
                if (includeNested) {
                    Text("Nested in $text")
                }
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                Text(text)
                if (includeNested) {
                    Text("Nested in $text")
                }
            }
        }

        compose { Composition() }

        validate { this.Composition() }

        text = "Ending"
        includeNested = false

        expectChanges()

        validate { this.Composition() }

        expectNoChanges()

        validate { this.Composition() }
    }

    @Test
    fun testInvalidateJoin_Start() = compositionTest {
        var text by mutableStateOf("Starting")
        var includeNested by mutableStateOf(true)

        @Composable
        fun Composition() {
            Linear {
                if (includeNested) {
                    Text("Nested in $text")
                }
                Text(text)
            }
        }

        fun MockViewValidator.Composition() {
            Linear {
                if (includeNested) {
                    Text("Nested in $text")
                }
                Text(text)
            }
        }

        compose { Composition() }

        validate { this.Composition() }

        text = "Ending"
        includeNested = false

        expectChanges()

        validate { this.Composition() }

        expectNoChanges()

        validate { this.Composition() }
    }

    // b/132638679
    @Test
    fun testJoinInvalidate() = compositionTest {
        var texts = 5
        var outerScope: RecomposeScope? = null
        var innerScope: RecomposeScope? = null
        var forceInvalidate = false

        @Composable
        fun Composition() {
            Linear {
                outerScope = currentRecomposeScope
                for (i in 1..texts) {
                    Text("Some text")
                }

                Container {
                    Text("Some text")

                    // Force the invalidation to survive the compose
                    val innerInvalidate = currentRecomposeScope
                    innerScope = innerInvalidate
                    if (forceInvalidate) {
                        innerInvalidate.invalidate()
                        forceInvalidate = false
                    }
                }
            }
        }

        compose { Composition() }

        texts = 4
        outerScope?.invalidate()
        innerScope?.invalidate()
        forceInvalidate = true
        expectChanges()

        texts = 3
        outerScope?.invalidate()
        forceInvalidate = true
        expectChanges()

        expectNoChanges()
    }

    @Test
    fun testRememberObserver_Remember_Simple() = compositionTest {
        val rememberedObject =
            object : RememberObserver {
                var count = 0

                override fun onRemembered() {
                    count++
                }

                override fun onForgotten() {
                    count--
                }

                override fun onAbandoned() {
                    assertEquals(0, count, "onRemember called on an abandon object")
                }
            }

        var scope: RecomposeScope? = null

        @Composable
        fun Composition() {
            Linear {
                scope = currentRecomposeScope
                remember { rememberedObject }
                Text("Some text")
            }
        }

        fun MockViewValidator.Composition() {
            Linear { Text("Some text") }
        }

        compose { Composition() }
        validate { this.Composition() }

        assertEquals(1, rememberedObject.count, "object should have been notified of a remember")

        scope?.invalidate()
        expectNoChanges()
        validate { this.Composition() }

        assertEquals(1, rememberedObject.count, "Object should have only been notified once")
    }

    @Test
    fun testRememberObserver_Remember_SingleNotification() = compositionTest {
        val rememberedObject =
            object : RememberObserver {
                var count = 0

                override fun onRemembered() {
                    count++
                }

                override fun onForgotten() {
                    count--
                }

                override fun onAbandoned() {
                    assertEquals(0, count, "onRemember called on an abandon object")
                }
            }

        var value by mutableIntStateOf(0)
        @Composable
        fun Composition() {
            Linear {
                val l = remember { rememberedObject }
                assertEquals(rememberedObject, l, "remembered object should be returned")
                Text("Some text $value")
            }
            Linear {
                val l = remember { rememberedObject }
                assertEquals(rememberedObject, l, "remembered object should be returned")
                Text("Some other text $value")
            }
        }

        fun MockViewValidator.Composition() {
            Linear { Text("Some text $value") }
            Linear { Text("Some other text $value") }
        }

        compose { Composition() }
        validate { this.Composition() }

        assertEquals(2, rememberedObject.count, "object should have been notified remembered twice")

        value++
        expectChanges()
        validate { this.Composition() }

        assertEquals(2, rememberedObject.count, "Object should have only been notified twice")
    }

    @Test
    fun testRememberObserver_Forget_Simple() = compositionTest {
        val rememberObject =
            object : RememberObserver {
                var count = 0

                override fun onRemembered() {
                    count++
                }

                override fun onForgotten() {
                    count--
                }

                override fun onAbandoned() {
                    assertEquals(0, count, "onRemember called on an abandon object")
                }
            }

        @Composable
        fun Composition(includeRememberObject: Boolean) {
            Linear {
                if (includeRememberObject) {
                    Linear {
                        val l = remember { rememberObject }
                        assertEquals(rememberObject, l, "Remember object should be returned")
                        Text("Some text")
                    }
                }
            }
        }

        fun MockViewValidator.Composition(includeRememberObject: Boolean) {
            Linear {
                if (includeRememberObject) {
                    Linear { Text("Some text") }
                }
            }
        }

        var scope: RecomposeScope? = null
        var value = true
        compose {
            scope = currentRecomposeScope
            Composition(value)
        }
        validate { this.Composition(true) }

        assertEquals(1, rememberObject.count, "object should have been notified of a remember")

        scope?.invalidate()
        expectNoChanges()
        validate { this.Composition(true) }

        assertEquals(1, rememberObject.count, "Object should have only been notified once")

        value = false
        scope?.invalidate()
        expectChanges()
        validate { this.Composition(false) }

        assertEquals(0, rememberObject.count, "Object should have been notified of a forget")
    }

    @Test
    fun testRemember_Forget_ForgetOnRemember() = compositionTest {
        var expectedRemember = true
        var expectedForget = true
        val rememberObject =
            object : RememberObserver {
                var count = 0

                override fun onRemembered() {
                    val remembered = count++ == 0
                    assertTrue(remembered && expectedRemember, "No remember expected")
                }

                override fun onForgotten() {
                    val forgotten = --count == 0
                    assertTrue(forgotten && expectedForget, "No forget expected")
                }

                override fun onAbandoned() {
                    assertEquals(0, count, "onAbandon called after onRemember")
                }
            }

        @Composable
        fun Composition(a: Boolean, b: Boolean, c: Boolean) {
            Linear {
                if (a) {
                    key(1) {
                        Linear {
                            val l = remember { rememberObject }
                            assertEquals(rememberObject, l, "Lifecycle object should be returned")
                            Text("a")
                        }
                    }
                }
                if (b) {
                    key(2) {
                        Linear {
                            val l = remember { rememberObject }
                            assertEquals(rememberObject, l, "Lifecycle object should be returned")
                            Text("b")
                        }
                    }
                }
                if (c) {
                    key(3) {
                        Linear {
                            val l = remember { rememberObject }
                            assertEquals(rememberObject, l, "Lifecycle object should be returned")
                            Text("c")
                        }
                    }
                }
            }
        }

        fun MockViewValidator.Composition(a: Boolean, b: Boolean, c: Boolean) {
            Linear {
                if (a) {
                    Linear { Text("a") }
                }
                if (b) {
                    Linear { Text("b") }
                }
                if (c) {
                    Linear { Text("c") }
                }
            }
        }

        expectedRemember = true
        expectedForget = false

        var a = true
        var b = false
        var c = false
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            Composition(a = a, b = b, c = c)
        }
        validate { this.Composition(a = true, b = false, c = false) }

        assertEquals(1, rememberObject.count, "object should have been notified of an enter")

        expectedRemember = false
        expectedForget = false
        scope?.invalidate()
        expectNoChanges()
        validate { this.Composition(a = true, b = false, c = false) }
        assertEquals(1, rememberObject.count, "Object should have only been notified once")

        expectedRemember = true
        expectedForget = true
        a = false
        b = true
        c = false
        scope?.invalidate()
        expectChanges()
        validate { this.Composition(a = false, b = true, c = false) }
        assertEquals(1, rememberObject.count, "No enter or leaves")

        expectedRemember = true
        expectedForget = true
        a = false
        b = false
        c = true
        scope?.invalidate()
        expectChanges()
        validate { this.Composition(a = false, b = false, c = true) }
        assertEquals(1, rememberObject.count, "No enter or leaves")

        expectedRemember = true
        expectedForget = true
        a = true
        b = false
        c = false
        scope?.invalidate()
        expectChanges()
        validate { this.Composition(a = true, b = false, c = false) }
        assertEquals(1, rememberObject.count, "No enter or leaves")

        expectedRemember = false
        expectedForget = true
        a = false
        b = false
        c = false
        scope?.invalidate()
        expectChanges()
        validate { this.Composition(a = false, b = false, c = false) }
        assertEquals(0, rememberObject.count, "A leave")
    }

    @Test
    fun testRemember_Forget_ForgetOnReplace() = compositionTest {
        val rememberObject1 =
            object : RememberObserver {
                var count = 0

                override fun onRemembered() {
                    count++
                }

                override fun onForgotten() {
                    count--
                }

                override fun onAbandoned() {
                    assertEquals(0, count, "onAbandon called after onRemember")
                }
            }

        val rememberObject2 =
            object : RememberObserver {
                var count = 0

                override fun onRemembered() {
                    count++
                }

                override fun onForgotten() {
                    count--
                }

                override fun onAbandoned() {
                    assertEquals(0, count, "onAbandon called after onRemember")
                }
            }

        var rememberObject: Any = rememberObject1
        var scope: RecomposeScope? = null

        @Composable
        fun Composition(obj: Any) {
            Linear {
                key(1) {
                    Linear {
                        remember(obj) { obj }
                        Text("Some value")
                    }
                }
            }
        }

        fun MockViewValidator.Composition() {
            Linear { Linear { Text("Some value") } }
        }

        compose {
            scope = currentRecomposeScope
            Composition(obj = rememberObject)
        }
        validate { this.Composition() }
        assertEquals(1, rememberObject1.count, "first object should enter")
        assertEquals(0, rememberObject2.count, "second object should not have entered")

        rememberObject = rememberObject2
        scope?.invalidate()
        expectChanges()
        validate { Composition() }
        assertEquals(0, rememberObject1.count, "first object should have left")
        assertEquals(1, rememberObject2.count, "second object should have entered")

        rememberObject = object {}
        scope?.invalidate()
        expectChanges()
        validate { Composition() }
        assertEquals(0, rememberObject1.count, "first object should have left")
        assertEquals(0, rememberObject2.count, "second object should have left")
    }

    @Test
    fun testRemember_forget_forgetAfterPredecessorReplaced() = compositionTest {
        val rememberObject =
            object : RememberObserver {
                var count = 0

                override fun onRemembered() {
                    count++
                }

                override fun onForgotten() {
                    count--
                }

                override fun onAbandoned() {
                    assertEquals(0, count, "onAbandon called after onRemember")
                }

                override fun toString() = "rememberObject"
            }

        var key = 0
        var include = true
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            if (include) {
                Linear {
                    key(key) { Linear { Text("Some value") } }
                    use(remember { rememberObject })
                }
            }
        }

        fun MockViewValidator.Composition() {
            if (include) Linear { Linear { Text("Some value") } }
        }

        validate { this.Composition() }
        assertEquals(1, rememberObject.count, "object should enter")

        key++
        scope?.invalidate()
        expectChanges()
        validate { Composition() }
        assertEquals(1, rememberObject.count, "object should still be remembered")

        include = false
        scope?.invalidate()
        expectChanges()
        validate { Composition() }
        assertEquals(0, rememberObject.count, "object should have left")
    }

    @Test
    fun testRemember_RememberForgetNestedOrder() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String ->
            object : RememberObserver, Counted, Ordered, Named {
                    override var name = name
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }
                }
                .also { objects.add(it) }
        }

        @Suppress("UNUSED_PARAMETER") fun used(v: Any) {}

        @Composable
        fun Tree() {
            used(remember { newRememberObject("L0B") })
            Linear {
                used(remember { newRememberObject("L1B") })
                Linear {
                    used(remember { newRememberObject("L2B") })
                    Linear {
                        used(remember { newRememberObject("L3B") })
                        Linear { used(remember { newRememberObject("Leaf") }) }
                        used(remember { newRememberObject("L3A") })
                    }
                    used(remember { newRememberObject("L2A") })
                }
                used(remember { newRememberObject("L1A") })
            }
            used(remember { newRememberObject("L0A") })
        }

        var includeTree by mutableStateOf(true)
        compose {
            if (includeTree) {
                Tree()
            }
        }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered",
        )

        includeTree = false
        expectChanges()

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 0 }.all { it },
            "All object should have left",
        )

        assertArrayEquals(
            "Expected enter order",
            arrayOf("L0B", "L1B", "L2B", "L3B", "Leaf", "L3A", "L2A", "L1A", "L0A"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.rememberOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )

        assertArrayEquals(
            "Expected exit order",
            arrayOf("L0A", "L1A", "L2A", "L3A", "Leaf", "L3B", "L2B", "L1B", "L0B"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.forgetOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )
    }

    @Test
    fun testRemember_RememberForgetNestedOrder_Inline() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String ->
            object : RememberObserver, Counted, Ordered, Named {
                    override var name = name
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }
                }
                .also { objects.add(it) }
        }

        @Suppress("UNUSED_PARAMETER") fun used(v: Any) {}

        @Composable
        fun Tree() {
            used(remember { newRememberObject("L0B") })
            InlineLinear {
                used(remember { newRememberObject("L1B") })
                InlineLinear {
                    used(remember { newRememberObject("L2B") })
                    InlineLinear {
                        used(remember { newRememberObject("L3B") })
                        InlineLinear { used(remember { newRememberObject("Leaf") }) }
                        used(remember { newRememberObject("L3A") })
                    }
                    used(remember { newRememberObject("L2A") })
                }
                used(remember { newRememberObject("L1A") })
            }
            used(remember { newRememberObject("L0A") })
        }

        var includeTree by mutableStateOf(true)
        compose { if (includeTree) Tree() }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered",
        )

        includeTree = false
        expectChanges()

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 0 }.all { it },
            "All object should have left",
        )

        assertArrayEquals(
            "Expected enter order",
            arrayOf("L0B", "L1B", "L2B", "L3B", "Leaf", "L3A", "L2A", "L1A", "L0A"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.rememberOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )

        assertArrayEquals(
            "Expected exit order",
            arrayOf("L0A", "L1A", "L2A", "L3A", "Leaf", "L3B", "L2B", "L1B", "L0B"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.forgetOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )
    }

    @Test
    @Ignore // b/346821372
    fun testRemember_RememberForgetNestedOrder_Incremental() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String ->
            object : RememberObserver, Counted, Ordered, Named {
                    override var name = name
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }
                }
                .also { objects.add(it) }
        }

        @Suppress("UNUSED_PARAMETER") fun used(v: Any) {}

        var level by mutableIntStateOf(0)

        @Composable
        fun Tree() {
            used(remember { newRememberObject("L0B") })
            Linear {
                if (level >= 1) {
                    used(remember { newRememberObject("L1B") })
                    if (level >= 2) {
                        Linear {
                            used(remember { newRememberObject("L2B") })
                            if (level >= 3) {
                                Linear {
                                    used(remember { newRememberObject("L3B") })
                                    if (level >= 4) {
                                        Linear { used(remember { newRememberObject("Leaf") }) }
                                    }
                                    used(remember { newRememberObject("L3A") })
                                }
                            }
                            used(remember { newRememberObject("L2A") })
                        }
                    }
                    used(remember { newRememberObject("L1A") })
                }
            }
            used(remember { newRememberObject("L0A") })
        }

        var includeTree by mutableStateOf(true)
        compose { if (includeTree) Tree() }

        while (level < 4) {
            level++
            expectChanges()
        }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered",
        )

        includeTree = false
        expectChanges()

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 0 }.all { it },
            "All object should have left",
        )

        assertArrayEquals(
            "Expected enter order",
            arrayOf("L0B", "L0A", "L1B", "L1A", "L2B", "L2A", "L3B", "L3A", "Leaf"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.rememberOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )

        val forgetOrder = objects.mapNotNull { it as? Ordered }.sortedBy { it.forgetOrder }

        // Even though the enter order is incremental, the order of onForgotten should
        // be called in the same order as if it came in all at once.
        assertArrayEquals(
            "Expected exit order",
            arrayOf("L0A", "L1A", "L2A", "L3A", "Leaf", "L3B", "L2B", "L1B", "L0A"),
            forgetOrder.map { (it as Named).name }.toTypedArray(),
        )
    }

    @Test
    fun testRemember_RememberForgetOrder() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String ->
            object : RememberObserver, Counted, Ordered, Named {
                    override var name = name
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }
                }
                .also { objects.add(it) }
        }

        @Composable
        fun RememberUser(name: String) {
            Linear {
                remember(name) { newRememberObject(name) }
                Text(value = name)
            }
        }

        /*
        A
        |- B
        |  |- C
        |  |- D
        |- E
        |- F
        |  |- G
        |  |- H
        |     |-I
        |- J

        Should enter as: A, B, C, D, E, F, G, H, I, J
        Should leave as: J, I, H, G, F, E, D, C, B, A
        */

        @Composable
        fun Tree() {
            Linear {
                RememberUser("A")
                Linear {
                    RememberUser("B")
                    Linear {
                        RememberUser("C")
                        RememberUser("D")
                    }
                    RememberUser("E")
                    RememberUser("F")
                    Linear {
                        RememberUser("G")
                        RememberUser("H")
                        Linear { RememberUser("I") }
                    }
                    RememberUser("J")
                }
            }
        }

        @Composable
        fun Composition(includeTree: Boolean) {
            Linear { if (includeTree) Tree() }
        }

        var value by mutableStateOf(true)

        compose { Composition(value) }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered",
        )

        value = false
        expectChanges()

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 0 }.all { it },
            "All object should have left",
        )

        assertArrayEquals(
            "Expected enter order",
            arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.rememberOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )

        assertArrayEquals(
            "Expected leave order",
            arrayOf("J", "I", "H", "G", "F", "E", "D", "C", "B", "A"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.forgetOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )
    }

    @Test
    fun testRemember_RememberForgetOrder_NonRestartable() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String ->
            object : RememberObserver, Counted, Ordered, Named {
                    override var name = name
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }

                    override fun toString(): String =
                        "$name: count($count), remember($rememberOrder), forgotten($forgetOrder)"
                }
                .also { objects.add(it) }
        }

        @Composable
        @NonRestartableComposable
        fun RememberUser(name: String) {
            remember(name) { newRememberObject(name) }
        }

        /*
        A
        |- B
        |  |- C
        |  |- D
        |- E
        |- F
        |  |- G
        |  |- H
        |     |-I
        |  |- J
        |- K

        Should enter as: A, B, C, D, E, F, G, H, I, J, K
        Should leave as: K, J, I, H, G, F, E, D, C, B, A
        */

        @Composable
        fun Tree() {
            Linear {
                RememberUser("A")
                Linear {
                    RememberUser("B")
                    Linear {
                        RememberUser("C")
                        RememberUser("D")
                    }
                    RememberUser("E")
                    RememberUser("F")
                    Linear {
                        RememberUser("G")
                        RememberUser("H")
                        Linear { RememberUser("I") }
                        RememberUser("J")
                    }
                    RememberUser("K")
                }
            }
        }

        @Composable
        fun Composition(includeTree: Boolean) {
            Linear { if (includeTree) Tree() }
        }

        var value by mutableStateOf(true)

        compose { Composition(value) }

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 1 }.all { it },
            "All object should have entered",
        )

        value = false
        expectChanges()

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 0 }.all { it },
            "All object should have left",
        )

        val namesInRememberOrder =
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.rememberOrder }
                .map { (it as Named).name }
                .toTypedArray()

        val namesInForgetOrder =
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.forgetOrder }
                .map { (it as Named).name }
                .toTypedArray()

        assertArrayEquals(
            "Expected enter order",
            arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K"),
            namesInRememberOrder,
        )

        assertArrayEquals(
            "Expected leave order",
            arrayOf("K", "J", "I", "H", "G", "F", "E", "D", "C", "B", "A"),
            namesInForgetOrder,
        )
    }

    @Test
    fun testRememberObserver_RememberForgetOrder_ReplaceOnRecompose() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String, data: Int ->
            object : RememberObserver, Counted, Ordered, Named, WithData {
                    override var name = name
                    override var data = data
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }

                    override fun toString(): String =
                        "$name: count($count), remember($rememberOrder), forgotten($forgetOrder)"
                }
                .also { objects.add(it) }
        }

        var changing by mutableIntStateOf(0)
        val fixed = 10

        @Composable
        @NonRestartableComposable
        fun RememberUser(name: String, data: Int) {
            remember(name, data) { newRememberObject(name, data) }
        }

        @Composable
        fun Tree() {
            Linear {
                RememberUser("A", changing)
                RememberUser("B", fixed)
            }
        }

        @Composable
        fun Composition(includeTree: Boolean) {
            Linear { if (includeTree) Tree() }
        }

        var includeTree by mutableStateOf(true)

        compose { Composition(includeTree) }

        changing++
        advance()

        includeTree = false
        advance()

        val nameAndDataInForgetOrder =
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.forgetOrder }
                .joinToString {
                    val named = it as Named
                    val withData = it as WithData
                    "${named.name}:${withData.data}"
                }

        assertEquals("A:0, B:10, A:1", nameAndDataInForgetOrder)
    }

    @Test
    fun testRememberObserver_RememberForgetOrder_RelativeOrder() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String, data: Int ->
            object : RememberObserver, Counted, Ordered, Named, WithData {
                    override var name = name
                    override var data = data
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }

                    override fun toString(): String =
                        "$name: count($count), remember($rememberOrder), forgotten($forgetOrder)"
                }
                .also { objects.add(it) }
        }

        var changing by mutableIntStateOf(0)
        var includeChildren by mutableStateOf(true)
        val fixed = 10

        @Composable
        @NonRestartableComposable
        fun RememberUser(name: String, data: Int) {
            remember(name, data) { newRememberObject(name, data) }
        }

        @Composable
        @NonRestartableComposable
        fun Children() {
            RememberUser("A2", fixed)
            RememberUser("B2", fixed)
        }

        @Composable @NonRestartableComposable fun NoChildren() {}

        @Composable
        fun Composition() {
            InlineLinear {
                RememberUser("A1", changing)
                if (includeChildren) {
                    Children()
                } else {
                    NoChildren()
                }
                RememberUser("B1", changing)
            }
        }

        compose { Composition() }

        changing++
        includeChildren = false
        advance()

        val nameAndDataInForgetOrder =
            objects
                .mapNotNull { item -> (item as? Ordered)?.takeIf { it.forgetOrder >= 0 } }
                .sortedBy { it.forgetOrder }
                .joinToString {
                    val named = it as Named
                    val withData = it as WithData
                    "${named.name}:${withData.data}"
                }

        assertEquals("B1:0, B2:10, A2:10, A1:0", nameAndDataInForgetOrder)
    }

    @Test
    fun testRemember_RememberForgetOrder_keyChange() = compositionTest {
        var order = 0
        val objects = mutableListOf<Any>()
        val newRememberObject = { name: String ->
            object : RememberObserver, Counted, Ordered, Named {
                    override var name = name
                    override var count = 0
                    override var rememberOrder = -1
                    override var forgetOrder = -1

                    override fun onRemembered() {
                        assertEquals(-1, rememberOrder, "Only one call to onRemembered expected")
                        rememberOrder = order++
                        count++
                    }

                    override fun onForgotten() {
                        assertEquals(-1, forgetOrder, "Only one call to onForgotten expected")
                        forgetOrder = order++
                        count--
                    }

                    override fun onAbandoned() {
                        assertEquals(0, count, "onAbandoned called after onRemembered")
                    }
                }
                .also { objects.add(it) }
        }

        @Suppress("UNUSED_PARAMETER") fun used(v: Any) {}
        var a by mutableIntStateOf(0)
        var b by mutableIntStateOf(0)

        @Composable
        fun Test() {
            use(remember(a) { newRememberObject("A$a") })
            use(remember(b) { newRememberObject("B$b") })
        }

        var include by mutableStateOf(true)
        compose {
            if (include) {
                Test()
            }
        }

        a++
        advance()
        b++
        advance()
        a++
        advance()
        include = false
        advance()

        assertTrue(
            objects.mapNotNull { it as? Counted }.map { it.count == 0 }.all { it },
            "All object should have left",
        )

        assertArrayEquals(
            "Expected enter order",
            arrayOf("A0", "B0", "A1", "B1", "A2"),
            objects
                .mapNotNull { it as? Ordered }
                .sortedBy { it.rememberOrder }
                .map { (it as Named).name }
                .toTypedArray(),
        )

        val forgetOrder = objects.mapNotNull { it as? Ordered }.sortedBy { it.forgetOrder }

        // Even though the enter order is incremental, the order of onForgotten should
        // be called in the same order as if it came in all at once.
        assertArrayEquals(
            "Expected exit order",
            arrayOf("A0", "B0", "A1", "B1", "A2"),
            forgetOrder.map { (it as Named).name }.toTypedArray(),
        )
    }

    @Test
    fun testRememberObserver_Abandon_Simple() = compositionTest {
        val abandonedObjects = mutableListOf<RememberObserver>()
        val observed =
            object : RememberObserver {
                override fun onAbandoned() {
                    abandonedObjects.add(this)
                }

                override fun onForgotten() {
                    error("Unexpected call to onForgotten")
                }

                override fun onRemembered() {
                    error("Unexpected call to onRemembered")
                }
            }

        assertFailsWith(IllegalStateException::class, message = "Throw") {
            compose {
                @Suppress("UNUSED_EXPRESSION") remember { observed }
                error("Throw")
            }
        }

        assertArrayEquals(listOf(observed), abandonedObjects)
    }

    @Test
    // The test for web is properly implemented in CompositionTests.web.kt
    @IgnoreJsTarget
    @IgnoreWasmTarget
    fun testRememberObserver_Abandon_Recompose() {
        val abandonedObjects = mutableListOf<RememberObserver>()
        val observed =
            object : RememberObserver {
                override fun onAbandoned() {
                    abandonedObjects.add(this)
                }

                override fun onForgotten() {
                    error("Unexpected call to onForgotten")
                }

                override fun onRemembered() {
                    error("Unexpected call to onRemembered")
                }
            }
        assertFailsWith(IllegalStateException::class, message = "Throw") {
            compositionTest {
                val rememberObject = mutableStateOf(false)

                compose {
                    if (rememberObject.value) {
                        @Suppress("UNUSED_EXPRESSION") remember { observed }
                        error("Throw")
                    }
                }

                assertTrue(abandonedObjects.isEmpty())

                rememberObject.value = true

                advance(ignorePendingWork = true)
            }
        }

        assertArrayEquals(listOf(observed), abandonedObjects)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun testRememberedObserver_Controlled_Dispose() = runTest {
        val recomposer = Recomposer(coroutineContext)
        val root = View()
        val controlled = ControlledComposition(ViewApplier(root), recomposer)

        val abandonedObjects = mutableListOf<RememberObserver>()
        val observed =
            object : RememberObserver {
                override fun onAbandoned() {
                    abandonedObjects.add(this)
                }

                override fun onForgotten() {
                    error("Unexpected call to onForgotten")
                }

                override fun onRemembered() {
                    error("Unexpected call to onRemembered")
                }
            }

        controlled.composeContent {
            @Suppress("UNUSED_EXPRESSION") remember<RememberObserver> { observed }
        }

        assertTrue(abandonedObjects.isEmpty())

        controlled.dispose()

        assertArrayEquals(listOf(observed), abandonedObjects)
        recomposer.close()
    }

    @Test
    fun testRemember_OrderingWithAlternatingEffects() = compositionTest {
        val order = mutableListOf<String>()
        fun RememberObject(name: String) =
            object : RememberObserver {
                override fun onRemembered() {
                    order += "R[$name]"
                }

                override fun onForgotten() {
                    order += "F[$name]"
                }

                override fun onAbandoned() {
                    order += "A[$name]"
                }

                override fun toString() = name
            }

        var showContent by mutableStateOf(true)
        var runEffects by mutableStateOf(false)
        compose {
            if (showContent) {
                remember { RememberObject("1") }
                if (runEffects) SideEffect {}
                remember { RememberObject("2") }
                if (runEffects) SideEffect {}
                remember { RememberObject("3") }
                if (runEffects) SideEffect {}
                remember { RememberObject("4") }
                if (runEffects) SideEffect {}
            }
        }

        showContent = false
        advance()
        assertEquals("R[1], R[2], R[3], R[4], F[4], F[3], F[2], F[1]", order.joinToString())
    }

    @Test
    fun testCompoundKeyHashCodeStaysTheSameAfterRecompositions() = compositionTest {
        val outerKeys = mutableListOf<CompositeKeyHashCode>()
        val innerKeys = mutableListOf<CompositeKeyHashCode>()
        var previousOuterKeysSize = 0
        var previousInnerKeysSize = 0
        var outerScope: RecomposeScope? = null
        var innerScope: RecomposeScope? = null

        @Composable
        fun Test() {
            outerScope = currentRecomposeScope
            outerKeys.add(currentComposer.compositeKeyHashCode)
            Container {
                Linear {
                    innerScope = currentRecomposeScope
                    innerKeys.add(currentComposer.compositeKeyHashCode)
                }
            }
            // asserts that the key is correctly rolled back after start and end of Observe
            assertEquals(outerKeys.last(), currentComposer.compositeKeyHashCode)
        }

        compose { Test() }

        assertNotEquals(previousOuterKeysSize, outerKeys.size)
        assertNotEquals(previousInnerKeysSize, innerKeys.size)

        previousOuterKeysSize = outerKeys.size
        outerScope?.invalidate()
        expectNoChanges()
        assertNotEquals(previousOuterKeysSize, outerKeys.size)

        previousInnerKeysSize = innerKeys.size
        innerScope?.invalidate()
        expectNoChanges()
        assertNotEquals(previousInnerKeysSize, innerKeys.size)

        assertNotEquals(innerKeys[0], outerKeys[0])
        innerKeys.forEach { assertEquals(innerKeys[0], it) }
        outerKeys.forEach { assertEquals(outerKeys[0], it) }
    }

    @Test // b/152753046
    fun testSwappingGroups() = compositionTest {
        val items = mutableListOf(0, 1, 2, 3, 4)
        var scope: RecomposeScope? = null

        @Composable fun NoNodes() {}

        @Composable
        fun Test() {
            scope = currentRecomposeScope
            for (item in items) {
                key(item) { NoNodes() }
            }
        }

        compose { Test() }

        // Swap 2 and 3
        items[2] = 3
        items[3] = 2
        scope?.invalidate()

        expectChanges()
    }

    @Test // b/154650546
    fun testInsertOnMultipleLevels() = compositionTest {
        val items =
            mutableListOf(1 to mutableListOf(0, 1, 2, 3, 4), 3 to mutableListOf(0, 1, 2, 3, 4))

        val invalidates = mutableListOf<RecomposeScope>()
        fun invalidateComposition() {
            invalidates.forEach { it.invalidate() }
            invalidates.clear()
        }

        @Composable
        fun Numbers(numbers: List<Int>) {
            Linear {
                Linear {
                    invalidates.add(currentRecomposeScope)
                    for (number in numbers) {
                        Text("$number")
                    }
                }
            }
        }

        @Composable
        fun Item(number: Int, numbers: List<Int>) {
            Linear {
                invalidates.add(currentRecomposeScope)
                Text("$number")
                Numbers(numbers)
            }
        }

        @Composable
        fun Test() {
            invalidates.add(currentRecomposeScope)

            Linear {
                invalidates.add(currentRecomposeScope)
                for ((number, numbers) in items) {
                    Item(number, numbers)
                }
            }
        }

        fun MockViewValidator.validateNumbers(numbers: List<Int>) {
            Linear {
                Linear {
                    for (number in numbers) {
                        Text("$number")
                    }
                }
            }
        }

        fun MockViewValidator.validateItem(number: Int, numbers: List<Int>) {
            Linear {
                Text("$number")
                validateNumbers(numbers)
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                for ((number, numbers) in items) {
                    validateItem(number, numbers)
                }
            }
        }

        compose { Test() }

        fun validate() {
            validate { this.Test() }
        }

        validate()

        // Add numbers to the list at 0 and 1
        items[0].second.add(2, 100)
        items[1].second.add(3, 200)

        // Add a list to the root.
        items.add(1, 2 to mutableListOf(0, 1, 2))

        invalidateComposition()

        expectChanges()
        validate()
    }

    @Test
    fun testInsertingAfterSkipping() = compositionTest {
        val items = mutableListOf(1 to listOf(0, 1, 2, 3, 4))

        val invalidates = mutableListOf<RecomposeScope>()
        fun invalidateComposition() {
            invalidates.forEach { it.invalidate() }
            invalidates.clear()
        }

        @Composable
        fun Test() {
            invalidates.add(currentRecomposeScope)

            Linear {
                for ((item, numbers) in items) {
                    Text(item.toString())
                    Linear {
                        invalidates.add(currentRecomposeScope)
                        for (number in numbers) {
                            Text(number.toString())
                        }
                    }
                }
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                for ((item, numbers) in items) {
                    Text(item.toString())
                    Linear {
                        for (number in numbers) {
                            Text(number.toString())
                        }
                    }
                }
            }
        }

        compose { Test() }

        validate { this.Test() }

        items.add(2 to listOf(3, 4, 5, 6))
        invalidateComposition()

        expectChanges()
        validate { this.Test() }
    }

    @Test
    fun evenOddRecomposeGroup() = compositionTest {
        var includeEven = true
        var includeOdd = true
        val invalidates = mutableListOf<RecomposeScope>()

        fun invalidateComposition() {
            for (scope in invalidates) {
                scope.invalidate()
            }
            invalidates.clear()
        }

        @Composable
        fun Wrapper(content: @Composable () -> Unit) {
            content()
        }

        @Composable
        fun EmitText() {
            invalidates.add(currentRecomposeScope)
            if (includeOdd) {
                key(1) { Text("odd 1") }
            }
            if (includeEven) {
                key(2) { Text("even 2") }
            }
            if (includeOdd) {
                key(3) { Text("odd 3") }
            }
            if (includeEven) {
                key(4) { Text("even 4") }
            }
        }

        @Composable
        fun Test() {
            Linear {
                Wrapper { EmitText() }
                EmitText()
                Wrapper { EmitText() }
                EmitText()
            }
        }

        fun MockViewValidator.Wrapper(children: () -> Unit) {
            children()
        }

        fun MockViewValidator.EmitText() {
            if (includeOdd) {
                Text("odd 1")
            }
            if (includeEven) {
                Text("even 2")
            }
            if (includeOdd) {
                Text("odd 3")
            }
            if (includeEven) {
                Text("even 4")
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                this.Wrapper { this.EmitText() }
                this.EmitText()
                this.Wrapper { this.EmitText() }
                this.EmitText()
            }
        }

        compose { Test() }

        fun validate() {
            validate { this.Test() }
        }
        validate()

        includeEven = false
        invalidateComposition()
        expectChanges()
        validate()

        includeEven = true
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        includeEven = false
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        includeEven = true
        invalidateComposition()
        expectChanges()
        validate()

        includeOdd = true
        invalidateComposition()
        expectChanges()
        validate()
    }

    @Test // regression test for b/199136503
    fun testMovingSlotsButNotNodes() = compositionTest {
        val order = mutableStateListOf(1, 2, 3, 4, 5)
        val includeText = mutableStateMapOf(4 to 4)
        compose {
            for (i in order) {
                key(i) {
                    if (i in includeText) {
                        Text("Text for $i")
                    }
                }
            }
            Text("End")
        }

        validate {
            for (i in order) {
                if (i in includeText) {
                    Text("Text for $i")
                }
            }
            Text("End")
        }

        order.swap(3, 5)
        includeText.remove(4)
        includeText[3] = 3
        expectChanges()
        revalidate()
    }

    @Test
    fun evenOddWithMovement() = compositionTest {
        var includeEven = true
        var includeOdd = true
        var order = listOf(1, 2, 3, 4)
        val invalidates = mutableListOf<RecomposeScope>()

        fun invalidateComposition() {
            for (scope in invalidates) {
                scope.invalidate()
            }
            invalidates.clear()
        }

        @Composable
        fun EmitText(all: Boolean) {
            invalidates.add(currentRecomposeScope)
            for (i in order) {
                if (i % 2 == 1 && (all || includeOdd)) {
                    key(i) { Text("odd $i") }
                }
                if (i % 2 == 0 && (all || includeEven)) {
                    key(i) { Text("even $i") }
                }
            }
        }

        @Composable
        fun Test() {
            Linear {
                invalidates.add(currentRecomposeScope)
                for (i in order) {
                    key(i) {
                        Text("group $i")
                        if (i == 2 || (includeEven && includeOdd)) {
                            Text("including everything")
                        } else {
                            if (includeEven) {
                                Text("including evens")
                            }
                            if (includeOdd) {
                                Text("including odds")
                            }
                        }
                        EmitText(i == 2)
                    }
                }
                EmitText(false)
            }
        }

        fun MockViewValidator.EmitText(all: Boolean) {
            for (i in order) {
                if (i % 2 == 1 && (includeOdd || all)) {
                    Text("odd $i")
                }
                if (i % 2 == 0 && (includeEven || all)) {
                    Text("even $i")
                }
            }
        }

        fun MockViewValidator.Test() {
            Linear {
                for (i in order) {
                    Text("group $i")
                    if (i == 2 || (includeEven && includeOdd)) {
                        Text("including everything")
                    } else {
                        if (includeEven) {
                            Text("including evens")
                        }
                        if (includeOdd) {
                            Text("including odds")
                        }
                    }
                    this.EmitText(i == 2)
                }
                this.EmitText(false)
            }
        }

        compose { Test() }

        fun validate() {
            validate { this.Test() }
        }
        validate()

        order = listOf(1, 2, 4, 3)
        includeEven = false
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(1, 4, 2, 3)
        includeEven = true
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(3, 4, 2, 1)
        includeEven = false
        includeOdd = false
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(4, 3, 2, 1)
        includeEven = true
        invalidateComposition()
        expectChanges()
        validate()

        order = listOf(1, 2, 3, 4)
        includeOdd = true
        invalidateComposition()
        expectChanges()
        validate()
    }

    /**
     * This tests behavior when changing the state object instances being observed - so not
     * `remember`ing the mutableStateOf calls is intentional, hence the Lint suppression.
     */
    @Suppress("UnrememberedMutableState")
    @Test
    fun testObservationScopes() = compositionTest {
        val states = mutableListOf<MutableState<Int>>()
        var iterations = 0

        @Composable
        fun Test() {
            val s1 = mutableIntStateOf(iterations++)
            Text("s1 ${s1.intValue}")
            states.add(s1)
            val s2 = mutableIntStateOf(iterations++)
            Text("s2 ${s2.intValue}")
            states.add(s2)
        }

        compose { Test() }

        fun invalidateFirst() {
            states.first().value++
        }

        fun invalidateLast() {
            states.last().value++
        }

        repeat(10) {
            invalidateLast()
            expectChanges()
        }

        invalidateFirst()
        expectNoChanges()
    }

    @Suppress("UnrememberedMutableState")
    @Composable
    fun Indirect(iteration: Int, states: MutableList<MutableState<Int>>) {
        val state = mutableIntStateOf(Random.nextInt())
        states.add(state)
        Text("$iteration state = ${state.intValue}")
    }

    @Composable
    fun ComposeIndirect(iteration: State<Int>, states: MutableList<MutableState<Int>>) {
        Text("Iteration ${iteration.value}")
        Indirect(iteration.value, states)
    }

    @Test // Regression b/182822837
    fun testObservationScopes_IndirectInvalidate() = compositionTest {
        val states = mutableListOf<MutableState<Int>>()
        val iteration = mutableIntStateOf(0)

        compose { ComposeIndirect(iteration, states) }

        fun nextIteration() = iteration.intValue++
        fun invalidateLast() = states.last().value++
        fun invalidateFirst() = states.first().value++

        repeat(10) {
            nextIteration()
            expectChanges()
        }

        invalidateFirst()
        expectNoChanges()
    }

    @Composable
    fun <T> calculateValue(state: State<T>): T {
        return remember { state.value }
    }

    private var observationScopeTestCalls = 0
    private var observationScopeTestForwardWrite = false

    @Composable
    fun <T> ObservationScopesTest(state: State<T>, forwardWrite: Boolean) {
        observationScopeTestCalls++
        calculateValue(state)
        observationScopeTestForwardWrite = forwardWrite
    }

    @Composable
    fun ForwardWrite(state: MutableState<String>) {
        state.value += ", forward write"
    }

    @Test // Regression test for b/186787946
    fun testObservationScopes_ReadInRemember() = compositionTest {
        val state = mutableStateOf("state")
        var mainState by mutableStateOf("main state")
        var doForwardWrite by mutableStateOf(false)
        compose {
            Text(mainState)
            ObservationScopesTest(state, doForwardWrite)
            if (doForwardWrite) ForwardWrite(state)
        }

        // Set up the case by skipping ObservationScopeTest
        mainState += ", changed"
        advance()

        // Do the forward write after skipping ObserveScopesTest.
        // This triggers a backward write in ForwardWrite because of the remember.
        // This backwards write is transitory as future writes will just be forward writes.
        doForwardWrite = true
        advance(ignorePendingWork = true)

        // Assert we saw true. In the bug this is false because a stale value was used for
        // `doForwardWrite` because the scope callback lambda was not updated correctly.
        assertTrue(observationScopeTestForwardWrite)
    }

    @Test
    fun testApplierBeginEndCallbacks() = compositionTest {
        val checks = mutableListOf<String>()
        compose {
            val myComposer = currentComposer
            val myApplier = myComposer.applier as ViewApplier
            assertEquals(0, myApplier.onBeginChangesCalled, "onBeginChanges during composition")
            assertEquals(0, myApplier.onEndChangesCalled, "onEndChanges during composition")
            checks += "composition"

            SideEffect {
                assertEquals(1, myApplier.onBeginChangesCalled, "onBeginChanges during side effect")
                assertEquals(1, myApplier.onEndChangesCalled, "onEndChanges during side effect")
                checks += "SideEffect"
            }

            // Memo to future self:
            // Without the explicit generic definition of RememberObserver here, the type of this
            // remember call is inferred to be `Unit` thanks to the call's position as the last
            // expression in a unit lambda (the argument to `compose {}`). The remember lambda is in
            // turn interpreted as returning Unit, the object expression is dropped on the floor for
            // the gc, and Unit is written into the slot table.
            remember<RememberObserver> {
                object : RememberObserver {
                    override fun onRemembered() {
                        assertEquals(
                            1,
                            myApplier.onBeginChangesCalled,
                            "onBeginChanges during lifecycle observer",
                        )
                        assertEquals(
                            1,
                            myApplier.onEndChangesCalled,
                            "onEndChanges during lifecycle observer",
                        )
                        checks += "RememberObserver"
                    }

                    override fun onForgotten() {
                        // Nothing to do
                    }

                    override fun onAbandoned() {
                        // Nothing to do
                    }
                }
            }
        }
        assertEquals(
            listOf("composition", "RememberObserver", "SideEffect"),
            checks,
            "expected order of calls",
        )
    }

    @Test // regression test for b/172660922
    fun testInvalidationOfRemovedContent() = compositionTest {
        var s1Scope: RecomposeScope? = null
        var viewS1 by mutableStateOf(true)
        var performBackwardsWrite = true
        @Composable
        fun S1() {
            s1Scope = currentRecomposeScope
            Text("In s1")
        }

        @Composable
        fun S2() {
            Text("In s2")
        }

        @Composable
        fun Test() {
            Text("$viewS1")
            Wrap {
                if (viewS1) {
                    S1()
                }
                S2()
            }

            if (performBackwardsWrite) {
                // This forces the equivalent of a backwards write.
                s1Scope?.invalidate()
                performBackwardsWrite = false
            }
        }

        fun MockViewValidator.S1() {
            Text("In s1")
        }

        fun MockViewValidator.S2() {
            Text("In s2")
        }

        fun MockViewValidator.Test() {
            Text("$viewS1")
            if (viewS1) {
                this.S1()
            }
            this.S2()
        }

        compose { Test() }

        fun validate() = validate { this.Test() }

        validate()

        s1Scope?.invalidate()
        performBackwardsWrite = true
        expectNoChanges()

        validate()

        viewS1 = false
        performBackwardsWrite = true
        expectChanges()
        validate()
    }

    @Test
    fun testModificationsPropagateToSubcomposition() = compositionTest {
        var value by mutableIntStateOf(0)
        val content: MutableState<@Composable () -> Unit> = mutableStateOf({})
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var subCompositionOccurred = false

        @Composable
        fun ComposeContent() {
            content.value()
        }

        fun updateContent(parentValue: Int) {
            content.value = {
                subCompositionOccurred = true
                assertEquals(parentValue, value)
            }
        }

        compose {
            updateContent(value)
            TestSubcomposition { ComposeContent() }
        }

        subCompositionOccurred = false

        value = 10
        expectChanges()

        assertTrue(subCompositionOccurred)
    }

    /**
     * This test checks that an updated ComposableLambda capture used in a subcomposition correctly
     * invalidates that subcomposition and schedules recomposition of that subcomposition.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testComposableLambdaSubcompositionInvalidation() = runTest {
        localRecomposerTest { recomposer ->
            val composition = Composition(EmptyApplier(), recomposer)
            try {
                var rootState by mutableStateOf(false)
                val composedResults = mutableListOf<Boolean>()
                Snapshot.notifyObjectsInitialized()
                composition.setContent {
                    // Read into local variable, local will be captured below
                    val capturedValue = rootState
                    TestSubcomposition { composedResults.add(capturedValue) }
                }
                assertEquals(listOf(false), composedResults)
                rootState = true
                Snapshot.sendApplyNotifications()
                // expect lambda to invalidate on the same frame (regression test for b/320385076)
                testScheduler.advanceTimeByFrame(coroutineContext)
                assertEquals(listOf(false, true), composedResults)
            } finally {
                composition.dispose()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCompositionContextIsRemembered() = runTest {
        localRecomposerTest { recomposer ->
            val composition = Composition(EmptyApplier(), recomposer)
            try {
                lateinit var scope: RecomposeScope
                val parentReferences = mutableListOf<CompositionContext>()
                composition.setContent {
                    scope = currentRecomposeScope
                    parentReferences += rememberCompositionContext()
                }
                scope.invalidate()
                testScheduler.advanceUntilIdle()
                check(parentReferences.size > 1) { "expected to be composed more than once" }
                check(parentReferences.toSet().size == 1) {
                    "expected all parentReferences to be the same; saw $parentReferences"
                }
            } finally {
                composition.dispose()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testParentCompositionRecomposesFirst() = runTest {
        localRecomposerTest { recomposer ->
            val composition = Composition(EmptyApplier(), recomposer)
            val results = mutableListOf<String>()
            try {
                var firstState by mutableStateOf("firstInitial")
                var secondState by mutableStateOf("secondInitial")
                Snapshot.notifyObjectsInitialized()
                composition.setContent {
                    results += firstState
                    TestSubcomposition { results += secondState }
                }
                secondState = "secondSet"
                Snapshot.sendApplyNotifications()
                firstState = "firstSet"
                Snapshot.sendApplyNotifications()
                testScheduler.advanceUntilIdle()
                assertEquals(
                    listOf("firstInitial", "secondInitial", "firstSet", "secondSet"),
                    results,
                    "Expected call ordering during recomposition of subcompositions",
                )
            } finally {
                composition.dispose()
            }
        }
    }

    /**
     * An [Applier] may inadvertently (or on purpose) run arbitrary user code as a side effect of
     * performing tree manipulations as a [Composer] is applying changes. This can happen if the
     * tree type dispatches event callbacks when nodes are added or removed from a tree. These
     * callbacks may cause snapshot state writes, which can in turn invalidate scopes in the
     * composition that produced the tree in the first place. Ensure that the recomposition
     * machinery is robust to this, and that these invalidations are processed on a subsequent
     * recomposition.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testStateWriteInApplier() = runTest {
        class MutateOnRemoveApplier(private val removeCounter: MutableState<Int>) :
            AbstractApplier<Unit>(Unit) {
            var insertCount: Int = 0
                private set

            override fun remove(index: Int, count: Int) {
                removeCounter.value += count
            }

            override fun onClear() {
                // do nothing
            }

            override fun insertTopDown(index: Int, instance: Unit) {
                insertCount++
            }

            override fun insertBottomUp(index: Int, instance: Unit) {
                // do nothing
            }

            override fun move(from: Int, to: Int, count: Int) {
                // do nothing
            }
        }

        localRecomposerTest { recomposer ->
            val stateMutatedOnRemove = mutableIntStateOf(0)
            var shouldEmitNode by mutableStateOf(true)
            var compositionCount = 0
            Snapshot.notifyObjectsInitialized()
            val applier = MutateOnRemoveApplier(stateMutatedOnRemove)
            val composition = Composition(applier, recomposer)
            try {
                composition.setContent {
                    compositionCount++
                    // Read the state here so that the emit removal will invalidate it
                    stateMutatedOnRemove.intValue
                    if (shouldEmitNode) {
                        ComposeNode<Unit, MutateOnRemoveApplier>({}) {}
                    }
                }
                // Initial composition should not contain the node we will remove. We want to test
                // recomposition for this case in particular.
                assertEquals(1, applier.insertCount, "expected setup node not inserted")
                shouldEmitNode = false
                Snapshot.sendApplyNotifications()
                // Only advance one frame since the next frame will be automatically scheduled.
                testScheduler.advanceTimeByFrame(coroutineContext)
                assertEquals(1, stateMutatedOnRemove.intValue, "observable removals performed")
                // Only two composition passes should have been performed by this point; a state
                // invalidation in the applier should not be picked up or acted upon until after
                // this frame is complete.
                assertEquals(2, compositionCount, "expected number of (re)compositions performed")
                // After sending apply notifications we expect the snapshot state change made by
                // the applier to trigger one final recomposition.
                Snapshot.sendApplyNotifications()
                testScheduler.advanceUntilIdle()
                assertEquals(3, compositionCount, "expected number of (re)compositions performed")
            } finally {
                composition.dispose()
            }
        }
    }

    @Test // Regression test for b/180124293
    @OptIn(ExperimentalCoroutinesApi::class)
    fun disposedCompositionShouldReportAsDisposed() = runTest {
        localRecomposerTest { recomposer ->
            val composition = Composition(EmptyApplier(), recomposer)
            assertFalse(composition.isDisposed)
            composition.dispose()
            assertTrue(composition.isDisposed)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testSubcomposeSingleComposition() = compositionTest {
        var flag by mutableStateOf(true)
        var flagCopy by mutableStateOf(true)
        var copyValue = true
        var rememberedValue = true
        compose {
            Text("Parent $flag")
            flagCopy = flag
            TestSubcomposition {
                copyValue = flagCopy
                rememberedValue = remember(flagCopy) { copyValue }
            }
        }

        flag = false
        val count = advanceCount()
        assertFalse(copyValue)
        assertFalse(rememberedValue)
        assertEquals(1, count)
    }

    @Test
    fun enumCompositeKeyShouldBeStable() = compositionTest {
        var parentHash = EmptyCompositeKeyHashCode
        var compositeHash = EmptyCompositeKeyHashCode
        compose {
            parentHash = currentCompositeKeyHashCode
            key(MyEnum.First) { compositeHash = currentCompositeKeyHashCode }
        }

        val effectiveHash = compositeHash xor (parentHash rol 6)
        assertEquals(0, effectiveHash)
    }

    @Test
    fun enumCompositeKeysShouldBeStable() = compositionTest {
        var parentHash = EmptyCompositeKeyHashCode
        var compositeHash = EmptyCompositeKeyHashCode
        compose {
            parentHash = currentCompositeKeyHashCode
            key(MyEnum.First, MyEnum.Second) { compositeHash = currentCompositeKeyHashCode }
        }

        val effectiveHash = compositeHash xor (parentHash rol 6)
        assertEquals(8, effectiveHash)
    }

    @Test // regression test for b/188015757
    fun testRestartOfDefaultFunctions() = compositionTest {
        @Composable
        fun Test() {
            Defaults()
            use(stateB)
        }

        compose { Test() }

        // Force Defaults to skip
        stateB++
        advance()

        // Force Defaults to recompose
        stateA++
        advance()
    }

    // Regression test for b/383769314
    @Suppress("OpaqueUnitKey")
    @Test
    fun testRememberNotRecomputedInElidedGroupAfterMovableGroup() = compositionTest {
        var baseKey by mutableIntStateOf(0)
        var rememberInvocations = 0

        compose {
            key(baseKey) { remember { baseKey.toString() } }

            key(Unit) {}

            val unused = remember {
                assertEquals(
                    1,
                    ++rememberInvocations,
                    "Remember block should be invoked exactly once",
                )
                1
            }
        }

        baseKey++
        advance()

        assertEquals(1, rememberInvocations, "Remember block should be invoked exactly once")
    }

    // Regression test for b/383769314
    @Suppress("OpaqueUnitKey")
    @Test
    fun testRememberNotRecomputedInElidedGroupAfterNestedMovableGroup() = compositionTest {
        var baseKey by mutableIntStateOf(0)
        var rememberInvocations = 0

        compose {
            key(baseKey) { remember { baseKey.toString() } }

            key(Unit) { key(Unit) {} }

            val unused = remember {
                assertEquals(
                    1,
                    ++rememberInvocations,
                    "Remember block should be invoked exactly once",
                )
                1
            }
        }

        baseKey++
        advance()

        assertEquals(1, rememberInvocations, "Remember block should be invoked exactly once")
    }

    // Regression test for b/383769314
    @Suppress("OpaqueUnitKey")
    @Test
    fun testInvalidateCapturingLambdaInElidedGroupAfterMovableGroup() = compositionTest {
        var baseKey by mutableIntStateOf(0)
        var lastOuterSeen = ""
        var lastInnerSeen = ""

        compose {
            val captor = key(baseKey) { remember { baseKey.toString() } }

            key(Unit) {}

            lastOuterSeen = captor
            Container { lastInnerSeen = captor }
        }

        assertEquals("0", lastOuterSeen, "Outer scope did not compose")
        assertEquals("0", lastInnerSeen, "Inner scope did not compose")

        baseKey++
        advance()

        assertEquals("1", lastOuterSeen, "Outer scope did not recompose")
        assertEquals("1", lastInnerSeen, "Inner scope did not recompose")
    }

    // Regression test for b/383769314
    @Suppress("OpaqueUnitKey")
    @Test
    fun testInvalidateCapturingLambdaInElidedGroupAfterNestedMovableGroup() = compositionTest {
        var baseKey by mutableIntStateOf(0)
        var lastOuterSeen = ""
        var lastInnerSeen = ""

        compose {
            val captor = key(baseKey) { remember { baseKey.toString() } }

            key(Unit) { key(Unit) {} }

            lastOuterSeen = captor
            Container { lastInnerSeen = captor }
        }

        assertEquals("0", lastOuterSeen, "Outer scope did not compose")
        assertEquals("0", lastInnerSeen, "Inner scope did not compose")

        baseKey++
        advance()

        assertEquals("1", lastOuterSeen, "Outer scope did not recompose")
        assertEquals("1", lastInnerSeen, "Inner scope did not recompose")
    }

    enum class MyEnum {
        First,
        Second,
    }

    /** set should set the value every time, update should only set after initial composition. */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun composeNodeSetVsUpdate() = runTest {
        localRecomposerTest { recomposer ->
            class SetUpdateNode(property: String) {
                var changeCount = 0
                var property: String = property
                    set(value) {
                        field = value
                        changeCount++
                    }
            }
            class SetUpdateNodeApplier : AbstractApplier<SetUpdateNode>(SetUpdateNode("root")) {
                override fun insertTopDown(index: Int, instance: SetUpdateNode) {}

                override fun insertBottomUp(index: Int, instance: SetUpdateNode) {}

                override fun remove(index: Int, count: Int) {}

                override fun move(from: Int, to: Int, count: Int) {}

                override fun onClear() {}
            }
            val composition = Composition(SetUpdateNodeApplier(), recomposer)
            val nodes = mutableListOf<SetUpdateNode>()
            fun makeNode(property: String) = SetUpdateNode(property).also { nodes += it }

            var value by mutableStateOf("initial")

            composition.setContent {
                ComposeNode<SetUpdateNode, SetUpdateNodeApplier>(
                    factory = { makeNode(value) },
                    update = { set(value) { property = value } },
                )
                ComposeNode<SetUpdateNode, SetUpdateNodeApplier>(
                    factory = { makeNode(value) },
                    update = { update(value) { property = value } },
                )
            }

            assertEquals("initial", nodes[0].property, "node 0 initial composition value")
            assertEquals("initial", nodes[1].property, "node 1 initial composition value")
            assertEquals(1, nodes[0].changeCount, "node 0 initial composition changeCount")
            assertEquals(0, nodes[1].changeCount, "node 1 initial composition changeCount")

            value = "changed"
            Snapshot.sendApplyNotifications()
            testScheduler.advanceUntilIdle()

            assertEquals("changed", nodes[0].property, "node 0 recomposition value")
            assertEquals("changed", nodes[1].property, "node 1 recomposition value")
            assertEquals(2, nodes[0].changeCount, "node 0 recomposition changeCount")
            assertEquals(1, nodes[1].changeCount, "node 1 recomposition changeCount")
        }
    }

    @Test
    fun internalErrorsAreReportedAsInternal() = compositionTest {
        expectError("internal") { compose { currentComposer.createNode { null } } }
    }

    @Test
    fun textWithElvis() = compositionTest {
        compose {
            val value: String? = null
            value?.let { Text("Bye!") } ?: Text("Hello!")
        }

        validate { Text("Hello!") }
    }

    @Test
    fun textWithIfNotNull() = compositionTest {
        val condition = false
        compose {
            val result =
                if (condition) {
                    Text("Bye!")
                } else null

            if (result == null) {
                Text("Hello!")
            }
        }

        validate { Text("Hello!") }
    }

    @Test // Regression test for b/249050560
    fun testFunctionInstances() = compositionTest {
        var state by mutableIntStateOf(0)
        functionInstance = { -1 }

        compose {
            val localStateCopy = state
            fun localStateReader() = localStateCopy
            UpdateInstance(::localStateReader)
        }

        assertEquals(state, functionInstance())

        state = 10
        advance()
        assertEquals(state, functionInstance())
    }

    @Test
    fun testNonLocalReturn_CM1_RetFunc_FalseTrue() = compositionTest {
        var condition by mutableStateOf(false)

        compose { Test_CM1_RetFun(condition) }

        validate { this.Test_CM1_RetFun(condition) }

        condition = true

        expectChanges()

        revalidate()
    }

    @Test
    fun testNonLocalReturn_CM1_RetFunc_TrueFalse() = compositionTest {
        var condition by mutableStateOf(true)

        compose { Test_CM1_RetFun(condition) }

        validate { this.Test_CM1_RetFun(condition) }

        condition = false

        expectChanges()

        revalidate()
    }

    @Test
    fun test_CM1_CCM1_RetFun_FalseTrue() = compositionTest {
        var condition by mutableStateOf(false)

        compose { Test_CM1_CCM1_RetFun(condition) }

        validate { this.Test_CM1_CCM1_RetFun(condition) }

        condition = true

        expectChanges()

        revalidate()
    }

    @Test
    fun test_CM1_CCM1_RetFun_TrueFalse() = compositionTest {
        var condition by mutableStateOf(true)

        compose { Test_CM1_CCM1_RetFun(condition) }

        validate { this.Test_CM1_CCM1_RetFun(condition) }

        condition = false

        expectChanges()

        revalidate()
    }

    @Test
    fun test_returnConditionally_fromInlineLambda() = compositionTest {
        var condition by mutableStateOf(true)

        compose {
            InlineWrapper {
                if (condition) return@InlineWrapper
                Text("Test")
            }
        }

        validate {
            if (!condition) {
                Text("Test")
            }
        }

        condition = false

        expectChanges()

        revalidate()
    }

    @Test
    fun test_returnConditionally_fromInlineLambda_nonLocal() = compositionTest {
        var condition by mutableStateOf(true)

        compose {
            InlineWrapper {
                M1 {
                    if (condition) return@InlineWrapper
                    Text("Test")
                }
            }
        }

        validate {
            if (!condition) {
                Text("Test")
            }
        }

        condition = false

        expectChanges()

        revalidate()
    }

    @Test
    fun test_returnConditionally_fromLambda_nonLocal() = compositionTest {
        var condition by mutableStateOf(true)

        compose {
            Wrap {
                M1 {
                    if (condition) return@Wrap
                    Text("Test")
                }
            }
        }

        validate {
            if (!condition) {
                Text("Test")
            }
        }

        condition = false

        expectChanges()

        revalidate()
    }

    @Test // regression test for 264467571
    fun test_returnConditionally_fromNodeLambda_local_initial_return() = compositionTest {
        var condition by mutableStateOf(true)

        compose {
            currentComposer.disableSourceInformation()
            Text("Before outer")
            InlineLinear {
                Text("Before inner")
                InlineLinear inner@{
                    Text("Before return")
                    if (condition) {
                        return@inner
                    }
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        validate {
            Text("Before outer")
            InlineLinear {
                Text("Before inner")
                InlineLinear inner@{
                    Text("Before return")
                    if (condition) return@inner
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        repeat(4) {
            condition = !condition
            expectChanges()
            revalidate()
        }
    }

    @Test // regression test for 264467571
    fun test_returnConditionally_fromNodeLambda_local_initial_no_return() = compositionTest {
        var condition by mutableStateOf(true)
        compose {
            currentComposer.disableSourceInformation()
            Text("Before outer")
            InlineLinear {
                Text("Before inner")
                InlineLinear inner@{
                    Text("Before return")
                    if (condition) return@inner
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        validate {
            Text("Before outer")
            InlineLinear {
                Text("Before inner")
                InlineLinear inner@{
                    Text("Before return")
                    if (condition) return@inner
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        repeat(4) {
            condition = !condition
            expectChanges()
            revalidate()
        }
    }

    @Test // regression test for 264467571
    fun test_returnConditionally_fromNodeLambda_nonLocal_initial_return() = compositionTest {
        var condition by mutableStateOf(true)
        compose {
            currentComposer.disableSourceInformation()
            Text("Before outer")
            InlineLinear outer@{
                Text("Before inner")
                InlineLinear {
                    Text("Before return")
                    if (condition) return@outer
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        validate {
            Text("Before outer")
            InlineLinear outer@{
                Text("Before inner")
                InlineLinear {
                    Text("Before return")
                    if (condition) return@outer
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        repeat(4) {
            condition = !condition
            expectChanges()
            verifyConsistent()
            revalidate()
        }
    }

    @Test // regression test for 264467571
    fun test_returnConditionally_fromNodeLambda_nonLocal_initial_no_return() = compositionTest {
        var condition by mutableStateOf(true)
        compose {
            currentComposer.disableSourceInformation()
            Text("Before outer")
            InlineLinear outer@{
                Text("Before inner")
                InlineLinear {
                    Text("Before return")
                    if (condition) return@outer
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        validate {
            Text("Before outer")
            InlineLinear outer@{
                Text("Before inner")
                InlineLinear {
                    Text("Before return")
                    if (condition) return@outer
                    Text("After return")
                }
                Text("After inner")
            }
            Text("Before outer")
        }

        repeat(4) {
            condition = !condition
            expectChanges()
            revalidate()
        }
    }

    @Test
    fun test_returnConditionally_fromConditionalNodeLambda_nonLocal_initial_no_return() =
        compositionTest {
            var condition by mutableStateOf(true)
            compose {
                currentComposer.disableSourceInformation()
                Text("Before outer")
                InlineLinear outer@{
                    Text("Before inner")
                    if (condition) {
                        InlineLinear {
                            Text("Before return")
                            return@outer
                        }
                    }
                    Text("After inner")
                }
                Text("Before outer")
            }

            validate {
                Text("Before outer")
                InlineLinear outer@{
                    Text("Before inner")
                    if (condition) {
                        InlineLinear {
                            Text("Before return")
                            return@outer
                        }
                    }
                    Text("After inner")
                }
                Text("Before outer")
            }

            repeat(4) {
                condition = !condition
                expectChanges()
                revalidate()
            }
        }

    @Test
    fun test_returnConditionally_fromFunction_nonLocal() = compositionTest {
        val text = mutableStateOf<String?>(null)

        compose { TextWithNonLocalReturn(text.value) }

        validate {
            if (text.value != null) {
                Text(text.value!!)
            }
        }

        text.value = "Test"

        expectChanges()

        revalidate()
    }

    @Test // regression test for 274889428
    fun test_returnConditionally_simulatedIf() = compositionTest {
        val condition1 = mutableStateOf(true)
        val condition2 = mutableStateOf(true)
        val condition3 = mutableStateOf(true)

        compose block@{
            Text("A")
            SimulatedIf(condition1.value) {
                return@block
            }
            Text("B")
            SimulatedIf(condition2.value) {
                return@block
            }
            Text("C")
            SimulatedIf(condition3.value) {
                return@block
            }
            Text("D")
        }

        validate block@{
            Text("A")
            this.SimulatedIf(condition1.value) {
                return@block
            }
            Text("B")
            this.SimulatedIf(condition2.value) {
                return@block
            }
            Text("C")
            this.SimulatedIf(condition3.value) {
                return@block
            }
            Text("D")
        }

        condition1.value = false
        expectChanges()
        revalidate()

        condition2.value = false
        expectChanges()
        revalidate()

        condition3.value = false
        expectChanges()
        revalidate()
    }

    @Test // regression test for 267586102
    fun test_remember_in_a_loop() = compositionTest {
        var i1 = 0
        var i2 = 0
        var i3 = 0
        var recomposeCounter = 0
        var scope: RecomposeScope? = null

        val content: @Composable SomeUnstableClass.() -> Unit = {
            for (index in 0 until recomposeCounter) {
                remember(this) { index }
            }
            scope = currentRecomposeScope
            // these remembered values are not expected to change, BUT they change on recompositions
            i1 = remember(this) { 1 }
            i2 = remember(this) { 2 }
            i3 = remember(this) { 3 }
            recomposeCounter++
        }

        compose { content(SomeUnstableClass()) }
        advance()
        verifyConsistent()

        assertEquals(1, i1)
        assertEquals(2, i2)
        assertEquals(3, i3)
        assertEquals(1, recomposeCounter)

        scope!!.invalidate()
        advance()
        verifyConsistent()

        assertEquals(2, recomposeCounter)
        assertEquals(1, i1)
        assertEquals(2, i2)
        assertEquals(3, i3)

        scope.invalidate()
        advance()
        verifyConsistent()

        assertEquals(3, recomposeCounter)
        assertEquals(1, i1)
        assertEquals(2, i2)
        assertEquals(3, i3)
    }

    @Test
    fun testRememberAddedAndRemovedInALoop() = compositionTest {
        val iterations = 100
        val counter = mutableIntStateOf(0)
        var seen = emptyList<Any?>()

        fun seen(list: List<Any>) {
            for (i in 0 until min(list.size, seen.size)) {
                assertEquals(list[i], seen[i])
            }
            seen = list
        }

        compose {
            val list = mutableListOf<Any>()
            for (i in 0 until counter.intValue) {
                list.add(remember { Any() })
            }
        }

        while (counter.intValue < iterations) {
            counter.intValue++
            advance()
        }

        while (counter.intValue > 0) {
            counter.intValue--
            advance()
        }
        assertEquals(emptyList(), seen)
    }

    @Test
    fun test_crossinline_differentComposition() = compositionTest {
        var branch by mutableStateOf(false)
        compose {
            if (branch) {
                Text("Content")
            }
            InlineSubcomposition {
                @Suppress("ConstantConditionIf") // Testing this case
                if (false) {
                    remember { "Something" }
                }
            }
        }
        validate {
            if (branch) {
                Text("Content")
            }
        }

        branch = true
        expectChanges()
        revalidate()
    }

    /* TODO: Restore after updating to Kotlin 2.2
        Due to a bug in Kotlin 2.1.2x https://youtrack.jetbrains.com/issue/KT-77508, compilation of
        the tests for K/JS and K/Native fails with
        "Wrong number of parameters in wrapper: expected: 0 bound and 2 unbound, but 0 found".
        So ignoring doesn't really work for this case. For now the test is moved to CompositionJvmTests

    @Test
    fun composableDelegates() = compositionTest {
        val local = compositionLocalOf { "Default" }
        val delegatedLocal by local
        compose {
            Text(delegatedLocal)

            CompositionLocalProvider(local provides "Scoped") { Text(delegatedLocal) }
        }
        validate {
            Text("Default")
            Text("Scoped")
        }
    }
    */

    @Test
    fun testCompositionAndRecomposerDeadlock() {
        runTest(timeout = 10.seconds) {
            withGlobalSnapshotManager {
                repeat(100) {
                    val job = Job(parent = coroutineContext[Job])
                    val coroutineContext = Dispatchers.Unconfined + job
                    val recomposer = Recomposer(coroutineContext)

                    launch(
                        coroutineContext + BroadcastFrameClock(),
                        start = CoroutineStart.UNDISPATCHED,
                    ) {
                        recomposer.runRecomposeAndApplyChanges()
                    }

                    val composition = Composition(EmptyApplier(), recomposer)
                    composition.setContent {
                        val innerComposition =
                            Composition(EmptyApplier(), rememberCompositionContext())

                        DisposableEffect(composition) { onDispose { innerComposition.dispose() } }
                    }

                    var value by mutableIntStateOf(1)
                    launch(Dispatchers.Default + job) {
                        while (true) {
                            value += 1
                            delay(1)
                        }
                    }

                    composition.dispose()
                    recomposer.close()
                    job.cancel()
                }
            }
        }
    }

    @Test
    fun earlyComposableUnitReturn() = compositionTest {
        var state by mutableStateOf(true)
        compose {
            when (state) {
                true -> return@compose Text("true")
                false -> Text("false")
            }
            Text("after")
        }
        validate { Text("true") }

        state = false
        expectChanges()

        validate {
            Text("false")
            Text("after")
        }
    }

    // Regression test for b/288717411
    @Test
    fun test_forgottenValue_isFreedFromSlotTable() = compositionTest {
        val value = Any()
        var rememberValue by mutableStateOf(false)
        val composers = mutableSetOf<Composer>()
        compose {
            composers += currentComposer
            if (rememberValue) {
                // <Any> prevents implicit coercion to Unit from inline lambda
                // https://youtrack.jetbrains.com/issue/KT-76579
                remember<Any> { value }
            }
        }

        assertFalse(value in composition!!.getSlots())

        rememberValue = true
        expectChanges()

        assertTrue(value in composition!!.getSlots())

        rememberValue = false
        expectChanges()

        assertFalse(value in composition!!.getSlots())
        assertFalse(composers.any { value in it.getInsertTableSlots() })
    }

    @Stable
    class VarargConsumer(var invokeCount: Int = 0) {
        @Composable
        fun Varargs(vararg ints: Int) {
            invokeCount++
            for (i in ints) {
                use(i)
            }
        }
    }

    // Regression test for b/286132194
    @Test
    fun composableVarargs_skipped() = compositionTest {
        val consumer = VarargConsumer()
        var recomposeTrigger by mutableIntStateOf(0)
        compose {
            Linear {
                use(recomposeTrigger)
                consumer.Varargs(0, 1, 2, 3)
            }
        }

        assertEquals(1, consumer.invokeCount)

        recomposeTrigger = 1
        advance()

        assertEquals(1, consumer.invokeCount)
    }

    fun interface TestFunInterface {
        fun compute(value: Int)
    }

    @Composable
    fun TestMemoizedFun(compute: TestFunInterface) {
        val oldCompute = remember { compute }
        assertEquals(oldCompute, compute)
    }

    @Test
    fun funInterface_isMemoized() = compositionTest {
        var recomposeTrigger by mutableIntStateOf(0)
        val capture = 0
        compose {
            use(recomposeTrigger)
            TestMemoizedFun {
                // no captures
                use(it)
            }
            TestMemoizedFun {
                // stable captures
                use(capture)
            }
        }

        recomposeTrigger++
        advance()
    }

    // regression test for b/264467571, checks that composing with continue doesn't crash runtime
    @Test
    fun continueInALoop() = compositionTest {
        var iterations by mutableIntStateOf(5)
        compose {
            for (i in 1..iterations) {
                if (i == 4) continue
                Text(i.toString())
            }
        }

        iterations++
        expectChanges()
    }

    data class Foo(var i: Int = 0)

    class UnstableCompConsumer(var invokeCount: Int = 0) {
        @Composable
        fun UnstableComp(foo: Foo) {
            use(foo)
            invokeCount++
        }
    }

    @Test
    fun composableWithUnstableParameters_skipped() = compositionTest {
        val consumer = UnstableCompConsumer()
        var recomposeTrigger by mutableIntStateOf(0)
        val data = Foo()
        compose {
            Linear {
                use(recomposeTrigger)
                consumer.UnstableComp(foo = data)
            }
        }

        assertEquals(1, consumer.invokeCount)

        recomposeTrigger = 1
        data.i++
        advance()

        assertEquals(1, consumer.invokeCount)
    }

    // regression test from b/232007227 with forEach
    @Test
    fun slotsAreUsedCorrectly_forEach() = compositionTest {
        class Car(val model: String)
        class Person(val name: String, val car: MutableStateFlow<Car>)

        val people =
            mutableListOf<MutableStateFlow<Person?>>(
                MutableStateFlow(Person("Ford", MutableStateFlow(Car("Model T")))),
                MutableStateFlow(Person("Musk", MutableStateFlow(Car("Model 3")))),
            )
        compose {
            people.forEach {
                val person = it.collectAsState().value
                Text(person?.name ?: "No person")
                if (person != null) {
                    val car = person.car.collectAsState().value
                    Text("    ${car.model}")
                }
            }
        }

        validate {
            people.forEach {
                val person = it.value
                Text(person?.name ?: "No person")
                if (person != null) {
                    val car = person.car.value
                    Text("    ${car.model}")
                }
            }
        }

        advanceTimeBy(16_000L)
        people[0].value = null
        advanceTimeBy(16_000L)

        expectChanges()
        revalidate()
    }

    @Test
    fun readingDerivedState_invalidatesWhenValueNotChanged() = compositionTest {
        var state by mutableIntStateOf(0)
        var condition by mutableStateOf(false)
        val derived by derivedStateOf { if (!condition) 0 else state }
        compose { Text(derived.toString()) }
        validate { Text(derived.toString()) }

        condition = true
        expectNoChanges()
        revalidate()

        state++
        expectChanges()
        revalidate()
    }

    @Composable
    fun goBoom(): Boolean {

        return true
    }

    @Test
    fun earlyReturnFromInlined() = compositionTest {
        compose {
            run {
                @Suppress("ConstantConditionIf") // Testing this case
                if (true) {
                    return@run
                } else {
                    Text("")
                    return@run
                }
            }
        }

        validate {}
    }

    @Composable private fun key() = "key"

    @Test
    fun remember_withComposableParam() = compositionTest {
        compose {
            val text = remember(key()) { "" }
            Text(text)
        }

        validate { Text("") }
    }

    private val LocalNumber = compositionLocalOf { 0 }

    @Composable
    fun Test(number: Int = LocalNumber.current) {
        val remembered = remember(number) { number + 1 }
        assertEquals(remembered, number + 1)
    }

    @Test
    fun remember_defaultParamInRestartableFunction() = compositionTest {
        var state by mutableIntStateOf(0)
        compose { CompositionLocalProvider(LocalNumber provides state) { Test() } }

        validate {}

        state++
        advance()
        revalidate()
    }

    @Test
    fun rememberAfterLoop() = compositionTest {
        // Removing the group around remember requires that updates to slots can be performed to
        // the parent even after we have moved off the parent to one of the children. Here this
        // inserts content as the child of the main composition. This moves the write to just after
        // the insert and then the `remember` call needs to update the `compose` group's slots.
        var count by mutableIntStateOf(1)
        compose {
            repeat(count) { Text("Some text") }
            val someRemember = remember(count) { count + 1 }
            Text("$someRemember")
        }

        count++
        advance()
    }

    @Test
    fun appendingRememberAfterLoop() = compositionTest {
        var count by mutableIntStateOf(1)
        compose {
            repeat(count) { Text("Some text") }
            repeat(count) { unused(remember { it }) }
        }

        validate { repeat(count) { Text("Some text") } }

        count++
        advance()
        revalidate()

        count++
        advance()
        revalidate()

        count = 1
        advance()
        revalidate()
    }

    @Test
    fun composerCleanup() = compositionTest {
        var state by mutableIntStateOf(0)

        compose { Text("State = $state") }

        val stackSizes = (composition as CompositionImpl).composerStacksSizes()
        repeat(100) {
            state++
            advance()
        }
        assertEquals(stackSizes, (composition as CompositionImpl).composerStacksSizes())
    }

    @Test
    fun movableContentNoopInDeactivatedComposition() = compositionTest {
        val state = mutableStateOf(false)
        val movableContent = movableContentOf { Text("Test") }

        var composition: Composition? = null
        var context: CompositionContext? = null
        compose {
            context = rememberCompositionContext()

            // read state to force recomposition
            state.value
            SideEffect { if (state.value) (composition as CompositionImpl).deactivate() }
        }

        composition =
            CompositionImpl(context!!, ViewApplier(root)).apply {
                setContent {
                    if (state.value) {
                        movableContent()
                    }
                }
            }

        state.value = true
        advance()
    }

    @Test // regression test for 339618126
    fun removeGroupAtEndOfGroup() = compositionTest {
        // Ensure the runtime handles aberrant code generation
        val state = mutableStateOf(true)
        compose {
            InlineLinear {
                InlineLinear {
                    ExplicitStartReplaceGroup(-0x7e52e5de) { Text("Before") }
                    ExplicitStartReplaceGroup(0x9222f9c, insertGroup = state.value) {}
                    ExplicitStartReplaceGroup(0x22d2581c) { Text("After") }
                }
                InlineLinear { Text("State is ${state.value}") }
                if (state.value) {
                    Text("State is on")
                }
                if (!state.value) {
                    Text("State is off")
                }
            }
        }

        validate {
            Linear {
                Linear {
                    Text("Before")
                    Text("After")
                }
                Linear { Text("State is ${state.value}") }
                if (state.value) {
                    Text("State is on")
                }
                if (!state.value) {
                    Text("State is off")
                }
            }
        }

        state.value = false
        advance()
        revalidate()

        state.value = true
        advance()
        revalidate()
    }

    @Test // regression test for b/362291064
    fun avoidsThrashingTheSlotTable() = compositionTest {
        val count = 100
        var data by mutableIntStateOf(0)
        compose { repeat(count) { Linear { Text("Value: $it, data: $data") } } }

        validate { repeat(count) { Linear { Text("Value: $it, data: $data") } } }

        data++
        advance()
        revalidate()
    }

    @Test
    fun setContentDeactivated() = compositionTest {
        var text = "test"
        val content = @Composable { Text(text) }

        compose(content)

        (composition as ReusableComposition).deactivate()
        text = "test2"
        (composition as ReusableComposition).setContent(content)

        validate { Text("test2") }
    }

    private inline fun CoroutineScope.withGlobalSnapshotManager(block: CoroutineScope.() -> Unit) {
        val channel = Channel<Unit>(Channel.CONFLATED)
        val job = launch { channel.consumeEach { Snapshot.sendApplyNotifications() } }
        val unregisterToken = Snapshot.registerGlobalWriteObserver { channel.trySend(Unit) }
        try {
            block()
        } finally {
            unregisterToken.dispose()
            job.cancel()
        }
    }
}

class SomeUnstableClass(val a: Any = "abc")

@Composable
fun Test_CM1_RetFun(condition: Boolean) {
    Text("Root - before")
    M1 {
        Text("M1 - before")
        if (condition) return
        Text("M1 - after")
    }
    Text("Root - after")
}

fun MockViewValidator.Test_CM1_RetFun(condition: Boolean) {
    Text("Root - before")
    Text("M1 - before")
    if (condition) return
    Text("M1 - after")
    Text("Root - after")
}

@Composable
fun Test_CM1_CCM1_RetFun(condition: Boolean) {
    Text("Root - before")
    M1 {
        Text("M1 - begin")
        if (condition) {
            Text("if - begin")
            M1 {
                Text("In CCM1")
                return@Test_CM1_CCM1_RetFun
            }
        }
        Text("M1 - end")
    }
    Text("Root - end")
}

fun MockViewValidator.Test_CM1_CCM1_RetFun(condition: Boolean) {
    Text("Root - before")
    Text("M1 - begin")
    if (condition) {
        Text("if - begin")
        Text("In CCM1")
        return
    }
    Text("M1 - end")
    Text("Root - end")
}

var functionInstance: () -> Int = { 0 }

@Composable
fun UpdateInstance(newInstance: () -> Int) {
    functionInstance = newInstance
}

var stateA by mutableIntStateOf(1000)
var stateB by mutableIntStateOf(2000)

fun use(@Suppress("UNUSED_PARAMETER") v: Int) {}

fun calculateSomething() = 4

@Composable // used in testRestartOfDefaultFunctions
fun Defaults(a: Int = 1, b: Int = 2, c: Int = 3, d: Int = calculateSomething()) {
    assertEquals(1, a)
    assertEquals(2, b)
    assertEquals(3, c)
    assertEquals(4, d)
    use(stateA)
}

@OptIn(InternalComposeApi::class)
@Composable
internal fun TestSubcomposition(content: @Composable () -> Unit) {
    val parentRef = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    DisposableEffect(parentRef) {
        val subcomposition = Composition(EmptyApplier(), parentRef)
        // TODO: work around for b/179701728
        callSetContent(subcomposition) {
            // Note: This is in a lambda invocation to keep the currentContent state read
            // in the sub-composition's content composable. Changing this to be
            // subcomposition.setContent(currentContent) would snapshot read only on initial set.
            currentContent()
        }
        onDispose { subcomposition.dispose() }
    }
}

private fun callSetContent(composition: Composition, content: @Composable () -> Unit) {
    composition.setContent(content)
}

class Ref<T : Any> {
    lateinit var value: T
}

@Composable
fun NarrowInvalidateForReference(ref: Ref<CompositionContext>) {
    ref.value = rememberCompositionContext()
}

@Composable
fun testDeferredSubcomposition(block: @Composable () -> Unit): () -> Unit {
    val container = remember { View() }
    val ref = Ref<CompositionContext>()
    NarrowInvalidateForReference(ref = ref)
    return { Composition(ViewApplier(container), ref.value).apply { setContent { block() } } }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun <R> TestScope.localRecomposerTest(block: CoroutineScope.(Recomposer) -> R) =
    coroutineScope {
        withContext(TestMonotonicFrameClock(this)) {
            val recomposer = Recomposer(coroutineContext)
            launch { recomposer.runRecomposeAndApplyChanges() }
            // ensure recomposition runner has started
            testScheduler.advanceUntilIdle()
            block(recomposer)
            // This call doesn't need to be in a finally; everything it does will be torn down
            // in exceptional cases by the coroutineScope failure
            recomposer.cancel()
        }
    }

/**
 * Advances this scheduler by exactly one frame, as defined by the [TestMonotonicFrameClock] in
 * [context].
 */
@OptIn(ExperimentalCoroutinesApi::class)
private fun TestCoroutineScheduler.advanceTimeByFrame(context: CoroutineContext) {
    val testClock = context[MonotonicFrameClock] as TestMonotonicFrameClock
    advanceTimeBy(testClock.frameDelayMillis)
    // advanceTimeBy doesn't run tasks at exactly frameDelayMillis.
    runCurrent()
}

@Composable
fun Wrap(content: @Composable () -> Unit) {
    content()
}

@Composable
fun Wrap(count: Int, content: @Composable () -> Unit) {
    if (count > 1) Wrap(count - 1, content) else content()
}

private fun <T> assertArrayEquals(message: String, expected: Array<T>, received: Array<T>) {
    fun Array<T>.getString() = this.joinToString(", ") { it.toString() }
    fun err(msg: String): Nothing =
        error(
            "$message: $msg, expected: [${
        expected.getString()}], received: [${received.getString()}]"
        )
    if (expected.size != received.size) err("sizes are different")
    expected.indices.forEach { index ->
        if (expected[index] != received[index])
            err(
                "item at index $index was different (expected [${
                expected[index]}], received: [${received[index]}]"
            )
    }
}

// Contact test data
private val bob = Contact("Bob Smith", email = "bob@smith.com")
private val jon = Contact(name = "Jon Alberton", email = "jon@alberton.com")
private val steve = Contact("Steve Roberson", email = "steverob@somemail.com")

private fun testModel(contacts: MutableList<Contact> = mutableListOf(bob, jon, steve)) =
    ContactModel(filter = "", contacts = contacts)

// Report test data
private val jim_reports_to_sally = Report("Jim", "Sally")
private val rob_reports_to_alice = Report("Rob", "Alice")
private val clark_reports_to_lois = Report("Clark", "Lois")

internal interface Counted {
    val count: Int
}

internal interface Ordered {
    val rememberOrder: Int
    val forgetOrder: Int
}

internal interface Named {
    val name: String
}

private interface WithData {
    val data: Int
}

private fun Int.isOdd() = this % 2 == 1

private fun Int.isEven() = this % 2 == 0

fun <T> MutableList<T>.swap(a: T, b: T) {
    val aIndex = indexOf(a)
    val bIndex = indexOf(b)
    require(aIndex >= 0 && bIndex >= 0)
    set(aIndex, b)
    set(bIndex, a)
}

@Composable private inline fun InlineWrapper(content: @Composable () -> Unit) = content()

@Composable private inline fun M1(content: @Composable () -> Unit) = InlineWrapper { content() }

@Composable
private fun TextWithNonLocalReturn(text: String?) {
    InlineWrapper {
        if (text == null) return
        Text(text)
    }
}

@Composable
private inline fun SimulatedIf(condition: Boolean, block: () -> Unit) {
    if (condition) block()
}

@Suppress("UnusedReceiverParameter")
private inline fun MockViewValidator.SimulatedIf(condition: Boolean, block: () -> Unit) {
    if (condition) block()
}

@Composable
private inline fun InlineSubcomposition(crossinline content: @Composable () -> Unit) =
    TestSubcomposition {
        content()
    }

@Composable
private operator fun <T> CompositionLocal<T>.getValue(thisRef: Any?, property: KProperty<*>) =
    current

// for 274185312

var itemRendererCalls = 0
var scrollingListCalls = 0

@Composable
fun TestSkippingContent(data: State<Int>) {
    ScrollingList { viewItem ->
        Text("${data.value}")
        scrollingListCalls++
        ItemRenderer(viewItem)
    }
}

@Composable
fun ItemRenderer(viewItem: ListViewItem) {
    itemRendererCalls++
    Text("${viewItem.id}")
}

@Composable
private fun ScrollingList(itemRenderer: @Composable (ListViewItem) -> Unit) {
    ListContent(
        viewItems = remember { listOf(ListViewItem(0), ListViewItem(1)) },
        itemRenderer = itemRenderer,
    )
}

@Composable
fun ListContent(viewItems: List<ListViewItem>, itemRenderer: @Composable (ListViewItem) -> Unit) {
    viewItems.forEach { viewItem ->
        ListContentItem(viewItem = viewItem, itemRenderer = itemRenderer)
    }
}

@Composable
fun ListContentItem(viewItem: ListViewItem, itemRenderer: @Composable (ListViewItem) -> Unit) {
    itemRenderer(viewItem)
}

data class ListViewItem(val id: Int)

private fun <T> unused(@Suppress("UNUSED_PARAMETER") value: T) {}

// Part of regression test for 339618126
@Composable
@ExplicitGroupsComposable
inline fun ExplicitStartReplaceGroup(
    key: Int,
    insertGroup: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (insertGroup) currentComposer.startReplaceGroup(key)
    content()
    if (insertGroup) currentComposer.endReplaceGroup()
}
