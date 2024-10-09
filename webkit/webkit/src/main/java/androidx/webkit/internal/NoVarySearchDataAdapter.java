/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.webkit.internal;

import androidx.annotation.NonNull;
import androidx.webkit.NoVarySearchData;

import org.chromium.support_lib_boundary.NoVarySearchDataBoundaryInterface;

import java.util.List;

public class NoVarySearchDataAdapter implements NoVarySearchDataBoundaryInterface {
    private final @NonNull NoVarySearchData mImpl;

    public NoVarySearchDataAdapter(@NonNull NoVarySearchData impl) {
        this.mImpl = impl;
    }

    @Override
    public boolean getVaryOnKeyOrder() {
        return mImpl.varyOnKeyOrder;
    }

    @Override
    public boolean getIgnoreDifferencesInParameters() {
        return mImpl.ignoreDifferencesInParameters;
    }

    @NonNull
    @Override
    public List<String> getIgnoredQueryParameters() {
        return mImpl.ignoredQueryParameters;
    }

    @NonNull
    @Override
    public List<String> getConsideredQueryParameters() {
        return mImpl.consideredQueryParameters;
    }
}
