/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.widget

import android.widget.TextView
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import androidx.testutils.PollingCheck
import androidx.viewpager2.adapter.StatefulAdapter
import androidx.viewpager2.widget.MutableCollectionsTest.Action
import androidx.viewpager2.widget.MutableCollectionsTest.Action.AddItem
import androidx.viewpager2.widget.MutableCollectionsTest.Action.ChangeItem
import androidx.viewpager2.widget.MutableCollectionsTest.Action.RemoveItem
import androidx.viewpager2.widget.MutableCollectionsTest.TestConfig
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import androidx.viewpager2.widget.swipe.PageView
import androidx.viewpager2.widget.swipe.SelfChecking
import org.hamcrest.Matchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val RANDOM_TESTS_PER_CONFIG = 0 // increase to have random tests generated

@RunWith(Parameterized::class)
@LargeTest
class MutableCollectionsTest(private val testConfig: TestConfig) : BaseTest() {

    @FlakyTest(bugId = 210157798)
    @Test
    fun test() {
        testConfig.apply {
            setUpTest(orientation).apply {
                setAdapterSync(adapterProvider.provider(items))

                verifyViewPagerContent(items) // quick check

                actions.forEach { action ->
                    if (items.isNotEmpty()) {
                        // useful when designing test cases, e.g. delete item before current
                        setCurrentItemSync(viewPager.middleItem)
                    }

                    action.perform(items)
                    if (action is ChangeItem && viewPager.adapter is StatefulAdapter) {
                        handleStatefulAdapterItemChange(action.position, items, this)
                    }

                    notifyDataSetChangedSync()
                    verifyViewPagerContent(items)
                }

                assertThat(items, equalTo(expectedEndItems))
            }
        }
    }

    /**
     * In the test setup:
     * - Each test page contains a [TextView] containing an item value.
     * - Adapters implementing [StatefulAdapter] save state of pages
     *
     * When an item value is changed ([ChangeItem]) in the collection [items], and it already has
     * a saved state associated with it, we might end up with state being restored and overriding
     * the new page value.
     *
     * To work around that, below we are changing the [TextView] as we change the value in the
     * backing item collection [items].
     *
     * A nicer test setup (potentially added later) would involve another [TextView] on each page
     * independent of its item value.. that would allow to verify that page state is maintained even
     * if item value changes.
     *
     * TODO(b/122667374)
     * - Consider adding a [TextView] independent of item value and use for testing state saving.
     * - Document the nuance above.
     * - Discuss the above when finalizing the API.
     */
    private fun handleStatefulAdapterItemChange(
        targetPage: Int,
        items: MutableList<String>,
        context: Context
    ) {
        context.apply {
            val currentPage = viewPager.currentItem
            val newContent = items[targetPage]

            setCurrentItemSync(targetPage)
            setCurrentPageContent(newContent)
            setCurrentItemSync(currentPage)
        }
    }

    private fun Context.setCurrentPageContent(newContent: String) {
        runOnUiThreadSync {
            PageView.setPageText(PageView.findPageInActivity(activity)!!, newContent)
        }
    }

    private fun Context.setCurrentItemSync(targetPage: Int) {
        viewPager.setCurrentItemSync(targetPage, false, 5, TimeUnit.SECONDS)
    }

    private fun Context.verifyViewPagerContent(items: MutableList<String>) {
        fun isPageContentExpected(expectedValue: String): Boolean {
            val page = PageView.findPageInActivity(activity) ?: return false
            return PageView.getPageText(page) == expectedValue
        }

        (0 until viewPager.adapter!!.itemCount).forEach { pageIx ->
            setCurrentItemSync(pageIx)
            val expectedValue = items[pageIx]

            // TODO(b/122667374): revising existing listeners so users don't have to rely on polling
            PollingCheck.waitFor(5000) { isPageContentExpected(expectedValue) }

            assertThat(isPageContentExpected(expectedValue), equalTo(true))
            assertThat(viewPager.currentItem, equalTo(pageIx))
            if (viewPager.adapter is SelfChecking) {
                (viewPager.adapter as SelfChecking).selfCheck()
            }
        }
    }

    private fun Context.notifyDataSetChangedSync() {
        val latch = CountDownLatch(1)
        viewPager.viewTreeObserver.addOnGlobalLayoutListener { latch.countDown() }

        runOnUiThreadSync { viewPager.adapter!!.notifyDataSetChanged() }
        latch.await(5, TimeUnit.SECONDS)
    }

    private val ViewPager2.middleItem: Int get() = adapter!!.itemCount / 2

    data class TestConfig(
        val name: String,
        private val _items: List<String>,
        val adapterProvider: AdapterProviderForItems,
        val actions: List<Action>,
        @ViewPager2.Orientation val orientation: Int,
        val expectedEndItems: List<String>
    ) {
        val items: MutableList<String> = _items.toMutableList()
    }

    sealed class Action(val position: Int) {
        data class AddItem(private val _position: Int, val item: String) : Action(_position)
        data class ChangeItem(private val _position: Int) : Action(_position)
        data class RemoveItem(private val _position: Int) : Action(_position)

