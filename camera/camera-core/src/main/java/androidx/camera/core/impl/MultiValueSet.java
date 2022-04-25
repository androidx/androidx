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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A value set implementation that store multiple values in type C.
 *
 * @param <C> The type of the parameter.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public abstract class MultiValueSet<C> {

    private Set<C> mSet = new HashSet<>();

    /**
     * Adds all of the elements in the specified collection to this value set if they're not already
     * present (optional operation).
     *
     * @param  value collection containing elements to be added to this value.
     */
    public void addAll(@NonNull List<C> value) {
        mSet.addAll(value);
    }

    /**
     * Returns the list of {@link C} which containing all the elements were added to this value set.
     */
    @NonNull
    public List<C> getAllItems() {
        return Collections.unmodifiableList(new ArrayList<>(mSet));
    }

    /**
     * Need to implement the clone method for object copy.
     * @return the cloned instance.
     */
    @Override
    public abstract @NonNull MultiValueSet<C> clone();
}
