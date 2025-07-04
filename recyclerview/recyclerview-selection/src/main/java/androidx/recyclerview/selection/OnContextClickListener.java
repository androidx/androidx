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

import android.view.MotionEvent;

import org.jspecify.annotations.NonNull;

/**
 * Override methods in this class to provide application specific behaviors
 * related to mouse input.
 */
/**
 * Register an OnContextClickListener to be notified when a context click
 * occurs.
 */
public interface OnContextClickListener {

    /**
     * Called when user performs a context click, usually via mouse pointer
     * right-click.
     *
     * @param e the event associated with the click.
     * @return true if the event was handled.
     */
    boolean onContextClick(@NonNull MotionEvent e);
}
