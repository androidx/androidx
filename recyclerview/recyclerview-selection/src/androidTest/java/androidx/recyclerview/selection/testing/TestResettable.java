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

package androidx.recyclerview.selection.testing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.recyclerview.selection.Resettable;

public final class TestResettable implements Resettable {

    private boolean mResetRequired;
    private boolean mWasReset;

    public TestResettable() {
        // mRequiresReset defaults to false.
    }

    public TestResettable(boolean requiresReset) {
        mResetRequired = requiresReset;
    }

    @Override
    public void reset() {
        mWasReset = true;
    }

    public void setResetRequired(boolean required) {
        mResetRequired = required;
    }

    @Override
    public boolean isResetRequired() {
        return mResetRequired;
    }

    public void assertReset() {
        assertTrue(mWasReset);
    }

    public void assertNotReset() {
        assertFalse(mWasReset);
    }
}
