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

package androidx.lifecycle.viewmodel.savedstate.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FakingSavedStateActivity extends FragmentActivity {

    static final String FAKE_SAVED_STATE = "fake_state_key";
    private static final String TAG = "tag";
    private volatile CountDownLatch mCountDownLatch;
    private Bundle mLastSavedState;

    public static Intent createIntent(Bundle savedState) {
        return new Intent().putExtra(FAKE_SAVED_STATE, savedState);
    }

    private Bundle getFakeSavedState() {
        return getIntent() != null ? getIntent().getBundleExtra(FAKE_SAVED_STATE) : null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Bundle alternativeState = savedInstanceState != null ?  savedInstanceState
                : getFakeSavedState();
        super.onCreate(alternativeState);
        if (alternativeState == null) {
            getSupportFragmentManager().beginTransaction().add(new Fragment(), TAG).commitNow();
        }
    }

    public Fragment getFragment() {
        return getSupportFragmentManager().findFragmentByTag(TAG);
    }

    public Bundle awaitSavedState() throws InterruptedException {
        mCountDownLatch = new CountDownLatch(1);
        if (mCountDownLatch.await(1, TimeUnit.SECONDS)) {
            Bundle result = mLastSavedState;
            mLastSavedState = null;
            mCountDownLatch = null;
            return result;
        }
        return null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCountDownLatch != null) {
            mLastSavedState = outState;
            mCountDownLatch.countDown();
        }
    }
}
