/*
 * Copyright (C) 2017 The Android Open Source Project
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

import COMMON
import androidx.kruth.assertThat
import androidx.room.Embedded
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.ext.CommonTypeNames
import androidx.room.parser.SQLTypeAffinity
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_GETTER_FOR_PROPERTY
import androidx.room.processor.ProcessorErrors.DATA_CLASS_PROPERTY_HAS_DUPLICATE_COLUMN_NAME
import androidx.room.processor.ProcessorErrors.MISSING_DATA_CLASS_CONSTRUCTOR
import androidx.room.processor.ProcessorErrors.junctionColumnWithoutIndex
import androidx.room.processor.ProcessorErrors.relationCannotFindEntityProperty
import androidx.room.processor.ProcessorErrors.relationCannotFindJunctionEntityProperty
import androidx.room.processor.ProcessorErrors.relationCannotFindJunctionParentProperty
import androidx.room.processor.ProcessorErrors.relationCannotFindParentEntityProperty
import androidx.room.testing.context
import androidx.room.vo.CallType
import androidx.room.vo.Constructor
import androidx.room.vo.DataClass
import androidx.room.vo.EmbeddedProperty
import androidx.room.vo.Property
import androidx.room.vo.PropertyGetter
import androidx.room.vo.PropertySetter
import androidx.room.vo.RelationCollector
import java.io.File
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock

/** Some of the functionality is tested via TableEntityProcessor. */
@RunWith(JUnit4::class)
class DataClassProcessorTest {

    companion object {
        val MY_DATA_CLASS = XClassName.get("foo.bar", "MyDataClass")
        val HEADER =
            """
            package foo.bar;
            import androidx.room.*;
            import java.util.*;
            public class MyDataClass {
            """
        val FOOTER = "\n}"
    }

    @Test
    fun inheritedPrivate() {
        val parent =
            """
            package foo.bar.x;
            import androidx.room.*;
            public class BaseClass {
                private String baseProperty;
                public String getBaseProperty(){ return baseProperty; }
                public void setBaseProperty(String baseProperty){ }
            }
        """
        runProcessorTest(
            sources =
                listOf(
                    Source.java(
                        MY_DATA_CLASS.canonicalName,
                        """
                    package foo.bar;
                    import androidx.room.*;
                    public class ${MY_DATA_CLASS.simpleNames.single()} extends foo.bar.x.BaseClass {
                        public String myProperty;
                    }
                    """
                    ),
                    Source.java("foo.bar.x.BaseClass", parent)
                )
        ) { invocation ->
            val dataClass =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process()
            assertThat(dataClass.properties.find { it.name == "myProperty" }, notNullValue())
            assertThat(dataClass.properties.find { it.name == "baseProperty" }, notNullValue())
        }
    }

    @Test
    fun transient_ignore() {
        singleRun(
            """
            transient int foo;
            int bar;
        """
        ) { dataClass, _ ->
            assertThat(dataClass.properties.size, `is`(1))
            assertThat(dataClass.properties[0].name, `is`("bar"))
        }
    }

    @Test
    fun transient_withColumnInfo() {
        singleRun(
            """
            @ColumnInfo
            transient int foo;
            int bar;
        """
        ) { dataClass, _ ->
            assertThat(dataClass.properties.map { it.name }.toSet(), `is`(setOf("bar", "foo")))
        }
    }

    @Test
    fun transient_embedded() {
        singleRun(
            """
            @Embedded
            transient Foo foo;
            int bar;
            static class Foo {
                int x;
            }
        """
        ) { dataClass, _ ->
            assertThat(dataClass.properties.map { it.name }.toSet(), `is`(setOf("x", "bar")))
        }
    }

    @Test
    fun transient_insideEmbedded() {
        singleRun(
            """
            @Embedded
            Foo foo;
            int bar;
            static class Foo {
                transient int x;
                int y;
            }
            """
        ) { dataClass, _ ->
            assertThat(dataClass.properties.map { it.name }.toSet(), `is`(setOf("bar", "y")))
        }
    }

