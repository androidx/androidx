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

import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

private const val operationTimeoutSeconds = 5L

@RunWith(AndroidJUnit4::class)
@LargeTest
class ItemDecorationTest : BaseTest() {
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
            val decoration1 = spy(DividerItemDecoration(activity, orientation))
            viewPager.addItemDecorationSync(decoration1)
            // then
            assertThat(viewPager.itemDecorationCount, equalTo(1))

            // when
            val decoration2 = spy(DividerItemDecoration(activity, orientation))
            viewPager.addItemDecorationSync(decoration2)
            // then
            assertThat(viewPager.itemDecorationCount, equalTo(2))

            // when
            swipeForwardSync()
            // then
            assertBasicState(1)
            verify(decoration1, atLeastOnce()).onDraw(any(), any(), any())
            verify(decoration2, atLeastOnce()).onDraw(any(), any(), any())

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
        // given
        val viewPager = ViewPager2(ApplicationProvider.getApplicationContext())
        val decoration1 = object : ItemDecoration() {}
        val decoration2 = object : ItemDecoration() {}

        // when
        viewPager.addItemDecoration(decoration1)
        // then
        assertThat(viewPager.itemDecorationCount, equalTo(1))
        assertThat(viewPager.getItemDecorationAt(0), equalTo<ItemDecoration>(decoration1))

        // when
        viewPager.addItemDecoration(decoration2, 0)
        // then
        assertThat(viewPager.itemDecorationCount, equalTo(2))
        assertThat(viewPager.getItemDecorationAt(0), equalTo<ItemDecoration>(decoration2))
        assertThat(viewPager.getItemDecorationAt(1), equalTo<ItemDecoration>(decoration1))

        // when
        viewPager.removeItemDecorationAt(0)
        // then
        assertThat(viewPager.itemDecorationCount, equalTo(1))
        assertThat(viewPager.getItemDecorationAt(0), equalTo<ItemDecoration>(decoration1))

        // when
        viewPager.addItemDecoration(decoration2)
        viewPager.removeItemDecoration(decoration1)
        // then
        assertThat(viewPager.getItemDecorationAt(0), equalTo<ItemDecoration>(decoration2))
        assertThat(viewPager.itemDecorationCount, equalTo(1))
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
}
