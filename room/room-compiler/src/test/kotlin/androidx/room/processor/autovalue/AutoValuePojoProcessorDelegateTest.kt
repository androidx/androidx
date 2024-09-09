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
import androidx.room.processor.FieldProcessor
import androidx.room.processor.PojoProcessor
import androidx.room.processor.ProcessorErrors
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.vo.Pojo
import com.google.auto.value.processor.AutoValueProcessor
import java.io.File
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AutoValuePojoProcessorDelegateTest {

    companion object {
        val MY_POJO = XClassName.get("foo.bar", "MyPojo")
        val AUTOVALUE_MY_POJO = XClassName.get("foo.bar", "AutoValue_MyPojo")
        val HEADER =
            """
            package foo.bar;

            import androidx.room.*;
            import java.util.*;
            import com.google.auto.value.*;

            @AutoValue
            public abstract class MyPojo {
            """
        val AUTO_VALUE_HEADER =
            """
            package foo.bar;

            import androidx.room.*;
            import java.util.*;
            import com.google.auto.value.*;

            public final class AutoValue_MyPojo extends MyPojo {
            """
        val FOOTER = "\n}"
    }

    @Test
    fun goodPojo() {
        singleRun(
            """
                @AutoValue.CopyAnnotations
                @PrimaryKey
                abstract long getId();
                static MyPojo create(long id) { return new AutoValue_MyPojo(id); }
                """,
            """
                @PrimaryKey
                private final long id;
                AutoValue_MyPojo(long id) { this.id = id; }
                @PrimaryKey
                long getId() { return this.id; }
                """
        ) { pojo, invocation ->
            assertThat(pojo.type.asTypeName(), `is`(MY_POJO))
            assertThat(pojo.fields.size, `is`(1))
            assertThat(pojo.constructor?.element, `is`(notNullValue()))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun goodLibraryPojo() {
        val libraryClasspath =
            compileFiles(
                sources =
                    listOf(
                        Source.java(
                            MY_POJO.canonicalName,
                            """
                    $HEADER
                    @AutoValue.CopyAnnotations
                    @PrimaryKey
                    abstract long getValue();
                    static MyPojo create(long value) { return new AutoValue_MyPojo(value); }
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
        runProcessorTestWithK1(
            sources = emptyList(),
            classpath = libraryClasspath,
        ) { invocation: XTestInvocation ->
            PojoProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_POJO),
                    bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
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
                static MyPojo create(long id) { return new AutoValue_MyPojo(id); }
                """,
            """
                private final long id;
                AutoValue_MyPojo(long id) { this.id = id; }
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

            public abstract class ParentPojo {
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
                public abstract class MyPojo extends ParentPojo {
                    @PrimaryKey
                    abstract long getId();
                    static MyPojo create(long id, String value) {
                        return new AutoValue_MyPojo(id, value);
                    }
                $FOOTER
                """,
            """
                $AUTO_VALUE_HEADER
                    private final long id;
                    private final String value;
                    AutoValue_MyPojo(long id, String value) { this.id = id; this.value = value; }
                    long getId() { return this.id; }
                    String getValue() { return this.value; };
                $FOOTER
                """,
            Source.java("foo.bar.ParentPojo", parent)
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

            public interface InterfacePojo {
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
                public abstract class MyPojo implements InterfacePojo {
                    @PrimaryKey
                    abstract long getId();
                    static MyPojo create(long id, String value) {
                        return new AutoValue_MyPojo(id, value);
                    }
                $FOOTER
                """,
            """
                $AUTO_VALUE_HEADER
                    private final long id;
                    private final String value;
                    AutoValue_MyPojo(long id, String value) { this.id = id; this.value = value; }
                    long getId() { return this.id; }
                    public String getValue() { return this.value; };
                $FOOTER
                """,
            Source.java("foo.bar.InterfacePojo", parent)
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningContaining(ProcessorErrors.MISSING_COPY_ANNOTATIONS)
                hasWarningCount(2)
            }
        }
    }

    private fun singleRun(
        pojoCode: String,
        autoValuePojoCode: String,
        classpathFiles: List<File> = emptyList(),
        vararg sources: Source,
        handler: (pojo: Pojo, invocation: XTestInvocation) -> Unit
    ) {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        singleRunFullClass(
            pojoCode =
                """
                    $HEADER
                    $pojoCode
                    $FOOTER
                    """,
            autoValuePojoCode =
                """
                    $AUTO_VALUE_HEADER
                    $autoValuePojoCode
                    $FOOTER
                    """,
            sources = sources,
            classpathFiles = classpathFiles,
            handler = handler
        )
    }

    private fun singleRunFullClass(
        pojoCode: String,
        autoValuePojoCode: String,
        vararg sources: Source,
        classpathFiles: List<File> = emptyList(),
        handler: (Pojo, XTestInvocation) -> Unit
    ) {
        val pojoSource = Source.java(MY_POJO.canonicalName, pojoCode)
        val autoValuePojoSource = Source.java(AUTOVALUE_MY_POJO.canonicalName, autoValuePojoCode)
        val all: List<Source> = sources.toList() + pojoSource + autoValuePojoSource
        runProcessorTestWithK1(sources = all, classpath = classpathFiles) { invocation ->
            handler.invoke(
                PojoProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_POJO),
                        bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                        parent = null
                    )
                    .process(),
                invocation
            )
        }
    }
}
