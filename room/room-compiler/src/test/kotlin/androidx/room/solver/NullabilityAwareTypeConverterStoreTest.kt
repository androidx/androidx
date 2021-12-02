/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.solver

import androidx.room.RoomKspProcessor
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.processor.Context.BooleanProcessorOptions.USE_NULL_AWARE_CONVERTER
import androidx.room.processor.CustomConverterProcessor
import androidx.room.processor.DaoProcessor
import androidx.room.solver.types.CustomTypeConverterWrapper
import androidx.room.solver.types.TypeConverter
import androidx.room.testing.context
import androidx.room.vo.BuiltInConverterFlags
import androidx.room.writer.DaoWriter
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.tools.Diagnostic

@RunWith(JUnit4::class)
class NullabilityAwareTypeConverterStoreTest {
    @get:Rule
    val tmpFolder = TemporaryFolder()
    val source = Source.kotlin(
        "Foo.kt",
        """
            import androidx.room.*
            class MyClass
            class NonNullConverters {
                @TypeConverter
                fun myClassToString(myClass: MyClass): String {
                    TODO()
                }
                @TypeConverter
                fun stringToMyClass(input: String): MyClass {
                    TODO()
                }
            }
            class MyNullableReceivingConverters {
                @TypeConverter
                fun nullableMyClassToNonNullString(myClass: MyClass?): String {
                    TODO()
                }
                @TypeConverter
                fun nullableStringToNonNullMyClass(input: String?): MyClass {
                    TODO()
                }
            }
            class MyFullyNullableConverters {
                @TypeConverter
                fun nullableMyClassToNullableString(myClass: MyClass?): String? {
                    TODO()
                }
                @TypeConverter
                fun nullableStringToNullableMyClass(input: String?): MyClass? {
                    TODO()
                }
            }
        """.trimIndent()
    )

    private fun XTestInvocation.createStore(
        vararg converters: String
    ): TypeConverterStore {
        val allConverters = converters.flatMap {
            CustomConverterProcessor(
                context = context,
                element = processingEnv.requireTypeElement(it)
            ).process()
        }.map(::CustomTypeConverterWrapper)
        return TypeAdapterStore.create(
            context = context,
            builtInConverterFlags = BuiltInConverterFlags.DEFAULT,
            allConverters
        ).typeConverterStore
    }

    @Test
    fun withOnlyNullableConverters() {
        val result = collectStringConversionResults(
            "MyFullyNullableConverters"
        )
        assertResult(
            result.trim(),
            """
            JAVAC
            String? to MyClass?: nullableStringToNullableMyClass
            MyClass? to String?: nullableMyClassToNullableString
            String? to MyClass!: nullableStringToNullableMyClass
            MyClass! to String?: nullableMyClassToNullableString
            String! to MyClass?: nullableStringToNullableMyClass
            MyClass? to String!: nullableMyClassToNullableString
            String! to MyClass!: nullableStringToNullableMyClass
            MyClass! to String!: nullableMyClassToNullableString
            KSP
            String? to MyClass?: nullableStringToNullableMyClass
            MyClass? to String?: nullableMyClassToNullableString
            String? to MyClass!: nullableStringToNullableMyClass / checkNotNull(MyClass?)
            MyClass! to String?: (MyClass! as MyClass?) / nullableMyClassToNullableString
            String! to MyClass?: (String! as String?) / nullableStringToNullableMyClass
            MyClass? to String!: nullableMyClassToNullableString / checkNotNull(String?)
            String! to MyClass!: (String! as String?) / nullableStringToNullableMyClass / checkNotNull(MyClass?)
            MyClass! to String!: (MyClass! as MyClass?) / nullableMyClassToNullableString / checkNotNull(String?)
            """.trimIndent())
    }

