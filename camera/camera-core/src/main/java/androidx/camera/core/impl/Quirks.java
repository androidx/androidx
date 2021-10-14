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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Wraps a list of {@link Quirk}s, allowing to easily retrieve a {@link Quirk} instance by its
 * class.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Quirks {

    @NonNull
    private final List<Quirk> mQuirks;

    /** Wraps the provided list of quirks. */
    public Quirks(@NonNull final List<Quirk> quirks) {
        mQuirks = new ArrayList<>(quirks);
    }

    /**
     * Retrieves a {@link Quirk} instance given its type.
     *
     * <p>Unlike {@link #contains(Class)}, a quirk can only be retrieved by the exact class. If a
     * superclass or superinterface is provided, {@code null} will be returned, even if a quirk
     * with the provided superclass or superinterface exists in this collection.
     *
     * @param quirkClass The type of quirk to retrieve.
     * @return A {@link Quirk} instance of the provided type, or {@code null} if it isn't found.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Quirk> T get(@NonNull final Class<T> quirkClass) {
        for (final Quirk quirk : mQuirks) {
            if (quirk.getClass() == quirkClass) {
                return (T) quirk;
            }
        }
        return null;
    }

    /**
     * Returns whether this collection of quirks contains a quirk with the provided type.
     *
     * <p>This checks whether the provided quirk type is the exact class, a superclass, or a
     * superinterface of any of the contained quirks, and will return true in all cases.
     * @param quirkClass The type of quirk to check for existence in this container.
     * @return {@code true} if this container contains a quirk with the given type, {@code false}
     * otherwise.
     */
    public boolean contains(@NonNull Class<? extends Quirk> quirkClass) {
        for (Quirk quirk : mQuirks) {
            if (quirkClass.isAssignableFrom(quirk.getClass())) {
                return true;
            }
        }

        return false;
    }
}
