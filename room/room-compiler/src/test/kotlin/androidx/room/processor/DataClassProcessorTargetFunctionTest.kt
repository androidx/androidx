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

import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.testing.context
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DataClassProcessorTargetFunctionTest {

    companion object {
        val MY_DATA_CLASS = XClassName.get("foo.bar", "MyDataClass")
        val AUTOVALUE_MY_DATA_CLASS = XClassName.get("foo.bar", "AutoValue_MyDataClass")
        const val HEADER =
            """
            package foo.bar;

            import androidx.room.*;
            import java.util.*;
            import com.google.auto.value.*;

            @AutoValue
            public abstract class MyDataClass {
            """
        const val AUTO_VALUE_HEADER =
            """
            package foo.bar;

            import androidx.room.*;
            import java.util.*;
            import com.google.auto.value.*;

            public final class AutoValue_MyDataClass extends MyDataClass {
            """
        const val FOOTER = "\n}"
    }

    @Test
    fun invalidAnnotationInFunction() {
        val source =
            Source.java(
                MY_DATA_CLASS.canonicalName,
                """
            package foo.bar;

            import androidx.room.*;

            class MyDataClass {
                @PrimaryKey
                void someRandomFunction() { }
            }
            """
            )
        singleRun(source) { invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidAnnotationTarget(
                        "PrimaryKey",
                        invocation.functionKindName
                    )
                )
            }
        }
    }

    @Test
    fun invalidAnnotationInStaticFunction() {
        val source =
            Source.java(
                MY_DATA_CLASS.canonicalName,
                """
            package foo.bar;

            import androidx.room.*;

            class MyDataClass {
                @PrimaryKey
                static void someRandomFunction() { }
            }
            """
            )
        singleRun(source) { invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidAnnotationTarget(
                        "PrimaryKey",
                        invocation.functionKindName
                    )
                )
            }
        }
    }

    @Test
    fun invalidAnnotationInAbstractFunction() {
        val source =
            Source.java(
                MY_DATA_CLASS.canonicalName,
                """
            package foo.bar;

            import androidx.room.*;

            abstract class MyDataClass {
                @PrimaryKey
                abstract void someRandomFunction();
            }
            """
            )
        singleRun(source) { invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidAnnotationTarget(
                        "PrimaryKey",
                        invocation.functionKindName
                    )
                )
            }
        }
    }

    @Test
    fun invalidAnnotationInAutoValueFunction() {
        singleRun(
            """
                @AutoValue.CopyAnnotations
                @PrimaryKey
                abstract long getId();
                @ColumnInfo(name = "column_name")
                void someRandomFunction() { }
                static MyDataClass create(long id) { return new AutoValue_MyDataClass(id); }
                """,
            """
                @PrimaryKey
                private final long id;
                AutoValue_MyDataClass(long id) { this.id = id; }
                @PrimaryKey
                long getId() { return this.id; }
                """
        ) { invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidAnnotationTarget(
                        "ColumnInfo",
                        invocation.functionKindName
                    )
                )
            }
        }
    }

    @Test
    fun invalidAnnotationInAutoValueParentFunction() {
        val parent =
            """
            package foo.bar;

            import androidx.room.*;

            public abstract class ParentDataClass {
                @ColumnInfo(name = "column_name")
                abstract String getValue();
                @ColumnInfo(name = "another_column_name")
                void someRandomFunction() { }
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
                    @PrimaryKey
                    long getId() { return this.id; }
                    @ColumnInfo(name = "column_name")
                    String getValue() { return this.value; };
                $FOOTER
                """,
            Source.java("foo.bar.ParentDataClass", parent)
        ) { invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.invalidAnnotationTarget(
                        "ColumnInfo",
                        invocation.functionKindName
                    )
                )
            }
        }
    }

    @Test
    fun validAnnotationInField() {
        val source =
            Source.java(
                MY_DATA_CLASS.canonicalName,
                """
            package foo.bar;

            import androidx.room.*;

            class MyDataClass {
                @PrimaryKey
                int someRandomField;
            }
            """
            )
        singleRun(source)
    }

    @Test
    fun validAnnotationInStaticField() {
        val source =
            Source.java(
                MY_DATA_CLASS.canonicalName,
                """
            package foo.bar;

            import androidx.room.*;

            class MyDataClass {
                @PrimaryKey
                static final int SOME_RANDOM_CONSTANT = 42;
            }
            """
            )
        singleRun(source)
    }

    @Test
    fun validAnnotationInAutoValueAbstractFunction() {
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
        )
    }

    @Test
    fun validAnnotationInAutoValueParentFunction() {
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
                    @PrimaryKey
                    long getId() { return this.id; }
                    @ColumnInfo(name = "column_name")
                    String getValue() { return this.value; };
                $FOOTER
                """,
            Source.java("foo.bar.ParentDataClass", parent)
        )
    }

    @Test
    fun validAnnotationInAutoValueImplementedInterfaceFunction() {
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
                    @PrimaryKey
                    long getId() { return this.id; }
                    @ColumnInfo(name = "column_name")
                    public String getValue() { return this.value; };
                $FOOTER
                """,
            Source.java("foo.bar.InterfaceDataClass", parent)
        )
    }

    @Test
    fun validEmbeddedAnnotationInAutoValueAbstractFunction() {
        val embeddedDataClass =
            """
            package foo.bar;

            public class EmbeddedDataClass {
                private final String value;
                public EmbeddedDataClass(String value) { this.value = value; }
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
                abstract EmbeddedDataClass getEmbedded();
                static MyDataClass create(long id, EmbeddedDataClass embedded) {
                    return new AutoValue_MyDataClass(id, embedded);
                }
                """,
            """
                @PrimaryKey
                private final long id;
                @Embedded
                private final EmbeddedDataClass embedded;
                AutoValue_MyDataClass(long id, EmbeddedDataClass embedded) {
                    this.id = id;
                    this.embedded = embedded;
                }
                @PrimaryKey
                long getId() { return this.id; }
                @Embedded
                EmbeddedDataClass getEmbedded() { return this.embedded; }
                """,
            Source.java("foo.bar.EmbeddedDataClass", embeddedDataClass)
        )
    }

    @Test
    fun validRelationAnnotationInAutoValueAbstractFunction() {
        val embeddedDataClass =
            """
            package foo.bar;

            import androidx.room.*;

            @Entity
            public class RelationDataClass {
                @PrimaryKey
                private final long parentId;
                public RelationDataClass(long parentId) { this.parentId = parentId; }
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
                abstract List<RelationDataClass> getRelations();
                static MyDataClass create(long id, List<RelationDataClass> relations) {
                    return new AutoValue_MyDataClass(id, relations);
                }
                """,
            """
                @PrimaryKey
                private final long id;
                @Relation(parentColumn = "id", entityColumn = "parentId")
                private final List<RelationDataClass> relations;
                AutoValue_MyDataClass(long id, List<RelationDataClass> relations) {
                    this.id = id;
                    this.relations = relations;
                }
                @PrimaryKey
                long getId() { return this.id; }
                @Relation(parentColumn = "id", entityColumn = "parentId")
                List<RelationDataClass> getRelations() { return this.relations; }
                """,
            Source.java("foo.bar.RelationDataClass", embeddedDataClass)
        )
    }

    private fun singleRun(vararg sources: Source, handler: ((XTestInvocation) -> Unit)? = null) {
        runProcessorTest(sources = sources.toList()) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                    parent = null
                )
                .process()
            handler?.invoke(invocation)
        }
    }

    private fun singleRun(
        dataClassCode: String,
        autoValueDataClassCode: String,
        vararg sources: Source,
        handler: ((XTestInvocation) -> Unit)? = null
    ) {
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
            handler = handler,
            sources = sources,
        )
    }

    private fun singleRunFullClass(
        dataClassCode: String,
        autoValueDataClassCode: String,
        vararg sources: Source,
        handler: ((XTestInvocation) -> Unit)? = null
    ) {
        val dataClassSource = Source.java(MY_DATA_CLASS.canonicalName, dataClassCode)
        val autoValueDataClassSource =
            Source.java(AUTOVALUE_MY_DATA_CLASS.canonicalName, autoValueDataClassCode)
        val all = sources.toList() + dataClassSource + autoValueDataClassSource
        return runProcessorTest(sources = all) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                    parent = null
                )
                .process()
            handler?.invoke(invocation)
        }
    }

    /**
     * KSP and JavaAP name functions differently. To feel more native, we use the name based on the
     * processor. It only matters for the test assertion
     */
    private val XTestInvocation.functionKindName: String
        get() =
            if (this.isKsp) {
                "function"
            } else {
                "method"
            }
}