    @Test
    fun withOnlyNullableConverters_cursor() {
        val result = collectCursorResults(
            "MyFullyNullableConverters"
        )
        assertResult(
            result.trim(),
            """
            JAVAC
            Cursor to MyClass?: nullableStringToNullableMyClass
            MyClass? to Cursor: nullableMyClassToNullableString
            Cursor to MyClass!: nullableStringToNullableMyClass
            MyClass! to Cursor: nullableMyClassToNullableString
            KSP
            Cursor to MyClass?: nullableStringToNullableMyClass
            MyClass? to Cursor: nullableMyClassToNullableString
            Cursor to MyClass!: nullableStringToNullableMyClass / checkNotNull(MyClass?)
            MyClass! to Cursor: (MyClass! as MyClass?) / nullableMyClassToNullableString
            """.trimIndent())
    }

    @Test
    fun withNonNullableConverters() {
        val result = collectStringConversionResults("NonNullConverters")
        assertResult(
            result.trim(),
            """
            JAVAC
            String? to MyClass?: stringToMyClass
            MyClass? to String?: myClassToString
            String? to MyClass!: stringToMyClass
            MyClass! to String?: myClassToString
            String! to MyClass?: stringToMyClass
            MyClass? to String!: myClassToString
            String! to MyClass!: stringToMyClass
            MyClass! to String!: myClassToString
            KSP
            String? to MyClass?: (String? == null ? null : stringToMyClass)
            MyClass? to String?: (MyClass? == null ? null : myClassToString)
            String? to MyClass!: (String? == null ? null : stringToMyClass) / checkNotNull(MyClass?)
            MyClass! to String?: myClassToString / (String! as String?)
            String! to MyClass?: stringToMyClass / (MyClass! as MyClass?)
            MyClass? to String!: (MyClass? == null ? null : myClassToString) / checkNotNull(String?)
            String! to MyClass!: stringToMyClass
            MyClass! to String!: myClassToString
            """.trimIndent()
        )
    }

    @Test
    fun withNonNullableConverters_cursor() {
        val result = collectCursorResults("NonNullConverters")
        assertResult(
            result.trim(),
            """
                JAVAC
                Cursor to MyClass?: stringToMyClass
                MyClass? to Cursor: myClassToString
                Cursor to MyClass!: stringToMyClass
                MyClass! to Cursor: myClassToString
                KSP
                Cursor to MyClass?: (String? == null ? null : stringToMyClass)
                MyClass? to Cursor: (MyClass? == null ? null : myClassToString)
                // when reading from cursor, we can assume non-null cursor value when
                // we don't have a converter that would convert it from String?
                Cursor to MyClass!: stringToMyClass
                MyClass! to Cursor: myClassToString
            """.trimIndent()
        )
    }

    @Test
    fun withNonNullAndNullableReceiving() {
        val result = collectStringConversionResults(
            "NonNullConverters",
            "MyNullableReceivingConverters"
        )
        assertResult(
            result.trim(),
            """
            JAVAC
            String? to MyClass?: stringToMyClass
            MyClass? to String?: myClassToString
            String? to MyClass!: stringToMyClass
            MyClass! to String?: myClassToString
            String! to MyClass?: stringToMyClass
            MyClass? to String!: myClassToString
            String! to MyClass!: stringToMyClass
            MyClass! to String!: myClassToString
            KSP
            String? to MyClass?: nullableStringToNonNullMyClass / (MyClass! as MyClass?)
            MyClass? to String?: nullableMyClassToNonNullString / (String! as String?)
            String? to MyClass!: nullableStringToNonNullMyClass
            MyClass! to String?: myClassToString / (String! as String?)
            String! to MyClass?: stringToMyClass / (MyClass! as MyClass?)
            MyClass? to String!: nullableMyClassToNonNullString
            String! to MyClass!: stringToMyClass
            MyClass! to String!: myClassToString
            """.trimIndent()
        )
    }

    @Test
    fun withNonNullAndNullableReceiving_cursor() {
        val result = collectCursorResults(
            "NonNullConverters",
            "MyNullableReceivingConverters"
        )
        assertResult(
            result.trim(),
            """
            JAVAC
            Cursor to MyClass?: stringToMyClass
            MyClass? to Cursor: myClassToString
            Cursor to MyClass!: stringToMyClass
            MyClass! to Cursor: myClassToString
            KSP
            // we start from nullable string because cursor values are assumed nullable when reading
            Cursor to MyClass?: nullableStringToNonNullMyClass / (MyClass! as MyClass?)
            // there is an additional upcast for String! to String? because when the written value
            // is nullable, we prioritize a nullable column
            MyClass? to Cursor: nullableMyClassToNonNullString / (String! as String?)
            Cursor to MyClass!: nullableStringToNonNullMyClass
            MyClass! to Cursor: myClassToString
            """.trimIndent()
        )
    }

