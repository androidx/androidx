/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room3.integration.kotlintestapp.vo.Email
import androidx.room3.integration.kotlintestapp.vo.User
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class WithClauseTest : TestDatabaseTest(UseDriver.BUNDLED) {
    @Test
    fun noSourceOfData() {
        database.withClauseDao().getFactorials(0).let { result ->
            assertThat(result).containsExactly(1)
        }

        database.withClauseDao().getFactorials(4).let { result ->
            assertThat(result).containsExactly(1, 1, 2, 6, 24)
        }
    }

    @Test
    fun sourceOfData() {
        database.withClauseDao().getUsersWithFactorialIds(0).let { result ->
            assertThat(result).isEmpty()
        }

        database.usersDao().insertUser(User(0, Email("0", "Zero"), null))
        database.withClauseDao().getUsersWithFactorialIds(0).let { result ->
            assertThat(result).isEmpty()
        }

        database.usersDao().insertUser(User(1, Email("1", "One"), null))
        database.withClauseDao().getUsersWithFactorialIds(0).let { result ->
            assertThat(result).containsExactly("One")
        }

        database.usersDao().insertUser(User(6, Email("6", "Six"), null))
        database.withClauseDao().getUsersWithFactorialIds(0).let { result ->
            assertThat(result).containsExactly("One")
        }
        database.withClauseDao().getUsersWithFactorialIds(3).let { result ->
            assertThat(result).containsExactly("One", "Six")
        }
    }
}
