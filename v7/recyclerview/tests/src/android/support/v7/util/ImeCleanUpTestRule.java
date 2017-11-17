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

import android.app.Instrumentation;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.testutils.PollingCheck;
import android.support.v7.widget.TestActivity;
import android.view.KeyEvent;
import android.view.View;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit rule that ensures that IME is closed after a test is finished by determining if the
 * keyboard is open, and if it is closing it.  If the rules determines the keyboard is open and is
 * unable to close it within a timeout (see source), an exception will be thrown.
 *
 * A test that wants to benefit from this functionality must call
 * {@link #setup(TestActivity, Instrumentation)} with the {@link TestActivity} under test and the
 * test's {@link Instrumentation}, or this rule does nothing.
 */
public class ImeCleanUpTestRule implements TestRule {

    // We consider the keyboard open if its height is at least this percentage of the available
    // screen height.
    private static final float KEYBOARD_HEIGHT_TO_SCREEN_RATIO = .15f;

    private View mContainerView;
    private Instrumentation mInstrumentation;

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
     * Call to enable the functionality of this TestRule.
     * @param testActivity The {@link TestActivity} under test.
     * @param instrumentation The test's {@link Instrumentation}.
     */
    public void setup(@NonNull TestActivity testActivity,
            @NonNull Instrumentation instrumentation) {
        mContainerView = testActivity.getContainer();
        mInstrumentation = instrumentation;
    }

    private void closeImeIfOpen() {
        if (mContainerView == null || mInstrumentation == null) {
            return;
        }

        final Rect r = new Rect();
        mContainerView.getWindowVisibleDisplayFrame(r);

        // This is the entire height of the screen available to both the view and IME
        final int screenHeight = mContainerView.getHeight();

        // r.bottom is the position above IME if it's open or device button.
        // if IME is shown, r.bottom is smaller than screenHeight.
        int imeHeight = screenHeight - r.bottom;

        if (imeHeight > screenHeight * KEYBOARD_HEIGHT_TO_SCREEN_RATIO) {
            // Soft keyboard is shown, therefore we click the back button to close it and wait for
            // it to be closed.
            mInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
            PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
                @Override
                public boolean canProceed() {
                    mContainerView.getWindowVisibleDisplayFrame(r);
                    int imeHeight = screenHeight - r.bottom;
                    return imeHeight < screenHeight * KEYBOARD_HEIGHT_TO_SCREEN_RATIO;
                }
            });
        }
    }
}
