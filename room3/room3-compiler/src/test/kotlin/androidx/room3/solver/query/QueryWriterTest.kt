/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room3.solver.query

import androidx.kruth.assertThat
import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.codegen.compat.XConverters.toString
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.ext.RoomTypeNames.ROOM_SQL_QUERY
import androidx.room3.processor.QueryFunctionProcessor
import androidx.room3.testing.context
import androidx.room3.writer.QueryWriter
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import testCodeGenScope

@RunWith(JUnit4::class)
class QueryWriterTest {
    companion object {
        const val DAO_PREFIX =
            """
                package foo.bar;
                import androidx.room3.*;
                import java.util.*;
                import com.google.common.collect.ImmutableList;
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val QUERY = ROOM_SQL_QUERY.toString(CodeLanguage.KOTLIN)
    }

    @Test
    fun simpleNoArgQuery() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users")
                abstract java.util.List<Integer> selectAllIds();
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _sql: kotlin.String = "SELECT id FROM users"
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, 0)
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun simpleStringArgs() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE name LIKE :name")
                abstract java.util.List<Integer> selectAllIds(String name);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            val expectedStringBind =
                """
                if (name == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  _stmt.bindText(_argIndex, name)
                }
                """
                    .trimIndent()
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                |val _sql: kotlin.String = "SELECT id FROM users WHERE name LIKE ?"
                |val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, 1)
                |var _argIndex: kotlin.Int = 1
                |$expectedStringBind
                """
                        .trimMargin()
                )
        }
    }

    @Test
    fun twoIntArgs() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE id IN(:id1,:id2)")
                abstract java.util.List<Integer> selectAllIds(int id1, int id2);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _sql: kotlin.String = "SELECT id FROM users WHERE id IN(?,?)"
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, 2)
                var _argIndex: kotlin.Int = 1
                _stmt.bindLong(_argIndex, id1.toLong())
                _argIndex = 2
                _stmt.bindLong(_argIndex, id2.toLong())
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun aLongAndIntVarArg() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE id IN(:ids) AND age > :time")
                abstract java.util.List<Integer> selectAllIds(long time, int... ids);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _stringBuilder: kotlin.text.StringBuilder = kotlin.text.StringBuilder()
                _stringBuilder.append("SELECT id FROM users WHERE id IN(")
                val _inputSize: kotlin.Int = if (ids == null) 1 else ids.size
                androidx.room3.util.appendPlaceholders(_stringBuilder, _inputSize)
                _stringBuilder.append(") AND age > ")
                _stringBuilder.append("?")
                val _sql: kotlin.String = _stringBuilder.toString()
                val _argCount: kotlin.Int = 1 + _inputSize
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, _argCount)
                var _argIndex: kotlin.Int = 1
                if (ids == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  for (_item: kotlin.Int in ids) {
                    _stmt.bindLong(_argIndex, _item.toLong())
                    _argIndex++
                  }
                }
                _argIndex = 1 + _inputSize
                _stmt.bindLong(_argIndex, time)
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun aLongAndIntegerList() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE id IN(:ids) AND age > :time")
                abstract List<Integer> selectAllIds(long time, List<Integer> ids);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _stringBuilder: kotlin.text.StringBuilder = kotlin.text.StringBuilder()
                _stringBuilder.append("SELECT id FROM users WHERE id IN(")
                val _inputSize: kotlin.Int = if (ids == null) 1 else ids.size
                androidx.room3.util.appendPlaceholders(_stringBuilder, _inputSize)
                _stringBuilder.append(") AND age > ")
                _stringBuilder.append("?")
                val _sql: kotlin.String = _stringBuilder.toString()
                val _argCount: kotlin.Int = 1 + _inputSize
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, _argCount)
                var _argIndex: kotlin.Int = 1
                if (ids == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  for (_item: kotlin.Int? in ids) {
                    if (_item == null) {
                      _stmt.bindNull(_argIndex)
                    } else {
                      _stmt.bindLong(_argIndex, _item.toLong())
                    }
                    _argIndex++
                  }
                }
                _argIndex = 1 + _inputSize
                _stmt.bindLong(_argIndex, time)
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun aLongAndIntegerImmutableList() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE id IN(:ids) AND age > :time")
                abstract ImmutableList<Integer> selectAllIds(long time, List<Integer> ids);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _stringBuilder: kotlin.text.StringBuilder = kotlin.text.StringBuilder()
                _stringBuilder.append("SELECT id FROM users WHERE id IN(")
                val _inputSize: kotlin.Int = if (ids == null) 1 else ids.size
                androidx.room3.util.appendPlaceholders(_stringBuilder, _inputSize)
                _stringBuilder.append(") AND age > ")
                _stringBuilder.append("?")
                val _sql: kotlin.String = _stringBuilder.toString()
                val _argCount: kotlin.Int = 1 + _inputSize
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, _argCount)
                var _argIndex: kotlin.Int = 1
                if (ids == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  for (_item: kotlin.Int? in ids) {
                    if (_item == null) {
                      _stmt.bindNull(_argIndex)
                    } else {
                      _stmt.bindLong(_argIndex, _item.toLong())
                    }
                    _argIndex++
                  }
                }
                _argIndex = 1 + _inputSize
                _stmt.bindLong(_argIndex, time)
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun aLongAndIntegerSet() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE id IN(:ids) AND age > :time")
                abstract List<Integer> selectAllIds(long time, Set<Integer> ids);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _stringBuilder: kotlin.text.StringBuilder = kotlin.text.StringBuilder()
                _stringBuilder.append("SELECT id FROM users WHERE id IN(")
                val _inputSize: kotlin.Int = if (ids == null) 1 else ids.size
                androidx.room3.util.appendPlaceholders(_stringBuilder, _inputSize)
                _stringBuilder.append(") AND age > ")
                _stringBuilder.append("?")
                val _sql: kotlin.String = _stringBuilder.toString()
                val _argCount: kotlin.Int = 1 + _inputSize
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, _argCount)
                var _argIndex: kotlin.Int = 1
                if (ids == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  for (_item: kotlin.Int? in ids) {
                    if (_item == null) {
                      _stmt.bindNull(_argIndex)
                    } else {
                      _stmt.bindLong(_argIndex, _item.toLong())
                    }
                    _argIndex++
                  }
                }
                _argIndex = 1 + _inputSize
                _stmt.bindLong(_argIndex, time)
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun testMultipleBindParamsWithSameName() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE age > :age OR bage > :age")
                abstract List<Integer> selectAllIds(int age);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _sql: kotlin.String = "SELECT id FROM users WHERE age > ? OR bage > ?"
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, 2)
                var _argIndex: kotlin.Int = 1
                _stmt.bindLong(_argIndex, age.toLong())
                _argIndex = 2
                _stmt.bindLong(_argIndex, age.toLong())
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun testMultipleBindParamsWithSameNameWithVarArg() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE age > :age OR bage > :age OR fage IN(:ages)")
                abstract List<Integer> selectAllIds(int age, int... ages);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _stringBuilder: kotlin.text.StringBuilder = kotlin.text.StringBuilder()
                _stringBuilder.append("SELECT id FROM users WHERE age > ")
                _stringBuilder.append("?")
                _stringBuilder.append(" OR bage > ")
                _stringBuilder.append("?")
                _stringBuilder.append(" OR fage IN(")
                val _inputSize: kotlin.Int = if (ages == null) 1 else ages.size
                androidx.room3.util.appendPlaceholders(_stringBuilder, _inputSize)
                _stringBuilder.append(")")
                val _sql: kotlin.String = _stringBuilder.toString()
                val _argCount: kotlin.Int = 2 + _inputSize
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, _argCount)
                var _argIndex: kotlin.Int = 1
                _stmt.bindLong(_argIndex, age.toLong())
                _argIndex = 2
                _stmt.bindLong(_argIndex, age.toLong())
                _argIndex = 3
                if (ages == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  for (_item: kotlin.Int in ages) {
                    _stmt.bindLong(_argIndex, _item.toLong())
                    _argIndex++
                  }
                }
                """
                        .trimIndent()
                )
        }
    }

    @Test
    fun testMultipleBindParamsWithSameNameWithVarArgInTwoBindings() {
        singleQueryMethod(
            """
                @Query("SELECT id FROM users WHERE age IN (:ages) OR bage > :age OR fage IN(:ages)")
                abstract List<Integer> selectAllIds(int age, int... ages);
                """
        ) { _, writer ->
            val scope = testCodeGenScope()
            writer.prepareReadAndBind("_sql", "_stmt", scope)
            assertThat(scope.generate().toString(CodeLanguage.KOTLIN).trim())
                .isEqualTo(
                    """
                val _stringBuilder: kotlin.text.StringBuilder = kotlin.text.StringBuilder()
                _stringBuilder.append("SELECT id FROM users WHERE age IN (")
                val _inputSize: kotlin.Int = if (ages == null) 1 else ages.size
                androidx.room3.util.appendPlaceholders(_stringBuilder, _inputSize)
                _stringBuilder.append(") OR bage > ")
                _stringBuilder.append("?")
                _stringBuilder.append(" OR fage IN(")
                val _inputSize_1: kotlin.Int = if (ages == null) 1 else ages.size
                androidx.room3.util.appendPlaceholders(_stringBuilder, _inputSize_1)
                _stringBuilder.append(")")
                val _sql: kotlin.String = _stringBuilder.toString()
                val _argCount: kotlin.Int = 1 + _inputSize + _inputSize_1
                val _stmt: $QUERY = $QUERY.Companion.acquire(_sql, _argCount)
                var _argIndex: kotlin.Int = 1
                if (ages == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  for (_item: kotlin.Int in ages) {
                    _stmt.bindLong(_argIndex, _item.toLong())
                    _argIndex++
                  }
                }
                _argIndex = 1 + _inputSize
                _stmt.bindLong(_argIndex, age.toLong())
                _argIndex = 2 + _inputSize
                if (ages == null) {
                  _stmt.bindNull(_argIndex)
                } else {
                  for (_item_1: kotlin.Int in ages) {
                    _stmt.bindLong(_argIndex, _item_1.toLong())
                    _argIndex++
                  }
                }
                """
                        .trimIndent()
                )
        }
    }

    fun singleQueryMethod(vararg input: String, handler: (Boolean, QueryWriter) -> Unit) {
        val source =
            Source.java("foo.bar.MyClass", DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX)
        runKspTest(sources = listOf(source)) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(Query::class) }.toList(),
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val parser =
                QueryFunctionProcessor(
                    baseContext = invocation.context,
                    containing = owner.type,
                    executableElement = methods.first(),
                )
            val method = parser.process()
            handler(invocation.isKsp, QueryWriter(method))
        }
    }
}
