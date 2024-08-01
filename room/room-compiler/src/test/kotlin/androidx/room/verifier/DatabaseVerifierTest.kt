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

package androidx.room.verifier

import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.processing.XConstructorElement
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.CommonTypeNames
import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.parser.SqlParser
import androidx.room.processor.Context
import androidx.room.testing.context
import androidx.room.vo.CallType
import androidx.room.vo.Constructor
import androidx.room.vo.Database
import androidx.room.vo.DatabaseView
import androidx.room.vo.Entity
import androidx.room.vo.Field
import androidx.room.vo.FieldGetter
import androidx.room.vo.FieldSetter
import androidx.room.vo.Fields
import androidx.room.vo.PrimaryKey
import java.sql.Connection
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

@RunWith(Parameterized::class)
class DatabaseVerifierTest(private val useLocalizedCollation: Boolean) {
    @Test
    fun testSimpleDatabase() {
        runProcessorTest { invocation ->
            val verifier = createVerifier(invocation)
            val stmt = verifier.connection.createStatement()
            val rs = stmt.executeQuery("select * from sqlite_master WHERE type='table'")
            assertThat(rs.collect { set -> set.getString("name") }, hasItem(`is`("User")))
            val table = verifier.connection.prepareStatement("select * from User")
            assertThat(table.columnNames(), `is`(listOf("id", "name", "lastName", "ratio")))

            assertThat(getPrimaryKeys(verifier.connection, "User"), `is`(listOf("id")))
        }
    }

    private fun createVerifier(invocation: XTestInvocation): DatabaseVerifier {
        val db = userDb(invocation)
        return DatabaseVerifier.create(
            invocation.context,
            mock(XElement::class.java),
            db.entities,
            db.views
        )!!
    }

