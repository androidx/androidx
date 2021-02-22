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

package androidx.recyclerview.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.test.core.app.ActivityScenario;
import androidx.testutils.ActivityScenarioResetRule;
import androidx.testutils.Resettable;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class TestActivity extends Activity implements Resettable {
    private volatile TestedFrameLayout mContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reset();

        // disable enter animation.
        overridePendingTransition(0, 0);
    }

    public void reset() {
        mContainer = new TestedFrameLayout(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(mContainer);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public TestedFrameLayout getContainer() {
        return mContainer;
    }

    @Override
    public void finish() {
        if (!mFinishEnabled) {
            return;
        }
        super.finish();

        // disable exit animation.
        overridePendingTransition(0, 0);
    }

    private boolean mFinishEnabled;

    @Override
    public void setFinishEnabled(boolean finishEnabled) {
        mFinishEnabled = finishEnabled;
    }

    static class ResetRule extends ActivityScenarioResetRule<TestActivity> {
        ResetRule(ActivityScenario<TestActivity> scenario) {
            super(scenario, new Function1<TestActivity, Unit>() {
                @Override
                public Unit invoke(TestActivity activity) {
                    activity.reset();
                    return null;
                }
            });
        }
    }
}
