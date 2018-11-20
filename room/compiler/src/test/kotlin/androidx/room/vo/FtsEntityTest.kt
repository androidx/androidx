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

package androidx.room.vo

import androidx.room.parser.FtsVersion
import mockElementAndType
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType

@RunWith(JUnit4::class)
class FtsEntityTest {

    @Test
    fun createStatement() {
        val primaryKeyField = createField("rowid")
        val languageIdField = createField("lid")
        val bodyField = createField("body")
        val dontIndexMe1Field = createField("dontIndexMe1")
        val dontIndexMe2Field = createField("dontIndexMe2")
        val entity = FtsEntity(
                element = mock(TypeElement::class.java),
                tableName = "Mail",
                type = mock(DeclaredType::class.java),
                fields = listOf(primaryKeyField, bodyField, languageIdField, dontIndexMe1Field,
                        dontIndexMe2Field),
                embeddedFields = emptyList(),
                primaryKey = PrimaryKey(
                        declaredIn = mock(Element::class.java),
                        fields = listOf(primaryKeyField),
                        autoGenerateId = true),
                constructor = null,
                shadowTableName = "Mail_context",
                ftsVersion = FtsVersion.FTS4,
                ftsOptions = FtsOptions(
                        tokenizer = androidx.room.FtsOptions.TOKENIZER_PORTER,
                        tokenizerArgs = emptyList(),
                        contentEntity = null,
                        languageIdColumnName = "lid",
                        matchInfo = androidx.room.FtsOptions.MatchInfo.FTS3,
                        notIndexedColumns = listOf("dontIndexMe1", "dontIndexMe2"),
                        prefixSizes = listOf(2, 4),
                        preferredOrder = androidx.room.FtsOptions.Order.DESC))

        assertThat(entity.createTableQuery,
                `is`("CREATE VIRTUAL TABLE IF NOT EXISTS `Mail` USING FTS4(" +
                        "`body` TEXT, " +
                        "`dontIndexMe1` TEXT, " +
                        "`dontIndexMe2` TEXT, " +
                        "tokenize=porter, " +
                        "languageid=`lid`, " +
                        "matchinfo=fts3, " +
                        "notindexed=`dontIndexMe1`, " +
                        "notindexed=`dontIndexMe2`, " +
                        "prefix=`2,4`, " +
                        "order=DESC" +
                        ")"
                )
        )
    }

    fun createField(name: String): Field {
        val (element, type) = mockElementAndType()
        return Field(
                element = element,
                name = name,
                type = type,
                affinity = null,
                collate = null,
                columnName = name)
    }
}