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

package androidx.room.util;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Java 8 Sneaky Throw technique.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SneakyThrow {

    /**
     * Re-throws a checked exception as if it was a runtime exception without wrapping it.
     *
     * @param e the exception to re-throw.
     */
    public static void reThrow(@NonNull Exception e) {
        sneakyThrow(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(@NonNull Throwable e) throws E {
        throw (E) e;
    }

    private SneakyThrow() {

    }
}
