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

import androidx.room.processor.FieldProcessor
import androidx.room.processor.PojoProcessor
import androidx.room.processor.ProcessorErrors
import androidx.room.testing.TestInvocation
import androidx.room.vo.Pojo
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.squareup.javapoet.ClassName
import compileLibrarySources
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import toJFO
import java.io.File
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class AutoValuePojoProcessorDelegateTest {

    companion object {
        val MY_POJO: ClassName = ClassName.get("foo.bar", "MyPojo")
        val AUTOVALUE_MY_POJO: ClassName = ClassName.get("foo.bar", "AutoValue_MyPojo")
        val HEADER = """
            package foo.bar;

            import androidx.room.*;
            import java.util.*;
            import com.google.auto.value.*;

            @AutoValue
            public abstract class MyPojo {
            """
        val AUTO_VALUE_HEADER = """
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
        ) { pojo ->
            assertThat(pojo.type.toString(), `is`("foo.bar.MyPojo"))
            assertThat(pojo.fields.size, `is`(1))
            assertThat(pojo.constructor?.element, `is`(notNullValue()))
        }.compilesWithoutError().withWarningCount(0)
    }

    @Test
    fun goodLibraryPojo() {
        val libraryClasspath = compileLibrarySources(
            JavaFileObjects.forSourceString(
                MY_POJO.toString(),
                """
                    $HEADER
                    @AutoValue.CopyAnnotations
                    @PrimaryKey
                    abstract long getArg0();
                    static MyPojo create(long arg0) { return new AutoValue_MyPojo(arg0); }
                    $FOOTER
                    """
            )
        )
        simpleRun(classpathFiles = libraryClasspath) { invocation ->
            PojoProcessor.createFor(
                context = invocation.context,
                element = invocation.processingEnv.requireTypeElement(MY_POJO),
                bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                parent = null
            ).process()
        }.compilesWithoutError().withWarningCount(0)
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
        ) { _ -> }
            .compilesWithoutError()
            .withWarningCount(1)
            .withWarningContaining(ProcessorErrors.MISSING_COPY_ANNOTATIONS)
    }

    @Test
    fun missingCopyAnnotationsWarning_inInheritedMethodFromSuper() {
        val parent = """
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
            parent.toJFO("foo.bar.ParentPojo")
        ) { _, _ -> }
            .compilesWithoutError()
            .withWarningCount(2)
            .withWarningContaining(ProcessorErrors.MISSING_COPY_ANNOTATIONS)
    }

    @Test
    fun missingCopyAnnotationsWarning_inInheritedMethodFromInterface() {
        val parent = """
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
            parent.toJFO("foo.bar.InterfacePojo")
        ) { _, _ -> }
            .compilesWithoutError()
            .withWarningCount(2)
            .withWarningContaining(ProcessorErrors.MISSING_COPY_ANNOTATIONS)
    }

    private fun singleRun(
        pojoCode: String,
        autoValuePojoCode: String,
        vararg jfos: JavaFileObject,
        handler: (Pojo) -> Unit
    ): CompileTester {
        return singleRun(pojoCode, autoValuePojoCode, *jfos) { pojo, _ ->
            handler(pojo)
        }
    }

    private fun singleRun(
        pojoCode: String,
        autoValuePojoCode: String,
        vararg jfos: JavaFileObject,
        classpathFiles: Set<File> = emptySet(),
        handler: (Pojo, TestInvocation) -> Unit
    ): CompileTester {
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        return singleRunFullClass(
            pojoCode = """
                    $HEADER
                    $pojoCode
                    $FOOTER
                    """,
            autoValuePojoCode = """
                    $AUTO_VALUE_HEADER
                    $autoValuePojoCode
                    $FOOTER
                    """,
            jfos = jfos,
            classpathFiles = classpathFiles,
            handler = handler
        )
    }

    private fun singleRunFullClass(
        pojoCode: String,
        autoValuePojoCode: String,
        vararg jfos: JavaFileObject,
        classpathFiles: Set<File> = emptySet(),
        handler: (Pojo, TestInvocation) -> Unit
    ): CompileTester {
        val pojoJFO = pojoCode.toJFO(MY_POJO.toString())
        val autoValuePojoJFO = autoValuePojoCode.toJFO(AUTOVALUE_MY_POJO.toString())
        val all = (jfos.toList() + pojoJFO + autoValuePojoJFO).toTypedArray()
        return simpleRun(*all, classpathFiles = classpathFiles) { invocation ->
            handler.invoke(
                PojoProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_POJO),
                    bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                    parent = null
                ).process(),
                invocation
            )
        }
    }
}