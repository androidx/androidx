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

import androidx.room.parser.Collate
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.Context
import androidx.room.testing.TestInvocation
import androidx.room.vo.CallType
import androidx.room.vo.Constructor
import androidx.room.vo.Database
import androidx.room.vo.Entity
import androidx.room.vo.Field
import androidx.room.vo.FieldGetter
import androidx.room.vo.FieldSetter
import androidx.room.vo.PrimaryKey
import collect
import columnNames
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import simpleRun
import java.sql.Connection
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror

@RunWith(Parameterized::class)
class DatabaseVerifierTest(private val useLocalizedCollation: Boolean) {
    @Test
    fun testSimpleDatabase() {
        simpleRun { invocation ->
            val verifier = createVerifier(invocation)
            val stmt = verifier.connection.createStatement()
            val rs = stmt.executeQuery("select * from sqlite_master WHERE type='table'")
            assertThat(
                    rs.collect { set -> set.getString("name") }, hasItem(`is`("User")))
            val table = verifier.connection.prepareStatement("select * from User")
            assertThat(table.columnNames(), `is`(listOf("id", "name", "lastName", "ratio")))

            assertThat(getPrimaryKeys(verifier.connection, "User"), `is`(listOf("id")))
        }.compilesWithoutError()
    }

    private fun createVerifier(invocation: TestInvocation): DatabaseVerifier {
        return DatabaseVerifier.create(invocation.context, mock(Element::class.java),
                userDb(invocation.context).entities)!!
    }

    @Test
    fun testFullEntityQuery() {
        validQueryTest("select * from User") {
            assertThat(it, `is`(
                    QueryResultInfo(listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER),
                            ColumnInfo("name", SQLTypeAffinity.TEXT),
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT),
                            ColumnInfo("ratio", SQLTypeAffinity.REAL)
                    ))))
        }
    }

    @Test
    fun testPartialFields() {
        validQueryTest("select id, lastName from User") {
            assertThat(it, `is`(
                    QueryResultInfo(listOf(
                            ColumnInfo("id", SQLTypeAffinity.INTEGER),
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT)
                    ))))
        }
    }

    @Test
    fun testRenamedField() {
        validQueryTest("select id as myId, lastName from User") {
            assertThat(it, `is`(
                    QueryResultInfo(listOf(
                            ColumnInfo("myId", SQLTypeAffinity.INTEGER),
                            ColumnInfo("lastName", SQLTypeAffinity.TEXT)
                    ))))
        }
    }

    @Test
    fun testGrouped() {
        validQueryTest("select MAX(ratio) from User GROUP BY name") {
            assertThat(it, `is`(
                    QueryResultInfo(listOf(
                            // unfortunately, we don't get this information
                            ColumnInfo("MAX(ratio)", SQLTypeAffinity.NULL)
                    ))))
        }
    }

    @Test
    fun testConcat() {
        validQueryTest("select name || lastName as mergedName from User") {
            assertThat(it, `is`(
                    QueryResultInfo(listOf(
                            // unfortunately, we don't get this information
                            ColumnInfo("mergedName", SQLTypeAffinity.NULL)
                    ))))
        }
    }

    @Test
    fun testResultWithArgs() {
        validQueryTest("select id, name || lastName as mergedName from User where name LIKE ?") {
            assertThat(it, `is`(
                    QueryResultInfo(listOf(
                            // unfortunately, we don't get this information
                            ColumnInfo("id", SQLTypeAffinity.INTEGER),
                            ColumnInfo("mergedName", SQLTypeAffinity.NULL)
                    ))))
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
        simpleRun { invocation ->
            val verifier = createVerifier(invocation)
            val (_, error) = verifier.analyze("select foo from User")
            assertThat(error, notNullValue())
        }.compilesWithoutError()
    }

    @Test
    fun testCollate() {
        validQueryTest("SELECT id, name FROM user ORDER BY name COLLATE LOCALIZED ASC") {
            assertThat(it, `is`(
                    QueryResultInfo(listOf(
                            // unfortunately, we don't get this information
                            ColumnInfo("id", SQLTypeAffinity.INTEGER),
                            ColumnInfo("name", SQLTypeAffinity.TEXT)
                    ))))
        }
    }

    @Test
    fun testCollateBasQuery() {
        simpleRun { invocation ->
            val verifier = createVerifier(invocation)
            val (_, error) = verifier.analyze(
                    "SELECT id, name FROM user ORDER BY name COLLATE LOCALIZEDASC")
            assertThat(error, notNullValue())
        }.compilesWithoutError()
    }

    private fun validQueryTest(sql: String, cb: (QueryResultInfo) -> Unit) {
        simpleRun { invocation ->
            val verifier = createVerifier(invocation)
            val info = verifier.analyze(sql)
            cb(info)
        }.compilesWithoutError()
    }

    private fun userDb(context: Context): Database {
        return database(entity("User",
                field("id", primitive(context, TypeKind.INT), SQLTypeAffinity.INTEGER),
                field("name", context.COMMON_TYPES.STRING, SQLTypeAffinity.TEXT),
                field("lastName", context.COMMON_TYPES.STRING, SQLTypeAffinity.TEXT),
                field("ratio", primitive(context, TypeKind.FLOAT), SQLTypeAffinity.REAL)))
    }

    private fun database(vararg entities: Entity): Database {
        return Database(
                element = mock(TypeElement::class.java),
                type = mock(TypeMirror::class.java),
                entities = entities.toList(),
                daoMethods = emptyList(),
                version = -1,
                exportSchema = false,
                enableForeignKeys = false)
    }

    private fun entity(tableName: String, vararg fields: Field): Entity {
        return Entity(
                element = mock(TypeElement::class.java),
                tableName = tableName,
                type = mock(DeclaredType::class.java),
                fields = fields.toList(),
                embeddedFields = emptyList(),
                indices = emptyList(),
                primaryKey = PrimaryKey(null, fields.take(1), false),
                foreignKeys = emptyList(),
                constructor = Constructor(mock(ExecutableElement::class.java), emptyList())
        )
    }

    private fun field(name: String, type: TypeMirror, affinity: SQLTypeAffinity): Field {
        val element = mock(Element::class.java)
        doReturn(type).`when`(element).asType()
        val f = Field(
                element = element,
                name = name,
                type = type,
                columnName = name,
                affinity = affinity,
                collate = if (useLocalizedCollation && affinity == SQLTypeAffinity.TEXT) {
                    Collate.LOCALIZED
                } else {
                    null
                }
        )
        assignGetterSetter(f, name, type)
        return f
    }

    private fun assignGetterSetter(f: Field, name: String, type: TypeMirror) {
        f.getter = FieldGetter(name, type, CallType.FIELD)
        f.setter = FieldSetter(name, type, CallType.FIELD)
    }

    private fun primitive(context: Context, kind: TypeKind): PrimitiveType {
        return context.processingEnv.typeUtils.getPrimitiveType(kind)
    }

    private fun getPrimaryKeys(connection: Connection, tableName: String): List<String> {
        val stmt = connection.createStatement()
        val resultSet = stmt.executeQuery("PRAGMA table_info($tableName)")
        return resultSet.collect {
            Pair(it.getString("name"), it.getInt("pk"))
        }
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
