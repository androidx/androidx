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

package androidx.browser.trusted;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

/**
 * Holds an {@link Intent} and other data necessary to start a Trusted Web Activity.
 */
public final class TrustedWebActivityIntent {
    @NonNull
    private final Intent mIntent;

    TrustedWebActivityIntent(@NonNull Intent intent) {
        mIntent = intent;
    }

    /**
     * Launches a Trusted Web Activity.
     */
    public void launchTrustedWebActivity(@NonNull Context context) {
        ContextCompat.startActivity(context, mIntent, null);
    }


    /**
     * Returns the held {@link Intent}. For launching a Trusted Web Activity prefer using
     * {@link #launchTrustedWebActivity}.
     */
    @NonNull
    public Intent getIntent() {
        return mIntent;
    }
}
