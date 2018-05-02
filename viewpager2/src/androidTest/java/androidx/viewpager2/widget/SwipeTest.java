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

package androidx.viewpager2.widget;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.view.View.OVER_SCROLL_NEVER;

import static androidx.viewpager2.widget.ViewPager2.Orientation.HORIZONTAL;
import static androidx.viewpager2.widget.ViewPager2.Orientation.VERTICAL;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.os.Build;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.util.Log;
import android.util.Pair;

import androidx.recyclerview.widget.RecyclerView;
import androidx.testutils.FragmentActivityUtils;
import androidx.viewpager2.test.R;
import androidx.viewpager2.widget.swipe.BaseActivity;
import androidx.viewpager2.widget.swipe.FragmentAdapterActivity;
import androidx.viewpager2.widget.swipe.PageSwiper;
import androidx.viewpager2.widget.swipe.ViewAdapterActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@LargeTest
@RunWith(Parameterized.class)
public class SwipeTest {
    private static final List<Class<? extends BaseActivity>> TEST_ACTIVITIES_ALL = asList(
            ViewAdapterActivity.class, FragmentAdapterActivity.class);
    private static final Set<Integer> NO_CONFIG_CHANGES = Collections.emptySet();
    private static final List<Pair<Integer, Integer>> NO_MUTATIONS = Collections.emptyList();
    private static final boolean RANDOM_PASS_ENABLED = false;

    private final TestConfig mTestConfig;
    private ActivityTestRule<? extends BaseActivity> mActivityTestRule;
    private PageSwiper mSwiper;

    public SwipeTest(TestConfig testConfig) {
        mTestConfig = testConfig;
    }

    @Test
    public void test() throws Throwable {
        BaseActivity activity = mActivityTestRule.getActivity();

        final int[] expectedValues = new int[mTestConfig.mTotalPages];
        for (int i = 0; i < mTestConfig.mTotalPages; i++) {
            expectedValues[i] = i;
        }

        int currentPage = 0, currentStep = 0;
        assertStateCorrect(expectedValues[currentPage], activity);
        for (int nextPage : mTestConfig.mPageSequence) {
            // value change
            if (mTestConfig.mStepToNewValue.containsKey(currentStep)) {
                expectedValues[currentPage] = mTestConfig.mStepToNewValue.get(currentStep);
                updatePage(currentPage, expectedValues[currentPage], activity);
                assertStateCorrect(expectedValues[currentPage], activity);
            }

            // config change
            if (mTestConfig.mConfigChangeSteps.contains(currentStep++)) {
                activity = FragmentActivityUtils.recreateActivity(mActivityTestRule, activity);
                assertStateCorrect(expectedValues[currentPage], activity);
            }

            // page swipe
            mSwiper.swipe(currentPage, nextPage);
            currentPage = nextPage;
            assertStateCorrect(expectedValues[currentPage], activity);
        }
    }

