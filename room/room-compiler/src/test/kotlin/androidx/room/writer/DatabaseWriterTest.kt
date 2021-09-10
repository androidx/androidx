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
import androidx.room.DatabaseProcessingStep
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.testing.asTestInvocationHandler
import loadTestSource
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class DatabaseWriterTest {

    class SmallDB {

        @Test
        fun testCompileAndVerifySources() {
            singleDb(
                loadTestSource(
                    fileName = "databasewriter/input/ComplexDatabase.java",
                    qName = "foo.bar.ComplexDatabase"
                ),
                loadTestSource(
                    fileName = "daoWriter/input/ComplexDao.java",
                    qName = "foo.bar.ComplexDao"
                )
            ) {
                it.assertCompilationResult {
                    generatedSource(
                        loadTestSource(
                            fileName = "databasewriter/output/ComplexDatabase.java",
                            qName = "foo.bar.ComplexDatabase_Impl"
                        )
                    )
                }
            }
        }
    }

    @RunWith(Parameterized::class)
    class BigDB(val config: Pair<Int, Int>) {

        @Test
        fun testCompile() {
            val (maxStatementCount, valuesPerEntity) = config
            val entitySources = mutableListOf<Pair<String, Source>>()
            var entityCount = 1
            var statementCount = 0
            while (statementCount < maxStatementCount) {
                val entityValues = StringBuilder().apply {
                    for (i in 1..valuesPerEntity) {
                        append(
                            """
                    private String value$i;
                    public String getValue$i() { return this.value$i; }
                    public void setValue$i(String value) { this.value$i = value; }

                    """
                        )
                    }
                }
                val entitySource = Source.java(
                    qName = "foo.bar.Entity$entityCount",
                    code = """
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
                    """
                )
                entitySources.add("Entity$entityCount" to entitySource)
                statementCount += valuesPerEntity
                entityCount++
            }
            val entityClasses = entitySources.joinToString { "${it.first}.class" }
            val dbSource = Source.java(
                qName = "foo.bar.TestDatabase",
                code = """
                    package foo.bar;

                    import androidx.room.*;

                    @Database(entities = {$entityClasses}, version = 1)
                    public abstract class TestDatabase extends RoomDatabase {}
                    """
            )
            singleDb(*(listOf(dbSource) + entitySources.map { it.second }).toTypedArray()) {
                // no assertion, if compilation succeeded, it is good
            }
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

private fun singleDb(
    vararg inputs: Source,
    handler: (XTestInvocation) -> Unit
) {
    val sources = listOf(
        COMMON.USER, COMMON.USER_SUMMARY, COMMON.LIVE_DATA, COMMON.COMPUTABLE_LIVE_DATA,
        COMMON.PARENT, COMMON.CHILD1, COMMON.CHILD2, COMMON.INFO, COMMON.GUAVA_ROOM,
        COMMON.LISTENABLE_FUTURE
    ) + inputs
    runProcessorTest(
        sources = sources,
        handler = DatabaseProcessingStep().asTestInvocationHandler(handler)
    )
}
