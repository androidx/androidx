/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.core.app;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.activity.ComponentActivity;
import androidx.core.test.R;

public class TestActivityRecreate extends ComponentActivity {
    private boolean mCalledRecreate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_compat_activity);
    }

    /**
     * Overrides Activity.recreate(), allowing us to use ActivityScenario's default (and only)
     * ActivityInvoker to handle lifecycle changes. Avoids re-entrance from ActivityRecreator's
     * posted call by only handling the first call to recreate().
     */
    public void recreate() {
        // Avoid re-entrance by delegating to super implementation.
        if (mCalledRecreate) {
            super.recreate();
        } else {
            mCalledRecreate = true;
            ActivityRecreator.recreate(this);
        }
    }
}
