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

package androidx.viewpager2.widget

import android.graphics.Canvas
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.assertThrows
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

private const val operationTimeoutSeconds = 5L

@RunWith(AndroidJUnit4::class)
@LargeTest
class ItemDecorationTest : BaseTest() {
    private val decoration1 = object : ItemDecoration() {}
    private val decoration2 = object : ItemDecoration() {}
    private val decoration3 = object : ItemDecoration() {}
    private val decoration4 = object : ItemDecoration() {}
    private val decoration5 = object : ItemDecoration() {}
    private val decoration6 = object : ItemDecoration() {}

    @Test
    fun test_dividers_render_views_horizontal() {
        test_dividers_render(ORIENTATION_HORIZONTAL, viewAdapterProvider)
    }

    @Test
    fun test_dividers_render_fragments_vertical() {
        test_dividers_render(ORIENTATION_VERTICAL, fragmentAdapterProvider)
    }

    private fun test_dividers_render(
        @ViewPager2.Orientation orientation: Int,
        adapterProvider: AdapterProviderForItems
    ) {
        // given
        setUpTest(orientation).run {
            setAdapterSync(adapterProvider(stringSequence(3)))

            // sanity checks
            assertBasicState(0)
            assertThat(viewPager.itemDecorationCount, equalTo(0))

            // when
            val decoration1 = ItemDecorationStub()
            viewPager.addItemDecorationSync(decoration1)
            // then
            assertThat(viewPager.itemDecorationCount, equalTo(1))

            // when
            val decoration2 = ItemDecorationStub()
            viewPager.addItemDecorationSync(decoration2)
            // then
            assertThat(viewPager.itemDecorationCount, equalTo(2))

            // when
            swipeForwardSync()
            // then
            assertBasicState(1)
            assertThat(decoration1.drawCount, greaterThan(0))
            assertThat(decoration2.drawCount, greaterThan(0))

            // when
            val layoutChangeLatch = viewPager.addWaitForLayoutChangeLatch()
            viewPager.invalidateItemDecorationsSync()
            val layoutHappened = layoutChangeLatch.await(operationTimeoutSeconds, SECONDS)
            // then
            assertThat(layoutHappened, equalTo(true))
        }
    }

    @Test
    fun test_divider_add_remove_count() {
        val viewPager = ViewPager2(ApplicationProvider.getApplicationContext())
        assertThat(viewPager.itemDecorations, equalTo(listOf()))

        viewPager.addItemDecoration(decoration1)
        assertThat(viewPager.itemDecorations, equalTo(listOf<ItemDecoration>(decoration1)))

        viewPager.addItemDecoration(decoration2, 0)
        assertThat(viewPager.itemDecorations, equalTo(listOf(decoration2, decoration1)))

        viewPager.removeItemDecorationAt(0)
        assertThat(viewPager.itemDecorations, equalTo(listOf<ItemDecoration>(decoration1)))

        viewPager.addItemDecoration(decoration2)
        viewPager.removeItemDecoration(decoration1)
        assertThat(viewPager.itemDecorations, equalTo(listOf<ItemDecoration>(decoration2)))
    }

    @Test
    fun test_divider_add_get_remove_edgeCaseIndexes() {
        // given
        val initialDecorations = listOf(decoration1, decoration2)
        val viewPager = ViewPager2(ApplicationProvider.getApplicationContext())
        initialDecorations.forEach { viewPager.addItemDecoration(it) }
        assertThat(viewPager.itemDecorations, equalTo(initialDecorations))

        // get / remove: illegal indexes
        listOf(-100, -1, 2, 5, 100).forEach { ix ->
            assertThrows<IndexOutOfBoundsException> { viewPager.getItemDecorationAt(ix) }
            assertThrows<IndexOutOfBoundsException> { viewPager.removeItemDecorationAt(ix) }
        }
        assertThat(viewPager.itemDecorations, equalTo(initialDecorations))

        // add: illegal indexes
        listOf(3, 5, 100).forEach { ix ->
            assertThrows<IndexOutOfBoundsException> { viewPager.addItemDecoration(decoration3, ix) }
        }
        assertThat(viewPager.itemDecorations, equalTo(initialDecorations))

        // add: negative indexes (they are legal)

        viewPager.addItemDecoration(decoration3, -1)
        assertThat(viewPager.itemDecorations, equalTo(initialDecorations.plus(decoration3)))

        viewPager.addItemDecoration(decoration4, -100)
        assertThat(
            viewPager.itemDecorations,
            equalTo(initialDecorations.plus(listOf(decoration3, decoration4)))
        )

        viewPager.addItemDecoration(decoration5, -50)
        assertThat(
            viewPager.itemDecorations,
            equalTo(initialDecorations.plus(listOf(decoration3, decoration4, decoration5)))
        )

        // add: lastIx + 1 (legal)
        viewPager.addItemDecoration(decoration6, viewPager.itemDecorationCount)
        assertThat(
            viewPager.itemDecorations,
            equalTo(
                initialDecorations.plus(
                    listOf(
                        decoration3,
                        decoration4,
                        decoration5,
                        decoration6
                    )
                )
            )
        )
    }

    private fun Context.swipeForwardSync() {
        val scrolledLatch = viewPager.addWaitForIdleLatch()
        swipeForward()
        scrolledLatch.await(operationTimeoutSeconds, SECONDS)
    }

    private fun (ViewPager2).addItemDecorationSync(decoration: ItemDecoration) {
        postSync { addItemDecoration(decoration) }
    }

    private fun (ViewPager2).invalidateItemDecorationsSync() {
        postSync { invalidateItemDecorations() }
    }

    private fun (ViewPager2).postSync(f: () -> Unit) {
        val latch = CountDownLatch(1)
        post {
            f()
            latch.countDown()
        }
        latch.await(operationTimeoutSeconds, SECONDS)
    }

    private val (ViewPager2).itemDecorations: List<ItemDecoration>
        get() = (0 until itemDecorationCount).map { getItemDecorationAt(it) }

    private class ItemDecorationStub : ItemDecoration() {
        var drawCount = 0

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            drawCount++
        }
    }
}
