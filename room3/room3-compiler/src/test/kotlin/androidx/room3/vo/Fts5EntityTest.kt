/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.vo

import androidx.room3.compiler.processing.XElement
import androidx.room3.compiler.processing.XType
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.parser.FtsVersion
import mockElementAndType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
class Fts5EntityTest {

    @Test
    fun createStatement() {
        val primaryKeyField = createField("rowid")
        val bodyField = createField("body")
        val dontIndexMe1Field = createField("dontIndexMe1")
        val dontIndexMe2Field = createField("dontIndexMe2")
        val entity =
            FtsEntity(
                element = mock(XTypeElement::class.java),
                tableName = "Mail",
                type = mock(XType::class.java),
                properties =
                    listOf(primaryKeyField, bodyField, dontIndexMe1Field, dontIndexMe2Field),
                embeddedProperties = emptyList(),
                primaryKey =
                    PrimaryKey(
                        declaredIn = mock(XElement::class.java),
                        properties = Properties(primaryKeyField),
                        autoGenerateId = true,
                        algorithm = androidx.room3.PrimaryKey.Algorithm.AUTOINCREMENT,
                    ),
                constructor = null,
                shadowTableName = "Mail_context",
                ftsVersion = FtsVersion.FTS5,
                ftsOptions =
                    FtsOptions(
                        tokenizer = androidx.room3.FtsOptions.TOKENIZER_UNICODE61,
                        tokenizerArgs = emptyList(),
                        contentEntity = null,
                        languageIdColumnName = "",
                        matchInfo = androidx.room3.FtsOptions.MatchInfo.FTS4,
                        notIndexedColumns = listOf("dontIndexMe1", "dontIndexMe2"),
                        prefixSizes = listOf(2, 4),
                        preferredOrder = androidx.room3.FtsOptions.Order.ASC,
                        contentRowId = null,
                        columnSize = false,
                        detail = androidx.room3.FtsOptions.Detail.COLUMN,
                    ),
            )

        assertThat(
            entity.createTableQuery,
            `is`(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `Mail` USING FTS5(" +
                    "`body`, " +
                    "`dontIndexMe1` UNINDEXED, " +
                    "`dontIndexMe2` UNINDEXED, " +
                    "tokenize=`unicode61`, " +
                    "prefix=`2 4`, " +
                    "columnsize=0, " +
                    "detail=column" +
                    ")"
            ),
        )
    }

    @Test
    fun createStatement_unicodeTokenizer_withTokenizerArgs() {
        val primaryKeyField = createField("rowid")
        val bodyField = createField("body")
        val entity =
            FtsEntity(
                element = mock(XTypeElement::class.java),
                tableName = "Mail",
                type = mock(XType::class.java),
                properties = listOf(primaryKeyField, bodyField),
                embeddedProperties = emptyList(),
                primaryKey =
                    PrimaryKey(
                        declaredIn = mock(XElement::class.java),
                        properties = Properties(primaryKeyField),
                        autoGenerateId = true,
                        algorithm = androidx.room3.PrimaryKey.Algorithm.AUTOINCREMENT,
                    ),
                constructor = null,
                shadowTableName = "Mail_context",
                ftsVersion = FtsVersion.FTS5,
                ftsOptions =
                    FtsOptions(
                        tokenizer = androidx.room3.FtsOptions.TOKENIZER_UNICODE61,
                        tokenizerArgs = listOf("remove_diacritics", "0", "tokenchars", "'-_'"),
                        contentEntity = null,
                        languageIdColumnName = "",
                        matchInfo = androidx.room3.FtsOptions.MatchInfo.FTS4,
                        notIndexedColumns = emptyList(),
                        prefixSizes = emptyList(),
                        preferredOrder = androidx.room3.FtsOptions.Order.ASC,
                        contentRowId = null,
                        columnSize = true,
                        detail = null,
                    ),
            )

        assertThat(
            entity.createTableQuery,
            `is`(
                "CREATE VIRTUAL TABLE IF NOT EXISTS `Mail` USING FTS5(" +
                    "`body`, " +
                    "tokenize=`unicode61 remove_diacritics 0 tokenchars '-_'`" +
                    ")"
            ),
        )
    }

    fun createField(name: String): Property {
        val (element, type) = mockElementAndType()
        return Property(
            element = element,
            name = name,
            type = type,
            affinity = null,
            collate = null,
            columnName = name,
        )
    }
}
