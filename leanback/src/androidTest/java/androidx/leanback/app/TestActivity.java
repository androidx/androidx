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
package androidx.leanback.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.test.rule.ActivityTestRule;

import androidx.annotation.CallSuper;

import java.util.HashMap;

/**
 * A general Activity that allows test set a Provider to custom activity's behavior in life
 * cycle events.
 */
public class TestActivity extends Activity {

    public static class Provider {

        TestActivity mActivity;

        /**
         * @return Currently attached activity.
         */
        public TestActivity getActivity() {
            return mActivity;
        }

        @CallSuper
        public void onCreate(TestActivity activity, Bundle savedInstanceState) {
            mActivity = activity;
        }

        public void onAttachedToWindow(TestActivity activity) {
        }

        public void onStart(TestActivity activity) {
        }

        public void onStop(TestActivity activity) {
        }

        public void onPause(TestActivity activity) {
        }

        public void onResume(TestActivity activity) {
        }

        public void onDestroy(TestActivity activity) {
        }
    }

    public static class TestActivityTestRule extends ActivityTestRule<TestActivity> {

        String mProviderName;
        public TestActivityTestRule(TestActivity.Provider provider, String providerName) {
            super(TestActivity.class, false, false);
            mProviderName = providerName;
            provider.mActivity = null;
            TestActivity.setProvider(mProviderName, provider);
        }

        public TestActivity launchActivity() {
            Intent intent = new Intent();
            intent.putExtra(TestActivity.EXTRA_PROVIDER, mProviderName);
            return launchActivity(intent);
        }
    }

    public static final String EXTRA_PROVIDER = "testActivityProvider";

    static HashMap<String, Provider> sProviders = new HashMap<>();

    String mProviderName;
    Provider mProvider;
    boolean mStarted;

    public TestActivity() {
    }

    public static void setProvider(String name, Provider provider) {
        sProviders.put(name, provider);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProviderName = getIntent().getStringExtra(EXTRA_PROVIDER);
        mProvider = sProviders.get(mProviderName);
        if (mProvider != null) {
            mProvider.onCreate(this, savedInstanceState);
        }
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mProvider != null) {
            mProvider.onAttachedToWindow(this);
        }
    }

    public boolean isStarted() {
        return mStarted;
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStarted = true;
        if (mProvider != null) {
            mProvider.onStart(this);
        }
    }

    @Override
    protected void onPause() {
        if (mProvider != null) {
            mProvider.onPause(this);
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mProvider != null) {
            mProvider.onResume(this);
        }
    }

    @Override
    protected void onStop() {
        mStarted = false;
        if (mProvider != null) {
            mProvider.onStop(this);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mProvider != null) {
            mProvider.onDestroy(this);
            setProvider(mProviderName, null);
        }
        super.onDestroy();
    }
}
