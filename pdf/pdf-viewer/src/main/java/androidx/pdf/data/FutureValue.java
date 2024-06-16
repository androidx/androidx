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

package androidx.pdf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * The result of an asynchronous operation that may complete at a later time. Useful to avoid
 * blocking while waiting for a long running process to create a value. Clients of this class who
 * need the result create a callback and pass it to {@link #get(Callback)} to receive either the
 * value or an exception when available.
 *
 * <p>Only one of {@link Callback#available(Object)} or {@link Callback#failed(Throwable)} will be
 * called once. The value may be <code>null</code>. All callbacks passed before and after the result
 * is complete will be called and then have their references removed.
 *
 * @param <T> The type of the value to be returned.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface FutureValue<T> {

    /** Get the value when it is available. Could supply the value immediately. */
    void get(@Nullable Callback<T> callback);

    /**
     * A callback to receive the result when available.
     *
     * @param <T> the type of result
     */
    interface Callback<T> {

        /** Gives the value when it is available. */
        void available(T value);

        /** Gives the exception thrown while attempting to get the value. */
        void failed(@NonNull Throwable thrown);

        /** Reports the ratio of completed to total work from 0 to 1. */
        void progress(float progress);
    }
}
