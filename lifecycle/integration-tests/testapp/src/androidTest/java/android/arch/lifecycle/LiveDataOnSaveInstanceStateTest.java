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

package android.arch.lifecycle;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.arch.lifecycle.testapp.LifecycleObservableActivity;
import android.arch.lifecycle.testapp.LifecycleObservableFragment;
import android.arch.lifecycle.testapp.NavigationDialogActivity;
import android.arch.lifecycle.testapp.OnSaveInstanceStateObservable;
import android.content.Intent;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class LiveDataOnSaveInstanceStateTest {
    @Rule
    public ActivityTestRule<LifecycleObservableActivity> mActivityTestRule =
            new ActivityTestRule<>(LifecycleObservableActivity.class);

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    public void liveData_partiallyObscuredActivity_maxSdkM() throws Throwable {
        LifecycleObservableActivity activity = mActivityTestRule.getActivity();

        liveData_partiallyObscuredLifecycleOwner_maxSdkM(activity, activity, activity);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    public void liveData_partiallyObscuredActivityWithFragment_maxSdkM() throws Throwable {
        LifecycleObservableActivity activity = mActivityTestRule.getActivity();
        LifecycleObservableFragment fragment = new LifecycleObservableFragment();
        mActivityTestRule.runOnUiThread(() -> activity.replaceFragment(fragment));

        liveData_partiallyObscuredLifecycleOwner_maxSdkM(activity, fragment, fragment);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    public void liveData_partiallyObscuredActivityFragmentInFragment_maxSdkM() throws Throwable {
        LifecycleObservableActivity activity = mActivityTestRule.getActivity();
        LifecycleObservableFragment fragment = new LifecycleObservableFragment();
        LifecycleObservableFragment fragment2 = new LifecycleObservableFragment();
        mActivityTestRule.runOnUiThread(() -> {
            activity.replaceFragment(fragment);
            fragment.replaceFragment(fragment2);
        });

        liveData_partiallyObscuredLifecycleOwner_maxSdkM(activity, fragment2, fragment2);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void liveData_partiallyObscuredActivity_minSdkN() throws Throwable {
        LifecycleObservableActivity activity = mActivityTestRule.getActivity();

        liveData_partiallyObscuredLifecycleOwner_minSdkN(activity, activity);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void liveData_partiallyObscuredActivityWithFragment_minSdkN() throws Throwable {
        LifecycleObservableActivity activity = mActivityTestRule.getActivity();
        LifecycleObservableFragment fragment = new LifecycleObservableFragment();
        mActivityTestRule.runOnUiThread(() -> activity.replaceFragment(fragment));

        liveData_partiallyObscuredLifecycleOwner_minSdkN(activity, fragment);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    public void liveData_partiallyObscuredActivityFragmentInFragment_minSdkN() throws Throwable {
        LifecycleObservableActivity activity = mActivityTestRule.getActivity();
        LifecycleObservableFragment fragment = new LifecycleObservableFragment();
        LifecycleObservableFragment fragment2 = new LifecycleObservableFragment();
        mActivityTestRule.runOnUiThread(() -> {
            activity.replaceFragment(fragment);
            fragment.replaceFragment(fragment2);
        });

        liveData_partiallyObscuredLifecycleOwner_minSdkN(activity, fragment2);
    }

    private void liveData_partiallyObscuredLifecycleOwner_maxSdkM(FragmentActivity activity,
            LifecycleOwner lifecycleOwner,
            OnSaveInstanceStateObservable onSaveInstanceStateObservable)
            throws Throwable {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        MutableLiveData<Integer> mutableLiveData = new MutableLiveData<>();
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(0));

        TestUtils.waitTillResumed(lifecycleOwner, mActivityTestRule);

        mutableLiveData.observe(lifecycleOwner, atomicInteger::set);

        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                NavigationDialogActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        // Launch the NavigationDialogActivity, partially obscuring the activity, and wait for the
        // the onSaveInstanceStateObservable's onSaveInstanceState method to be called.  On API 23
        // and below, this should be the final 'lifecycle' method called.
        CountDownLatch countDownLatch = new CountDownLatch(1);
        onSaveInstanceStateObservable.setOnSaveInstanceStateListener(() -> {
            countDownLatch.countDown();
            onSaveInstanceStateObservable.setOnSaveInstanceStateListener(null);
        });
        Intent dummyIntent = new Intent(InstrumentationRegistry.getTargetContext(),
                NavigationDialogActivity.class);
        activity.startActivity(dummyIntent);
        countDownLatch.await(1, TimeUnit.SECONDS);

        // Sanity check.
        assertThat(lifecycleOwner.getLifecycle().getCurrentState(),
                is(Lifecycle.State.CREATED));

        // Change the LiveData value and assert that the observer is not called given that the
        // lifecycle is in the CREATED state.
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(1));
        assertThat(atomicInteger.get(), is(0));

        // Finish the dialog Activity, wait for the main activity to be resumed, and assert that
        // the observer's onChanged method is called.
        FragmentActivity dialogActivity = (FragmentActivity) monitor.waitForActivity();
        mActivityTestRule.runOnUiThread(dialogActivity::finish);
        TestUtils.waitTillResumed(lifecycleOwner, mActivityTestRule);
        assertThat(atomicInteger.get(), is(1));
    }

    private void liveData_partiallyObscuredLifecycleOwner_minSdkN(FragmentActivity activity,
            LifecycleOwner lifecycleOwner)
            throws Throwable {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        MutableLiveData<Integer> mutableLiveData = new MutableLiveData<>();
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(0));

        TestUtils.waitTillResumed(lifecycleOwner, mActivityTestRule);

        mutableLiveData.observe(lifecycleOwner, atomicInteger::set);

        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                NavigationDialogActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        // Launch the NavigationDialogActivity, partially obscuring the activity, and wait for the
        // lifecycleOwner to hit onPause (or enter the STARTED state).  On API 24 and above, this
        // onPause should be the last lifecycle method called (and the STARTED state should be the
        // final resting state).
        Intent dummyIntent = new Intent(InstrumentationRegistry.getTargetContext(),
                NavigationDialogActivity.class);
        activity.startActivity(dummyIntent);
        TestUtils.waitTillStarted(lifecycleOwner, mActivityTestRule);

        // Sanity check, in a previous version of this test, we were still RESUMED at this point.
        assertThat(lifecycleOwner.getLifecycle().getCurrentState(),
                is(Lifecycle.State.STARTED));

        // Change the LiveData's value and verify that the observer's onChanged method is called
        // since we are in the STARTED state.
        mActivityTestRule.runOnUiThread(() -> mutableLiveData.setValue(1));
        assertThat(atomicInteger.get(), is(1));
    }
}
