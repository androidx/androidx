/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer;

import androidx.annotation.RestrictTo;

/**
 * Denotes a class that implements methods that should be called when the {@link PaginationModel} is
 * updated.
 *
 * <p>Implementing classes must call {@link PaginationModel#addObserver(PaginationModelObserver)}
 * to register themselves for updates.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PaginationModelObserver {

    /**
     * Notifies the implementation that a page has been added to the {@link PaginationModel}.
     *
     * <p>The {@link PaginationModel} does not enforce any implementation expectations.
     * Implementations are free to use this information as desired.
     */
    default void onPageAdded() {}

    /**
     * Notifies the implementation that the {@code viewArea} of the {@link PaginationModel} has
     * changed.
     *
     * <p>The {@link PaginationModel} does not enforce any implementation expectations.
     * Implementations are free to use this information as desired.
     */
    default void onViewAreaChanged() {}
}
