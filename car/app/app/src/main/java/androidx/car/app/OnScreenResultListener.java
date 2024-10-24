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

package androidx.car.app;

import org.jspecify.annotations.Nullable;

/** A listener to provide the result set by a {@link Screen}. */
public interface OnScreenResultListener {
    /**
     * Provides the {@code result} from the {@link Screen} that was pushed using {@link
     * ScreenManager#pushForResult}, or {@code null} if no result was set.
     *
     * <p>This callback will be called right before the
     * {@link androidx.lifecycle.Lifecycle.State}
     * of the {@link Screen} that set the {@code result} becomes {@link
     * androidx.lifecycle.Lifecycle.State#DESTROYED}.
     *
     * @param result the result provided by the {@link Screen} that was pushed using {@link
     *               ScreenManager#pushForResult} or {@code null} if no result was set
     * @see Screen#setResult
     */
    void onScreenResult(@Nullable Object result);
}
