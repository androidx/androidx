/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.sqlite.db
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFails
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SupportSQLiteQueryBuilderTest {

    @Test
    fun null_columns_should_not_throw_npe() {
        val query = SupportSQLiteQueryBuilder.builder("Books")
            .columns(null)
            .groupBy("pages")
            .having(">100")
            .create()
        assertThat(query.sql).isEqualTo("SELECT * FROM Books GROUP BY pages HAVING >100")
    }
    @Test
    fun null_groupBy_and_having_throws_error() {
        val error = assertFails {
            SupportSQLiteQueryBuilder.builder("Books")
                .having(">100")
                .create()
        }.message
        assertThat(error).isEqualTo("HAVING clauses are only permitted when using a groupBy clause")
    }

    @Test
    fun groupBy_and_having_does_not_throw_error() {
        val query = SupportSQLiteQueryBuilder.builder("Books")
            .columns(arrayOf("name", "pages"))
            .groupBy("pages")
            .having(">100")
            .create()
        assertThat(query.sql).isEqualTo("SELECT name, pages FROM Books GROUP BY pages HAVING >100")
    }

    @Test
    fun select_star_groupBy_and_having_does_not_throw_error() {
        val query = SupportSQLiteQueryBuilder.builder("Books")
            .columns(emptyArray())
            .groupBy("pages")
            .having(">100")
            .create()
        assertThat(query.sql).isEqualTo("SELECT * FROM Books GROUP BY pages HAVING >100")
    }

    @Test
    fun subtypes_in_array_selection_does_not_throw_error() {
        val bindArgs: Array<String?> = arrayOf("USA")

        val query = SupportSQLiteQueryBuilder.builder("Books")
            .columns(arrayOf("country_published"))
            .selection("country_published=USA", bindArgs)
            .create()
        assertThat(query.sql).isEqualTo(
            "SELECT country_published FROM Books WHERE country_published=USA"
        )
    }
}
