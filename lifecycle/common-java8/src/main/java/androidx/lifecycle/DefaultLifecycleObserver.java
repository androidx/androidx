/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.lifecycle;

import androidx.annotation.NonNull;

/**
 * Callback interface for listening to {@link LifecycleOwner} state changes.
 * <p>
 * If you use Java 8 language, <b>always</b> prefer it over annotations.
 */
@SuppressWarnings("unused")
public interface DefaultLifecycleObserver extends FullLifecycleObserver {

    /**
     * Notifies that {@code ON_CREATE} event occurred.
     * <p>
     * This method will be called after the {@link LifecycleOwner}'s {@code onCreate}
     * method returns.
     *
     * @param owner the component, whose state was changed
     */
    @Override
    default void onCreate(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_START} event occurred.
     * <p>
     * This method will be called after the {@link LifecycleOwner}'s {@code onStart} method returns.
     *
     * @param owner the component, whose state was changed
     */
    @Override
    default void onStart(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_RESUME} event occurred.
     * <p>
     * This method will be called after the {@link LifecycleOwner}'s {@code onResume}
     * method returns.
     *
     * @param owner the component, whose state was changed
     */
    @Override
    default void onResume(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_PAUSE} event occurred.
     * <p>
     * This method will be called before the {@link LifecycleOwner}'s {@code onPause} method
     * is called.
     *
     * @param owner the component, whose state was changed
     */
    @Override
    default void onPause(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_STOP} event occurred.
     * <p>
     * This method will be called before the {@link LifecycleOwner}'s {@code onStop} method
     * is called.
     *
     * @param owner the component, whose state was changed
     */
    @Override
    default void onStop(@NonNull LifecycleOwner owner) {
    }

    /**
     * Notifies that {@code ON_DESTROY} event occurred.
     * <p>
     * This method will be called before the {@link LifecycleOwner}'s {@code onStop} method
     * is called.
     *
     * @param owner the component, whose state was changed
     */
    @Override
    default void onDestroy(@NonNull LifecycleOwner owner) {
    }
}

