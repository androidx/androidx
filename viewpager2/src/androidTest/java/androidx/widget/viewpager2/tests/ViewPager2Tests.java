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
import static android.view.View.OVER_SCROLL_NEVER;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    // allows to wait until swipe operation is finished (Smooth Scroller done)
    private CountDownLatch mStableAfterSwipe;

    public ViewPager2Tests() {
        mActivityTestRule = new ActivityTestRule<>(TestActivity.class);
    }

    @Before
    public void setUp() {
        mViewPager = mActivityTestRule.getActivity().findViewById(R.id.view_pager);

        mViewPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                // coming to idle from another state (dragging or setting) means we're stable now
                if (newState == SCROLL_STATE_IDLE) {
                    mStableAfterSwipe.countDown();
                }
            }
        });
    }

    /**
     * Responsible for setting an adapter on a target {@link ViewPager2}.
     *
     * Allows for parameterization of a test (so it can run with different adapters). Adapter
     * setters can vary by signature hence the overly flexible signature of this interface.
     */
    private interface AdapterStrategy {
        void apply(Activity activity, ViewPager2 target);
    }

    private static class ViewAdapterStrategy implements AdapterStrategy {
        @Override
        public void apply(final Activity activity, ViewPager2 target) {
            target.setAdapter(
                    new Adapter<ViewHolder>() {
                        @NonNull
                        @Override
                        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                int viewType) {
                            return new ViewHolder(
                                    activity.getLayoutInflater().inflate(
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
                            return sColors.length;
                        }
                    });
        }
    }

    private static class FragmentAdapterStrategy implements AdapterStrategy {
        @Override
        public void apply(Activity activity, ViewPager2 target) {
            target.setAdapter(((FragmentActivity) activity).getSupportFragmentManager(),
                    new ViewPager2.FragmentProvider() {
                        @Override
                        public Fragment getItem(int position) {
                            return new PageFragment(position);
                        }

                        @Override
                        public int getCount() {
                            return sColors.length;
                        }
                    }, ViewPager2.FragmentRetentionPolicy.ALWAYS_RECREATE);
        }

        public static class PageFragment extends Fragment {
            private final int mPosition;

            PageFragment(int position) {
                mPosition = position;
            }

            @NonNull
            @Override
            public View onCreateView(@NonNull LayoutInflater inflater,
                    @Nullable ViewGroup container,
                    @Nullable Bundle savedInstanceState) {
                View result = inflater.inflate(R.layout.item_test_layout,
                        container, false);
                TextView textView = result.findViewById(R.id.text_view);
                textView.setText(String.valueOf(mPosition));
                textView.setBackgroundColor(sColors[mPosition]);
                return result;
            }
        }
    }

    @Test
    public void rendersAndHandlesSwipingViewAdapter() throws Throwable {
        rendersAndHandlesSwiping(new ViewAdapterStrategy());
    }

    @Test
    public void rendersAndHandlesSwipingFragmentAdapter() throws Throwable {
        rendersAndHandlesSwiping(new FragmentAdapterStrategy());
        // TODO: test Fragment lifecycle
    }

    public void rendersAndHandlesSwiping(final AdapterStrategy adapterStrategy) throws Throwable {
        final int pageCount = sColors.length;

        if (Build.VERSION.SDK_INT < 16) { // TODO(b/71500143): remove temporary workaround
            RecyclerView mRecyclerView = (RecyclerView) mViewPager.getChildAt(0);
            mRecyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        }

        onView(withId(mViewPager.getId())).check(matches(isDisplayed()));
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapterStrategy.apply(mActivityTestRule.getActivity(), mViewPager);
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
                matches(allOf(withText(String.valueOf(pageNumber)),
                        new BackgroundColorMatcher(pageNumber))));
    }

    private static class BackgroundColorMatcher extends BaseMatcher<View> {
        private final int mPageNumber;

        BackgroundColorMatcher(int pageNumber) {
            mPageNumber = pageNumber;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("should have background color: ").appendValue(
                    sColors[mPageNumber]);
        }

        @Override
        public boolean matches(Object item) {
            ColorDrawable background = (ColorDrawable) ((View) item).getBackground();
            return background.getColor() == sColors[mPageNumber];
        }
    }

    private void performSwipe(ViewAction swipeAction) throws InterruptedException {
        mStableAfterSwipe = new CountDownLatch(1);
        onView(allOf(isDisplayed(), withId(R.id.text_view))).perform(swipeAction);
        mStableAfterSwipe.await(1, TimeUnit.SECONDS);
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