    @Test
    fun withFullyNullableConverters() {
        val result = collectStringConversionResults(
            "NonNullConverters",
            "MyNullableReceivingConverters",
            "MyFullyNullableConverters"
        )
        assertResult(
            result.trim(),
            """
                JAVAC
                String? to MyClass?: stringToMyClass
                MyClass? to String?: myClassToString
                String? to MyClass!: stringToMyClass
                MyClass! to String?: myClassToString
                String! to MyClass?: stringToMyClass
                MyClass? to String!: myClassToString
                String! to MyClass!: stringToMyClass
                MyClass! to String!: myClassToString
                KSP
                String? to MyClass?: nullableStringToNullableMyClass
                MyClass? to String?: nullableMyClassToNullableString
                String? to MyClass!: nullableStringToNonNullMyClass
                // another alternative is to use nonNullMyClassToNullableString and then upcast
                // both are equal weight
                MyClass! to String?: (MyClass! as MyClass?) / nullableMyClassToNullableString
                String! to MyClass?: (String! as String?) / nullableStringToNullableMyClass
                MyClass? to String!: nullableMyClassToNonNullString
                String! to MyClass!: stringToMyClass
                MyClass! to String!: myClassToString
            """.trimIndent()
        )
    }

    @Test
    fun withFullyNullableConverters_cursor() {
        val result = collectCursorResults(
            "NonNullConverters",
            "MyNullableReceivingConverters",
            "MyFullyNullableConverters"
        )
        assertResult(
            result.trim(),
            """
                JAVAC
                Cursor to MyClass?: stringToMyClass
                MyClass? to Cursor: myClassToString
                Cursor to MyClass!: stringToMyClass
                MyClass! to Cursor: myClassToString
                KSP
                Cursor to MyClass?: nullableStringToNullableMyClass
                MyClass? to Cursor: nullableMyClassToNullableString
                Cursor to MyClass!: nullableStringToNonNullMyClass
                MyClass! to Cursor: myClassToString
            """.trimIndent()
        )
    }

    @Test
    fun pojoProcess() {
        // This is a repro case from trying to run TestApp with null aware converter.
        // It reproduces the case where if we don't know nullability, we shouldn't try to
        // prioritize nullable or non-null; instead YOLO and find whichever we can find first.
        val user = Source.java(
            "User", """
            import androidx.room.*;
            import java.util.*;
            @TypeConverters({TestConverters.class})
            @Entity
            public class User {
                @PrimaryKey
                public int mId;
                public Set<Day> mWorkDays = new HashSet<>();
            }
        """.trimIndent()
        )
        val converters = Source.java(
            "TestConverters", """
            import androidx.room.*;
            import java.util.Date;
            import java.util.HashSet;
            import java.util.Set;
            class TestConverters {
                @TypeConverter
                public static Set<Day> decomposeDays(int flags) {
                    Set<Day> result = new HashSet<>();
                    for (Day day : Day.values()) {
                        if ((flags & (1 << day.ordinal())) != 0) {
                            result.add(day);
                        }
                    }
                    return result;
                }

                @TypeConverter
                public static int composeDays(Set<Day> days) {
                    int result = 0;
                    for (Day day : days) {
                        result |= 1 << day.ordinal();
                    }
                    return result;
                }
            }
        """.trimIndent()
        )
        val day = Source.java(
            "Day", """
            public enum Day {
                MONDAY,
                TUESDAY,
                WEDNESDAY,
                THURSDAY,
                FRIDAY,
                SATURDAY,
                SUNDAY
            }
        """.trimIndent()
        )
        val dao = Source.java(
            "MyDao", """
            import androidx.room.*;
            @Dao
            interface MyDao {
                @Insert
                void insert(User user);
            }
        """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(user, day, converters, dao),
            options = mapOf(
                USE_NULL_AWARE_CONVERTER.argName to "true"
            )
        ) { invocation ->
            val daoProcessor = DaoProcessor(
                baseContext = invocation.context,
                element = invocation.processingEnv.requireTypeElement("MyDao"),
                dbType = invocation.processingEnv.requireType("androidx.room.RoomDatabase"),
                dbVerifier = null
            )
            DaoWriter(
                dao = daoProcessor.process(),
                dbElement = invocation.processingEnv
                    .requireTypeElement("androidx.room.RoomDatabase"),
                processingEnv = invocation.processingEnv
            ).write(invocation.processingEnv)
            invocation.assertCompilationResult {
                generatedSourceFileWithPath("MyDao_Impl.java").let {
                    // make sure it bounded w/o upcasting to Boolean
                    it.contains("final int _tmp = TestConverters.composeDays(value.mWorkDays);")
                    it.contains("stmt.bindLong(2, _tmp);")
                }
            }
        }
    }

