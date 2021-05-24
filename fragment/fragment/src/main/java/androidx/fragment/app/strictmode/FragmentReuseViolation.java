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

/** See #{@link FragmentStrictMode.Policy.Builder#detectFragmentReuse()}. */
public final class FragmentReuseViolation extends Violation {

    @NonNull
    private final String mPreviousWho;

    FragmentReuseViolation(@NonNull Fragment fragment, @NonNull String previousWho) {
        super(fragment);
        this.mPreviousWho = previousWho;
    }

    /**
     * Gets the unique ID of the previous instance of the {@link Fragment} causing the Violation.
     */
    @NonNull
    public String getPreviousFragmentId() {
        return mPreviousWho;
    }

    @NonNull
    @Override
    public String getMessage() {
        return "Attempting to reuse fragment " + mFragment + " with previous ID " + mPreviousWho;
    }
}
