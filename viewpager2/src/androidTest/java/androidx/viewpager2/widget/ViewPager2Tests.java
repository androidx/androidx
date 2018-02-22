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

package androidx.viewpager2.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.view.View.OVER_SCROLL_NEVER;

import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.testutils.FragmentActivityUtils;
import androidx.viewpager2.test.R;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ViewPager2Tests {
    private static final Random RANDOM = new Random();
    private static final int[] sColors = {
            Color.parseColor("#BBA9FF00"),
            Color.parseColor("#BB00E87E"),
            Color.parseColor("#BB00C7FF"),
            Color.parseColor("#BBB30CE8"),
            Color.parseColor("#BBFF00D0")};

    /** mean of injecting different adapters into {@link TestActivity#onCreate(Bundle)} */
    static AdapterStrategy sAdapterStrategy;
    interface AdapterStrategy {
        void setAdapter(ViewPager2 viewPager);
    }

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule;
    @Rule
    public ExpectedException mExpectedException = ExpectedException.none();

    // allows to wait until swipe operation is finished (Smooth Scroller done)
    private CountDownLatch mStableAfterSwipe;

    public ViewPager2Tests() {
        mActivityTestRule = new ActivityTestRule<>(TestActivity.class, true, false);
    }

    private void setUpActivity(AdapterStrategy adapterStrategy) {
        sAdapterStrategy = Preconditions.checkNotNull(adapterStrategy);
        mActivityTestRule.launchActivity(null);

        ViewPager2 viewPager = mActivityTestRule.getActivity().findViewById(R.id.view_pager);
        viewPager.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                // coming to idle from another state (dragging or setting) means we're stable now
                if (newState == SCROLL_STATE_IDLE) {
                    mStableAfterSwipe.countDown();
                }
            }
        });

        if (Build.VERSION.SDK_INT < 16) { // TODO(b/71500143): remove temporary workaround
            RecyclerView mRecyclerView = (RecyclerView) viewPager.getChildAt(0);
            mRecyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        }

        onView(withId(viewPager.getId())).check(matches(isDisplayed()));
    }

    @Before
    public void setUp() {
        sAdapterStrategy = null;

        final long seed = RANDOM.nextLong();
        RANDOM.setSeed(seed);
        Log.i(getClass().getName(), "Random seed: " + seed);
    }

    public static class PageFragment extends Fragment {
        private static final String KEY_VALUE = "value";

        public interface EventListener {
            void onEvent(PageFragment fragment);

            EventListener NO_OP = new EventListener() {
                @Override
                public void onEvent(PageFragment fragment) {
                    // do nothing
                }
            };
        }

        private EventListener mOnAttachListener = EventListener.NO_OP;
        private EventListener mOnDestroyListener = EventListener.NO_OP;

        private int mPosition;
        private int mValue;

        public static PageFragment create(int position, int value) {
            PageFragment result = new PageFragment();
            Bundle args = new Bundle(1);
            args.putInt(KEY_VALUE, value);
            result.setArguments(args);
            result.mPosition = position;
            return result;
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            mOnAttachListener.onEvent(this);
        }

        @NonNull
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.item_test_layout, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            Bundle data = savedInstanceState != null ? savedInstanceState : getArguments();
            setValue(data.getInt(KEY_VALUE));
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mOnDestroyListener.onEvent(this);
        }

        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
            outState.putInt(KEY_VALUE, mValue);
        }

        public void setValue(int value) {
            mValue = value;
            TextView textView = getView().findViewById(R.id.text_view);
            applyViewValue(textView, mValue);
        }
    }

    private static void applyViewValue(TextView textView, int value) {
        textView.setText(String.valueOf(value));
        textView.setBackgroundColor(getColor(value));
    }

    private static int getColor(int value) {
        return sColors[value % sColors.length];
    }

    @Test
    public void fragmentAdapter_fullPass() throws Throwable {
        testFragmentLifecycle(8, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0));
    }

    @Test
    public void fragmentAdapter_random() throws Throwable {
        final int totalPages = 8; // increase when stress testing locally
        final int sequenceLength = 20; // increase when stress testing locally
        testFragmentLifecycle_random(totalPages, sequenceLength, PageMutator.NO_OP);
    }

    @Test
    public void fragmentAdapter_random_withMutations() throws Throwable {
        final int totalPages = 8; // increase when stress testing locally
        final int sequenceLength = 20; // increase when stress testing locally
        testFragmentLifecycle_random(totalPages, sequenceLength, PageMutator.RANDOM);
    }

    private void testFragmentLifecycle_random(int totalPages, int sequenceLength,
            PageMutator pageMutator) throws Throwable {
        List<Integer> pageSequence = generateRandomPageSequence(totalPages, sequenceLength);

        Log.i(getClass().getName(),
                String.format("Testing with a sequence [%s]", TextUtils.join(", ", pageSequence)));

        testFragmentLifecycle(totalPages, pageSequence, pageMutator);
    }

    @NonNull
    private List<Integer> generateRandomPageSequence(int totalPages, int sequenceLength) {
        List<Integer> pageSequence = new ArrayList<>(sequenceLength);

        int pageIx = 0;
        Double goRightProbability = null;
        while (pageSequence.size() != sequenceLength) {
            boolean goRight;
            if (pageIx == 0) {
                goRight = true;
                goRightProbability = 0.7;
            } else if (pageIx == totalPages - 1) { // last page
                goRight = false;
                goRightProbability = 0.3;
            } else {
                goRight = RANDOM.nextDouble() < goRightProbability;
            }

            pageSequence.add(goRight ? ++pageIx : --pageIx);
        }

        return pageSequence;
    }

    /**
     * Test added when caught a bug: after the last swipe: actual=6, expected=4
     * <p>
     * Bug was caused by an invalid test assumption (new Fragment value can be inferred from number
     * of instances created) - invalid in a case when we sometimes create Fragments off-screen and
     * end up scrapping them.
     **/
    @Test
    public void fragmentAdapter_regression1() throws Throwable {
        testFragmentLifecycle(10, Arrays.asList(1, 2, 3, 2, 1, 2, 3, 4));
    }

    /**
     * Test added when caught a bug: after the last swipe: actual=4, expected=5
     * <p>
     * Bug was caused by mSavedStates.add(position, ...) instead of mSavedStates.set(position, ...)
     **/
    @Test
    public void fragmentAdapter_regression2() throws Throwable {
        testFragmentLifecycle(10, Arrays.asList(1, 2, 3, 4, 3, 2, 1, 2, 3, 4, 5));
    }

    /**
     * Test added when caught a bug: after the last swipe: ArrayIndexOutOfBoundsException: length=5;
     * index=-1 at androidx.viewpager2.widget.tests.ViewPager2Tests$PageFragment.onCreateView
     * <p>
     * Bug was caused by always saving states of unattached fragments as null (even if there was a
     * valid previously saved state)
     */
    @Test
    public void fragmentAdapter_regression3() throws Throwable {
        testFragmentLifecycle(10, Arrays.asList(1, 2, 3, 2, 1, 2, 3, 2, 1, 0));
    }

    /** Goes left on left edge / right on right edge */
    @Test
    public void fragmentAdapter_edges() throws Throwable {
        testFragmentLifecycle(4, Arrays.asList(0, 0, 1, 2, 3, 3, 3, 2, 1, 0, 0, 0));
    }

    private interface PageMutator {
        void mutate(PageFragment fragment);

        PageMutator NO_OP = new PageMutator() {
            @Override
            public void mutate(PageFragment fragment) {
                // do nothing
            }
        };

        /** At random modifies the page under Fragment */
        PageMutator RANDOM = new PageMutator() {
            @Override
            public void mutate(PageFragment fragment) {
                Random random = ViewPager2Tests.RANDOM;
                if (random.nextDouble() < 0.125) {
                    int delta = (1 + random.nextInt(5)) * sColors.length;
                    fragment.setValue(fragment.mValue + delta);
                }
            }
        };
    }

    /** @see this#testFragmentLifecycle(int, List, PageMutator) */
    private void testFragmentLifecycle(final int totalPages, List<Integer> pageSequence)
            throws Throwable {
        testFragmentLifecycle(totalPages, pageSequence, PageMutator.NO_OP);
    }

    /**
     * Verifies:
     * <ul>
     * <li>page content / background
     * <li>maximum number of Fragments held in memory
     * <li>Fragment state saving / restoring
     * </ul>
     */
    private void testFragmentLifecycle(final int totalPages, List<Integer> pageSequence,
            final PageMutator pageMutator) throws Throwable {
        final AtomicInteger attachCount = new AtomicInteger(0);
        final AtomicInteger destroyCount = new AtomicInteger(0);
        final boolean[] wasEverAttached = new boolean[totalPages];
        final PageFragment[] fragments = new PageFragment[totalPages];

        final int[] expectedValues = new int[totalPages];
        for (int i = 0; i < totalPages; i++) {
            expectedValues[i] = i;
        }

        setUpActivity(new AdapterStrategy() {
            @Override
            public void setAdapter(ViewPager2 viewPager) {
                viewPager.setAdapter(
                        ((FragmentActivity) viewPager.getContext()).getSupportFragmentManager(),
                        new ViewPager2.FragmentProvider() {
                            @Override
                            public Fragment getItem(final int position) {
                                // if the fragment was attached in the past, it means we have
                                // provided it with the correct value already; give a dummy one
                                // to prove state save / restore functionality works
                                int value = wasEverAttached[position] ? -1 : position;
                                PageFragment fragment = PageFragment.create(position, value);

                                fragment.mOnAttachListener = new PageFragment.EventListener() {
                                    @Override
                                    public void onEvent(PageFragment fragment) {
                                        attachCount.incrementAndGet();
                                        wasEverAttached[fragment.mPosition] = true;
                                    }
                                };

                                fragment.mOnDestroyListener = new PageFragment.EventListener() {
                                    @Override
                                    public void onEvent(PageFragment fragment) {
                                        destroyCount.incrementAndGet();
                                    }
                                };

                                fragments[position] = fragment;
                                return fragment;
                            }

                            @Override
                            public int getCount() {
                                return totalPages;
                            }
                        }, ViewPager2.FragmentRetentionPolicy.SAVE_STATE);
            }
        });

        final AtomicInteger currentPage = new AtomicInteger(0);
        verifyCurrentPage(expectedValues[currentPage.get()]);
        for (int nextPage : pageSequence) {
            swipe(currentPage.get(), nextPage, totalPages);
            currentPage.set(nextPage);
            verifyCurrentPage(expectedValues[currentPage.get()]);

            // TODO: validate Fragments that are instantiated, but not attached. No destruction
            // steps are done to them - they're just left to the Garbage Collector. Maybe
            // WeakReferences could help, but the GC behaviour is not predictable. Alternatively,
            // we could only create Fragments onAttach, but there is a potential performance
            // trade-off.
            assertThat(attachCount.get() - destroyCount.get(), isBetween(1, 4));

            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int page = currentPage.get();
                    PageFragment fragment = fragments[page];
                    pageMutator.mutate(fragment);
                    expectedValues[page] = fragment.mValue;
                }
            });
        }
    }

    private void swipe(int currentPageIx, int nextPageIx, int totalPages)
            throws InterruptedException {
        if (nextPageIx >= totalPages) {
            throw new IllegalArgumentException("Invalid nextPageIx: >= totalPages.");
        }

        if (currentPageIx == nextPageIx) { // dedicated for testing edge behaviour
            if (nextPageIx == 0) {
                swipeRight(); // bounce off the left edge
                return;
            }
            if (nextPageIx == totalPages - 1) { // bounce off the right edge
                swipeLeft();
                return;
            }
            throw new IllegalArgumentException(
                    "Invalid sequence. Not on an edge, and currentPageIx/nextPageIx pages same.");
        }

        if (Math.abs(nextPageIx - currentPageIx) > 1) {
            throw new IllegalArgumentException(
                    "Specified nextPageIx not adjacent to the current page.");
        }

        if (nextPageIx > currentPageIx) {
            swipeLeft();
        } else {
            swipeRight();
        }
    }

    private Matcher<Integer> isBetween(int min, int max) {
        return allOf(greaterThanOrEqualTo(min), lessThanOrEqualTo(max));
    }

    @Test
    public void viewAdapter_edges() throws Throwable {
        testViewAdapter(4, Arrays.asList(0, 0, 1, 2, 3, 3, 3, 2, 1, 0, 0, 0),
                Collections.<Integer>emptySet());
    }

    @Test
    public void viewAdapter_fullPass() throws Throwable {
        testViewAdapter(8, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0),
                Collections.<Integer>emptySet());
    }

    @Test
    public void viewAdapter_activityRecreation() throws Throwable {
        testViewAdapter(7,
                Arrays.asList(1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0),
                new HashSet<>(Arrays.asList(3, 5, 7)));
    }

    @Test
    public void saveState_parcelWriteRestore() throws Throwable {
        // given
        ViewPager2 viewPager = new ViewPager2(InstrumentationRegistry.getContext());
        assertThat(viewPager.getChildAt(0), instanceOf(RecyclerView.class));
        final int recyclerViewId = viewPager.getChildAt(0).getId();

        // when
        Parcel parcel = Parcel.obtain();
        ViewPager2.SavedState state = (ViewPager2.SavedState) viewPager.onSaveInstanceState();
        //noinspection ConstantConditions
        state.writeToParcel(parcel, 0);
        final String parcelSuffix = UUID.randomUUID().toString();
        parcel.writeString(parcelSuffix); // to verify parcel boundaries
        parcel.setDataPosition(0);
        ViewPager2.SavedState recreatedState = ViewPager2.SavedState.CREATOR.createFromParcel(
                parcel);

        // then
        assertThat("Parcel reading should not go out of bounds", parcel.readString(),
                equalTo(parcelSuffix));
        assertThat("All of the parcel should be read", parcel.dataAvail(), equalTo(0));
        assertThat("Previous RecyclerView id is restored from parcel",
                recreatedState.mRecyclerViewId,
                equalTo(recyclerViewId));
    }

    private void testViewAdapter(final int totalPages, List<Integer> pageSequence,
            Set<Integer> activityRecreateSteps) throws InterruptedException {
        setUpActivity(new AdapterStrategy() {
            @Override
            public void setAdapter(final ViewPager2 viewPager) {
                viewPager.setAdapter(
                        new Adapter<ViewHolder>() {
                            @NonNull
                            @Override
                            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                    int viewType) {
                                return new ViewHolder(
                                        LayoutInflater.from(viewPager.getContext()).inflate(
                                                R.layout.item_test_layout, parent, false)) {
                                };
                            }

                            @Override
                            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                                TextView view = (TextView) holder.itemView;
                                applyViewValue(view, position);
                            }

                            @Override
                            public int getItemCount() {
                                return totalPages;
                            }
                        });
            }
        });

        verifyCurrentPage(0);
        int currentPage = 0;
        int stepCounter = 0;
        for (int nextPage : pageSequence) {
            if (activityRecreateSteps.contains(stepCounter++)) {
                FragmentActivityUtils.recreateActivity(mActivityTestRule,
                        mActivityTestRule.getActivity());
            }
            swipe(currentPage, nextPage, totalPages);
            currentPage = nextPage;
            verifyCurrentPage(currentPage);
        }
    }

    /**
     * Verifies that the current page displays the correct value and has the correct color.
     *
     * @param expectedPageValue value expected to be displayed on the page
     */
    private void verifyCurrentPage(int expectedPageValue) {
        onView(allOf(withId(R.id.text_view), isDisplayed())).check(
                matches(allOf(withText(String.valueOf(expectedPageValue)),
                        new BackgroundColorMatcher(getColor(expectedPageValue)))));
    }

    private static class BackgroundColorMatcher extends BaseMatcher<View> {
        private final int mColor;

        BackgroundColorMatcher(int color) {
            mColor = color;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("should have background color: ").appendValue(mColor);
        }

        @Override
        public boolean matches(Object item) {
            ColorDrawable background = (ColorDrawable) ((View) item).getBackground();
            return background.getColor() == mColor;
        }
    }

    private void swipeLeft() throws InterruptedException {
        performSwipe(ViewActions.swipeLeft());
    }

    private void swipeRight() throws InterruptedException {
        performSwipe(ViewActions.swipeRight());
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
    // TODO: port some of the fragment adapter tests as view adapter tests
}