    @Test
    fun checkSyntheticConverters() {
        class MockTypeConverter(
            from: XType,
            to: XType,
        ) : TypeConverter(
            from = from,
            to = to
        ) {
            override fun doConvert(
                inputVarName: String,
                outputVarName: String,
                scope: CodeGenScope
            ) {
            }
        }
        runProcessorTest { invocation ->
            val string = invocation.processingEnv.requireType(String::class)
                .makeNonNullable()
            val int = invocation.processingEnv.requireType(Int::class)
                .makeNonNullable()
            val long = invocation.processingEnv.requireType(Long::class)
                .makeNonNullable()
            val number = invocation.processingEnv.requireType(Number::class)
                .makeNonNullable()
            NullAwareTypeConverterStore(
                context = invocation.context,
                typeConverters = listOf(
                    MockTypeConverter(
                        from = string.makeNullable(),
                        to = int.makeNullable()
                    )
                ),
                knownColumnTypes = emptyList()
            ).let { store ->
                // nullable converter, don't duplicate anything
                assertThat(
                    store.typeConverters
                ).hasSize(1)
            }
            NullAwareTypeConverterStore(
                context = invocation.context,
                typeConverters = listOf(
                    MockTypeConverter(
                        from = string,
                        to = int
                    )
                ),
                knownColumnTypes = emptyList()
            ).let { store ->
                if (invocation.isKsp) {
                    // add a null wrapper version
                    assertThat(store.typeConverters).hasSize(2)
                } else {
                    // do not duplicate unless we run in KSP
                    assertThat(store.typeConverters).hasSize(1)
                }
            }
            NullAwareTypeConverterStore(
                context = invocation.context,
                typeConverters = listOf(
                    MockTypeConverter(
                        from = string,
                        to = int
                    ),
                    MockTypeConverter(
                        from = string.makeNullable(),
                        to = int
                    )
                ),
                knownColumnTypes = emptyList()
            ).let { store ->
                // don't duplicate, we already have a null receiving version
                assertThat(store.typeConverters).hasSize(2)
            }
            NullAwareTypeConverterStore(
                context = invocation.context,
                typeConverters = listOf(
                    MockTypeConverter(
                        from = string,
                        to = int
                    ),
                    MockTypeConverter(
                        from = string.makeNullable(),
                        to = int.makeNullable()
                    )
                ),
                knownColumnTypes = emptyList()
            ).let { store ->
                // don't duplicate, we already have a null receiving version
                assertThat(store.typeConverters).hasSize(2)
            }
            NullAwareTypeConverterStore(
                context = invocation.context,
                typeConverters = listOf(
                    MockTypeConverter(
                        from = string,
                        to = int
                    ),
                    MockTypeConverter(
                        from = string,
                        to = long
                    ),
                    MockTypeConverter(
                        from = string.makeNullable(),
                        to = int.makeNullable()
                    )
                ),
                knownColumnTypes = emptyList()
            ).let { store ->
                // don't duplicate, we already have a null receiving version
                if (invocation.isKsp) {
                    // duplicate the long receiving one
                    assertThat(store.typeConverters).hasSize(4)
                } else {
                    // don't duplicate in javac
                    assertThat(store.typeConverters).hasSize(3)
                }
            }
            NullAwareTypeConverterStore(
                context = invocation.context,
                typeConverters = listOf(
                    MockTypeConverter(
                        from = string,
                        to = number
                    ),
                    MockTypeConverter(
                        from = string.makeNullable(),
                        to = int
                    ),
                ),
                knownColumnTypes = emptyList()
            ).let { store ->
                // don't duplicate string number converter since we have string? to int
                assertThat(store.typeConverters).hasSize(2)
            }
            NullAwareTypeConverterStore(
                context = invocation.context,
                typeConverters = listOf(
                    MockTypeConverter(
                        from = string,
                        to = number.makeNullable()
                    ),
                    MockTypeConverter(
                        from = string.makeNullable(),
                        to = int
                    ),
                ),
                knownColumnTypes = emptyList()
            ).let { store ->
                // don't duplicate string number converter since we have string? to int
                assertThat(store.typeConverters).hasSize(2)
            }
        }
    }

