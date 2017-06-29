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

package android.support.v7.util;

import android.graphics.Rect;
import android.support.testutils.PollingCheck;
import android.view.View;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit rule that ensures that IME is closed after a test is finished (or exception thrown).
 * A test that triggers IME open/close should call setContainerView with the activity's container
 * view in order to trigger this cleanup at the end of the test. Otherwise, no cleanup happens.
 */
public class ImeCleanUpTestRule implements TestRule {

    private View mContainerView;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    closeImeIfOpen();
                }
            }
        };
    }

    /**
     * Sets the container view used to calculate the total screen height and the height available
     * to the test activity.
     */
    public void setContainerView(View containerView) {
        mContainerView = containerView;
    }

    private void closeImeIfOpen() {
        if (mContainerView == null) {
            return;
        }
        // Ensuring that IME is closed after starting each test.
        final Rect r = new Rect();
        mContainerView.getWindowVisibleDisplayFrame(r);
        // This is the entire height of the screen available to both the view and IME
        final int screenHeight = mContainerView.getRootView().getHeight();

        // r.bottom is the position above IME if it's open or device button.
        // if IME is shown, r.bottom is smaller than screenHeight.
        int imeHeight = screenHeight - r.bottom;

        // Picking a threshold to detect when IME is open
        if (imeHeight > screenHeight * 0.15) {
            // Soft keyboard is shown, will wait for it to close after running the test. Note that
            // we don't press back button here as the IME should close by itself when a test
            // finishes. If the wait isn't done here, the IME can mess up with the layout of the
            // next test.
            PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
                @Override
                public boolean canProceed() {
                    mContainerView.getWindowVisibleDisplayFrame(r);
                    int imeHeight = screenHeight - r.bottom;
                    return imeHeight < screenHeight * 0.15;
                }
            });
        }
    }
}
