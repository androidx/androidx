/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.arch.core.util;

import androidx.annotation.NonNull;

/**
 * Token representing a cancellable operation.
 * @deprecated Consider using <code>Disposable</code>.
 */
@Deprecated
public interface Cancellable {
    /**
     * An instance of Cancellable that is always cancelled - i.e., {@link #isCancelled()} will
     * always return true.
     * @deprecated Deprecated along with {@link Cancellable} itself.
     */
    @Deprecated
    @NonNull
    Cancellable CANCELLED = new Cancellable() {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    };

    /**
     * Cancel the subscription. This call should be idempotent, making it safe to
     * call multiple times.
     * @deprecated Deprecated along with {@link Cancellable} itself.
     */
    @Deprecated
    void cancel();

    /**
     * Returns true if the subscription has been cancelled. This is inherently a
     * racy operation if you are calling {@link #cancel()} on another thread, so this
     * should be treated as a 'best effort' signal.
     *
     * @return Whether the subscription has been cancelled.
     * @deprecated Deprecated along with {@link Cancellable} itself.
     */
    @Deprecated
    boolean isCancelled();
}
