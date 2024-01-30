/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.graphics.Point;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Provides support for auto-scrolling a view.
 *
 */
@RestrictTo(LIBRARY)
public abstract class AutoScroller {

    /**
     * Resets state of the scroller. Call this when the user activity that is driving
     * auto-scrolling is done.
     */
    public abstract void reset();

    /**
     * Processes a new input location.
     * @param location
     */
    public abstract void scroll(@NonNull Point location);
}
