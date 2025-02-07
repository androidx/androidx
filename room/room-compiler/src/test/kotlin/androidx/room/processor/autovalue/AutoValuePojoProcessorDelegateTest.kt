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

package androidx.room.processor.autovalue

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.processor.DataClassProcessor
import androidx.room.processor.ProcessorErrors
import androidx.room.processor.PropertyProcessor
import androidx.room.testing.context
import androidx.room.vo.DataClass
import com.google.auto.value.processor.AutoValueProcessor
import java.io.File
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AutoValueDataClassProcessorDelegateTest {

    companion object {
        val MY_DATA_CLASS = XClassName.get("foo.bar", "MyDataClass")
        val AUTOVALUE_MY_DATA_CLASS = XClassName.get("foo.bar", "AutoValue_MyDataClass")
        val HEADER =
            """
            package foo.bar;

            import androidx.room.*;
            import java.util.*;
            import com.google.auto.value.*;

            @AutoValue
            public abstract class MyDataClass {
            """
        val AUTO_VALUE_HEADER =
            """
            package foo.bar;

            import androidx.room.*;
            import java.util.*;
            import com.google.auto.value.*;

            public final class AutoValue_MyDataClass extends MyDataClass {
            """
        val FOOTER = "\n}"
    }

    @Test
    fun goodDataClass() {
        singleRun(
            """
                @AutoValue.CopyAnnotations
                @PrimaryKey
                abstract long getId();
                static MyDataClass create(long id) { return new AutoValue_MyDataClass(id); }
                """,
            """
                @PrimaryKey
                private final long id;
                AutoValue_MyDataClass(long id) { this.id = id; }
                @PrimaryKey
                long getId() { return this.id; }
                """
        ) { dataClass, invocation ->
            assertThat(dataClass.type.asTypeName(), `is`(MY_DATA_CLASS))
            assertThat(dataClass.properties.size, `is`(1))
            assertThat(dataClass.constructor?.element, `is`(notNullValue()))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun goodLibraryDataClass() {
        val libraryClasspath =
            compileFiles(
                sources =
                    listOf(
                        Source.java(
                            MY_DATA_CLASS.canonicalName,
                            """
                    $HEADER
                    @AutoValue.CopyAnnotations
                    @PrimaryKey
                    abstract long getValue();
                    static MyDataClass create(long value) { return new AutoValue_MyDataClass(value); }
                    $FOOTER
                    """
                        )
                    ),
                annotationProcessors = listOf(AutoValueProcessor()),
                // keep parameters as the naming convention for parameters is not the same
                // between javac (argN) and kotlinc (pN).
                javacArguments = listOf("-parameters")
            )
        // https://github.com/google/ksp/issues/2033
        runProcessorTest(
            sources = emptyList(),
            classpath = libraryClasspath,
        ) { invocation: XTestInvocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                    parent = null
                )
                .process()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun missingCopyAnnotationsWarning() {
        singleRun(
            """
                @PrimaryKey
                abstract long getId();
                static MyDataClass create(long id) { return new AutoValue_MyDataClass(id); }
                """,
            """
                private final long id;
                AutoValue_MyDataClass(long id) { this.id = id; }
                long getId() { return this.id; }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningContaining(ProcessorErrors.MISSING_COPY_ANNOTATIONS)
                hasWarningCount(1)
            }
        }
    }

    @Test
    fun missingCopyAnnotationsWarning_inInheritedMethodFromSuper() {
        val parent =
            """
            package foo.bar;

            import androidx.room.*;

            public abstract class ParentDataClass {
                @ColumnInfo(name = "column_name")
                abstract String getValue();
            }
            """
        singleRunFullClass(
            """
                package foo.bar;

                import androidx.room.*;
                import java.util.*;
                import com.google.auto.value.*;

                @AutoValue
                public abstract class MyDataClass extends ParentDataClass {
                    @PrimaryKey
                    abstract long getId();
                    static MyDataClass create(long id, String value) {
                        return new AutoValue_MyDataClass(id, value);
                    }
                $FOOTER
                """,
            """
                $AUTO_VALUE_HEADER
                    private final long id;
                    private final String value;
                    AutoValue_MyDataClass(long id, String value) { this.id = id; this.value = value; }
                    long getId() { return this.id; }
                    String getValue() { return this.value; };
                $FOOTER
                """,
            Source.java("foo.bar.ParentDataClass", parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningCount(2)
                hasWarningContaining(ProcessorErrors.MISSING_COPY_ANNOTATIONS)
            }
        }
    }

    @Test
    fun missingCopyAnnotationsWarning_inInheritedMethodFromInterface() {
        val parent =
            """
            package foo.bar;

            import androidx.room.*;

            public interface InterfaceDataClass {
                @ColumnInfo(name = "column_name")
                String getValue();
            }
            """
        singleRunFullClass(
            """
                package foo.bar;

                import androidx.room.*;
                import java.util.*;
                import com.google.auto.value.*;

                @AutoValue
                public abstract class MyDataClass implements InterfaceDataClass {
                    @PrimaryKey
                    abstract long getId();
                    static MyDataClass create(long id, String value) {
                        return new AutoValue_MyDataClass(id, value);
                    }
                $FOOTER
                """,
            """
                $AUTO_VALUE_HEADER
                    private final long id;
                    private final String value;
                    AutoValue_MyDataClass(long id, String value) { this.id = id; this.value = value; }
                    long getId() { return this.id; }
                    public String getValue() { return this.value; };
                $FOOTER
                """,
            Source.java("foo.bar.InterfaceDataClass", parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningContaining(ProcessorErrors.MISSING_COPY_ANNOTATIONS)
                hasWarningCount(2)
            }
        }
    }

    private fun singleRun(
        dataClassCode: String,
        autoValueDataClassCode: String,
        classpathFiles: List<File> = emptyList(),
        vararg sources: Source,
        handler: (dataClass: DataClass, invocation: XTestInvocation) -> Unit
    ) {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        singleRunFullClass(
            dataClassCode =
                """
                    $HEADER
                    $dataClassCode
                    $FOOTER
                    """,
            autoValueDataClassCode =
                """
                    $AUTO_VALUE_HEADER
                    $autoValueDataClassCode
                    $FOOTER
                    """,
            sources = sources,
            classpathFiles = classpathFiles,
            handler = handler
        )
    }

    private fun singleRunFullClass(
        dataClassCode: String,
        autoValueDataClassCode: String,
        vararg sources: Source,
        classpathFiles: List<File> = emptyList(),
        handler: (DataClass, XTestInvocation) -> Unit
    ) {
        val dataClassSource = Source.java(MY_DATA_CLASS.canonicalName, dataClassCode)
        val autoValueDataClassSource =
            Source.java(AUTOVALUE_MY_DATA_CLASS.canonicalName, autoValueDataClassCode)
        val all: List<Source> = sources.toList() + dataClassSource + autoValueDataClassSource
        runProcessorTest(sources = all, classpath = classpathFiles) { invocation ->
            handler.invoke(
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process(),
                invocation
            )
        }
    }
}
