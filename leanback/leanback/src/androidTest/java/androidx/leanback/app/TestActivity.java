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

import androidx.annotation.CallSuper;
import androidx.testutils.AnimationActivityTestRule;

/**
 * A general Activity that allows test set a Provider to custom activity's behavior in life
 * cycle events.
 */
public class TestActivity extends Activity {

    public static class TestActivity2 extends  TestActivity {
    }

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

    public static class TestActivityTestRule extends AnimationActivityTestRule<TestActivity> {

        final String mProviderName;

        public TestActivityTestRule(Class<? extends TestActivity.Provider> providerClass) {
            super(TestActivity.class, false, false);
            mProviderName = providerClass.getName();
        }

        public TestActivityTestRule() {
            super(TestActivity.class, false, false);
            mProviderName = null;
        }

        public String getProviderName() {
            return mProviderName;
        }

        public TestActivity launchActivity() {
            Intent intent = new Intent();
            intent.putExtra(TestActivity.EXTRA_PROVIDER, mProviderName);
            return launchActivity(intent);
        }

        public TestActivity launchActivity(Class<? extends TestActivity.Provider> providerClass) {
            Intent intent = new Intent();
            intent.putExtra(TestActivity.EXTRA_PROVIDER, providerClass.getName());
            return launchActivity(intent);
        }
    }

    public static class TestActivityTestRule2 extends AnimationActivityTestRule<TestActivity2> {

        public TestActivityTestRule2() {
            super(TestActivity2.class, false, false);
        }

        public TestActivity launchActivity(Class<? extends TestActivity.Provider> providerClass) {
            Intent intent = new Intent();
            intent.putExtra(TestActivity.EXTRA_PROVIDER, providerClass.getName());
            return launchActivity(intent);
        }
    }

    public static final String EXTRA_PROVIDER = "testActivityProvider";

    String mProviderName;
    Provider mProvider;
    boolean mStarted;

    public TestActivity() {
    }

    public Provider getProvider() {
        return mProvider;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProviderName = getIntent().getStringExtra(EXTRA_PROVIDER);
        try {
            mProvider = (Provider) Class.forName(mProviderName).newInstance();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
        }
        super.onDestroy();
    }
}
