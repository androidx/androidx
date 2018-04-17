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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Bundle;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FragmentViewLifecycleTest {

    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<>(FragmentTestActivity.class);

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycle() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final StrictViewFragment fragment = new StrictViewFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).commitNow();
        assertEquals(Lifecycle.State.RESUMED,
                fragment.getViewLifecycleOwner().getLifecycle().getCurrentState());
    }

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycleNullView() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final Fragment fragment = new Fragment();
        fm.beginTransaction().add(fragment, "fragment").commitNow();
        try {
            fragment.getViewLifecycleOwner();
            fail("getViewLifecycleOwner should be unavailable if onCreateView returned null");
        } catch (IllegalStateException expected) {
            // Expected
        }
    }

    @Test
    @UiThreadTest
    public void testObserveInOnCreateViewNullView() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final Fragment fragment = new ObserveInOnCreateViewFragment();
        try {
            fm.beginTransaction().add(fragment, "fragment").commitNow();
            fail("Fragments accessing view lifecycle should fail if onCreateView returned null");
        } catch (IllegalStateException expected) {
            // We need to clean up the Fragment to avoid it still being around
            // when the instrumentation test Activity pauses. Real apps would have
            // just crashed right after onCreateView().
            fm.beginTransaction().remove(fragment).commitNow();
        }
    }

    @Test
    public void testFragmentViewLifecycleRunOnCommit() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final StrictViewFragment fragment = new StrictViewFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).runOnCommit(new Runnable() {
            @Override
            public void run() {
                assertEquals(Lifecycle.State.RESUMED,
                        fragment.getViewLifecycleOwner().getLifecycle().getCurrentState());
                countDownLatch.countDown();

            }
        }).commit();
        countDownLatch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testFragmentViewLifecycleOwnerLiveData() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        final StrictViewFragment fragment = new StrictViewFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getViewLifecycleOwnerLiveData().observe(activity,
                        new Observer<LifecycleOwner>() {
                            @Override
                            public void onChanged(LifecycleOwner lifecycleOwner) {
                                if (lifecycleOwner != null) {
                                    assertTrue("Fragment View LifecycleOwner should be "
                                                    + "only be set after  onCreateView()",
                                            fragment.mOnCreateViewCalled);
                                    countDownLatch.countDown();
                                } else {
                                    assertTrue("Fragment View LifecycleOwner should be "
                                            + "set to null after onDestroyView()",
                                            fragment.mOnDestroyViewCalled);
                                    countDownLatch.countDown();
                                }
                            }
                        });
                fm.beginTransaction().add(R.id.content, fragment).commitNow();
                // Now remove the Fragment to trigger the destruction of the view
                fm.beginTransaction().remove(fragment).commitNow();
            }
        });
        countDownLatch.await(1, TimeUnit.SECONDS);
    }

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycleDetach() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final ObservingFragment fragment = new ObservingFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).commitNow();
        LifecycleOwner viewLifecycleOwner = fragment.getViewLifecycleOwner();
        assertEquals(Lifecycle.State.RESUMED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertTrue("LiveData should have active observers when RESUMED",
                fragment.mLiveData.hasActiveObservers());

        fm.beginTransaction().detach(fragment).commitNow();
        assertEquals(Lifecycle.State.DESTROYED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertFalse("LiveData should not have active observers after detach()",
                fragment.mLiveData.hasActiveObservers());
        try {
            fragment.getViewLifecycleOwner();
            fail("getViewLifecycleOwner should be unavailable after onDestroyView");
        } catch (IllegalStateException expected) {
            // Expected
        }
    }

    @Test
    @UiThreadTest
    public void testFragmentViewLifecycleReattach() {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        final ObservingFragment fragment = new ObservingFragment();
        fragment.setLayoutId(R.layout.fragment_a);
        fm.beginTransaction().add(R.id.content, fragment).commitNow();
        LifecycleOwner viewLifecycleOwner = fragment.getViewLifecycleOwner();
        assertEquals(Lifecycle.State.RESUMED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertTrue("LiveData should have active observers when RESUMED",
                fragment.mLiveData.hasActiveObservers());

        fm.beginTransaction().detach(fragment).commitNow();
        // The existing view lifecycle should be destroyed
        assertEquals(Lifecycle.State.DESTROYED,
                viewLifecycleOwner.getLifecycle().getCurrentState());
        assertFalse("LiveData should not have active observers after detach()",
                fragment.mLiveData.hasActiveObservers());

        fm.beginTransaction().attach(fragment).commitNow();
        assertNotEquals("A new view LifecycleOwner should be returned after reattachment",
                viewLifecycleOwner, fragment.getViewLifecycleOwner());
        assertEquals(Lifecycle.State.RESUMED,
                fragment.getViewLifecycleOwner().getLifecycle().getCurrentState());
        assertTrue("LiveData should have active observers when RESUMED",
                fragment.mLiveData.hasActiveObservers());
    }

    public static class ObserveInOnCreateViewFragment extends Fragment {
        MutableLiveData<Boolean> mLiveData = new MutableLiveData<>();
        private Observer<Boolean> mOnCreateViewObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mLiveData.observe(getViewLifecycleOwner(), mOnCreateViewObserver);
            assertTrue("LiveData should have observers after onCreateView observe",
                    mLiveData.hasObservers());
            // Return null - oops!
            return null;
        }

    }

    public static class ObservingFragment extends StrictViewFragment {
        MutableLiveData<Boolean> mLiveData = new MutableLiveData<>();
        private Observer<Boolean> mOnCreateViewObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };
        private Observer<Boolean> mOnViewCreatedObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };
        private Observer<Boolean> mOnViewStateRestoredObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean value) {
            }
        };

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mLiveData.observe(getViewLifecycleOwner(), mOnCreateViewObserver);
            assertTrue("LiveData should have observers after onCreateView observe",
                    mLiveData.hasObservers());
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mLiveData.observe(getViewLifecycleOwner(), mOnViewCreatedObserver);
            assertTrue("LiveData should have observers after onViewCreated observe",
                    mLiveData.hasObservers());
        }

        @Override
        public void onViewStateRestored(Bundle savedInstanceState) {
            super.onViewStateRestored(savedInstanceState);
            mLiveData.observe(getViewLifecycleOwner(), mOnViewStateRestoredObserver);
            assertTrue("LiveData should have observers after onViewStateRestored observe",
                    mLiveData.hasObservers());
        }
    }
}
