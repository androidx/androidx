/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appsearch.app;

import androidx.annotation.RestrictTo;
import androidx.appsearch.safeparcel.AbstractSafeParcelable;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base class representing a collection of enabled features.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class EnabledFeatures extends AbstractSafeParcelable {

    @Field(id = 1, getter = "getEnabledFeatures")
    protected final @NonNull List<String> mEnabledFeatures;

    EnabledFeatures(
            @Param(id = 1) @NonNull List<String> enabledFeatures) {
        mEnabledFeatures = Collections.unmodifiableList(
                Preconditions.checkNotNull(enabledFeatures));
    }

    /**
     * Get the list of enabled features that the caller is intending to use in this search call.
     *
     * @return the set of {@link Features} enabled in this {@link SearchSpec} Entry.
     */
    public @NonNull List<String> getEnabledFeatures() {
        return mEnabledFeatures;
    }
}
