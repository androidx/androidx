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

import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import org.apache.commons.codec.digest.DigestUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
class DatabaseTest {

    @Test
    fun indexLegacyHash() {
        val database =
            Database(
                element = mock(XTypeElement::class.java),
                type = mock(XType::class.java),
                entities =
                    listOf(
                        Entity(
                            mock(XTypeElement::class.java),
                            tableName = "TheTable",
                            type = mock(XType::class.java),
                            fields = emptyList(),
                            embeddedFields = emptyList(),
                            primaryKey = PrimaryKey(mock(XElement::class.java), Fields(), false),
                            indices =
                                listOf(
                                    Index(
                                        name = "leIndex",
                                        unique = false,
                                        fields = Fields(),
                                        orders = emptyList()
                                    ),
                                    Index(
                                        name = "leIndex2",
                                        unique = true,
                                        fields = Fields(),
                                        orders = emptyList()
                                    )
                                ),
                            foreignKeys = emptyList(),
                            constructor =
                                Constructor(mock(XConstructorElement::class.java), emptyList()),
                            shadowTableName = null
                        )
                    ),
                views = emptyList(),
                daoMethods = emptyList(),
                version = 1,
                exportSchema = false,
                enableForeignKeys = false,
                overrideClearAllTables = true,
                constructorObject = null,
            )

        val expectedLegacyHash =
            DigestUtils.md5Hex(
                "CREATE TABLE IF NOT EXISTS `TheTable` ()¯\\_(ツ)_/¯" +
                    "CREATE  INDEX `leIndex` ON `TheTable` ()¯\\_(ツ)_/¯" +
                    "CREATE UNIQUE INDEX `leIndex2` ON `TheTable` ()"
            )
        assertEquals(expectedLegacyHash, database.legacyIdentityHash)
    }
}
