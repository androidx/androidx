/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.lifecycle.viewmodel.savedstate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import android.app.Activity;
import android.app.Instrumentation;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.SavedStateVMFactory;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.viewmodel.savedstate.activity.FakingSavedStateActivity;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
@SmallTest
public class ViewModelsWithStateTests {

    private static final String FRAGMENT_MODE = "fragment";
    private static final String ACTIVITY_MODE = "activity";

    @Parameterized.Parameters(name = "using: {0}")
    public static Collection<String> getParameters() {
        return Arrays.asList(FRAGMENT_MODE, ACTIVITY_MODE);
    }

    @Parameterized.Parameter
    public String mode;

    @Rule
    public ActivityTestRule<FakingSavedStateActivity> activityRule = new ActivityTestRule<>(
            FakingSavedStateActivity.class, false, false);

    @Test
    public void testSimpleSavingVM() throws Throwable {
        final String newValue = "para";
        final FakingSavedStateActivity activity = activityRule.launchActivity(null);
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VM vm = vmProvider(activity).get(VM.class);
                vm.mLiveData.setValue(newValue);
            }
        });

        assertThat(activity.moveTaskToBack(true), is(true));

        Bundle savedState = activity.awaitSavedState();
        assertThat(savedState, notNullValue());
        activityRule.finishActivity();
        final FakingSavedStateActivity recreated = activityRule.launchActivity(
                FakingSavedStateActivity.createIntent(savedState));
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VM vm = vmProvider(recreated).get(VM.class);
                assertThat(vm.mLiveData.getValue(), is(newValue));
            }
        });
    }

    @Test
    public void testReattachment() throws Throwable {
        final String newValue = "newValue";
        final FakingSavedStateActivity activity = activityRule.launchActivity(null);

        final ViewModel[] escape = new ViewModel[1];
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // viewmodel is created
                escape[0] = vmProvider(activity).get(VM.class);
            }
        });
        final FakingSavedStateActivity recreated = recreateActivity(activity,
                activityRule);

        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VM vm = vmProvider(recreated).get(VM.class);
                assertThat(vm, is(escape[0]));
                vm.mLiveData.setValue(newValue);
            }
        });
        assertThat(recreated.moveTaskToBack(true), is(true));

        Bundle savedState = recreated.awaitSavedState();
        recreated.finish();
        // clear reference, that activity instance should be gone already
        activityRule.finishActivity();
        final FakingSavedStateActivity relaunched = activityRule.launchActivity(
                FakingSavedStateActivity.createIntent(savedState));

        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                VM vm = vmProvider(relaunched).get(VM.class);
                assertThat(vm.mLiveData.getValue(), is(newValue));
            }
        });
    }

    public static class VM extends ViewModel {
        private final MutableLiveData<String> mLiveData;

        public VM(SavedStateHandle handle) {
            mLiveData = handle.getLiveData("state");
        }
    }

    private ViewModelProvider vmProvider(FakingSavedStateActivity activity) {
        if (FRAGMENT_MODE.equals(mode)) {
            Fragment fragment = activity.getFragment();
            return new ViewModelProvider(fragment, new SavedStateVMFactory(
                    fragment.requireActivity().getApplication(),
                    fragment));
        }
        return new ViewModelProvider(activity, new SavedStateVMFactory(
                activity.getApplication(),
                activity));
    }

    // copy copy copy paste
    @SuppressWarnings("unchecked")
    private static <T extends Activity> T recreateActivity(final T activity,
            ActivityTestRule<?> rule)
            throws Throwable {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                activity.getClass().getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.recreate();
            }
        });
        T result;

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (monitor) {
            do {
                // the documentation says "Block until an Activity is created
                // that matches this monitor." This statement is true, but there are some other
                // true statements like: "Block until an Activity is destroyed" or
                // "Block until an Activity is resumed"...

                // this call will release synchronization monitor's monitor
                result = (T) monitor.waitForActivityWithTimeout(2000L);
                if (result == null) {
                    throw new RuntimeException("Timeout. Failed to recreate an activity");
                }
            } while (result == activity);
        }
        return result;
    }
}
