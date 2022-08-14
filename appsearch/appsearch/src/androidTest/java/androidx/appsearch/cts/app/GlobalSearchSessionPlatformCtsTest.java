/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.cts.app;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.app.GlobalSearchSession;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
public class GlobalSearchSessionPlatformCtsTest extends GlobalSearchSessionCtsTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<GlobalSearchSession> createGlobalSearchSessionAsync() {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createGlobalSearchSessionAsync(
                new PlatformStorage.GlobalSearchContext.Builder(context).build());
    }
}
