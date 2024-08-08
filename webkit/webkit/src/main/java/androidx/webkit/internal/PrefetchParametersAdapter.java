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
import androidx.annotation.Nullable;
import androidx.webkit.NoVarySearchData;
import androidx.webkit.PrefetchParameters;

import org.chromium.support_lib_boundary.PrefetchParamsBoundaryInterface;
import org.chromium.support_lib_boundary.util.BoundaryInterfaceReflectionUtil;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

public class PrefetchParametersAdapter implements
        PrefetchParamsBoundaryInterface {
    private final PrefetchParameters mPrefetchParameters;

    public PrefetchParametersAdapter(@Nullable PrefetchParameters impl) {
        mPrefetchParameters = impl;
    }

    @NonNull
    @Override
    public Map<String, String> getAdditionalHeaders() {
        if (mPrefetchParameters == null) return new HashMap<>();
        return mPrefetchParameters.getAdditionalHeaders();
    }

    @Nullable
    @Override
    public InvocationHandler getNoVarySearchData() {
        if (mPrefetchParameters == null) return null;
        NoVarySearchData noVarySearchData = mPrefetchParameters.getExpectedNoVarySearchData();
        if (noVarySearchData == null) return null;
        return BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                new NoVarySearchDataAdapter(noVarySearchData));
    }
}
