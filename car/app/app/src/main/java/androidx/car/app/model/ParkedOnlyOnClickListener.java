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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import android.annotation.SuppressLint;
import android.os.Looper;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.car.app.annotations.CarProtocol;

/**
 * An {@link OnClickListener} that wraps another one and executes its {@link #onClick} method only
 * when the car is parked.
 *
 * <p>When the car is not parked, the handler won't be executed and the host will display a message
 * to the user indicating that the action can only be used while parked.
 *
 * <p>Actions that direct the users to their phones must only execute while parked. This class
 * should be used for wrapping any click listeners that invoke such actions.
 *
 * <p>Example:
 *
 * <pre>{@code
 * builder.setOnClickListener(ParkedOnlyOnClickListener.create(
 *     () -> myClickAction()));
 * }</pre>
 */
// Lint check wants this to be renamed *Callback.
@SuppressLint("ListenerInterface")
@CarProtocol
public final class ParkedOnlyOnClickListener implements OnClickListener {
    @Keep
    private final OnClickListener mListener;

    /**
     * Triggers the {@link OnClickListener#onClick()} method in the listener wrapped by this
     * object.
     */
    @Override
    public void onClick() {
        mListener.onClick();
    }

    /**
     * Constructs a new instance of a {@link ParkedOnlyOnClickListener}.
     *
     * <p>Note that the listener relates to UI events and will be executed on the main thread
     * using {@link Looper#getMainLooper()}.
     *
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    @NonNull
    @SuppressLint("ExecutorRegistration")
    public static ParkedOnlyOnClickListener create(@NonNull OnClickListener listener) {
        return new ParkedOnlyOnClickListener(requireNonNull(listener));
    }

    private ParkedOnlyOnClickListener(OnClickListener listener) {
        mListener = listener;
    }
}
