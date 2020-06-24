/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.rxjava3;

import androidx.annotation.NonNull;

/**
 * Thrown by Room when the query in a Single&lt;T&gt; DAO method needs to return a result but the
 * returned result from the database is empty.
 * <p>
 * Since a Single&lt;T&gt; must either emit a single non-null value or an error, this exception is
 * thrown instead of emitting a null value when the query resulted empty. If the Single&lt;T&gt;
 * contains a type argument of a collection (e.g. Single&lt;List&lt;Song&gt&gt;) then this
 * exception is not thrown an an empty collection is emitted instead.
 */
public final class EmptyResultSetException extends RuntimeException {
    /**
     * Constructs a new EmptyResultSetException with the exception.
     * @param message The SQL query which didn't return any results.
     */
    public EmptyResultSetException(@NonNull String message) {
        super(message);
    }
}
