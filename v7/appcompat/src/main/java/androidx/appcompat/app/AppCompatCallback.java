/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.appcompat.app;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;

/**
 * Implemented this in order for AppCompat to be able to callback in certain situations.
 * <p>
 * This should be provided to
 * {@link AppCompatDelegate#create(android.app.Activity, AppCompatCallback)}.
 */
public interface AppCompatCallback {

    /**
     * Called when a support action mode has been started.
     *
     * @param mode The new action mode.
     */
    void onSupportActionModeStarted(ActionMode mode);

    /**
     * Called when a support action mode has finished.
     *
     * @param mode The action mode that just finished.
     */
    void onSupportActionModeFinished(ActionMode mode);

    /**
     * Called when a support action mode is being started for this window. Gives the
     * callback an opportunity to handle the action mode in its own unique and
     * beautiful way. If this method returns null the system can choose a way
     * to present the mode or choose not to start the mode at all.
     *
     * @param callback Callback to control the lifecycle of this action mode
     * @return The ActionMode that was started, or null if the system should present it
     */
    @Nullable
    ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback);

}
