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

package androidx.appsearch.exceptions;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Indicates that a {@link androidx.appsearch.app.AppSearchSchema} has logical inconsistencies such
 * as unpopulated mandatory fields or illegal combinations of parameters.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class IllegalSchemaException extends IllegalArgumentException {
    /**
     * Constructs a new {@link IllegalSchemaException}.
     *
     * @param message A developer-readable description of the issue with the bundle.
     */
    public IllegalSchemaException(@NonNull String message) {
        super(message);
    }
}
