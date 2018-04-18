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

package androidx.lifecycle;

import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;
import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;
import static androidx.lifecycle.TestUtils.OrderedTuples.CREATE;
import static androidx.lifecycle.TestUtils.OrderedTuples.DESTROY;
import static androidx.lifecycle.TestUtils.OrderedTuples.PAUSE;
import static androidx.lifecycle.TestUtils.OrderedTuples.RESUME;
import static androidx.lifecycle.TestUtils.OrderedTuples.START;
import static androidx.lifecycle.TestUtils.OrderedTuples.STOP;
import static androidx.lifecycle.TestUtils.flatMap;
import static androidx.lifecycle.testapp.TestEvent.LIFECYCLE_EVENT;
import static androidx.lifecycle.testapp.TestEvent.OWNER_CALLBACK;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;

import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.testapp.CollectingLifecycleOwner;
import androidx.lifecycle.testapp.CollectingSupportActivity;
import androidx.lifecycle.testapp.CollectingSupportFragment;
import androidx.lifecycle.testapp.NavigationDialogActivity;
import androidx.lifecycle.testapp.TestEvent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Runs tests about the state when an activity is partially covered by another activity. Pre
 * API 24, framework behavior changes so the test rely on whether state is saved or not and makes
 * assertions accordingly.
 */
@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
@LargeTest
public class PartiallyCoveredActivityTest {
    private static final List[] IF_SAVED = new List[]{
            // when overlaid
            flatMap(CREATE, START, RESUME, PAUSE,
                    singletonList(new Pair<>(LIFECYCLE_EVENT, ON_STOP))),
            // post dialog dismiss
            asList(new Pair<>(OWNER_CALLBACK, ON_RESUME),
                    new Pair<>(LIFECYCLE_EVENT, ON_START),
                    new Pair<>(LIFECYCLE_EVENT, ON_RESUME)),
            // post finish
            flatMap(PAUSE, STOP, DESTROY)};

    private static final List[] IF_NOT_SAVED = new List[]{
            // when overlaid
            flatMap(CREATE, START, RESUME, PAUSE),
            // post dialog dismiss
            flatMap(RESUME),
            // post finish
            flatMap(PAUSE, STOP, DESTROY)};

    private static final boolean sShouldSave = Build.VERSION.SDK_INT < Build.VERSION_CODES.N;
    private static final List<Pair<TestEvent, Lifecycle.Event>>[] EXPECTED =
            sShouldSave ? IF_SAVED : IF_NOT_SAVED;

    @Rule
    public ActivityTestRule<CollectingSupportActivity> activityRule =
            new ActivityTestRule<CollectingSupportActivity>(
                    CollectingSupportActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    // helps with less flaky API 16 tests
                    Intent intent = new Intent(InstrumentationRegistry.getTargetContext(),
                            CollectingSupportActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    return intent;
                }
            };
    private final boolean mDismissDialog;

    @Parameterized.Parameters(name = "dismissDialog_{0}")
    public static List<Boolean> dismissDialog() {
        return asList(true, false);
    }

    public PartiallyCoveredActivityTest(boolean dismissDialog) {
        mDismissDialog = dismissDialog;
    }

    @Test
    public void coveredWithDialog_activity() throws Throwable {
        final CollectingSupportActivity activity = activityRule.getActivity();
        runTest(activity);
    }

    @Test
    public void coveredWithDialog_fragment() throws Throwable {
        CollectingSupportFragment fragment = new CollectingSupportFragment();
        activityRule.runOnUiThread(() -> activityRule.getActivity().replaceFragment(fragment));
        runTest(fragment);
    }

    @Test
    public void coveredWithDialog_childFragment() throws Throwable {
        CollectingSupportFragment parentFragment = new CollectingSupportFragment();
        CollectingSupportFragment childFragment = new CollectingSupportFragment();
        activityRule.runOnUiThread(() -> {
            activityRule.getActivity().replaceFragment(parentFragment);
            parentFragment.replaceFragment(childFragment);
        });
        runTest(childFragment);
    }

    private void runTest(CollectingLifecycleOwner owner) throws Throwable {
        TestUtils.waitTillResumed(owner, activityRule);
        FragmentActivity dialog = launchDialog();
        assertStateSaving();
        waitForIdle();
        assertThat(owner.copyCollectedEvents(), is(EXPECTED[0]));
        List<Pair<TestEvent, Lifecycle.Event>> expected;
        if (mDismissDialog) {
            dialog.finish();
            TestUtils.waitTillResumed(activityRule.getActivity(), activityRule);
            assertThat(owner.copyCollectedEvents(), is(flatMap(EXPECTED[0], EXPECTED[1])));
            expected = flatMap(EXPECTED[0], EXPECTED[1], EXPECTED[2]);
        } else {
            expected = flatMap(CREATE, START, RESUME, PAUSE, STOP, DESTROY);
        }
        CollectingSupportActivity activity = activityRule.getActivity();
        activityRule.finishActivity();
        TestUtils.waitTillDestroyed(activity, activityRule);
        assertThat(owner.copyCollectedEvents(), is(expected));
    }

    // test sanity
    private void assertStateSaving() throws ExecutionException, InterruptedException {
        final CollectingSupportActivity activity = activityRule.getActivity();
        if (sShouldSave) {
            // state should be saved. wait for it to be saved
            assertThat("test sanity",
                    activity.waitForStateSave(20), is(true));
            assertThat("test sanity", activity.getSupportFragmentManager()
                    .isStateSaved(), is(true));
        } else {
            // should should not be saved
            assertThat("test sanity", activity.getSupportFragmentManager()
                    .isStateSaved(), is(false));
        }
    }

    private void waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private FragmentActivity launchDialog() throws Throwable {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                NavigationDialogActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        FragmentActivity activity = activityRule.getActivity();

        Intent intent = new Intent(activity, NavigationDialogActivity.class);
        // disabling animations helps with less flaky API 16 tests
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        FragmentActivity fragmentActivity = (FragmentActivity) monitor.waitForActivity();
        TestUtils.waitTillResumed(fragmentActivity, activityRule);
        return fragmentActivity;
    }
}
