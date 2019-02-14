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

package androidx.paging.futures;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.concurrent.Executor;

/**
 * Executor that runs each task in the thread that invokes {@link Executor#execute execute}
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class DirectExecutor implements Executor {
    /**
     * Returns an {@link Executor} that runs each task in the thread that invokes {@link
     * Executor#execute execute}.
     *
     * <p>This instance is equivalent to:
     *
     * <pre>{@code
     * final class DirectExecutor implements Executor {
     *   public void execute(Runnable r) {
     *     r.run();
     *   }
     * }
     * }</pre>
     */
    @NonNull
    public static DirectExecutor INSTANCE = new DirectExecutor();

    private DirectExecutor() {}
    @Override
    public void execute(@NonNull Runnable runnable) {
        runnable.run();
    }
}
