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

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Sets the instrumentation context as a verified Trusted Web Activity Provider, meaning that the
 * TrustedWebActivityService will accept calls from it.
 */
public class VerifiedProviderTestRule extends TestWatcher {
    @Override
    protected void starting(Description description) {
        set(true);
    }

    @Override
    protected void finished(Description description) {
        set(false);
    }

    /**
     * Manually disables verification, causing TrustedWebActivityService calls to throw an
     * exception.
     */
    public void manuallyDisable() {
        set(false);
    }

    @SuppressWarnings("deprecation")
    private void set(boolean enabled) {
        Context context = androidx.test.InstrumentationRegistry.getContext();
        TestTrustedWebActivityService.setVerifiedProvider(enabled
                ? Token.create(context.getPackageName(), context.getPackageManager())
                : null);
    }
}
