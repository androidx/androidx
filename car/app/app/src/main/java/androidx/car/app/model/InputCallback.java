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

package androidx.car.app.model;

import androidx.annotation.NonNull;
import androidx.car.app.annotations.RequiresCarApi;

/** A listener for handling text input completion event. */
@RequiresCarApi(2)
public interface InputCallback {
    /**
     * Notifies when the user finished entering text in an input box.
     *
     * <p>This event is sent when the user finishes typing in the keyboard and pressed enter.
     * If the user simply stops typing and closes the keyboard, this event will not be sent.
     *
     * @param text the text that was entered, or an empty string if no text was typed.
     */
    default void onInputSubmitted(@NonNull String text) {
    }

    /**
     * Notifies the current {@code text} has changed in an input box.
     *
     * <p>The host may invoke this callback as the user types an input text. The frequency of
     * these updates is not guaranteed to be after every individual keystroke. The host may
     * decide to wait for several keystrokes before sending a single update.
     *
     * @param text the current text that the user has typed
     */
    default void onInputTextChanged(@NonNull String text) {
    }
}
