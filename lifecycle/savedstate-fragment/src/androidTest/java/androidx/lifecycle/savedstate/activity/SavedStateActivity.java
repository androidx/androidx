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

package androidx.lifecycle.savedstate.activity;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.SavedStateStore;
import androidx.lifecycle.SavedStateStores;

public class SavedStateActivity extends FragmentActivity {
    private static final String TAG = "fragment";

    private static Function<SavedStateStore, Void> sOnCreateRunnable;
    private static boolean sInFragment;

    public static void duringOnCreate(boolean inFragment,
            Function<SavedStateStore, Void> f) {
        sInFragment = inFragment;
        sOnCreateRunnable = f;
    }

    private static void shotOnCreateRunnable(SavedStateStore store) {
        if (sOnCreateRunnable != null) {
            sOnCreateRunnable.apply(store);
            sOnCreateRunnable = null;
        }
    }

    private SavedStateStore mSavedStateStore;

    public SavedStateActivity() {
        if (!sInFragment && sOnCreateRunnable != null) {
            mSavedStateStore = SavedStateStores.of(this);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(new FragmentWithRunnable(), TAG)
                    .commitNow();
        }
        if (!sInFragment) {
            shotOnCreateRunnable(mSavedStateStore);
        }
    }

    public Fragment getFragment() {
        return getSupportFragmentManager().findFragmentByTag(TAG);
    }

    public static class FragmentWithRunnable extends Fragment {
        private SavedStateStore mSavedStateStore;
        @SuppressWarnings("WeakerAccess")
        public FragmentWithRunnable() {
            if (sInFragment && sOnCreateRunnable != null) {
                mSavedStateStore = SavedStateStores.of(this);
            }
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (sInFragment) {
                shotOnCreateRunnable(mSavedStateStore);
            }
        }
    }
}