        fun perform(items: MutableList<String>) {
            when (this) {
                is AddItem -> {
                    items.add(position, item)
                }
                is ChangeItem -> {
                    items[position] += "-suffix"
                }
                is RemoveItem -> {
                    items.removeAt(position)
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> = createTestSet()
    }
}

private fun createTestSet(): List<TestConfig> {
    val result = mutableListOf<TestConfig>()

    listOf(
        fragmentAdapterProviderValueId,
        viewAdapterProviderValueId,
        viewAdapterProvider
    ).forEach { adapterProvider ->
        listOf(ORIENTATION_HORIZONTAL, ORIENTATION_VERTICAL).forEach { orientation ->
            result += TestConfig(
                name = "remove first",
                _items = stringSequence(pageCount = 9),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(_position = 0)),
                orientation = orientation,
                expectedEndItems = listOf("1", "2", "3", "4", "5", "6", "7", "8")
            )

            result += TestConfig(
                name = "remove middle",
                _items = stringSequence(pageCount = 9),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(_position = 4)),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2", "3", "5", "6", "7", "8")
            )

            result += TestConfig(
                name = "remove before middle",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(_position = 1)),
                orientation = orientation,
                expectedEndItems = listOf("0", "2", "3", "4")
            )

            result += TestConfig(
                name = "remove after middle",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(_position = 3)),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2", "4")
            )

            result += TestConfig(
                name = "remove last",
                _items = stringSequence(pageCount = 9),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(_position = 8)),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2", "3", "4", "5", "6", "7")
            )

            result += TestConfig(
                name = "add before first",
                _items = stringSequence(pageCount = 9),
                adapterProvider = adapterProvider,
                actions = listOf(AddItem(_position = 0, item = "1000")),
                orientation = orientation,
                expectedEndItems = listOf("1000", "0", "1", "2", "3", "4", "5", "6", "7", "8")
            )

            result += TestConfig(
                name = "add before middle",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(AddItem(_position = 1, item = "99")),
                orientation = orientation,
                expectedEndItems = listOf("0", "99", "1", "2", "3", "4")
            )

            result += TestConfig(
                name = "add on middle",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(AddItem(_position = 2, item = "99")),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "99", "2", "3", "4")
            )

            result += TestConfig(
                name = "add after middle",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(AddItem(_position = 3, item = "99")),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2", "99", "3", "4")
            )

            result += TestConfig(
                name = "add before last",
                _items = stringSequence(pageCount = 9),
                adapterProvider = adapterProvider,
                actions = listOf(AddItem(_position = 8, item = "88")),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2", "3", "4", "5", "6", "7", "88", "8")
            )

            result += TestConfig(
                name = "add after last",
                _items = stringSequence(pageCount = 9),
                adapterProvider = adapterProvider,
                actions = listOf(AddItem(_position = 9, item = "99")),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "99")
            )

            result += TestConfig(
                name = "change first",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(ChangeItem(_position = 0)),
                orientation = orientation,
                expectedEndItems = listOf("0-suffix", "1", "2", "3", "4")
            )

            result += TestConfig(
                name = "change middle",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(ChangeItem(_position = 2)),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2-suffix", "3", "4")
            )

            result += TestConfig(
                name = "change last",
                _items = stringSequence(pageCount = 5),
                adapterProvider = adapterProvider,
                actions = listOf(ChangeItem(_position = 4)),
                orientation = orientation,
                expectedEndItems = listOf("0", "1", "2", "3", "4-suffix")
            )

            result += TestConfig(
                name = "regression1",
                _items = stringSequence(pageCount = 1),
                adapterProvider = adapterProvider,
                actions = listOf(AddItem(_position = 0, item = "1"), RemoveItem(0)),
                orientation = orientation,
                expectedEndItems = listOf("0")
            )

            result += TestConfig(
                name = "regression2",
                _items = stringSequence(pageCount = 6),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(2), RemoveItem(1)),
                orientation = orientation,
                expectedEndItems = listOf("0", "3", "4", "5")
            )

            result += TestConfig(
                name = "regression3",
                _items = stringSequence(pageCount = 3),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(_position = 0), AddItem(_position = 1, item = "3")),
                orientation = orientation,
                expectedEndItems = listOf("1", "3", "2")
            )

            result += TestConfig(
                name = "regression4",
                _items = stringSequence(pageCount = 3),
                adapterProvider = adapterProvider,
                actions = listOf(RemoveItem(_position = 0), AddItem(_position = 0, item = "3")),
                orientation = orientation,
                expectedEndItems = listOf("3", "1", "2")
            )

            repeat(RANDOM_TESTS_PER_CONFIG) {
                result += generateRandomTest(adapterProvider, orientation)
            }
        }
    }

    return result
}

private fun generateRandomTest(
    adapterProvider: AdapterProviderForItems,
    orientation: Int
): TestConfig {
    return generateRandomTest(adapterProvider, orientation, seed = Random().nextLong())
}

private fun generateRandomTest(
    adapterProvider: AdapterProviderForItems,
    orientation: Int,
    seed: Long
): TestConfig {
    val random = Random(seed)
    val initialItems: List<String> = stringSequence(random.nextInt(10))
    val currentItems: MutableList<String> = initialItems.toMutableList()
    val actions: MutableList<Action> = mutableListOf()

    val nextValue = AtomicInteger(initialItems.map { it.toInt() + 1 }.maxOrNull() ?: 0)
    (0..random.nextInt(5)).forEach {
        val choice = random.nextInt(3)
        when {
            currentItems.isEmpty() || choice == 0 -> {
                val value = nextValue.getAndIncrement().toString()
                val index = random.nextInt(currentItems.size + 1)
                actions.add(AddItem(index, value))
            }
            else -> {
                val index = random.nextInt(currentItems.size)
                actions.add(
                    when (choice) {
                        1 -> ChangeItem(index)
                        2 -> RemoveItem(index)
                        else -> throw IllegalStateException()
                    }
                )
            }
        }
        actions.last().perform(currentItems)
    }

    return TestConfig(
        name = "random_$seed",
        _items = initialItems,
        adapterProvider = adapterProvider,
        actions = actions.toList(),
        orientation = orientation,
        expectedEndItems = currentItems.toList()
    )
}