    private static void updatePage(final int pageIx, final int newValue,
            final BaseActivity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.updatePage(pageIx, newValue);
            }
        });
    }

    private void assertStateCorrect(int expectedValue, BaseActivity activity) {
        onView(allOf(withId(R.id.text_view), isDisplayed())).check(
                matches(withText(String.valueOf(expectedValue))));
        activity.validateState();
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<TestConfig> getParams() {
        List<TestConfig> tests = new ArrayList<>();

        if (RANDOM_PASS_ENABLED) { // run locally after making larger changes
            tests.addAll(generateRandomTests());
        }

        for (Class<? extends BaseActivity> activityClass : TEST_ACTIVITIES_ALL) {
            tests.add(new TestConfig("full pass", asList(1, 2, 3, 4, 5, 6, 7, 6, 5, 4, 3, 2, 1, 0),
                    NO_CONFIG_CHANGES, NO_MUTATIONS, 8, activityClass, HORIZONTAL));

            tests.add(new TestConfig("basic vertical", asList(0, 1, 2, 3, 3, 2, 1, 0, 0),
                    NO_CONFIG_CHANGES, NO_MUTATIONS, 4, activityClass, VERTICAL));
            tests.add(new TestConfig("swipe beyond edge pages",
                    asList(0, 0, 1, 2, 3, 3, 3, 2, 1, 0, 0, 0), NO_CONFIG_CHANGES, NO_MUTATIONS, 4,
                    activityClass, HORIZONTAL));

            tests.add(new TestConfig("config change", asList(1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0),
                    asList(3, 5, 7), NO_MUTATIONS, 7, activityClass, HORIZONTAL));

            tests.add(
                    new TestConfig("regression1", asList(1, 2, 3, 2, 1, 2, 3, 4), NO_CONFIG_CHANGES,
                            NO_MUTATIONS, 10, activityClass, HORIZONTAL));

            tests.add(new TestConfig("regression2", asList(1, 2, 3, 4, 3, 2, 1, 2, 3, 4, 5),
                    NO_CONFIG_CHANGES, NO_MUTATIONS, 10, activityClass, HORIZONTAL));

            tests.add(new TestConfig("regression3", asList(1, 2, 3, 2, 1, 2, 3, 2, 1, 0),
                    NO_CONFIG_CHANGES, NO_MUTATIONS, 10, activityClass, HORIZONTAL));
        }

        // mutations only apply to Fragment state persistence
        tests.add(new TestConfig("mutations", asList(1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1, 0),
                singletonList(8),
                asList(Pair.create(0, 999), Pair.create(1, 100), Pair.create(3, 300),
                        Pair.create(5, 500)), 7, FragmentAdapterActivity.class, HORIZONTAL));

        return checkTestNamesUnique(tests);
    }

    private static Collection<TestConfig> generateRandomTests() {
        List<TestConfig> result = new ArrayList<>();

        int id = 0;
        for (int i = 0; i < 10; i++) {
            // both adapters
            for (Class<? extends BaseActivity> adapterClass : TEST_ACTIVITIES_ALL) {
                result.add(createRandomTest(id++, 8, 50, 0, 0, 0.875, adapterClass));
                result.add(createRandomTest(id++, 8, 10, 0.5, 0, 0.875, adapterClass));
            }

            // fragment adapter specific
            result.add(
                    createRandomTest(id++, 8, 50, 0, 0.125, 0.875, FragmentAdapterActivity.class));
            result.add(createRandomTest(id++, 8, 10, 0.5, 0.125, 0.875,
                    FragmentAdapterActivity.class));
        }

        return result;
    }

    /**
     * @param advanceProbability determines the probability of a swipe direction being towards
     *                           the next edge - e.g. if we start from the left, it's the
     *                           probability that the next swipe will go right. <p> Setting it to
     *                           values closer to 0.5 results in a lot of back and forth, while
     *                           setting it closer to 1.0 results in going edge to edge with few
     *                           back-swipes.
     */
    @SuppressWarnings("SameParameterValue")
    private static TestConfig createRandomTest(int id, int totalPages, int sequenceLength,
            double configChangeProbability, double mutationProbability, double advanceProbability,
            Class<? extends BaseActivity> activityClass) {
        Random random = new Random();

        List<Integer> pageSequence = new ArrayList<>();
        List<Integer> configChanges = new ArrayList<>();
        List<Pair<Integer, Integer>> stepToNewValue = new ArrayList<>();

        int pageIx = 0;
        Double goRightProbability = null;
        for (int currentStep = 0; currentStep < sequenceLength; currentStep++) {
            if (random.nextDouble() < configChangeProbability) {
                configChanges.add(currentStep);
            }

            if (random.nextDouble() < mutationProbability) {
                stepToNewValue.add(Pair.create(currentStep, random.nextInt(10_000)));
            }

            boolean goRight;
            if (pageIx == 0) {
                goRight = true;
                goRightProbability = advanceProbability;
            } else if (pageIx == totalPages - 1) { // last page
                goRight = false;
                goRightProbability = 1 - advanceProbability;
            } else {
                goRight = random.nextDouble() < goRightProbability;
            }
            pageSequence.add(goRight ? ++pageIx : --pageIx);
        }

        return new TestConfig("random_" + id, pageSequence, configChanges, stepToNewValue,
                totalPages, activityClass, HORIZONTAL);
    }

    private static List<TestConfig> checkTestNamesUnique(List<TestConfig> configs) {
        Set<String> names = new HashSet<>();
        for (TestConfig config : configs) {
            names.add(config.toString());
        }
        assertThat(names.size(), is(configs.size()));
        return configs;
    }

    @Before
    public void setUp() {
        Log.i(getClass().getSimpleName(), mTestConfig.toFullSpecString());

        mActivityTestRule = new ActivityTestRule<>(mTestConfig.mActivityClass, true, false);
        mActivityTestRule.launchActivity(BaseActivity.createIntent(mTestConfig.mTotalPages));

        final ViewPager2 viewPager = mActivityTestRule.getActivity().findViewById(R.id.view_pager);
        RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0); // HACK
        mSwiper = new PageSwiper(mTestConfig.mTotalPages, recyclerView, mTestConfig.mOrientation);

        mActivityTestRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewPager.setOrientation(mTestConfig.mOrientation);
            }
        });

        // Disabling edge animations on API < 16. Espresso discourages animations altogether, but
        // keeping them for now where they work - as closer to the real environment.
        if (Build.VERSION.SDK_INT < 16) {
            recyclerView.setOverScrollMode(OVER_SCROLL_NEVER);
        }

        onView(withId(R.id.view_pager)).check(matches(isDisplayed()));
    }

    private static class TestConfig {
        final String mMessage;
        final List<Integer> mPageSequence;
        final Set<Integer> mConfigChangeSteps;
        /** {@link Map.Entry#getKey()} = step, {@link Map.Entry#getValue()} = new value */
        final Map<Integer, Integer> mStepToNewValue;
        final int mTotalPages;
        final Class<? extends BaseActivity> mActivityClass;
        final @ViewPager2.Orientation int mOrientation;

        /**
         * @param stepToNewValue {@link Pair#first} = step, {@link Pair#second} = new value
         */
        TestConfig(String message, List<Integer> pageSequence,
                Collection<Integer> configChangeSteps,
                List<Pair<Integer, Integer>> stepToNewValue,
                int totalPages,
                Class<? extends BaseActivity> activityClass,
                int orientation) {
            mMessage = message;
            mPageSequence = pageSequence;
            mConfigChangeSteps = new HashSet<>(configChangeSteps);
            mStepToNewValue = mapFromPairList(stepToNewValue);
            mTotalPages = totalPages;
            mActivityClass = activityClass;
            mOrientation = orientation;
        }

        private static Map<Integer, Integer> mapFromPairList(List<Pair<Integer, Integer>> list) {
            Map<Integer, Integer> result = new HashMap<>();
            for (Pair<Integer, Integer> pair : list) {
                Integer prevValueAtKey = result.put(pair.first, pair.second);
                assertThat("there should be only one value defined for a key", prevValueAtKey,
                        equalTo(null));
            }
            return result;
        }

        @Override
        public String toString() {
            return mActivityClass.getSimpleName() + ": " + mMessage;
        }

        String toFullSpecString() {
            return String.format(
                    "Test: %s\nPage sequence: %s\nTotal pages: %s\nMutations {step1:newValue1, "
                            + "step2:newValue2, ...}: %s",
                    toString(),
                    mPageSequence,
                    mTotalPages,
                    mStepToNewValue.toString().replace('=', ':'));
        }
    }
}
