/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.biometric;

import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

public class TestActivity extends FragmentActivity {
    private boolean mIsChangingConfigurationsOverride = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setId(android.R.id.content);
        setContentView(root);
    }

    /**
     * Shows a test prompt view.
     *
     * @param signal The cancellation signal to listen to.
     */
    public void showTestPrompt(int promptId, android.os.CancellationSignal signal) {
        android.view.View prompt = new android.view.View(this);
        prompt.setId(promptId);
        prompt.setBackgroundColor(android.graphics.Color.RED);
        prompt.setMinimumWidth(100);
        prompt.setMinimumHeight(100);
        ((android.widget.FrameLayout) findViewById(android.R.id.content)).addView(prompt);

        if (signal != null) {
            signal.setOnCancelListener(this::hideTestPrompt);
        }
    }

    /**
     * Hides the test prompt view.
     */
    public void hideTestPrompt() {
        android.view.View prompt = findViewById(android.R.id.primary);
        if (prompt != null) {
            ((android.view.ViewGroup) prompt.getParent()).removeView(prompt);
        }
    }

    /**
     * Sets whether the activity should report that it is undergoing a configuration change.
     */
    public void setChangingConfigurationsOverride(boolean value) {
        mIsChangingConfigurationsOverride = value;
    }

    @Override
    public boolean isChangingConfigurations() {
        // Return the override if set, otherwise fallback to the system value.
        return mIsChangingConfigurationsOverride || super.isChangingConfigurations();
    }
}
