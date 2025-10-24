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

package androidx.compose.remote.creation.modifiers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.creation.RFloat;

import org.jspecify.annotations.NonNull;

/**
 * Provides an interface for Component layout and measures changes
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ComponentLayoutChanges {
    /** setter for x */
    void setX(@NonNull Number value);

    /** setter for y */
    void setY(@NonNull Number value);

    /** setter for width */
    void setWidth(@NonNull Number value);

    /** setter for height */
    void setHeight(@NonNull Number value);

    /** getter for x */
    @NonNull Number getX();

    /** getter for y */
    @NonNull Number getY();

    /** getter for width */
    @NonNull Number getWidth();

    /** getter for height */
    @NonNull Number getHeight();

    /** getter for parent width */
    @NonNull RFloat getParentWidth();

    /** getter for parent height */
    @NonNull RFloat getParentHeight();
}

