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
package androidx.room

import androidx.annotation.IntDef

/**
 * Set of conflict handling strategies for various {@link Dao} methods.
 */
@Retention(AnnotationRetention.BINARY)
@Suppress("DEPRECATION")
@IntDef(
    OnConflictStrategy.NONE,
    OnConflictStrategy.REPLACE,
    OnConflictStrategy.ROLLBACK,
    OnConflictStrategy.ABORT,
    OnConflictStrategy.FAIL,
    OnConflictStrategy.IGNORE
)
public annotation class OnConflictStrategy {
    public companion object {
        /**
         * OnConflict strategy constant used by default when no other strategy is set. Using it
         * prevents Room from generating ON CONFLICT clause. It may be useful when there is a need
         * to use ON CONFLICT clause within a trigger. The runtime behavior is the same as
         * when [ABORT] strategy is applied. *The transaction is rolled back.*
         */
        public const val NONE: Int = 0
        /**
         * OnConflict strategy constant to replace the old data and continue the transaction.
         *
         * An [Insert] DAO method that returns the inserted rows ids will never return -1 since
         * this strategy will always insert a row even if there is a conflict.
         */
        public const val REPLACE: Int = 1
        /**
         * OnConflict strategy constant to rollback the transaction.
         *
         * @deprecated Does not work with Android's current SQLite bindings. Use [ABORT] to
         * roll back the transaction.
         */
        @Deprecated("Use ABORT instead.")
        public const val ROLLBACK: Int = 2
        /**
         * OnConflict strategy constant to abort the transaction. *The transaction is rolled
         * back.*
         */
        public const val ABORT: Int = 3
        /**
         * OnConflict strategy constant to fail the transaction.
         *
         * @deprecated Does not work as expected. The transaction is rolled back. Use
         * [ABORT].
         */
        @Deprecated("Use ABORT instead.")
        public const val FAIL: Int = 4
        /**
         * OnConflict strategy constant to ignore the conflict.
         *
         * An [Insert] DAO method that returns the inserted rows ids will return -1 for rows
         * that are not inserted since this strategy will ignore the row if there is a conflict.
         */
        public const val IGNORE: Int = 5
    }
}
