/*
 * Copyright 2026 The Android Open Source Project
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

import android.webkit.WebResourceRequest;

import androidx.webkit.HeaderInterceptor;
import androidx.webkit.HeaderUpdates;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * Adapter between {@link HeaderInterceptor} and the support library boundary function.
 */
public class HeaderInterceptorAdapter
        implements Function<
                @NonNull WebResourceRequest, @NonNull Map<@NonNull String, @Nullable String>> {
    private final @NonNull HeaderInterceptor mInterceptor;

    public HeaderInterceptorAdapter(@NonNull HeaderInterceptor interceptor) {
        mInterceptor = interceptor;
    }

    @Override
    public @NonNull Map<@NonNull String, @Nullable String> apply(
            @NonNull WebResourceRequest request) {
        HeaderUpdates updates = mInterceptor.interceptRequest(request);
        if (updates == null) {
            return Collections.emptyMap();
        }
        return updates.toBoundaryMap();
    }
}
