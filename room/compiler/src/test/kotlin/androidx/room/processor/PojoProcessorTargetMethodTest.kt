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

package androidx.room.processor

import com.google.testing.compile.CompileTester
import com.squareup.javapoet.ClassName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import toJFO
import javax.lang.model.element.ElementKind
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class PojoProcessorTargetMethodTest {

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
    fun invalidAnnotationInMethod() {
        val source = """
            package foo.bar;

            import androidx.room.*;

            class MyPojo {
                @PrimaryKey
                void someRandomMethod() { }
            }
            """.toJFO(MY_POJO.toString())
        singleRun(source)
                .failsToCompile()
                .withErrorContaining(
                        ProcessorErrors.invalidAnnotationTarget("PrimaryKey", ElementKind.METHOD))
    }

    @Test
    fun invalidAnnotationInStaticMethod() {
        val source = """
            package foo.bar;

            import androidx.room.*;

            class MyPojo {
                @PrimaryKey
                static void someRandomMethod() { }
            }
            """.toJFO(MY_POJO.toString())
        singleRun(source)
                .failsToCompile()
                .withErrorContaining(
                        ProcessorErrors.invalidAnnotationTarget("PrimaryKey", ElementKind.METHOD))
    }

    @Test
    fun invalidAnnotationInAbstractMethod() {
        val source = """
            package foo.bar;

            import androidx.room.*;

            abstract class MyPojo {
                @PrimaryKey
                abstract void someRandomMethod();
            }
            """.toJFO(MY_POJO.toString())
        singleRun(source)
                .failsToCompile()
                .withErrorContaining(
                        ProcessorErrors.invalidAnnotationTarget("PrimaryKey", ElementKind.METHOD))
    }

    @Test
    fun invalidAnnotationInAutoValueMethod() {
        singleRun(
                """
                @AutoValue.CopyAnnotations
                @PrimaryKey
                abstract long getId();
                @ColumnInfo(name = "column_name")
                void someRandomMethod() { }
                static MyPojo create(long id) { return new AutoValue_MyPojo(id); }
                """,
                """
                @PrimaryKey
                private final long id;
                AutoValue_MyPojo(long id) { this.id = id; }
                @PrimaryKey
                long getId() { return this.id; }
                """
        ).failsToCompile().withErrorContaining(
                ProcessorErrors.invalidAnnotationTarget("ColumnInfo", ElementKind.METHOD))
    }

    @Test
    fun invalidAnnotationInAutoValueParentMethod() {
        val parent = """
            package foo.bar;

            import androidx.room.*;

            public abstract class ParentPojo {
                @ColumnInfo(name = "column_name")
                abstract String getValue();
                @ColumnInfo(name = "another_column_name")
                void someRandomMethod() { }
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
                    @PrimaryKey
                    long getId() { return this.id; }
                    @ColumnInfo(name = "column_name")
                    String getValue() { return this.value; };
                $FOOTER
                """,
                parent.toJFO("foo.bar.ParentPojo")
        ).failsToCompile().withErrorContaining(
                ProcessorErrors.invalidAnnotationTarget("ColumnInfo", ElementKind.METHOD))
    }

    @Test
    fun validAnnotationInField() {
        val source = """
            package foo.bar;

            import androidx.room.*;

            class MyPojo {
                @PrimaryKey
                int someRandomField;
            }
            """.toJFO(MY_POJO.toString())
        singleRun(source)
                .compilesWithoutError()
    }

    @Test
    fun validAnnotationInStaticField() {
        val source = """
            package foo.bar;

            import androidx.room.*;

            class MyPojo {
                @PrimaryKey
                static final int SOME_RANDOM_CONSTANT = 42;
            }
            """.toJFO(MY_POJO.toString())
        singleRun(source)
                .compilesWithoutError()
    }

    @Test
    fun validAnnotationInAutoValueAbstractMethod() {
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
        ).compilesWithoutError()
    }

    @Test
    fun validAnnotationInAutoValueParentMethod() {
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
                    @PrimaryKey
                    long getId() { return this.id; }
                    @ColumnInfo(name = "column_name")
                    String getValue() { return this.value; };
                $FOOTER
                """,
                parent.toJFO("foo.bar.ParentPojo")
        ).compilesWithoutError()
    }

    @Test
    fun validAnnotationInAutoValueImplementedInterfaceMethod() {
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
                    @PrimaryKey
                    long getId() { return this.id; }
                    @ColumnInfo(name = "column_name")
                    public String getValue() { return this.value; };
                $FOOTER
                """,
                parent.toJFO("foo.bar.InterfacePojo")
        ).compilesWithoutError()
    }

    @Test
    fun validEmbeddedAnnotationInAutoValueAbstractMethod() {
        val embeddedPojo = """
            package foo.bar;

            public class EmbeddedPojo {
                private final String value;
                public EmbeddedPojo(String value) { this.value = value; }
                String getValue() { return this.value; }
            }
            """
        singleRun(
                """
                @AutoValue.CopyAnnotations
                @PrimaryKey
                abstract long getId();
                @AutoValue.CopyAnnotations
                @Embedded
                abstract EmbeddedPojo getEmbedded();
                static MyPojo create(long id, EmbeddedPojo embedded) {
                    return new AutoValue_MyPojo(id, embedded);
                }
                """,
                """
                @PrimaryKey
                private final long id;
                @Embedded
                private final EmbeddedPojo embedded;
                AutoValue_MyPojo(long id, EmbeddedPojo embedded) {
                    this.id = id;
                    this.embedded = embedded;
                }
                @PrimaryKey
                long getId() { return this.id; }
                @Embedded
                EmbeddedPojo getEmbedded() { return this.embedded; }
                """,
                embeddedPojo.toJFO("foo.bar.EmbeddedPojo")
        ).compilesWithoutError()
    }

    @Test
    fun validRelationAnnotationInAutoValueAbstractMethod() {
        val embeddedPojo = """
            package foo.bar;

            import androidx.room.*;

            @Entity
            public class RelationPojo {
                @PrimaryKey
                private final long parentId;
                public RelationPojo(long parentId) { this.parentId = parentId; }
                long getParentId() { return this.parentId; }
            }
            """
        singleRun(
                """
                @AutoValue.CopyAnnotations
                @PrimaryKey
                abstract long getId();
                @AutoValue.CopyAnnotations
                @Relation(parentColumn = "id", entityColumn = "parentId")
                abstract List<RelationPojo> getRelations();
                static MyPojo create(long id, List<RelationPojo> relations) {
                    return new AutoValue_MyPojo(id, relations);
                }
                """,
                """
                @PrimaryKey
                private final long id;
                @Relation(parentColumn = "id", entityColumn = "parentId")
                private final List<RelationPojo> relations;
                AutoValue_MyPojo(long id, List<RelationPojo> relations) {
                    this.id = id;
                    this.relations = relations;
                }
                @PrimaryKey
                long getId() { return this.id; }
                @Relation(parentColumn = "id", entityColumn = "parentId")
                List<RelationPojo> getRelations() { return this.relations; }
                """,
                embeddedPojo.toJFO("foo.bar.RelationPojo")
        ).compilesWithoutError()
    }

    private fun singleRun(vararg jfos: JavaFileObject) = simpleRun(*jfos) { invocation ->
        PojoProcessor.createFor(context = invocation.context,
            element = invocation.typeElement(MY_POJO.toString()),
            bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
            parent = null).process()
    }

    private fun singleRun(
        pojoCode: String,
        autoValuePojoCode: String,
        vararg jfos: JavaFileObject
    ): CompileTester {
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
                jfos = *jfos
        )
    }

    private fun singleRunFullClass(
        pojoCode: String,
        autoValuePojoCode: String,
        vararg jfos: JavaFileObject
    ): CompileTester {
        val pojoJFO = pojoCode.toJFO(MY_POJO.toString())
        val autoValuePojoJFO = autoValuePojoCode.toJFO(AUTOVALUE_MY_POJO.toString())
        val all = (jfos.toList() + pojoJFO + autoValuePojoJFO).toTypedArray()
        return simpleRun(*all) { invocation ->
            PojoProcessor.createFor(context = invocation.context,
                element = invocation.typeElement(MY_POJO.toString()),
                bindingScope = FieldProcessor.BindingScope.READ_FROM_CURSOR,
                parent = null).process()
        }
    }
}