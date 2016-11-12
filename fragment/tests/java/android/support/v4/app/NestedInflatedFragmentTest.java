/*
 * Copyright (C) 2016 The Android Open Source Project
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


package android.support.v4.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.fragment.test.R;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.FragmentTestActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class NestedInflatedFragmentTest {
    private static final String TAG = "NestedInflatedFragmentTest";

    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<>(FragmentTestActivity.class);

    @Test
    @UiThreadTest
    public void inflatedChildFragment() throws Throwable {
        final FragmentTestActivity activity = mActivityRule.getActivity();
        final FragmentManager fm = activity.getSupportFragmentManager();

        ParentFragment parentFragment = new ParentFragment();
        fm.beginTransaction().add(android.R.id.content, parentFragment).commitNow();

        fm.beginTransaction().replace(android.R.id.content, new SimpleFragment())
                .addToBackStack(null).commit();
        fm.executePendingTransactions();

        fm.popBackStackImmediate();
    }

    public static class ParentFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.nested_inflated_fragment_parent, container, false);
        }
    }

    public static class InflatedChildFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.nested_inflated_fragment_child, container, false);
        }
    }

    public static class SimpleFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            TextView textView = new TextView(inflater.getContext());
            textView.setText("Simple fragment");
            return textView;
        }
    }
}
