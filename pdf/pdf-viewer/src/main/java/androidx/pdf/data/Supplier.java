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
import androidx.annotation.RestrictTo;

/**
 * Supply a value that may take a long time to complete.
 *
 * @param <T> The type of the value to be supplied.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface Supplier<T> {

    /**
     * A blocking call to supply the value or throw an Exception.
     *
     * @param progress Used to report the progress of supplying the value.
     * @return The value to be passed to {@link FutureValue.Callback#available(Object)} on the main
     * thread.
     * @throws Exception Any exception thrown will be {@link FutureValue.Callback#failed(Throwable)}
     */
    T supply(@NonNull Progress progress) throws Exception;
}