    @Test
    fun transient_relation() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public transient List<User> user;
                """,
            COMMON.USER,
        ) { dataClass, invocation ->
            assertThat(dataClass.relations.size, `is`(1))
            assertThat(dataClass.relations.first().entityProperty.name, `is`("uid"))
            assertThat(dataClass.relations.first().parentProperty.name, `is`("id"))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun embedded() {
        singleRun(
            """
                int id;
                @Embedded
                Point myPoint;
                static class Point {
                    int x;
                    int y;
                }
                """
        ) { dataClass, _ ->
            assertThat(dataClass.properties.size, `is`(3))
            assertThat(dataClass.properties[1].name, `is`("x"))
            assertThat(dataClass.properties[2].name, `is`("y"))
            assertThat(dataClass.properties[0].parent, nullValue())
            assertThat(dataClass.properties[1].parent, notNullValue())
            assertThat(dataClass.properties[2].parent, notNullValue())
            val parent = dataClass.properties[2].parent!!
            assertThat(parent.prefix, `is`(""))
            assertThat(parent.property.name, `is`("myPoint"))
            assertThat(
                parent.dataClass.typeName,
                `is`(XClassName.get("foo.bar", "MyDataClass", "Point"))
            )
        }
    }

    @Test
    fun embeddedWithPrefix() {
        singleRun(
            """
                int id;
                @Embedded(prefix = "foo")
                Point myPoint;
                static class Point {
                    int x;
                    @ColumnInfo(name = "y2")
                    int y;
                }
                """
        ) { dataClass, _ ->
            assertThat(dataClass.properties.size, `is`(3))
            assertThat(dataClass.properties[1].name, `is`("x"))
            assertThat(dataClass.properties[2].name, `is`("y"))
            assertThat(dataClass.properties[1].columnName, `is`("foox"))
            assertThat(dataClass.properties[2].columnName, `is`("fooy2"))
            val parent = dataClass.properties[2].parent!!
            assertThat(parent.prefix, `is`("foo"))
        }
    }

    @Test
    fun nestedEmbedded() {
        singleRun(
            """
                int id;
                @Embedded(prefix = "foo")
                Point myPoint;
                static class Point {
                    int x;
                    @ColumnInfo(name = "y2")
                    int y;
                    @Embedded(prefix = "bar")
                    Coordinate coordinate;
                }
                static class Coordinate {
                    double lat;
                    double lng;
                    @Ignore
                    String ignored;
                }
                """
        ) { dataClass, _ ->
            assertThat(dataClass.properties.size, `is`(5))
            assertThat(
                dataClass.properties.map { it.columnName },
                `is`(listOf("id", "foox", "fooy2", "foobarlat", "foobarlng"))
            )
        }
    }

    @Test
    fun embedded_generic() {
        val point =
            Source.java(
                "foo.bar.Point",
                """
            package foo.bar;
            public class Point {
                public int x;
                public int y;
            }
            """
            )
        val base =
            Source.java(
                "foo.bar.BaseClass",
                """
            package foo.bar;
            import ${Embedded::class.java.canonicalName};
            public class BaseClass<T> {
                @Embedded
                public T genericProperty;
            }
            """
            )
        singleRunFullClass(
            """
                package foo.bar;
                public class MyDataClass extends BaseClass<Point> {
                    public int normalProperty;
                }
                """,
            point,
            base
        ) { dataClass, _ ->
            assertThat(dataClass.properties.size, `is`(3))
            assertThat(
                dataClass.properties.map { it.columnName }.toSet(),
                `is`(setOf("x", "y", "normalProperty"))
            )
            val pointProperty =
                dataClass.embeddedProperties.first { it.property.name == "genericProperty" }
            assertThat(pointProperty.dataClass.typeName, `is`(XClassName.get("foo.bar", "Point")))
        }
    }

    @Test
    fun embedded_badType() {
        singleRun(
            """
                int id;
                @Embedded
                int embeddedPrimitive;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.EMBEDDED_TYPES_MUST_BE_A_CLASS_OR_INTERFACE)
            }
        }
    }

    @Test
    fun duplicateColumnNames() {
        singleRun(
            """
                int id;
                @ColumnInfo(name = "id")
                int another;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.dataClassDuplicatePropertyNames("id", listOf("id", "another"))
                )
                hasErrorContaining(DATA_CLASS_PROPERTY_HAS_DUPLICATE_COLUMN_NAME)
                hasErrorCount(3)
            }
        }
    }

    @Test
    fun duplicateColumnNamesFromEmbedded() {
        singleRun(
            """
                int id;
                @Embedded
                Foo foo;
                static class Foo {
                    @ColumnInfo(name = "id")
                    int x;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.dataClassDuplicatePropertyNames("id", listOf("id", "foo > x"))
                )
                hasErrorContaining(DATA_CLASS_PROPERTY_HAS_DUPLICATE_COLUMN_NAME)
                hasErrorCount(3)
            }
        }
    }

    @Test
    fun dropSubPrimaryKeyNoWarningForPojo() {
        singleRun(
            """
                @PrimaryKey
                int id;
                @Embedded
                Point myPoint;
                static class Point {
                    @PrimaryKey
                    int x;
                    int y;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun relation_view() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public List<UserSummary> user;
                """,
            COMMON.USER_SUMMARY
        ) { _, _ ->
        }
    }

    @Test
    fun relation_notCollection() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public User user;
                """,
            COMMON.USER
        ) { _, _ ->
        }
    }

    @Test
    fun relation_columnInfo() {
        singleRun(
            """
                int id;
                @ColumnInfo
                @Relation(parentColumn = "id", entityColumn = "uid")
                public List<User> user;
                """,
            COMMON.USER
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.CANNOT_USE_MORE_THAN_ONE_DATA_CLASS_PROPERTY_ANNOTATION
                )
            }
        }
    }

    @Test
    fun relation_notEntity() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public List<NotAnEntity> user;
                """,
            COMMON.NOT_AN_ENTITY
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.NOT_ENTITY_OR_VIEW)
            }
        }
    }

    @Test
    fun relation_notDeclared() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public long user;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.RELATION_TYPE_MUST_BE_A_CLASS_OR_INTERFACE)
            }
        }
    }

    @Test
    fun relation_missingParent() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "idk", entityColumn = "uid")
                public List<User> user;
                """,
            COMMON.USER
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    relationCannotFindParentEntityProperty(
                        "foo.bar.MyDataClass",
                        "idk",
                        listOf("id")
                    )
                )
            }
        }
    }

    @Test
    fun relation_missingEntityProperty() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "idk")
                public List<User> user;
                """,
            COMMON.USER
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    relationCannotFindEntityProperty(
                        "foo.bar.User",
                        "idk",
                        listOf("uid", "name", "lastName", "age")
                    )
                )
            }
        }
    }

    @Test
    fun relation_missingType() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public List<User> user;
                """
        ) { _, invocation ->
            if (invocation.isKsp) {
                // TODO https://github.com/google/ksp/issues/371
                // KSP is losing `isError` information in some cases. Compilation should still
                // fail (as class does not exist) but it will fail with a different error
                invocation.assertCompilationResult { compilationDidFail() }
            } else {
                invocation.assertCompilationResult {
                    hasErrorContaining(
                        "Element 'foo.bar.MyDataClass' references a type that is not present"
                    )
                }
            }
        }
    }

    @Test
    fun relation_nestedProperty() {
        singleRun(
            """
                static class Nested {
                    @ColumnInfo(name = "foo")
                    public int id;
                }
                @Embedded
                Nested nested;
                @Relation(parentColumn = "foo", entityColumn = "uid")
                public List<User> user;
                """,
            COMMON.USER
        ) { dataClass, _ ->
            assertThat(dataClass.relations.first().parentProperty.columnName, `is`("foo"))
        }
    }

    @Test
    fun relation_nestedRelation() {
        singleRun(
            """
                static class UserWithNested {
                    @Embedded
                    public User user;
                    @Relation(parentColumn = "uid", entityColumn = "uid")
                    public List<User> selfs;
                }
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid", entity = User.class)
                public List<UserWithNested> user;
                """,
            COMMON.USER
        ) { dataClass, invocation ->
            assertThat(dataClass.relations.first().parentProperty.name, `is`("id"))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun relation_affinityMismatch() {
        singleRun(
            """
                String id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public List<User> user;
                """,
            COMMON.USER
        ) { dataClass, invocation ->
            // trigger assignment evaluation
            RelationCollector.createCollectors(invocation.context, dataClass.relations)
            assertThat(dataClass.relations.size, `is`(1))
            assertThat(dataClass.relations.first().entityProperty.name, `is`("uid"))
            assertThat(dataClass.relations.first().parentProperty.name, `is`("id"))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.relationAffinityMismatch(
                        parentAffinity = SQLTypeAffinity.TEXT,
                        childAffinity = SQLTypeAffinity.INTEGER,
                        parentColumn = "id",
                        childColumn = "uid"
                    )
                )
            }
        }
    }

    @Test
    fun relation_simple() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public List<User> user;
                """,
            COMMON.USER
        ) { dataClass, invocation ->
            assertThat(dataClass.relations.size, `is`(1))
            assertThat(dataClass.relations.first().entityProperty.name, `is`("uid"))
            assertThat(dataClass.relations.first().parentProperty.name, `is`("id"))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun relation_badProjection() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid", projection={"i_dont_exist"})
                public List<User> user;
                """,
            COMMON.USER
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.relationBadProject(
                        "foo.bar.User",
                        listOf("i_dont_exist"),
                        listOf("uid", "name", "lastName", "ageColumn")
                    )
                )
            }
        }
    }

    @Test
    fun relation_badReturnTypeInGetter() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                private List<User> user;
                public void setUser(List<User> user){ this.user = user;}
                public User getUser(){return null;}
                """,
            COMMON.USER
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(CANNOT_FIND_GETTER_FOR_PROPERTY)
            }
        }
    }

    @Test
    fun relation_primitiveList() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid",  projection={"uid"},
                        entity = User.class)
                public List<Integer> userIds;
                """,
            COMMON.USER
        ) { dataClass, _ ->
            assertThat(dataClass.relations.size, `is`(1))
            val rel = dataClass.relations.first()
            assertThat(rel.projection, `is`(listOf("uid")))
            assertThat(rel.entity.typeName, `is`(COMMON.USER_TYPE_NAME))
        }
    }

    @Test
    fun relation_stringList() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid",  projection={"name"},
                        entity = User.class)
                public List<String> userNames;
                """,
            COMMON.USER
        ) { dataClass, _ ->
            assertThat(dataClass.relations.size, `is`(1))
            val rel = dataClass.relations.first()
            assertThat(rel.projection, `is`(listOf("name")))
            assertThat(rel.entity.typeName, `is`(COMMON.USER_TYPE_NAME))
        }
    }

    @Test
    fun relation_extendsBounds() {
        singleRun(
            """
                int id;
                @Relation(parentColumn = "id", entityColumn = "uid")
                public List<? extends User> user;
                """,
            COMMON.USER
        ) { dataClass, invocation ->
            assertThat(dataClass.relations.size, `is`(1))
            assertThat(dataClass.relations.first().entityProperty.name, `is`("uid"))
            assertThat(dataClass.relations.first().parentProperty.name, `is`("id"))
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun relation_associateBy() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRef",
                """
            package foo.bar;

            import androidx.room.*;

            @Entity(
                primaryKeys = {"uid","friendId"},
                foreignKeys = {
                    @ForeignKey(
                            entity = User.class,
                            parentColumns = "uid",
                            childColumns = "uid",
                            onDelete = ForeignKey.CASCADE),
                    @ForeignKey(
                            entity = User.class,
                            parentColumns = "uid",
                            childColumns = "friendId",
                            onDelete = ForeignKey.CASCADE),
                },
                indices = { @Index("uid"), @Index("friendId") }
            )
            public class UserFriendsXRef {
                public int uid;
                public int friendId;
            }
            """
            )
        singleRun(
            """
                int id;
                @Relation(
                    parentColumn = "id", entityColumn = "uid",
                    associateBy = @Junction(
                        value = UserFriendsXRef.class,
                        parentColumn = "uid", entityColumn = "friendId")
                )
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { dataClass, invocation ->
            assertThat(dataClass.relations.size, `is`(1))
            assertThat(dataClass.relations.first().junction, notNullValue())
            assertThat(
                dataClass.relations.first().junction!!.parentProperty.columnName,
                `is`("uid")
            )
            assertThat(
                dataClass.relations.first().junction!!.entityProperty.columnName,
                `is`("friendId")
            )
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun relation_associateBy_withView() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRefView",
                """
            package foo.bar;

            import androidx.room.*;

            @DatabaseView("SELECT 1, 2, FROM User")
            public class UserFriendsXRefView {
                public int uid;
                public int friendId;
            }
        """
            )
        singleRun(
            """
                int id;
                @Relation(
                    parentColumn = "id", entityColumn = "uid",
                    associateBy = @Junction(
                        value = UserFriendsXRefView.class,
                        parentColumn = "uid", entityColumn = "friendId")
                )
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun relation_associateBy_defaultColumns() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRef",
                """
            package foo.bar;

            import androidx.room.*;

            @Entity(
                primaryKeys = {"uid","friendId"},
                foreignKeys = {
                    @ForeignKey(
                            entity = User.class,
                            parentColumns = "uid",
                            childColumns = "uid",
                            onDelete = ForeignKey.CASCADE),
                    @ForeignKey(
                            entity = User.class,
                            parentColumns = "uid",
                            childColumns = "friendId",
                            onDelete = ForeignKey.CASCADE),
                },
                indices = { @Index("uid"), @Index("friendId") }
            )
            public class UserFriendsXRef {
                public int uid;
                public int friendId;
            }
        """
            )
        singleRun(
            """
                int friendId;
                @Relation(
                    parentColumn = "friendId", entityColumn = "uid",
                    associateBy = @Junction(UserFriendsXRef.class))
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { _, invocation ->
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun relation_associateBy_missingParentColumn() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRef",
                """
            package foo.bar;

            import androidx.room.*;

            @Entity(primaryKeys = {"friendFrom","uid"})
            public class UserFriendsXRef {
                public int friendFrom;
                public int uid;
            }
        """
            )
        singleRun(
            """
                int id;
                @Relation(
                    parentColumn = "id", entityColumn = "uid",
                    associateBy = @Junction(UserFriendsXRef.class)
                )
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    relationCannotFindJunctionParentProperty(
                        "foo.bar.UserFriendsXRef",
                        "id",
                        listOf("friendFrom", "uid")
                    )
                )
            }
        }
    }

    @Test
    fun relation_associateBy_missingEntityColumn() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRef",
                """
            package foo.bar;

            import androidx.room.*;

            @Entity(primaryKeys = {"friendA","friendB"})
            public class UserFriendsXRef {
                public int friendA;
                public int friendB;
            }
        """
            )
        singleRun(
            """
                int friendA;
                @Relation(
                    parentColumn = "friendA", entityColumn = "uid",
                    associateBy = @Junction(UserFriendsXRef.class)
                )
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    relationCannotFindJunctionEntityProperty(
                        "foo.bar.UserFriendsXRef",
                        "uid",
                        listOf("friendA", "friendB")
                    )
                )
            }
        }
    }

    @Test
    fun relation_associateBy_missingSpecifiedParentColumn() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRef",
                """
            package foo.bar;

            import androidx.room.*;

            @Entity(primaryKeys = {"friendA","friendB"})
            public class UserFriendsXRef {
                public int friendA;
                public int friendB;
            }
        """
            )
        singleRun(
            """
                int friendA;
                @Relation(
                    parentColumn = "friendA", entityColumn = "uid",
                    associateBy = @Junction(
                        value = UserFriendsXRef.class,
                        parentColumn = "bad_col")
                )
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    relationCannotFindJunctionParentProperty(
                        "foo.bar.UserFriendsXRef",
                        "bad_col",
                        listOf("friendA", "friendB")
                    )
                )
            }
        }
    }

    @Test
    fun relation_associateBy_missingSpecifiedEntityColumn() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRef",
                """
            package foo.bar;

            import androidx.room.*;

            @Entity(primaryKeys = {"friendA","friendB"})
            public class UserFriendsXRef {
                public int friendA;
                public int friendB;
            }
        """
            )
        singleRun(
            """
                int friendA;
                @Relation(
                    parentColumn = "friendA", entityColumn = "uid",
                    associateBy = @Junction(
                        value = UserFriendsXRef.class,
                        entityColumn = "bad_col")
                )
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    relationCannotFindJunctionEntityProperty(
                        "foo.bar.UserFriendsXRef",
                        "bad_col",
                        listOf("friendA", "friendB")
                    )
                )
            }
        }
    }

    @Test
    fun relation_associateBy_warnIndexOnJunctionColumn() {
        val junctionEntity =
            Source.java(
                "foo.bar.UserFriendsXRef",
                """
                package foo.bar;
                import androidx.room.*;
                @Entity
                public class UserFriendsXRef {
                    @PrimaryKey(autoGenerate = true)
                    public long rowid;
                    public int uid;
                    public int friendId;
                }
            """
            )
        singleRun(
            """
                int friendId;
                @Relation(
                    parentColumn = "friendId", entityColumn = "uid",
                    associateBy = @Junction(UserFriendsXRef.class))
                public List<User> user;
                """,
            COMMON.USER,
            junctionEntity
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasWarningCount(2)
                hasWarningContaining(junctionColumnWithoutIndex("foo.bar.UserFriendsXRef", "uid"))
            }
        }
    }

    @Test
    fun cache() {
        val dataClass =
            Source.java(
                MY_DATA_CLASS.canonicalName,
                """
            $HEADER
            int id;
            $FOOTER
            """
            )
        runProcessorTest(sources = listOf(dataClass)) { invocation ->
            val element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS)
            val dataClass1 =
                DataClassProcessor.createFor(
                        invocation.context,
                        element,
                        PropertyProcessor.BindingScope.BIND_TO_STMT,
                        null
                    )
                    .process()
            assertThat(dataClass1, notNullValue())
            val dataClass2 =
                DataClassProcessor.createFor(
                        invocation.context,
                        element,
                        PropertyProcessor.BindingScope.BIND_TO_STMT,
                        null
                    )
                    .process()
            assertThat(dataClass2, sameInstance(dataClass1))

            val dataClass3 =
                DataClassProcessor.createFor(
                        invocation.context,
                        element,
                        PropertyProcessor.BindingScope.READ_FROM_STMT,
                        null
                    )
                    .process()
            assertThat(dataClass3, notNullValue())
            assertThat(dataClass3, not(sameInstance(dataClass1)))

            val dataClass4 =
                DataClassProcessor.createFor(
                        invocation.context,
                        element,
                        PropertyProcessor.BindingScope.TWO_WAY,
                        null
                    )
                    .process()
            assertThat(dataClass4, notNullValue())
            assertThat(dataClass4, not(sameInstance(dataClass1)))
            assertThat(dataClass4, not(sameInstance(dataClass3)))

            val dataClass5 =
                DataClassProcessor.createFor(
                        invocation.context,
                        element,
                        PropertyProcessor.BindingScope.TWO_WAY,
                        null
                    )
                    .process()
            assertThat(dataClass5, sameInstance(dataClass4))

            val type = invocation.context.processingEnv.requireType(CommonTypeNames.STRING)
            val mockElement = mock(XFieldElement::class.java)
            doReturn(type).`when`(mockElement).type
            val fakeProperty =
                Property(
                    element = mockElement,
                    name = "foo",
                    type = type,
                    affinity = SQLTypeAffinity.TEXT,
                    columnName = "foo",
                    parent = null,
                    indexed = false
                )
            val fakeEmbedded = EmbeddedProperty(fakeProperty, "", null)

            val dataClass6 =
                DataClassProcessor.createFor(
                        invocation.context,
                        element,
                        PropertyProcessor.BindingScope.TWO_WAY,
                        fakeEmbedded
                    )
                    .process()
            assertThat(dataClass6, notNullValue())
            assertThat(dataClass6, not(sameInstance(dataClass1)))
            assertThat(dataClass6, not(sameInstance(dataClass3)))
            assertThat(dataClass6, not(sameInstance(dataClass4)))

            val dataClass7 =
                DataClassProcessor.createFor(
                        invocation.context,
                        element,
                        PropertyProcessor.BindingScope.TWO_WAY,
                        fakeEmbedded
                    )
                    .process()
            assertThat(dataClass7, sameInstance(dataClass6))
        }
    }

    @Test
    fun constructor_empty() {
        val dataClassCode =
            """
            public String mName;
            """
        singleRun(dataClassCode) { dataClass, _ ->
            assertThat(dataClass.constructor, notNullValue())
            assertThat(dataClass.constructor?.params, `is`(emptyList()))
        }
    }

    @Test
    fun constructor_ambiguous_twoPropertiesExactMatch() {
        val dataClassCode =
            """
            public String mName;
            public String _name;
            public MyDataClass(String mName) {
            }
            """
        singleRun(dataClassCode) { dataClass, _ ->
            val param = dataClass.constructor?.params?.first()
            assertThat(param, instanceOf(Constructor.Param.PropertyParam::class.java))
            assertThat((param as Constructor.Param.PropertyParam).property.name, `is`("mName"))
            assertThat(
                dataClass.properties.find { it.name == "mName" }?.setter?.callType,
                `is`(CallType.CONSTRUCTOR)
            )
        }
    }

    @Test
    fun constructor_ambiguous_oneTypeMatches() {
        val dataClassCode =
            """
            public String mName;
            public int _name;
            public MyDataClass(String name) {
            }
            """
        singleRun(dataClassCode) { dataClass, _ ->
            val param = dataClass.constructor?.params?.first()
            assertThat(param, instanceOf(Constructor.Param.PropertyParam::class.java))
            assertThat((param as Constructor.Param.PropertyParam).property.name, `is`("mName"))
            assertThat(
                dataClass.properties.find { it.name == "mName" }?.setter?.callType,
                `is`(CallType.CONSTRUCTOR)
            )
        }
    }

    @Test
    fun constructor_ambiguous_twoProperties() {
        val dataClass =
            """
            String mName;
            String _name;
            public MyDataClass(String name) {
            }
            """
        singleRun(dataClass) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.ambiguousConstructor(
                        MY_DATA_CLASS.canonicalName,
                        "name",
                        listOf("mName", "_name")
                    )
                )
            }
        }
    }

    @Test
    fun constructor_noMatchBadType() {
        singleRun(
            """
            int foo;
            public MyDataClass(String foo) {
            }
        """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(MISSING_DATA_CLASS_CONSTRUCTOR)
            }
        }
    }

    @Test
    fun constructor_noMatch() {
        singleRun(
            """
            String mName;
            String _name;
            public MyDataClass(String foo) {
            }
        """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(MISSING_DATA_CLASS_CONSTRUCTOR)
            }
        }
    }

    @Test
    fun constructor_noMatchMultiArg() {
        singleRun(
            """
            String mName;
            int bar;
            public MyDataClass(String foo, String name) {
            }
        """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(MISSING_DATA_CLASS_CONSTRUCTOR)
            }
        }
    }

    @Test
    fun constructor_multipleMatching() {
        singleRun(
            """
            String mName;
            String mLastName;
            public MyDataClass(String name) {
            }
            public MyDataClass(String name, String lastName) {
            }
        """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.TOO_MANY_DATA_CLASS_CONSTRUCTORS)
            }
        }
    }

    @Test
    fun constructor_multipleMatchingWithIgnored() {
        singleRun(
            """
            String mName;
            String mLastName;
            @Ignore
            public MyDataClass(String name) {
            }
            public MyDataClass(String name, String lastName) {
            }
        """
        ) { dataClass, _ ->
            assertThat(dataClass.constructor, notNullValue())
            assertThat(dataClass.constructor?.params?.size, `is`(2))
            assertThat(
                dataClass.properties.find { it.name == "mName" }?.setter?.callType,
                `is`(CallType.CONSTRUCTOR)
            )
            assertThat(
                dataClass.properties.find { it.name == "mLastName" }?.setter?.callType,
                `is`(CallType.CONSTRUCTOR)
            )
        }
    }

    @Test
    fun constructor_dontTryForBindToScope() {
        singleRun(
            """
            String mName;
            String mLastName;
        """
        ) { _, invocation ->
            val process2 =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.BIND_TO_STMT,
                        parent = null
                    )
                    .process()
            assertThat(process2.constructor, nullValue())
        }
    }

    @Test
    fun constructor_bindForTwoWay() {
        singleRun(
            """
            String mName;
            String mLastName;
        """
        ) { _, invocation ->
            val process2 =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.TWO_WAY,
                        parent = null
                    )
                    .process()
            assertThat(process2.constructor, notNullValue())
        }
    }

    @Test
    fun constructor_multipleMatching_withNoArg() {
        singleRun(
            """
            String mName;
            String mLastName;
            public MyDataClass() {
            }
            public MyDataClass(String name, String lastName) {
            }
        """
        ) { dataClass, invocation ->
            assertThat(dataClass.constructor?.params?.size ?: -1, `is`(0))
            invocation.assertCompilationResult {
                hasWarningContaining(
                    ProcessorErrors.TOO_MANY_DATA_CLASS_CONSTRUCTORS_CHOOSING_NO_ARG
                )
            }
        }
    }

    @Test // added for b/69562125
    fun constructor_withNullabilityAnnotation() {
        singleRun(
            """
            String mName;
            public MyDataClass(@androidx.annotation.NonNull String name) {}
            """
        ) { dataClass, _ ->
            val constructor = dataClass.constructor
            assertThat(constructor, notNullValue())
            assertThat(constructor!!.params.size, `is`(1))
        }
    }

    @Test
    fun constructor_relationParameter() {
        singleRun(
            """
            @Relation(entity = foo.bar.User.class, parentColumn = "uid", entityColumn="uid",
            projection = "name")
            public List<String> items;
            public String uid;
            public MyDataClass(String uid, List<String> items) {
            }
            """,
            COMMON.USER
        ) { _, _ ->
        }
    }

    @Test
    fun recursion_1Level_embedded() {
        singleRun(
            """
                @Embedded
                MyDataClass MyDataClass;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_1Level_relation() {
        singleRun(
            """
                long id;
                long parentId;
                @Relation(parentColumn = "id", entityColumn = "parentId")
                Set<MyDataClass> children;
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_1Level_relation_specifyEntity() {
        singleRun(
            """
                @Embedded
                A a;

                static class A {
                    long id;
                    long parentId;
                    @Relation(entity = A.class, parentColumn = "id", entityColumn = "parentId")
                    Set<AWithB> children;
                }

                static class B {
                   long id;
                }

                static class AWithB {
                    @Embedded
                    A a;
                    @Embedded
                    B b;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass.A -> foo.bar.MyDataClass.A"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_2Levels_relationToEmbed() {
        singleRun(
            """
                int dataClassId;

                @Relation(parentColumn = "dataClassId", entityColumn = "entityId")
                List<MyEntity> myEntity;

                @Entity
                static class MyEntity {
                    int entityId;

                    @Embedded
                    MyDataClass MyDataClass;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass.MyEntity -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_2Levels_onlyEmbeds_dataClassToEntity() {
        singleRun(
            """
                @Embedded
                A a;

                @Entity
                static class A {
                    @Embedded
                    MyDataClass MyDataClass;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass.A -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_2Levels_onlyEmbeds_onlyPojos() {
        singleRun(
            """
                @Embedded
                A a;
                static class A {
                    @Embedded
                    MyDataClass MyDataClass;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass.A -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_2Level_relationToEmbed() {
        singleRun(
            """
                @Embedded
                A a;

                static class A {
                    long id;
                    long parentId;
                    @Relation(parentColumn = "id", entityColumn = "parentId")
                    Set<AWithB> children;
                }

                static class B {
                   long id;
                }

                static class AWithB {
                    @Embedded
                    A a;
                    @Embedded
                    B b;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass.A -> foo.bar.MyDataClass.AWithB -> foo.bar.MyDataClass.A"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_3Levels() {
        singleRun(
            """
                @Embedded
                A a;
                public static class A {
                    @Embedded
                    B b;
                }
                public static class B {
                    @Embedded
                    MyDataClass MyDataClass;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass.A -> foo.bar.MyDataClass.B -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_1Level_1LevelDown() {
        singleRun(
            """
                @Embedded
                A a;
                static class A {
                    @Embedded
                    B b;
                }
                static class B {
                    @Embedded
                    A a;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass.A -> foo.bar.MyDataClass.B -> foo.bar.MyDataClass.A"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_branchAtLevel0_afterBackTrack() {
        singleRun(
            """
                @PrimaryKey
                int id;
                @Embedded
                A a;
                @Embedded
                C c;
                static class A {
                    @Embedded
                    B b;
                }
                static class B {
                }
                static class C {
                    @Embedded
                    MyDataClass MyDataClass;
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass.C -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun recursion_branchAtLevel1_afterBackTrack() {
        singleRun(
            """
                @PrimaryKey
                int id;
                @Embedded
                A a;
                static class A {
                    @Embedded
                    B b;
                    @Embedded
                    MyDataClass MyDataClass;
                }
                static class B {
                    @Embedded
                    C c;
                }
                static class C {
                }
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.RECURSIVE_REFERENCE_DETECTED.format(
                        "foo.bar.MyDataClass -> foo.bar.MyDataClass.A -> foo.bar.MyDataClass"
                    )
                )
            }
        }
    }

    @Test
    fun dataClass_primaryConstructor() {
        listOf(
                "foo.bar.TestData.AllDefaultVals",
                "foo.bar.TestData.AllDefaultVars",
                "foo.bar.TestData.SomeDefaultVals",
                "foo.bar.TestData.SomeDefaultVars",
                "foo.bar.TestData.WithJvmOverloads"
            )
            .forEach {
                runProcessorTest(sources = listOf(TEST_DATA)) { invocation ->
                    DataClassProcessor.createFor(
                            context = invocation.context,
                            element = invocation.processingEnv.requireTypeElement(it),
                            bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                            parent = null
                        )
                        .process()
                    invocation.assertCompilationResult { hasNoWarnings() }
                }
            }
    }

    @Test
    fun dataClass_withJvmOverloads_primaryConstructor() {
        runProcessorTest(sources = listOf(TEST_DATA)) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element =
                        invocation.processingEnv.requireTypeElement(
                            "foo.bar.TestData.WithJvmOverloads"
                        ),
                    bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                    parent = null
                )
                .process()
            invocation.assertCompilationResult { hasNoWarnings() }
        }
    }

    @Test
    fun ignoredColumns() {
        val source =
            Source.java(
                MY_DATA_CLASS.canonicalName,
                """
            package foo.bar;
            import androidx.room.*;
            @Entity(ignoredColumns = {"bar"})
            public class ${MY_DATA_CLASS.simpleNames.single()} {
                public String foo;
                public String bar;
            }
            """
            )
        runProcessorTest(sources = listOf(source)) { invocation ->
            val dataClass =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process()
            assertThat(dataClass.properties.find { it.name == "foo" }, notNullValue())
            assertThat(dataClass.properties.find { it.name == "bar" }, nullValue())
        }
    }

    @Test
    fun ignoredColumns_noConstructor() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    @Entity(ignoredColumns = {"bar"})
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private final String foo;
                        private final String bar;
                        public ${MY_DATA_CLASS.simpleNames.single()}(String foo) {
                          this.foo = foo;
                          this.bar = null;
                        }

                        public String getFoo() {
                          return this.foo;
                        }
                    }
                    """
                )
            )
        ) { invocation ->
            val dataClass =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process()
            assertThat(dataClass.properties.find { it.name == "foo" }, notNullValue())
            assertThat(dataClass.properties.find { it.name == "bar" }, nullValue())
        }
    }

    @Test
    fun ignoredColumns_noSetterGetter() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    @Entity(ignoredColumns = {"bar"})
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private String foo;
                        private String bar;
                        public String getFoo() {
                          return this.foo;
                        }
                        public void setFoo(String foo) {
                          this.foo = foo;
                        }
                    }
                    """
                )
            )
        ) { invocation ->
            val dataClass =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process()
            assertThat(dataClass.properties.find { it.name == "foo" }, notNullValue())
            assertThat(dataClass.properties.find { it.name == "bar" }, nullValue())
        }
    }

    @Test
    fun ignoredColumns_columnInfo() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    @Entity(ignoredColumns = {"my_bar"})
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        public String foo;
                        @ColumnInfo(name = "my_bar")
                        public String bar;
                    }
                    """
                )
            )
        ) { invocation ->
            val dataClass =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process()
            assertThat(dataClass.properties.find { it.name == "foo" }, notNullValue())
            assertThat(dataClass.properties.find { it.name == "bar" }, nullValue())
        }
    }

    @Test
    fun ignoredColumns_missing() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    @Entity(ignoredColumns = {"no_such_column"})
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        public String foo;
                        public String bar;
                    }
                    """
                )
            )
        ) { invocation ->
            val dataClass =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process()
            assertThat(dataClass.properties.find { it.name == "foo" }, notNullValue())
            assertThat(dataClass.properties.find { it.name == "bar" }, notNullValue())
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.missingIgnoredColumns(listOf("no_such_column")))
            }
        }
    }

    @Test
    fun noSetter_scopeBindStmt() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private String foo;
                        private String bar;
                        public String getFoo() { return foo; }
                        public String getBar() { return bar; }
                    }
                    """
                )
            )
        ) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.BIND_TO_STMT,
                    parent = null
                )
                .process()
        }
    }

    @Test
    fun noSetter_scopeTwoWay() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private String foo;
                        private String bar;
                        public String getFoo() { return foo; }
                        public String getBar() { return bar; }
                    }
                    """
                )
            )
        ) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.TWO_WAY,
                    parent = null
                )
                .process()
            invocation.assertCompilationResult {
                hasErrorContaining("Cannot find setter for property.")
            }
        }
    }

    @Test
    fun noSetter_scopeReadFromCursor() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private String foo;
                        private String bar;
                        public String getFoo() { return foo; }
                        public String getBar() { return bar; }
                    }
                    """
                )
            )
        ) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                    parent = null
                )
                .process()
            invocation.assertCompilationResult {
                hasErrorContaining("Cannot find setter for property.")
            }
        }
    }

    @Test
    fun noGetter_scopeBindStmt() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private String foo;
                        private String bar;
                        public void setFoo(String foo) { this.foo = foo; }
                        public void setBar(String bar) { this.bar = bar; }
                    }
                    """
                )
            )
        ) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.BIND_TO_STMT,
                    parent = null
                )
                .process()
            invocation.assertCompilationResult {
                hasErrorContaining("Cannot find getter for property.")
            }
        }
    }

    @Test
    fun noGetter_scopeTwoWay() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private String foo;
                        private String bar;
                        public void setFoo(String foo) { this.foo = foo; }
                        public void setBar(String bar) { this.bar = bar; }
                    }
                    """
                )
            )
        ) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.TWO_WAY,
                    parent = null
                )
                .process()
            invocation.assertCompilationResult {
                hasErrorContaining("Cannot find getter for property.")
            }
        }
    }

    @Test
    fun noGetter_scopeReadCursor() {
        runProcessorTest(
            listOf(
                Source.java(
                    MY_DATA_CLASS.canonicalName,
                    """
                    package foo.bar;
                    import androidx.room.*;
                    public class ${MY_DATA_CLASS.simpleNames.single()} {
                        private String foo;
                        private String bar;
                        public void setFoo(String foo) { this.foo = foo; }
                        public void setBar(String bar) { this.bar = bar; }
                    }
                    """
                )
            )
        ) { invocation ->
            DataClassProcessor.createFor(
                    context = invocation.context,
                    element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                    bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                    parent = null
                )
                .process()
        }
    }

    @Test
    fun setterStartsWithIs() {
        runProcessorTest(
            listOf(
                Source.kotlin(
                    "Book.kt",
                    """
                    package foo.bar;
                    data class Book(
                        var isbn: String
                    ) {
                        var isbn2: String? = null
                    }
                    """
                )
            )
        ) { invocation ->
            val result =
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement("foo.bar.Book"),
                        bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                        parent = null
                    )
                    .process()
            val fields = result.properties.associateBy { it.name }
            val stringType = invocation.context.processingEnv.requireType(CommonTypeNames.STRING)
            assertThat(fields["isbn"]?.getter)
                .isEqualTo(
                    PropertyGetter(
                        propertyName = "isbn",
                        jvmName = if (invocation.isKsp2) "isbn" else "getIsbn",
                        type = stringType,
                        callType = CallType.SYNTHETIC_FUNCTION
                    )
                )
            assertThat(fields["isbn"]?.setter)
                .isEqualTo(
                    PropertySetter(
                        propertyName = "isbn",
                        jvmName = "isbn",
                        type = stringType,
                        callType = CallType.CONSTRUCTOR
                    )
                )

            assertThat(fields["isbn2"]?.getter)
                .isEqualTo(
                    PropertyGetter(
                        propertyName = "isbn2",
                        jvmName = if (invocation.isKsp2) "isbn2" else "getIsbn2",
                        type = stringType.makeNullable(),
                        callType = CallType.SYNTHETIC_FUNCTION
                    )
                )
            assertThat(fields["isbn2"]?.setter)
                .isEqualTo(
                    PropertySetter(
                        propertyName = "isbn2",
                        jvmName = if (invocation.isKsp2) "setbn2" else "setIsbn2",
                        type = stringType.makeNullable(),
                        callType = CallType.SYNTHETIC_FUNCTION
                    )
                )
        }
    }

    @Test
    fun embedded_nullability() {
        listOf("foo.bar.TestData.SomeEmbeddedVals").forEach {
            runProcessorTest(sources = listOf(TEST_DATA)) { invocation ->
                val result =
                    DataClassProcessor.createFor(
                            context = invocation.context,
                            element = invocation.processingEnv.requireTypeElement(it),
                            bindingScope = PropertyProcessor.BindingScope.READ_FROM_STMT,
                            parent = null
                        )
                        .process()

                val embeddedProperties = result.embeddedProperties

                assertThat(embeddedProperties.size, `is`(2))
                assertThat(embeddedProperties[0].nonNull, `is`(true))
                assertThat(embeddedProperties[1].nonNull, `is`(false))
            }
        }
    }

    private fun singleRun(
        code: String,
        vararg sources: Source,
        classpath: List<File> = emptyList(),
        handler: (DataClass, XTestInvocation) -> Unit
    ) {
        val dataClassCode =
            """
                $HEADER
                $code
                $FOOTER
                """
        @Suppress("CHANGING_ARGUMENTS_EXECUTION_ORDER_FOR_NAMED_VARARGS")
        singleRunFullClass(
            code = dataClassCode,
            sources = sources,
            classpath = classpath,
            handler = handler
        )
    }

    private fun singleRunFullClass(
        code: String,
        vararg sources: Source,
        classpath: List<File> = emptyList(),
        handler: (DataClass, XTestInvocation) -> Unit
    ) {
        val dataClassSource = Source.java(MY_DATA_CLASS.canonicalName, code)
        val all = sources.toList() + dataClassSource
        runProcessorTest(sources = all, classpath = classpath) { invocation ->
            handler.invoke(
                DataClassProcessor.createFor(
                        context = invocation.context,
                        element = invocation.processingEnv.requireTypeElement(MY_DATA_CLASS),
                        bindingScope = PropertyProcessor.BindingScope.TWO_WAY,
                        parent = null
                    )
                    .process(),
                invocation
            )
        }
    }

    // Kotlin data classes to verify the PojoProcessor.
    private val TEST_DATA =
        Source.kotlin(
            "TestData.kt",
            """
        package foo.bar
        import ${Embedded::class.java.canonicalName}
        private class TestData {
            data class AllDefaultVals(
                val name: String = "",
                val number: Int = 0,
                val bit: Boolean = false
            )
            data class AllDefaultVars(
                var name: String = "",
                var number: Int = 0,
                var bit: Boolean = false
            )
            data class SomeDefaultVals(
                val name: String,
                val number: Int = 0,
                val bit: Boolean
            )
            data class SomeDefaultVars(
                var name: String,
                var number: Int = 0,
                var bit: Boolean
            )
            data class WithJvmOverloads @JvmOverloads constructor(
                val name: String,
                val lastName: String = "",
                var number: Int = 0,
                var bit: Boolean
            )
            data class AllNullableVals(
                val name: String?,
                val number: Int?,
                val bit: Boolean?
            )
            data class SomeEmbeddedVals(
                val id: String,
                @Embedded(prefix = "non_nullable_") val nonNullableVal: AllNullableVals,
                @Embedded(prefix = "nullable_") val nullableVal: AllNullableVals?
            )
        }
        """
        )
}
