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

package androidx.room.solver.query

import androidx.kruth.assertThat
import androidx.room.Dao
import androidx.room.Query
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.ext.RoomTypeNames.ROOM_SQL_QUERY
import androidx.room.ext.RoomTypeNames.STRING_UTIL
import androidx.room.processor.QueryMethodProcessor
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.writer.QueryWriter
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
                import androidx.room.*;
                import java.util.*;
                import com.google.common.collect.ImmutableList;
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
        val QUERY = ROOM_SQL_QUERY.toString(CodeLanguage.JAVA)
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
            assertThat(scope.generate().toString().trim())
                .isEqualTo(
                    """
                final java.lang.String _sql = "SELECT id FROM users";
                final $QUERY _stmt = $QUERY.acquire(_sql, 0);
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
                  _stmt.bindNull(_argIndex);
                } else {
                  _stmt.bindString(_argIndex, name);
                }
                """
                    .trimIndent()
            assertThat(scope.generate().toString().trim())
                .isEqualTo(
                    """
                |final java.lang.String _sql = "SELECT id FROM users WHERE name LIKE ?";
                |final $QUERY _stmt = $QUERY.acquire(_sql, 1);
                |int _argIndex = 1;
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
            assertThat(scope.generate().toString().trim())
                .isEqualTo(
                    """
                final java.lang.String _sql = "SELECT id FROM users WHERE id IN(?,?)";
                final $QUERY _stmt = $QUERY.acquire(_sql, 2);
                int _argIndex = 1;
                _stmt.bindLong(_argIndex, id1);
                _argIndex = 2;
                _stmt.bindLong(_argIndex, id2);
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
            assertThat(scope.generate().toString().trim())
                .isEqualTo(
                    """
                final java.lang.StringBuilder _stringBuilder = new java.lang.StringBuilder();
                _stringBuilder.append("SELECT id FROM users WHERE id IN(");
                final int _inputSize = ids == null ? 1 : ids.length;
                ${STRING_UTIL.canonicalName}.appendPlaceholders(_stringBuilder, _inputSize);
                _stringBuilder.append(") AND age > ");
                _stringBuilder.append("?");
                final java.lang.String _sql = _stringBuilder.toString();
                final int _argCount = 1 + _inputSize;
                final $QUERY _stmt = $QUERY.acquire(_sql, _argCount);
                int _argIndex = 1;
                if (ids == null) {
                  _stmt.bindNull(_argIndex);
                } else {
                  for (int _item : ids) {
                    _stmt.bindLong(_argIndex, _item);
                    _argIndex++;
                  }
                }
                _argIndex = 1 + _inputSize;
                _stmt.bindLong(_argIndex, time);
                """
                        .trimIndent()
                )
        }
    }

    val collectionOut =
        """
        final java.lang.StringBuilder _stringBuilder = new java.lang.StringBuilder();
        _stringBuilder.append("SELECT id FROM users WHERE id IN(");
        final int _inputSize = ids == null ? 1 : ids.size();
        ${STRING_UTIL.canonicalName}.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(") AND age > ");
        _stringBuilder.append("?");
        final java.lang.String _sql = _stringBuilder.toString();
        final int _argCount = 1 + _inputSize;
        final $QUERY _stmt = $QUERY.acquire(_sql, _argCount);
        int _argIndex = 1;
        if (ids == null) {
          _stmt.bindNull(_argIndex);
        } else {
          for (java.lang.Integer _item : ids) {
            if (_item == null) {
              _stmt.bindNull(_argIndex);
            } else {
              _stmt.bindLong(_argIndex, _item);
            }
            _argIndex++;
          }
        }
        _argIndex = 1 + _inputSize;
        _stmt.bindLong(_argIndex, time);
    """
            .trimIndent()

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
            assertThat(scope.generate().toString().trim()).isEqualTo(collectionOut)
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
            assertThat(scope.generate().toString().trim()).isEqualTo(collectionOut)
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
            assertThat(scope.generate().toString().trim()).isEqualTo(collectionOut)
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
            assertThat(scope.generate().toString().trim())
                .isEqualTo(
                    """
                final java.lang.String _sql = "SELECT id FROM users WHERE age > ? OR bage > ?";
                final $QUERY _stmt = $QUERY.acquire(_sql, 2);
                int _argIndex = 1;
                _stmt.bindLong(_argIndex, age);
                _argIndex = 2;
                _stmt.bindLong(_argIndex, age);
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
            assertThat(scope.generate().toString().trim())
                .isEqualTo(
                    """
                final java.lang.StringBuilder _stringBuilder = new java.lang.StringBuilder();
                _stringBuilder.append("SELECT id FROM users WHERE age > ");
                _stringBuilder.append("?");
                _stringBuilder.append(" OR bage > ");
                _stringBuilder.append("?");
                _stringBuilder.append(" OR fage IN(");
                final int _inputSize = ages == null ? 1 : ages.length;
                ${STRING_UTIL.canonicalName}.appendPlaceholders(_stringBuilder, _inputSize);
                _stringBuilder.append(")");
                final java.lang.String _sql = _stringBuilder.toString();
                final int _argCount = 2 + _inputSize;
                final $QUERY _stmt = $QUERY.acquire(_sql, _argCount);
                int _argIndex = 1;
                _stmt.bindLong(_argIndex, age);
                _argIndex = 2;
                _stmt.bindLong(_argIndex, age);
                _argIndex = 3;
                if (ages == null) {
                  _stmt.bindNull(_argIndex);
                } else {
                  for (int _item : ages) {
                    _stmt.bindLong(_argIndex, _item);
                    _argIndex++;
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
            assertThat(scope.generate().toString().trim())
                .isEqualTo(
                    """
                final java.lang.StringBuilder _stringBuilder = new java.lang.StringBuilder();
                _stringBuilder.append("SELECT id FROM users WHERE age IN (");
                final int _inputSize = ages == null ? 1 : ages.length;
                ${STRING_UTIL.canonicalName}.appendPlaceholders(_stringBuilder, _inputSize);
                _stringBuilder.append(") OR bage > ");
                _stringBuilder.append("?");
                _stringBuilder.append(" OR fage IN(");
                final int _inputSize_1 = ages == null ? 1 : ages.length;
                ${STRING_UTIL.canonicalName}.appendPlaceholders(_stringBuilder, _inputSize_1);
                _stringBuilder.append(")");
                final java.lang.String _sql = _stringBuilder.toString();
                final int _argCount = 1 + _inputSize + _inputSize_1;
                final $QUERY _stmt = $QUERY.acquire(_sql, _argCount);
                int _argIndex = 1;
                if (ages == null) {
                  _stmt.bindNull(_argIndex);
                } else {
                  for (int _item : ages) {
                    _stmt.bindLong(_argIndex, _item);
                    _argIndex++;
                  }
                }
                _argIndex = 1 + _inputSize;
                _stmt.bindLong(_argIndex, age);
                _argIndex = 2 + _inputSize;
                if (ages == null) {
                  _stmt.bindNull(_argIndex);
                } else {
                  for (int _item_1 : ages) {
                    _stmt.bindLong(_argIndex, _item_1);
                    _argIndex++;
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
        runProcessorTestWithK1(sources = listOf(source)) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods().filter { it.hasAnnotation(Query::class) }.toList()
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val parser =
                QueryMethodProcessor(
                    baseContext = invocation.context,
                    containing = owner.type,
                    executableElement = methods.first()
                )
            val method = parser.process()
            handler(invocation.isKsp, QueryWriter(method))
        }
    }
}
