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

import androidx.webkit.NoVarySearchHeader;
import androidx.webkit.Profile;
import androidx.webkit.SpeculativeLoadingParameters;

import org.chromium.support_lib_boundary.SpeculativeLoadingParametersBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

@Profile.ExperimentalUrlPrefetch
public class SpeculativeLoadingParametersAdapter
        implements SpeculativeLoadingParametersBoundaryInterface {

    private final SpeculativeLoadingParameters mSpeculativeLoadingParameters;

    public SpeculativeLoadingParametersAdapter(@Nullable SpeculativeLoadingParameters impl) {
        mSpeculativeLoadingParameters = impl;
    }

    @Override
    public @NonNull Map<String, String> getAdditionalHeaders() {
        if (mSpeculativeLoadingParameters == null) return new HashMap<>();
        return mSpeculativeLoadingParameters.getAdditionalHeaders();
    }

    @Override
    public @Nullable InvocationHandler getNoVarySearchData() {
        if (mSpeculativeLoadingParameters == null) return null;
        NoVarySearchHeader noVarySearchHeader =
                mSpeculativeLoadingParameters.getExpectedNoVarySearchData();
        if (noVarySearchHeader == null) return null;
        return BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                new NoVarySearchHeaderAdapter(noVarySearchHeader));
    }

    @Override
    public boolean isJavaScriptEnabled() {
        if (mSpeculativeLoadingParameters == null) return false;
        return mSpeculativeLoadingParameters.isJavaScriptEnabled();
    }
}
