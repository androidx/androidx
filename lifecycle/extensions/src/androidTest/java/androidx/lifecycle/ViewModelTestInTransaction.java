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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.os.Bundle;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.viewmodeltest.TestViewModel;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ViewModelTestInTransaction {

    @Rule
    public ActivityTestRule<EmptyActivity> mActivityRule =
            new ActivityTestRule<>(EmptyActivity.class);

    @Test
    @UiThreadTest
    public void testViewModelInTransactionActivity() {
        EmptyActivity activity = mActivityRule.getActivity();
        TestFragment fragment = new TestFragment();
        activity.getSupportFragmentManager().beginTransaction().add(fragment, "tag").commitNow();
        TestViewModel viewModel = ViewModelProviders.of(activity).get(TestViewModel.class);
        assertThat(viewModel, is(fragment.mViewModel));
    }

    @Test
    @UiThreadTest
    public void testViewModelInTransactionFragment() {
        EmptyActivity activity = mActivityRule.getActivity();
        ParentFragment parent = new ParentFragment();
        activity.getSupportFragmentManager().beginTransaction().add(parent, "parent").commitNow();
        assertThat(parent.mExecuted, is(true));
    }


    public static class ParentFragment extends Fragment {

        private boolean mExecuted;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            TestFragment fragment = new TestFragment();
            getChildFragmentManager().beginTransaction().add(fragment, "tag").commitNow();
            TestViewModel viewModel = ViewModelProviders.of(this).get(TestViewModel.class);
            assertThat(viewModel, is(fragment.mViewModel));
            mExecuted = true;
        }
    }

    public static class TestFragment extends Fragment {

        TestViewModel mViewModel;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Fragment parentFragment = getParentFragment();
            ViewModelProvider provider = parentFragment != null
                    ? ViewModelProviders.of(parentFragment) : ViewModelProviders.of(getActivity());
            mViewModel = provider.get(TestViewModel.class);
            assertThat(mViewModel, notNullValue());
        }
    }
}
