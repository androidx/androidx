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

package androidx.sqlite.driver

import androidx.kruth.assertThat
import androidx.sqlite.driver.SupportSQLiteStatement.Companion.getStatementPrefix
import androidx.test.filters.SmallTest
import kotlin.test.Test

@SmallTest
class SupportSQLiteStatementTest {

    @Test
    fun statementPrefixScanner() {
        // Validate trimming
        assertThat(getStatementPrefix("SELECT")).isEqualTo("SEL")
        assertThat(getStatementPrefix("   SELECT")).isEqualTo("SEL")
        assertThat(getStatementPrefix(" \nSELECT")).isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(getStatementPrefix(" \tSELECT")).isEqualTo("SEL")

        // Validate short leading comments
        assertThat(getStatementPrefix("SE")).isNull()
        assertThat(getStatementPrefix("SE LECT")).isEqualTo("SE ")
        assertThat(getStatementPrefix("-- cmt\n SE")).isNull()
        assertThat(getStatementPrefix("-- cmt\n SELECT")).isEqualTo("SEL")
        assertThat(getStatementPrefix("-")).isNull()
        assertThat(getStatementPrefix("--")).isNull()
        assertThat(getStatementPrefix("/***")).isNull()
        assertThat(getStatementPrefix("/* foo */ SELECT")).isEqualTo("SEL")

        // Validate leading line comments
        assertThat(
                getStatementPrefix(
                    """
                        -- cmt
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        -- line 1
                        -- line 2
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        -- line 1
                        SELECT
                        -- line 2
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")

        // Validate leading multiline comments
        assertThat(
                getStatementPrefix(
                    """
                        /* cmt */
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        /* line 1
                           line 2 */
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        /*
                         * line 1
                         * line 2
                         */
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        /*
                         * /* foo
                         * * bar
                         * // baz
                         */
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        /********
                         * line 1
                         * line 2
                         ********/
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")

        // Validate mixed comments
        assertThat(
                getStatementPrefix(
                    """
                        /* line 1 */
                        -- line 2
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
        assertThat(
                getStatementPrefix(
                    """
                        -- line 1
                        /* line 2 */
                        SELECT
                        """
                        .trimIndent()
                )
            )
            .isEqualTo("SEL")
    }
}
