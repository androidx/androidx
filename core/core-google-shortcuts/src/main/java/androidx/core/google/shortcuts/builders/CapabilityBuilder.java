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

package androidx.core.google.shortcuts.builders;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.google.shortcuts.builders.Constants.CAPABILITY_PARAMETER_KEY;
import static androidx.core.google.shortcuts.builders.Constants.CAPABILITY_TYPE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.firebase.appindexing.builders.IndexableBuilder;

/**
 * Builder for the Capability section in the Shortcut Corpus.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class CapabilityBuilder extends IndexableBuilder<CapabilityBuilder> {
    private ParameterBuilder[] mParameters;

    public CapabilityBuilder() {
        super(CAPABILITY_TYPE);
    }

    /** Sets one or more parameters for the given capability. */
    @NonNull
    public CapabilityBuilder setParameter(
            @NonNull ParameterBuilder... parameter) {
        mParameters = parameter;
        return put(CAPABILITY_PARAMETER_KEY, parameter);
    }

    @Nullable
    public ParameterBuilder[] getParameters() {
        return mParameters;
    }
}
