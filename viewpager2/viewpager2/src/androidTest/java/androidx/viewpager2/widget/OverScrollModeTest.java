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

package androidx.viewpager2.widget;

import static androidx.core.util.Preconditions.checkNotNull;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.viewpager2.test.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class OverScrollModeTest {
    @Test
    public void test_overScrollMode_noAttrs() {
        ViewPager2 viewPager = new ViewPager2(ApplicationProvider.getApplicationContext());
        assertThat(viewPager.getOverScrollMode(),
                equalTo(View.OVER_SCROLL_IF_CONTENT_SCROLLS));
        assertOverScrollModeSyncWithRecyclerView(viewPager);
    }

    @Test
    public void test_overScrollMode_nullAttrs() {
        ViewPager2 viewPager = new ViewPager2(ApplicationProvider.getApplicationContext(),
                null);
        assertThat(viewPager.getOverScrollMode(),
                equalTo(View.OVER_SCROLL_IF_CONTENT_SCROLLS));
        assertOverScrollModeSyncWithRecyclerView(viewPager);
    }

    @Test
    public void test_overScrollMode_default() {
        assertOverScrollModeCorrect(R.layout.overscroll_mode_default,
                View.OVER_SCROLL_IF_CONTENT_SCROLLS);
    }

    @Test
    public void test_overScrollMode_always() {
        assertOverScrollModeCorrect(R.layout.overscroll_mode_always, View.OVER_SCROLL_ALWAYS);
    }

    @Test
    public void test_overScrollMode_ifContentScrolls() {
        assertOverScrollModeCorrect(R.layout.overscroll_mode_if_content_scrolls,
                View.OVER_SCROLL_IF_CONTENT_SCROLLS);
    }

    @Test
    public void test_overScrollMode_never() {
        assertOverScrollModeCorrect(R.layout.overscroll_mode_never, View.OVER_SCROLL_NEVER);
    }

    @Test
    public void test_overScrollMode_manual_set() {
        ViewPager2 viewPager = new ViewPager2(ApplicationProvider.getApplicationContext());
        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        assertThat(viewPager.getOverScrollMode(), equalTo(View.OVER_SCROLL_NEVER));
        assertOverScrollModeSyncWithRecyclerView(viewPager);
    }

    private void assertOverScrollModeSyncWithRecyclerView(ViewPager2 viewPager2) {
        final int expectedOverScrollMode = viewPager2.getOverScrollMode();
        final int childCount = viewPager2.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View childView = viewPager2.getChildAt(i);
            if (childView instanceof RecyclerView) {
                final int rvOverScrollMode = childView.getOverScrollMode();
                assertThat(rvOverScrollMode, equalTo(expectedOverScrollMode));
                return;
            }
        }
    }

    private void assertOverScrollModeCorrect(int layoutId, int expectedOverScrollMode) {
        LayoutInflater layoutInflater = (LayoutInflater) checkNotNull(
                ApplicationProvider.getApplicationContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE));
        ViewPager2 viewPager = (ViewPager2) layoutInflater.inflate(layoutId, null);
        assertThat(viewPager.getOverScrollMode(), equalTo(expectedOverScrollMode));
        assertOverScrollModeSyncWithRecyclerView(viewPager);
    }
}
