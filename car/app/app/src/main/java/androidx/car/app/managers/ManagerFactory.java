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

package androidx.car.app.managers;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A class capable of producing a new instance of a given {@link Manager}
 *
 * @param <T> type of {@link Manager} this factory is able to produce
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ManagerFactory<T extends Manager> {
    /**
     * Returns a new instance of a given {@link Manager} type
     *
     * @throws IllegalStateException if the given manager can not be instantiated
     */
    @NonNull
    T create();
}
