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

package androidx.fragment.app.test;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.test.R;
import androidx.lifecycle.ViewModelProvider;

public class ViewModelActivity extends FragmentActivity {
    public static final String KEY_FRAGMENT_MODEL = "fragment-model";
    public static final String KEY_ACTIVITY_MODEL = "activity-model";
    public static final String FRAGMENT_TAG_1 = "f1";
    public static final String FRAGMENT_TAG_2 = "f2";

    public TestViewModel activityModel;
    public TestViewModel defaultActivityModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_model);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ViewModelFragment(), FRAGMENT_TAG_1)
                    .add(new ViewModelFragment(), FRAGMENT_TAG_2)
                    .commit();
        }

        ViewModelProvider viewModelProvider = new ViewModelProvider(this,
                new ViewModelProvider.NewInstanceFactory());
        activityModel = viewModelProvider.get(KEY_ACTIVITY_MODEL, TestViewModel.class);
        defaultActivityModel = viewModelProvider.get(TestViewModel.class);
    }

    public static class ViewModelFragment extends Fragment {
        public TestViewModel fragmentModel;
        public TestViewModel activityModel;
        public TestViewModel defaultActivityModel;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ViewModelProvider viewModelProvider = new ViewModelProvider(this,
                    new ViewModelProvider.NewInstanceFactory());
            fragmentModel = viewModelProvider.get(KEY_FRAGMENT_MODEL,
                    TestViewModel.class);
            ViewModelProvider activityViewModelProvider = new ViewModelProvider(getActivity(),
                    new ViewModelProvider.NewInstanceFactory());
            activityModel = activityViewModelProvider.get(KEY_ACTIVITY_MODEL,
                    TestViewModel.class);
            defaultActivityModel = activityViewModelProvider.get(TestViewModel.class);
        }
    }
}
