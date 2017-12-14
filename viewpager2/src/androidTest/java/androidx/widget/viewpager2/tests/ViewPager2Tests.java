/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.widget.viewpager2.tests;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_IDLE;
import static android.support.v7.widget.RecyclerView.SCROLL_STATE_SETTLING;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.idling.CountingIdlingResource;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import androidx.widget.ViewPager2;
import androidx.widget.viewpager2.test.R;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ViewPager2Tests {
    private static final int[] sColors = {
            Color.parseColor("#BBA9FF00"),
            Color.parseColor("#BB00E87E"),
            Color.parseColor("#BB00C7FF"),
            Color.parseColor("#BBB30CE8"),
            Color.parseColor("#BBFF00D0")};

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule;
    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    private ViewPager2 mViewPager;
    private CountingIdlingResource mIdlingResource;

    public ViewPager2Tests() {
        mActivityTestRule = new ActivityTestRule<>(TestActivity.class);
    }

    @Before
    public void setUp() {
        mViewPager = mActivityTestRule.getActivity().findViewById(R.id.view_pager);

        mIdlingResource = new CountingIdlingResource(getClass().getSimpleName() + "IdlingResource");
        mViewPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == SCROLL_STATE_IDLE && !mIdlingResource.isIdleNow()) {
                    mIdlingResource.decrement();
                } else if (newState == SCROLL_STATE_SETTLING && mIdlingResource.isIdleNow()) {
                    mIdlingResource.increment();
                }
            }
        });
        IdlingRegistry.getInstance().register(mIdlingResource);
    }

    @After
    public void tearDown() {
        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    @Test
    public void rendersAndHandlesSwiping() throws Throwable {
        final int pageCount = sColors.length;

        onView(withId(mViewPager.getId())).check(matches(isDisplayed()));
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mViewPager.setAdapter(
                        new Adapter<ViewHolder>() {
                            @NonNull
                            @Override
                            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                    int viewType) {
                                return new ViewHolder(
                                        mActivityTestRule.getActivity().getLayoutInflater().inflate(
                                                R.layout.item_test_layout, parent, false)) {
                                };
                            }

                            @Override
                            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                                TextView view = (TextView) holder.itemView;
                                view.setText(String.valueOf(position));
                                view.setBackgroundColor(sColors[position]);
                            }

                            @Override
                            public int getItemCount() {
                                return pageCount;
                            }
                        });
            }
        });

        final int pageIxFirst = 0;
        final int pageIxLast = pageCount - 1;
        final int swipeCount = pageCount + 1; // two swipes beyond edge to test 'edge behavior'
        int pageNumber = pageIxFirst;
        for (int i = 0; i < swipeCount; i++, pageNumber = Math.min(pageIxLast, ++pageNumber)) {
            verifyView(pageNumber);
            performSwipe(ViewActions.swipeLeft());
        }
        assertThat(pageNumber, equalTo(pageIxLast));
        for (int i = 0; i < swipeCount; i++, pageNumber = Math.max(pageIxFirst, --pageNumber)) {
            verifyView(pageNumber);
            performSwipe(ViewActions.swipeRight());
        }
        assertThat(pageNumber, equalTo(pageIxFirst));
    }

    private void verifyView(int pageNumber) {
        onView(allOf(withId(R.id.text_view), isDisplayed())).check(
                matches(withText(String.valueOf(pageNumber))));
    }

    private void performSwipe(ViewAction swipeAction) throws InterruptedException {
        onView(allOf(isDisplayed(), withId(R.id.text_view))).perform(swipeAction);
    }

    @Test
    public void itemViewSizeMatchParentEnforced() {
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage(
                "Item's root view must fill the whole ViewPager2 (use match_parent)");

        ViewPager2 viewPager = new ViewPager2(InstrumentationRegistry.getContext());
        viewPager.setAdapter(new Adapter<ViewHolder>() {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = new View(parent.getContext());
                view.setLayoutParams(new ViewGroup.LayoutParams(50, 50)); // arbitrary fixed size
                return new ViewHolder(view) {
                };
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                // do nothing
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });

        viewPager.measure(0, 0); // equivalent of unspecified
    }

    @Test
    public void childrenNotAllowed() throws Exception {
        mExpectedException.expect(IllegalStateException.class);
        mExpectedException.expectMessage("ViewPager2 does not support direct child views");

        Context context = InstrumentationRegistry.getContext();
        ViewPager2 viewPager = new ViewPager2(context);
        viewPager.addView(new View(context));
    }

    // TODO: verify correct padding behavior
    // TODO: add test for screen orientation change
}
