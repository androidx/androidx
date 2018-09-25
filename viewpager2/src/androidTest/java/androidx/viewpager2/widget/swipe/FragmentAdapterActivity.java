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
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.concurrent.atomic.AtomicInteger;

public class FragmentAdapterActivity extends BaseActivity {
    private AtomicInteger mAttachCount = new AtomicInteger(0);
    private AtomicInteger mDestroyCount = new AtomicInteger(0);
    private PageFragment[] mFragments;

    @Override
    protected void setAdapter() {
        mFragments = new PageFragment[mTotalPages];

        mViewPager.setAdapter(
                new FragmentStateAdapter(getSupportFragmentManager()) {
                    @Override
                    public int getItemCount() {
                        return mTotalPages;
                    }

                    @Override
                    public Fragment getItem(int position) {
                        PageFragment fragment = PageFragment.create(position);

                        fragment.mOnAttachListener = new PageFragment.EventListener() {
                            @Override
                            public void onEvent(PageFragment fragment) {
                                mAttachCount.incrementAndGet();
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
                });
    }

    @Override
    public void updatePageContent(int pageIx, int newValue) {
        mFragments[pageIx].updateValue(newValue);
    }

    @Override
    public void validateState() {
        assertThat(mAttachCount.get() - mDestroyCount.get(),
                allOf(greaterThanOrEqualTo(1), lessThanOrEqualTo(4)));
    }
}
