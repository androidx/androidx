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

package androidx.car.app.model;

import android.content.Context;

import androidx.annotation.NonNull;

/** An interface used to denote a model that can act as a root for a tree of other models. */
public interface Template {
    /**
     * Checks that the application has the required permissions for this template.
     *
     * @throws SecurityException if the application is missing any required permission.
     */
    default void checkPermissions(@NonNull Context context) {
    }
}
