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

import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.PageTransformer
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that setting a transformer disables data-set change animations, and that those are restored
 * when a transformer is removed.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PageTransformerItemAnimatorTest : BaseTest() {

    @Test
    fun test() {
        setUpTest(ORIENTATION_HORIZONTAL).apply {
            setAdapterSync(viewAdapterProviderValueId.provider(stringSequence(5)))
            assertBasicState(0)

            val rv = viewPager.getChildAt(0) as RecyclerView
            val animatorDefault = rv.itemAnimator as ItemAnimator
            val animatorCustom = object : DefaultItemAnimator() {} as ItemAnimator
            assertThat(animatorDefault, notNullValue())
            assertThat(animatorDefault, not(equalTo(animatorCustom)))

            val transformer1 = PageTransformer { _, _ -> }
            val transformer2 = PageTransformer { _, _ -> }
            val transformer3 = PageTransformer { _, _ -> }

            runOnUiThreadSync {
                assertThat(rv.itemAnimator, equalTo(animatorDefault))
                viewPager.setPageTransformer(MarginPageTransformer(50))
                assertThat(rv.itemAnimator, nullValue())

                viewPager.setPageTransformer(transformer1)
                assertThat(rv.itemAnimator, nullValue())

                viewPager.setPageTransformer(null)
                assertThat(rv.itemAnimator, equalTo(animatorDefault))

                rv.itemAnimator = animatorCustom
                viewPager.setPageTransformer(transformer2)
                assertThat(rv.itemAnimator, nullValue())

                viewPager.setPageTransformer(MarginPageTransformer(100))
                assertThat(rv.itemAnimator, nullValue())

                viewPager.setPageTransformer(CompositePageTransformer())
                assertThat(rv.itemAnimator, nullValue())

                viewPager.setPageTransformer(null)
                assertThat(rv.itemAnimator, equalTo(animatorCustom))

                viewPager.setPageTransformer(transformer3)
                assertThat(rv.itemAnimator, nullValue())

                viewPager.setPageTransformer(null)
                assertThat(rv.itemAnimator, equalTo(animatorCustom))

                viewPager.setPageTransformer(null)
                assertThat(rv.itemAnimator, equalTo(animatorCustom))
            }
        }
    }
}
