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

package androidx.appsearch.testutil;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.observer.AppSearchObserverCallback;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.SchemaChangeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-thread-safe simple {@link androidx.appsearch.observer.AppSearchObserverCallback}
 * implementation for testing.
 *
 * <p>Should only be used with {@link com.google.common.util.concurrent.DirectExecutor}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SimpleTestObserverCallback implements AppSearchObserverCallback {
    @NonNull
    public List<SchemaChangeInfo> mSchemaChanges = new ArrayList<>();

    @NonNull
    public List<DocumentChangeInfo> mDocumentChanges = new ArrayList<>();

    @Override
    public void onSchemaChanged(@NonNull SchemaChangeInfo changeInfo) {
        mSchemaChanges.add(changeInfo);
    }

    @Override
    public void onDocumentChanged(@NonNull DocumentChangeInfo changeInfo) {
        mDocumentChanges.add(changeInfo);
    }
}
