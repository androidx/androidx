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

package androidx.room.vo

import org.apache.commons.codec.digest.DigestUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror

@RunWith(JUnit4::class)
class DatabaseTest {

    @Test
    fun indexLegacyHash() {
        val database = Database(
            element = mock(TypeElement::class.java),
            type = mock(TypeMirror::class.java),
            entities = listOf(
                Entity(
                    mock(TypeElement::class.java),
                    tableName = "TheTable",
                    type = mock(DeclaredType::class.java),
                    fields = emptyList(),
                    embeddedFields = emptyList(),
                    primaryKey = PrimaryKey(mock(Element::class.java), Fields(), false),
                    indices = listOf(
                        Index(
                            name = "leIndex",
                            unique = false,
                            fields = Fields()),
                        Index(
                            name = "leIndex2",
                            unique = true,
                            fields = Fields())
                    ),
                    foreignKeys = emptyList(),
                    constructor = Constructor(mock(ExecutableElement::class.java), emptyList()),
                    shadowTableName = null
                )
            ),
            views = emptyList(),
            daoMethods = emptyList(),
            version = 1,
            exportSchema = false,
            enableForeignKeys = false
        )

        val expectedLegacyHash = DigestUtils.md5Hex(
            "CREATE TABLE IF NOT EXISTS `TheTable` ()¯\\_(ツ)_/¯" +
                    "CREATE  INDEX `leIndex` ON `TheTable` ()¯\\_(ツ)_/¯" +
                    "CREATE UNIQUE INDEX `leIndex2` ON `TheTable` ()")
        assertEquals(expectedLegacyHash, database.legacyIdentityHash)
    }
}