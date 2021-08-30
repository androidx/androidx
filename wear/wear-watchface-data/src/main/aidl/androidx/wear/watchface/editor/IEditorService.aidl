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

package androidx.wear.watchface.editor;

import androidx.wear.watchface.editor.IEditorObserver;

/**
 * Interface for the watchface editor service.
 *
 * @hide
 */
interface IEditorService {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 5

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int API_VERSION = 1;

    /**
     * Returns the version number for this API which the client can use to determine which methods
     * are available.
     *
     * @since API version 1.
     */
    int getApiVersion() = 1;

    /**
     * Registers an {@link IEditorObserver} which will be notiifed everytime the watch face editor
     * state changes
     *
     * @return An ID to be passed into {@link #unregisterObserver} to unregister for events.
     * @since API version 1.
     */
    int registerObserver(IEditorObserver observer) = 2;

    /**
     * Unregisters an observer registered by {@link #registerObserver}.
     *
     * @param observerId The ID returned by {@link #registerObserver} for the observer to unregister
     * @since API version 1.
     */
    void unregisterObserver(int observerId) = 3;

    /**
     * Instructs any open editor to close.
     */
    void closeEditor() = 4;
}
