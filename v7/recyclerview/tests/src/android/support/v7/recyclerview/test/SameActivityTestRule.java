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
package android.support.v7.recyclerview.test;

import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.TestActivity;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Like ActivityTestRule but re-uses the same activity
 */
public class SameActivityTestRule extends ActivityTestRule<TestActivity> {
    static TestActivity sTestActivity;
    public SameActivityTestRule() {
        super(TestActivity.class, false, false);
    }

    public boolean canReUseActivity() {
        return true;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new ReUsedActivityStatement(base);
    }

    @Override
    public TestActivity getActivity() {
        if (sTestActivity != null) {
            return sTestActivity;
        }
        return super.getActivity();
    }

    private class ReUsedActivityStatement extends Statement {

        private final Statement mBase;

        public ReUsedActivityStatement(Statement base) {
            mBase = base;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                if (sTestActivity == null || !sTestActivity.canBeReUsed() || !canReUseActivity()) {
                    launchActivity(getActivityIntent());
                    sTestActivity = getActivity();
                    sTestActivity.setAllowFinish(!canReUseActivity());
                    if (!canReUseActivity()) {
                        sTestActivity = null;
                    }
                } else {
                    sTestActivity.resetContent();
                }
                mBase.evaluate();
            } finally {
                afterActivityFinished();
            }
        }
    }
}