    @Test
    fun warnIfTurnedOffInKsp() {
        val sources = Source.kotlin("Foo.kt", "")
        arrayOf("", "true", "false").forEach { value ->
            val result = compile(
                workingDir = tmpFolder.newFolder(),
                arguments = TestCompilationArguments(
                    sources = listOf(sources),
                    symbolProcessorProviders = listOf(
                        RoomKspProcessor.Provider()
                    ),
                    processorOptions = mapOf(
                        USE_NULL_AWARE_CONVERTER.argName to value
                    )
                )
            )
            val warnings = result.diagnostics[Diagnostic.Kind.WARNING]?.map {
                it.msg
            }?.filter {
                it.contains("Disabling null-aware type analysis in KSP is a temporary flag")
            } ?: emptyList()
            val expected = if (value == "false") {
                1
            } else {
                0
            }
            assertThat(
                warnings
            ).hasSize(expected)
        }
    }

    /**
     * Test converting a known column type into another type due to explicit affinity
     */
    @Test
    fun knownColumnTypeToExplicitType() {
        val source = Source.kotlin(
            "Subject.kt", """
            import androidx.room.*
            object MyByteArrayConverter {
                @TypeConverter
                fun toByteArray(input:String): ByteArray { TODO() }
                @TypeConverter
                fun fromByteArray(input:ByteArray): String { TODO() }
            }
            class Subject(val arr:ByteArray)
        """.trimIndent()
        )
        runProcessorTest(
            sources = listOf(source),
            options = mapOf(
                USE_NULL_AWARE_CONVERTER.argName to "true"
            )
        ) { invocation ->
            val byteArray = invocation.processingEnv.requireTypeElement("Subject")
                .getDeclaredFields().first().type.makeNonNullable()
            val string = invocation.processingEnv.requireType("java.lang.String")
            invocation.createStore().let { storeWithoutConverter ->
                val intoStatement = storeWithoutConverter.findConverterIntoStatement(
                    input = byteArray,
                    columnTypes = listOf(
                        string.makeNullable(),
                        string.makeNonNullable()
                    )
                )
                assertThat(intoStatement).isNull()
                val fromCursor = storeWithoutConverter.findConverterFromCursor(
                    output = byteArray,
                    columnTypes = listOf(
                        string.makeNullable(),
                        string.makeNonNullable()
                    )
                )
                assertThat(fromCursor).isNull()
            }

            invocation.createStore(
                "MyByteArrayConverter"
            ).let { storeWithConverter ->
                val intoStatement = storeWithConverter.findConverterIntoStatement(
                    input = byteArray,
                    columnTypes = listOf(
                        string.makeNullable(),
                        string.makeNonNullable()
                    )
                )
                assertThat(intoStatement?.toSignature()).isEqualTo("fromByteArray")
                assertThat(intoStatement?.to).isEqualTo(string.makeNonNullable())
                assertThat(intoStatement?.from).isEqualTo(byteArray.makeNonNullable())
                val fromCursor = storeWithConverter.findConverterFromCursor(
                    output = byteArray,
                    columnTypes = listOf(
                        string.makeNullable(),
                        string.makeNonNullable()
                    )
                )
                assertThat(fromCursor?.toSignature()).isEqualTo("toByteArray")
                assertThat(fromCursor?.to).isEqualTo(byteArray.makeNonNullable())
                assertThat(fromCursor?.from).isEqualTo(string.makeNonNullable())
            }
        }
    }

