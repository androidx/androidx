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

package androidx.fragment.app;

import static androidx.lifecycle.Lifecycle.Event.ON_DESTROY;
import static androidx.lifecycle.Lifecycle.Event.ON_RESUME;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.test.TestViewModel;
import androidx.fragment.app.test.ViewModelActivity;
import androidx.fragment.app.test.ViewModelActivity.ViewModelFragment;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ViewModelTest {
    private static final int TIMEOUT = 2; // secs

    @Rule
    public ActivityTestRule<ViewModelActivity> mActivityRule =
            new ActivityTestRule<>(ViewModelActivity.class);

    @Test(expected = IllegalStateException.class)
    @UiThreadTest
    public void testNotAttachedActivity() {
        // This is similar to calling getViewModelStore in Activity's constructor
        new FragmentActivity().getViewModelStore();
    }

    @Test(expected = IllegalStateException.class)
    @UiThreadTest
    public void testNotAttachedFragment() {
        // This is similar to calling getViewModelStore in Fragment's constructor
        new Fragment().getViewModelStore();
    }

    @Test
    public void testSameActivityViewModels() throws Throwable {
        final TestViewModel[] activityModel = new TestViewModel[1];
        final TestViewModel[] defaultActivityModel = new TestViewModel[1];
        final ViewModelActivity[] viewModelActivity = new ViewModelActivity[1];
        viewModelActivity[0] = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activityModel[0] = viewModelActivity[0].activityModel;
                defaultActivityModel[0] = viewModelActivity[0].defaultActivityModel;
                assertThat(defaultActivityModel[0], not(is(activityModel[0])));

                ViewModelFragment fragment1 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_1);
                ViewModelFragment fragment2 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_2);
                assertThat(fragment1, notNullValue());
                assertThat(fragment2, notNullValue());

                assertThat(fragment1.activityModel, is(activityModel[0]));
                assertThat(fragment2.activityModel, is(activityModel[0]));

                assertThat(fragment1.defaultActivityModel, is(defaultActivityModel[0]));
                assertThat(fragment2.defaultActivityModel, is(defaultActivityModel[0]));
            }
        });
        viewModelActivity[0] = recreateActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertThat(viewModelActivity[0].activityModel, is(activityModel[0]));
                assertThat(viewModelActivity[0].defaultActivityModel,
                        is(defaultActivityModel[0]));

                ViewModelFragment fragment1 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_1);
                ViewModelFragment fragment2 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_2);
                assertThat(fragment1, notNullValue());
                assertThat(fragment2, notNullValue());

                assertThat(fragment1.activityModel, is(activityModel[0]));
                assertThat(fragment2.activityModel, is(activityModel[0]));

                assertThat(fragment1.defaultActivityModel, is(defaultActivityModel[0]));
                assertThat(fragment2.defaultActivityModel, is(defaultActivityModel[0]));
            }
        });
    }

    @Test
    public void testSameFragmentViewModels() throws Throwable {
        final TestViewModel[] fragment1Model = new TestViewModel[1];
        final TestViewModel[] fragment2Model = new TestViewModel[1];
        final ViewModelActivity[] viewModelActivity = new ViewModelActivity[1];
        viewModelActivity[0] = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewModelFragment fragment1 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_1);
                ViewModelFragment fragment2 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_2);
                assertThat(fragment1, notNullValue());
                assertThat(fragment2, notNullValue());

                assertThat(fragment1.fragmentModel, not(is(fragment2.fragmentModel)));
                fragment1Model[0] = fragment1.fragmentModel;
                fragment2Model[0] = fragment2.fragmentModel;
            }
        });
        viewModelActivity[0] = recreateActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewModelFragment fragment1 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_1);
                ViewModelFragment fragment2 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_2);
                assertThat(fragment1, notNullValue());
                assertThat(fragment2, notNullValue());

                assertThat(fragment1.fragmentModel, is(fragment1Model[0]));
                assertThat(fragment2.fragmentModel, is(fragment2Model[0]));
            }
        });
    }

    @Test
    public void testActivityOnCleared() throws Throwable {
        final ViewModelActivity activity = mActivityRule.getActivity();
        final CountDownLatch latch = new CountDownLatch(1);
        final LifecycleObserver observer = new LifecycleObserver() {
            @SuppressWarnings("unused")
            @OnLifecycleEvent(ON_DESTROY)
            void onDestroy() {
                activity.getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            assertThat(activity.activityModel.mCleared, is(true));
                            assertThat(activity.defaultActivityModel.mCleared, is(true));
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
        };

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLifecycle().addObserver(observer);
            }
        });
        activity.finish();
        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
    }

    @Test
    public void testFragmentOnClearedWhenFinished() throws Throwable {
        final ViewModelActivity activity = mActivityRule.getActivity();
        final ViewModelFragment fragment = getFragment(activity,
                ViewModelActivity.FRAGMENT_TAG_1);
        final CountDownLatch latch = new CountDownLatch(1);
        final LifecycleObserver observer = new LifecycleObserver() {
            @SuppressWarnings("unused")
            @OnLifecycleEvent(ON_DESTROY)
            void onDestroy() {
                activity.getWindow().getDecorView().post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            assertThat(fragment.fragmentModel.mCleared, is(true));
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
        };

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLifecycle().addObserver(observer);
            }
        });
        activity.finish();
        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
    }

    @Test
    public void testFragmentOnCleared() throws Throwable {
        final ViewModelActivity activity = mActivityRule.getActivity();
        final CountDownLatch latch = new CountDownLatch(1);
        final LifecycleObserver observer = new LifecycleObserver() {
            @SuppressWarnings("unused")
            @OnLifecycleEvent(ON_RESUME)
            void onResume() {
                try {
                    final FragmentManager manager = activity.getSupportFragmentManager();
                    Fragment fragment = new Fragment();
                    manager.beginTransaction().add(fragment, "temp").commitNow();
                    ViewModelProvider viewModelProvider = new ViewModelProvider(fragment,
                            new ViewModelProvider.NewInstanceFactory());
                    TestViewModel vm = viewModelProvider.get(TestViewModel.class);
                    assertThat(vm.mCleared, is(false));
                    manager.beginTransaction().remove(fragment).commitNow();
                    assertThat(vm.mCleared, is(true));
                } finally {
                    latch.countDown();
                }
            }
        };

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLifecycle().addObserver(observer);
            }
        });
        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
    }

    private ViewModelFragment getFragment(FragmentActivity activity, String tag) {
        return (ViewModelFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(tag);
    }

    private ViewModelActivity recreateActivity() throws Throwable {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                ViewModelActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);
        final ViewModelActivity previous = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                previous.recreate();
            }
        });
        ViewModelActivity result;

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
                result = (ViewModelActivity) monitor.waitForActivityWithTimeout(4000);
                if (result == null) {
                    throw new RuntimeException("Timeout. Failed to recreate an activity");
                }
            } while (result == previous);
        }
        return result;
    }
}
