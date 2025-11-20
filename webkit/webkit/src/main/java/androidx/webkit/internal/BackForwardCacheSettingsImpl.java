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

package androidx.webkit.internal;

import androidx.webkit.BackForwardCacheSettings;
import androidx.webkit.WebSettingsCompat;

import org.chromium.support_lib_boundary.WebViewBackForwardCacheSettingsBoundaryInterface;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Callable;

@WebSettingsCompat.ExperimentalBackForwardCacheSettings
public class BackForwardCacheSettingsImpl implements
        WebViewBackForwardCacheSettingsBoundaryInterface {
    BackForwardCacheSettings mSettings;

    public BackForwardCacheSettingsImpl(@NonNull BackForwardCacheSettings settings) {
        mSettings = settings;
    }

    @Override
    public int getTimeoutInSeconds() {
        return (int) mSettings.getTimeoutSeconds();
    }

    @Override
    public int getMaxPagesInCache() {
        return mSettings.getMaxPagesInCache();
    }

    @Override
    public @Nullable Object getOrCreatePeer(@NonNull Callable<Object> creationCallable) {
        return mSettings;
    }
}
