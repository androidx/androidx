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

package androidx.appsearch.observer;

import androidx.annotation.NonNull;

/**
 * An interface which apps can implement to subscribe to notifications of changes to AppSearch data.
 */
public interface ObserverCallback {
    /**
     * Callback to trigger after schema changes (schema type added, updated or removed).
     *
     * @param changeInfo Information about the nature of the change.
     */
    void onSchemaChanged(@NonNull SchemaChangeInfo changeInfo);

    /**
     * Callback to trigger after document changes (documents added, updated or removed).
     *
     * @param changeInfo Information about the nature of the change.
     */
    void onDocumentChanged(@NonNull DocumentChangeInfo changeInfo);
}
