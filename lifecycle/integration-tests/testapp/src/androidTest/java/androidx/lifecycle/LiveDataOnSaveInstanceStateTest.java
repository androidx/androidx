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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.testapp.CollectingSupportActivity;
import androidx.lifecycle.testapp.CollectingSupportFragment;
import androidx.lifecycle.testapp.NavigationDialogActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LiveDataOnSaveInstanceStateTest {
    @Rule
    public ActivityTestRule<CollectingSupportActivity> mActivityTestRule =
            new ActivityTestRule<>(CollectingSupportActivity.class);

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    public void liveData_partiallyObscuredActivity_maxSdkM() throws Throwable {
        CollectingSupportActivity activity = mActivityTestRule.getActivity();

        liveData_partiallyObscuredLifecycleOwner_maxSdkM(activity);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    public void liveData_partiallyObscuredActivityWithFragment_maxSdkM() throws Throwable {
        CollectingSupportActivity activity = mActivityTestRule.getActivity();
        CollectingSupportFragment fragment = new CollectingSupportFragment();
        mActivityTestRule.runOnUiThread(() -> activity.replaceFragment(fragment));

        liveData_partiallyObscuredLifecycleOwner_maxSdkM(fragment);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    public void liveData_partiallyObscuredActivityFragmentInFragment_maxSdkM() throws Throwable {
        CollectingSupportActivity activity = mActivityTestRule.getActivity();
        CollectingSupportFragment fragment = new CollectingSupportFragment();
        CollectingSupportFragment fragment2 = new CollectingSupportFragment();
        mActivityTestRule.runOnUiThread(() -> {
            activity.replaceFragment(fragment);
            fragment.replaceFragment(fragment2);
        });

        liveData_partiallyObscuredLifecycleOwner_maxSdkM(fragment2);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void liveData_partiallyObscuredActivity_minSdkN() throws Throwable {
        CollectingSupportActivity activity = mActivityTestRule.getActivity();

        liveData_partiallyObscuredLifecycleOwner_minSdkN(activity);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void liveData_partiallyObscuredActivityWithFragment_minSdkN() throws Throwable {
        CollectingSupportActivity activity = mActivityTestRule.getActivity();
        CollectingSupportFragment fragment = new CollectingSupportFragment();
        mActivityTestRule.runOnUiThread(() -> activity.replaceFragment(fragment));

        liveData_partiallyObscuredLifecycleOwner_minSdkN(fragment);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void liveData_partiallyObscuredActivityFragmentInFragment_minSdkN() throws Throwable {
        CollectingSupportActivity activity = mActivityTestRule.getActivity();
        CollectingSupportFragment fragment = new CollectingSupportFragment();
        CollectingSupportFragment fragment2 = new CollectingSupportFragment();
        mActivityTestRule.runOnUiThread(() -> {
            activity.replaceFragment(fragment);
            fragment.replaceFragment(fragment2);
        });

        liveData_partiallyObscuredLifecycleOwner_minSdkN(fragment2);
    }

    private void liveData_partiallyObscuredLifecycleOwner_maxSdkM(LifecycleOwner lifecycleOwner)
            throws Throwable {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        MutableLiveData<Integer> mutableLiveData = new MutableLiveData<>();
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(0));

        TestUtils.waitTillResumed(lifecycleOwner, mActivityTestRule);
        mActivityTestRule.runOnUiThread(() ->
                mutableLiveData.observe(lifecycleOwner, atomicInteger::set));

        final FragmentActivity dialogActivity = launchDialog();

        TestUtils.waitTillCreated(lifecycleOwner, mActivityTestRule);

        // Change the LiveData value and assert that the observer is not called given that the
        // lifecycle is in the CREATED state.
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(1));
        assertThat(atomicInteger.get(), is(0));

        // Finish the dialog Activity, wait for the main activity to be resumed, and assert that
        // the observer's onChanged method is called.
        mActivityTestRule.runOnUiThread(dialogActivity::finish);
        TestUtils.waitTillResumed(lifecycleOwner, mActivityTestRule);
        assertThat(atomicInteger.get(), is(1));
    }

    private void liveData_partiallyObscuredLifecycleOwner_minSdkN(LifecycleOwner lifecycleOwner)
            throws Throwable {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        MutableLiveData<Integer> mutableLiveData = new MutableLiveData<>();
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(0));

        TestUtils.waitTillResumed(lifecycleOwner, mActivityTestRule);

        mActivityTestRule.runOnUiThread(() ->
                mutableLiveData.observe(lifecycleOwner, atomicInteger::set));

        // Launch the NavigationDialogActivity, partially obscuring the activity, and wait for the
        // lifecycleOwner to hit onPause (or enter the STARTED state).  On API 24 and above, this
        // onPause should be the last lifecycle method called (and the STARTED state should be the
        // final resting state).
        launchDialog();
        TestUtils.waitTillStarted(lifecycleOwner, mActivityTestRule);

        // Change the LiveData's value and verify that the observer's onChanged method is called
        // since we are in the STARTED state.
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(1));
        assertThat(atomicInteger.get(), is(1));
    }

    private FragmentActivity launchDialog() throws Throwable {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                NavigationDialogActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        FragmentActivity activity = mActivityTestRule.getActivity();
        // helps with less flaky API 16 tests
        Intent intent = new Intent(activity, NavigationDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        FragmentActivity fragmentActivity = (FragmentActivity) monitor.waitForActivity();
        TestUtils.waitTillResumed(fragmentActivity, mActivityTestRule);
        return fragmentActivity;
    }
}
