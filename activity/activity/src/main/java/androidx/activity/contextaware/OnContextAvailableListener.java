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

package androidx.activity.contextaware;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Listener for receiving a callback at the first moment a {@link Context} is made
 * available to the {@link ContextAware} class.
 *
 * @see ContextAware#addOnContextAvailableListener(OnContextAvailableListener)
 */
public interface OnContextAvailableListener {

    /**
     * Called when the given {@link ContextAware} object is associated to a {@link Context}.
     *
     * @param contextAware The object that this listener for added to.
     * @param context The {@link Context} the {@link ContextAware} object is now associated with.
     * @param savedInstanceState The saved instance state, if any.
     */
    void onContextAvailable(@NonNull ContextAware contextAware,
            @SuppressLint("ContextFirst") /* The object being operated on should be first */
            @NonNull Context context,
            @Nullable Bundle savedInstanceState);
}