    @Test
    fun testFullEntityQuery() {
        validQueryTest("select * from User") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER, "User"),
                            ColumnInfo("name", SQLTypeAffinity.TEXT, "User"),
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT, "User"),
                            ColumnInfo("ratio", SQLTypeAffinity.REAL, "User")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testPartialFields() {
        validQueryTest("select id, lastName from User") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER, "User"),
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT, "User")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testFlattenQuery() {
        validQueryTest("SELECT id, lastName FROM (select * from User)") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER, "User"),
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT, "User"),
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testColumnSubquery() {
        validQueryTest(
            """
            SELECT
                lastName,
                (SELECT COUNT(*) FROM user AS iu WHERE iu.lastName = u.lastName) = 1 AS isUnique
            FROM user AS u
            GROUP BY lastName
            """
                .trimIndent()
        ) {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT, "User"),
                            ColumnInfo("isUnique", SQLTypeAffinity.NULL, null),
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testRenamedField() {
        validQueryTest("select id as myId, lastName from User") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("myId", SQLTypeAffinity.INTEGER, "User"),
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT, "User")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testGrouped() {
        validQueryTest("select MAX(ratio) from User GROUP BY name") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            // unfortunately, we don't get this information
                            ColumnInfo("MAX(ratio)", SQLTypeAffinity.NULL, null)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testConcat() {
        validQueryTest("select name || lastName as mergedName from User") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            // unfortunately, we don't get this information
                            ColumnInfo("mergedName", SQLTypeAffinity.NULL, null)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testResultWithArgs() {
        validQueryTest("select id, name || lastName as mergedName from User where name LIKE ?") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER, "User"),
                            // unfortunately, we don't get this information
                            ColumnInfo("mergedName", SQLTypeAffinity.NULL, null)
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testDeleteQuery() {
        validQueryTest("delete from User where name LIKE ?") {
            assertThat(it, `is`(QueryResultInfo(emptyList())))
        }
    }

    @Test
    fun testUpdateQuery() {
        validQueryTest("update User set name = ? WHERE id = ?") {
            assertThat(it, `is`(QueryResultInfo(emptyList())))
        }
    }

    @Test
    fun testBadQuery() {
        runProcessorTest { invocation ->
            val verifier = createVerifier(invocation)
            val (_, error) = verifier.analyze("select foo from User")
            assertThat(error, notNullValue())
        }
    }

    @Test
    fun testCollate() {
        validQueryTest("SELECT id, name FROM user ORDER BY name COLLATE LOCALIZED ASC") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER, "User"),
                            ColumnInfo("name", SQLTypeAffinity.TEXT, "User")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testCollateBasQuery() {
        runProcessorTest { invocation ->
            val verifier = createVerifier(invocation)
            val (_, error) =
                verifier.analyze("SELECT id, name FROM user ORDER BY name COLLATE LOCALIZEDASC")
            assertThat(error, notNullValue())
        }
    }

    @Test
    fun testFullViewQuery() {
        validQueryTest("select * from UserSummary") {
            assertThat(
                it,
                `is`(
                    QueryResultInfo(
                        listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER, "User"),
                            ColumnInfo("name", SQLTypeAffinity.TEXT, "User")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun testViewNoSuchColumn() {
        runProcessorTest { invocation ->
            val verifier = createVerifier(invocation)
            val (_, error) = verifier.analyze("SELECT ratio FROM UserSummary")
            assertThat(error, notNullValue())
            assertThat(error?.message, containsString("no such column: ratio"))
        }
    }

    @Test
    fun defaultValue_exprError() {
        runProcessorTest { invocation ->
            val db =
                database(
                    listOf(
                        entity(
                            invocation,
                            "User",
                            field(
                                "id",
                                primitive(invocation.context, XTypeName.PRIMITIVE_INT),
                                SQLTypeAffinity.INTEGER
                            ),
                            field(
                                "name",
                                invocation.context.processingEnv.requireType(
                                    CommonTypeNames.STRING
                                ),
                                SQLTypeAffinity.TEXT,
                                defaultValue = "(NO_SUCH_CONSTANT)"
                            )
                        )
                    ),
                    emptyList()
                )
            invocation.assertCompilationResult {
                hasErrorContaining("default value of column [name]")
            }
            val element = mock(XElement::class.java)
            DatabaseVerifier.create(invocation.context, element, db.entities, db.views)!!
        }
    }

    private fun validQueryTest(sql: String, cb: (QueryResultInfo) -> Unit) {
        runProcessorTest { invocation ->
            val verifier = createVerifier(invocation)
            val info = verifier.analyze(sql)
            cb(info)
        }
    }

    private fun userDb(invocation: XTestInvocation): Database {
        val context = invocation.context
        return database(
            listOf(
                entity(
                    invocation,
                    "User",
                    field(
                        "id",
                        primitive(context, XTypeName.PRIMITIVE_INT),
                        SQLTypeAffinity.INTEGER
                    ),
                    field(
                        "name",
                        context.processingEnv.requireType(CommonTypeNames.STRING),
                        SQLTypeAffinity.TEXT
                    ),
                    field(
                        "lastName",
                        context.processingEnv.requireType(CommonTypeNames.STRING),
                        SQLTypeAffinity.TEXT
                    ),
                    field(
                        "ratio",
                        primitive(context, XTypeName.PRIMITIVE_FLOAT),
                        SQLTypeAffinity.REAL
                    )
                )
            ),
            listOf(
                view(
                    "UserSummary",
                    "SELECT id, name FROM User",
                    field(
                        "id",
                        primitive(context, XTypeName.PRIMITIVE_INT),
                        SQLTypeAffinity.INTEGER
                    ),
                    field(
                        "name",
                        context.processingEnv.requireType(CommonTypeNames.STRING),
                        SQLTypeAffinity.TEXT
                    )
                )
            )
        )
    }

    private fun database(entities: List<Entity>, views: List<DatabaseView>): Database {
        return Database(
            element = mock(XTypeElement::class.java),
            type = mock(XType::class.java),
            entities = entities,
            views = views,
            daoMethods = emptyList(),
            version = -1,
            exportSchema = false,
            enableForeignKeys = false,
            overrideClearAllTables = true,
            constructorObjectElement = null,
        )
    }

    private fun entity(
        invocation: XTestInvocation,
        tableName: String,
        vararg fields: Field
    ): Entity {
        val element = invocation.processingEnv.requireTypeElement("java.lang.String")
        return Entity(
            element = element,
            tableName = tableName,
            type = mock(XType::class.java),
            fields = fields.toList(),
            embeddedFields = emptyList(),
            indices = emptyList(),
            primaryKey = PrimaryKey(null, Fields(fields.take(1)), false),
            foreignKeys = emptyList(),
            constructor = Constructor(mock(XConstructorElement::class.java), emptyList()),
            shadowTableName = null
        )
    }

    private fun view(viewName: String, query: String, vararg fields: Field): DatabaseView {
        return DatabaseView(
            element = mock(XTypeElement::class.java),
            viewName = viewName,
            type = mock(XType::class.java),
            fields = fields.toList(),
            embeddedFields = emptyList(),
            query = SqlParser.parse(query),
            constructor = Constructor(mock(XConstructorElement::class.java), emptyList())
        )
    }

    private fun field(
        name: String,
        type: XType,
        affinity: SQLTypeAffinity,
        defaultValue: String? = null
    ): Field {
        val element = mock(XFieldElement::class.java)
        doReturn(type).`when`(element).type
        val f =
            Field(
                element = element,
                name = name,
                type = type,
                columnName = name,
                affinity = affinity,
                collate =
                    if (useLocalizedCollation && affinity == SQLTypeAffinity.TEXT) {
                        Collate.LOCALIZED
                    } else {
                        null
                    },
                defaultValue = defaultValue
            )
        assignGetterSetter(f, name, type)
        return f
    }

    private fun assignGetterSetter(f: Field, name: String, type: XType) {
        f.getter = FieldGetter(f.name, name, type, CallType.FIELD)
        f.setter = FieldSetter(f.name, name, type, CallType.FIELD)
    }

    private fun primitive(context: Context, typeName: XTypeName): XType {
        return context.processingEnv.requireType(typeName)
    }

    private fun getPrimaryKeys(connection: Connection, tableName: String): List<String> {
        val stmt = connection.createStatement()
        val resultSet = stmt.executeQuery("PRAGMA table_info($tableName)")
        return resultSet
            .collect { Pair(it.getString("name"), it.getInt("pk")) }
            .filter { it.second > 0 }
            .sortedBy { it.second }
            .map { it.first }
    }

    companion object {
        @Parameterized.Parameters(name = "useLocalizedCollation={0}")
        @JvmStatic
        fun params() = arrayListOf(true, false)
    }
}
