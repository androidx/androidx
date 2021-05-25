/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.fragment.app.strictmode;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/** See #{@link FragmentStrictMode.Policy.Builder#detectTargetFragmentUsage()}. */
public final class SetTargetFragmentUsageViolation extends TargetFragmentUsageViolation {

    private final Fragment mTargetFragment;
    private final int mRequestCode;

    SetTargetFragmentUsageViolation(
            @NonNull Fragment violatingFragment,
            @NonNull Fragment targetFragment,
            int requestCode) {
        super(violatingFragment);
        this.mTargetFragment = targetFragment;
        this.mRequestCode = requestCode;
    }

    @NonNull
    public Fragment getTargetFragment() {
        return mTargetFragment;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    @NonNull
    @Override
    public String getMessage() {
        return "Attempting to set target fragment " + mTargetFragment + " with request code "
                + mRequestCode + " for fragment " + mFragment;
    }
}