    /**
     * Collect results for conversion from String to our type
     */
    private fun collectStringConversionResults(
        vararg selectedConverters: String
    ): String {
        val result = StringBuilder()
        runProcessorTest(
            sources = listOf(source),
            options = mapOf(
                USE_NULL_AWARE_CONVERTER.argName to "true"
            )
        ) { invocation ->
            val store = invocation.createStore(*selectedConverters)
            assertThat(store).isInstanceOf(NullAwareTypeConverterStore::class.java)
            val myClassTypeElement = invocation.processingEnv.requireTypeElement(
                "MyClass"
            )
            val stringTypeElement = invocation.processingEnv.requireTypeElement(
                "java.lang.String"
            )

            result.appendLine(invocation.processingEnv.backend.name)
            listOf(
                stringTypeElement.type.makeNullable(),
                stringTypeElement.type.makeNonNullable(),
            ).forEach { stringType ->
                listOf(
                    myClassTypeElement.type.makeNullable(),
                    myClassTypeElement.type.makeNonNullable()
                ).forEach { myClassType ->
                    val fromString = store.findTypeConverter(
                        input = stringType,
                        output = myClassType
                    )
                    val toString = store.findTypeConverter(
                        input = myClassType,
                        output = stringType
                    )
                    result.apply {
                        append(stringType.toSignature())
                        append(" to ")
                        append(myClassType.toSignature())
                        append(": ")
                        appendLine(fromString?.toSignature() ?: "null")
                    }

                    result.apply {
                        append(myClassType.toSignature())
                        append(" to ")
                        append(stringType.toSignature())
                        append(": ")
                        appendLine(toString?.toSignature() ?: "null")
                    }
                }
            }
        }
        return result.toString()
    }

    /**
     * Collect results for conversion from an unknown cursor type to our type
     */
    private fun collectCursorResults(
        vararg selectedConverters: String
    ): String {
        val result = StringBuilder()
        runProcessorTest(
            sources = listOf(source),
            options = mapOf(
                USE_NULL_AWARE_CONVERTER.argName to "true"
            )
        ) { invocation ->
            val store = invocation.createStore(*selectedConverters)
            assertThat(store).isInstanceOf(NullAwareTypeConverterStore::class.java)
            val myClassTypeElement = invocation.processingEnv.requireTypeElement(
                "MyClass"
            )

            result.appendLine(invocation.processingEnv.backend.name)
            listOf(
                myClassTypeElement.type.makeNullable(),
                myClassTypeElement.type.makeNonNullable()
            ).forEach { myClassType ->
                val toMyClass = store.findConverterFromCursor(
                    columnTypes = null,
                    output = myClassType
                )
                val fromMyClass = store.findConverterIntoStatement(
                    input = myClassType,
                    columnTypes = null
                )

                result.apply {
                    append("Cursor to ")
                    append(myClassType.toSignature())
                    append(": ")
                    appendLine(toMyClass?.toSignature() ?: "null")
                }

                result.apply {
                    append(myClassType.toSignature())
                    append(" to Cursor: ")
                    appendLine(fromMyClass?.toSignature() ?: "null")
                }
            }
        }
        return result.toString()
    }

    private fun assertResult(result: String, expected: String) {
        // remove commented lines from expected as they are used to explain cases for test's
        // readability
        assertThat(result).isEqualTo(
            expected
                .lines()
                .filterNot { it.trim().startsWith("//") }
                .joinToString("\n")
        )
    }
}