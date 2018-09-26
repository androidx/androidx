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

package androidx.room.writer

import COMMON
import androidx.room.RoomProcessor
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import loadJavaCode
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import javax.tools.JavaFileObject

@RunWith(Enclosed::class)
class DatabaseWriterTest {

    class SmallDB {

        @Test
        fun testCompileAndVerifySources() {
            singleDb(
                    loadJavaCode("databasewriter/input/ComplexDatabase.java",
                            "foo.bar.ComplexDatabase"),
                    loadJavaCode("daoWriter/input/ComplexDao.java",
                            "foo.bar.ComplexDao")
            ).compilesWithoutError().and().generatesSources(
                    loadJavaCode("databasewriter/output/ComplexDatabase.java",
                            "foo.bar.ComplexDatabase_Impl")
            )
        }
    }

    @RunWith(Parameterized::class)
    class BigDB(val config: Pair<Int, Int>) {

        @Test
        fun testCompile() {
            val (maxStatementCount, valuesPerEntity) = config
            val entitySources = mutableListOf<Pair<String, JavaFileObject>>()
            var entityCount = 1
            var statementCount = 0
            while (statementCount < maxStatementCount) {
                val entityValues = StringBuilder().apply {
                    for (i in 1..valuesPerEntity) {
                        append("""
                    private String value$i;
                    public String getValue$i() { return this.value$i; }
                    public void setValue$i(String value) { this.value$i = value; }

                    """)
                    }
                }
                val entitySource = JavaFileObjects.forSourceLines("foo.bar.Entity$entityCount",
                        """
                    package foo.bar;

                    import androidx.room.*;

                    @Entity
                    public class Entity$entityCount {

                        @PrimaryKey
                        private long id;

                        public long getId() { return this.id; }
                        public void setId(long id) { this.id = id; }

                        $entityValues
                    }
                    """)
                entitySources.add("Entity$entityCount" to entitySource)
                statementCount += valuesPerEntity
                entityCount++
            }
            val entityClasses = entitySources.joinToString { "${it.first}.class" }
            val dbSource = JavaFileObjects.forSourceLines("foo.bar.TestDatabase",
                    """
                    package foo.bar;

                    import androidx.room.*;

                    @Database(entities = {$entityClasses}, version = 1)
                    public abstract class TestDatabase extends RoomDatabase {}
                    """)
            singleDb(*(listOf(dbSource) + entitySources.map { it.second }).toTypedArray())
                    .compilesWithoutError()
        }

        companion object {
            @Parameterized.Parameters(name = "(maxStatementCount, valuesPerEntity)={0}")
            @JvmStatic
            fun getParams(): List<Pair<Int, Int>> {
                val result = arrayListOf<Pair<Int, Int>>()
                arrayListOf(500, 1000, 3000).forEach { maxStatementCount ->
                    arrayListOf(50, 100, 200).forEach { valuesPerEntity ->
                        result.add(maxStatementCount to valuesPerEntity)
                    }
                }
                return result
            }
        }
    }
}

private fun singleDb(vararg jfo: JavaFileObject): CompileTester {
    return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
            .that(jfo.toList() + COMMON.USER + COMMON.USER_SUMMARY + COMMON.LIVE_DATA +
                    COMMON.COMPUTABLE_LIVE_DATA)
            .processedWith(RoomProcessor())
}
