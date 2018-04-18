/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class FragmentAdapterActivity extends BaseActivity {
    private static final Random RANDOM = new Random();

    private AtomicInteger mAttachCount = new AtomicInteger(0);
    private AtomicInteger mDestroyCount = new AtomicInteger(0);
    private PageFragment[] mFragments;

    @Override
    protected void setAdapter() {
        mFragments = new PageFragment[mTotalPages];

        ViewPager2.FragmentProvider fragmentProvider = new ViewPager2.FragmentProvider() {
            final boolean[] mWasEverAttached = new boolean[mTotalPages];

            @Override
            public Fragment getItem(final int position) {
                PageFragment fragment = PageFragment.create(valueForPosition(position));

                fragment.mOnAttachListener = new PageFragment.EventListener() {
                    @Override
                    public void onEvent(PageFragment fragment) {
                        mAttachCount.incrementAndGet();
                        mWasEverAttached[position] = true;
                    }
                };

                fragment.mOnDestroyListener = new PageFragment.EventListener() {
                    @Override
                    public void onEvent(PageFragment fragment) {
                        mDestroyCount.incrementAndGet();
                    }
                };

                return mFragments[position] = fragment;
            }

            private int valueForPosition(int position) {
                // only supply correct value ones; then rely on it being kept by Fragment state
                return mWasEverAttached[position]
                        ? RANDOM.nextInt() // junk value to be overridden by state saved value
                        : position;
            }

            @Override
            public int getCount() {
                return mTotalPages;
            }
        };

        mViewPager.setAdapter(getSupportFragmentManager(), fragmentProvider,
                ViewPager2.FragmentRetentionPolicy.SAVE_STATE);
    }

    @Override
    public void updatePage(int pageIx, int newValue) {
        mFragments[pageIx].updateValue(newValue);
    }

    @Override
    public void validateState() {
        assertThat(mAttachCount.get() - mDestroyCount.get(),
                allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(4)));
    }
}
