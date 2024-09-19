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

package androidx.appsearch.cts.ast.searchtest;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchSession;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.flags.RequiresFlagsEnabled;
import androidx.appsearch.localstorage.LocalStorage;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.util.concurrent.ListenableFuture;

@RequiresFlagsEnabled(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public class AbstractSyntaxTreeSearchLocalCtsTest extends AbstractSyntaxTreeSearchCtsTestBase {

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName)
            throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        return LocalStorage.createSearchSessionAsync(
                new LocalStorage.SearchContext.Builder(context, dbName).build());
    }
}
