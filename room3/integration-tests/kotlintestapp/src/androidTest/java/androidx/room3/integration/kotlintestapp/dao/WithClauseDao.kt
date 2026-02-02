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

package androidx.room3.integration.kotlintestapp.dao

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room3.Dao
import androidx.room3.Query

@Dao
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
interface WithClauseDao {
    @Query(
        """
        WITH RECURSIVE factorial(n, fact) AS (
            SELECT 0, 1
            UNION ALL
            SELECT n+1, (n+1)*fact FROM factorial
            WHERE n < :maxIndexInclusive
        )
        SELECT fact FROM factorial
        """
    )
    fun getFactorials(maxIndexInclusive: Int): List<Int>

    @Query(
        """
        WITH RECURSIVE factorial(n, fact) AS (
            SELECT 0, 1
            UNION ALL
            SELECT n+1, (n+1)*fact FROM factorial
            WHERE n < :maxIndexInclusive
        )
        SELECT email_address FROM User WHERE User.uId IN (SELECT fact FROM factorial)
        """
    )
    fun getUsersWithFactorialIds(maxIndexInclusive: Int): List<String>
}
