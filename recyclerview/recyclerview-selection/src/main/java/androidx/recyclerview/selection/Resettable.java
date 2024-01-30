/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.annotation.RestrictTo;

/**
 * Represents an object that can be reset and can advise on it's
 * need to be reset.
 *
 * <p>Calling {@link #isResetRequired()} on an instance of {@link Resettable}
 * should always return false when called immediately after {@link #reset()}
 * has been called.
 *
 */
@RestrictTo(LIBRARY)
public interface Resettable {

    /**
     * @return true if the object requires reset.
     */
    boolean isResetRequired();

    /**
     * Resets the object state.
     */
    void reset();
}
