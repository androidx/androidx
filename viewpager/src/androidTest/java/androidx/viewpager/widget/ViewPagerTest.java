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

package androidx.viewpager.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Rule;
import org.junit.Test;

@MediumTest
public final class ViewPagerTest {
    @Rule
    public final ActivityTestRule<ViewPagerActivity> activityRule = new ActivityTestRule<>(
            ViewPagerActivity.class);

    public static final class ViewPagerActivity extends Activity {
        public ViewPager pager;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            pager = new ViewPager(this);
            setContentView(pager);
        }
    }

    @Test
    public void setPrimaryItemNotCalledWhenAdapterIsEmpty() {
        ViewPager pager = activityRule.getActivity().pager;
        final PrimaryItemPagerAdapter adapter = new PrimaryItemPagerAdapter();
        pager.setAdapter(adapter);

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertFalse(adapter.primaryCalled);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                adapter.count = 1;
                adapter.notifyDataSetChanged();
            }
        });

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        assertTrue(adapter.primaryCalled);
    }

    static final class PrimaryItemPagerAdapter extends PagerAdapter {
        public volatile int count;
        public volatile boolean primaryCalled;

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            primaryCalled = true;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view = new View(container.getContext());
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position,
                @NonNull Object object) {
            container.removeView((View) object);
        }
    }
}
