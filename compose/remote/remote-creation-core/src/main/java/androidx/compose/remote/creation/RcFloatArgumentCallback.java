/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.creation;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/** Provides an interface to a remote compose writer where callbacks have a RFloat argument
 * Used by loop and rFunc methods
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface RcFloatArgumentCallback {
    /** A lambda to execute */
    void run(@NonNull RFloat variable);
}
