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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import org.junit.Ignore;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.testapp.CollectingLifecycleOwner;
import androidx.lifecycle.testapp.CollectingSupportActivity;
import androidx.lifecycle.testapp.CollectingSupportFragment;
import androidx.lifecycle.testapp.NavigationDialogActivity;
import androidx.lifecycle.testapp.TestEvent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import kotlin.Pair;

/**
 * Runs tests about the state when an activity is partially covered by another activity. Pre
 * API 24, framework behavior changes so the test rely on whether state is saved or not and makes
 * assertions accordingly.
 */
@SuppressWarnings("unchecked")
@SdkSuppress(maxSdkVersion = 28) // framework issue for API 29: b/142125019
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
                    Intent intent = new Intent(ApplicationProvider.getApplicationContext(),
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
    @Ignore("b/173596006")
    public void coveredWithDialog_fragment() throws Throwable {
        CollectingSupportFragment fragment = new CollectingSupportFragment();
        activityRule.runOnUiThread(() -> activityRule.getActivity().replaceFragment(fragment));
        runTest(fragment);
    }

    @FlakyTest(bugId = 173596006)
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
        assertThat(owner.copyCollectedEvents()).isEqualTo(EXPECTED[0]);
        List<Pair<TestEvent, Lifecycle.Event>> expected;
        if (mDismissDialog) {
            dialog.finish();
            TestUtils.waitTillResumed(activityRule.getActivity(), activityRule);
            assertThat(owner.copyCollectedEvents()).isEqualTo(flatMap(EXPECTED[0], EXPECTED[1]));
            expected = flatMap(EXPECTED[0], EXPECTED[1], EXPECTED[2]);
        } else {
            expected = flatMap(CREATE, START, RESUME, PAUSE, STOP, DESTROY);
        }
        CollectingSupportActivity activity = activityRule.getActivity();
        activityRule.finishActivity();
        TestUtils.waitTillDestroyed(activity, activityRule);
        assertThat(owner.copyCollectedEvents()).isEqualTo(expected);
    }

    // test sanity
    private void assertStateSaving() throws InterruptedException {
        final CollectingSupportActivity activity = activityRule.getActivity();
        if (sShouldSave) {
            // state should be saved. wait for it to be saved
            assertWithMessage("activity failed to call saveInstanceState")
                    .that(activity.waitForStateSave(20)).isTrue();
            assertWithMessage("the state should have been saved")
                    .that(activity.getSupportFragmentManager().isStateSaved()).isTrue();
        } else {
            // should should not be saved
            assertWithMessage("the state should not be saved")
                    .that(activity.getSupportFragmentManager().isStateSaved()).isFalse();
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
