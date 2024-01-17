/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.java.JavaCodeFile
import androidx.navigation.safe.args.generator.java.JavaNavWriter
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.ResReference
import com.google.common.collect.ImmutableList
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import javax.tools.JavaFileObject
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class JavaNavWriterTest {

    private fun generateDirectionsCodeFile(
        destination: Destination,
        parentDirectionsFileList: List<JavaCodeFile>,
        useAndroidX: Boolean
    ) = JavaNavWriter(useAndroidX).generateDirectionsCodeFile(destination, parentDirectionsFileList)

    private fun generateDirectionsTypeSpec(action: Action, useAndroidX: Boolean) =
        JavaNavWriter(useAndroidX).generateDirectionsTypeSpec(action)

    private fun generateArgsCodeFile(
        destination: Destination,
        useAndroidX: Boolean
    ) = JavaNavWriter(useAndroidX).generateArgsCodeFile(destination)

    private fun id(id: String) = ResReference("a.b", "id", id)

    private fun wrappedInnerClass(spec: TypeSpec): JavaFileObject {
        val wrappedSpec = TypeSpec.classBuilder("BoringWrapper").addType(spec).build()
        return JavaFile.builder("a.b", wrappedSpec).build().toJavaFileObject()
    }

    private fun toJavaFileObject(spec: TypeSpec) =
        JavaFile.builder("a.b", spec).build().toJavaFileObject()

    private fun assertCompilesWithoutError(javaFileObject: JavaFileObject) {
        JavaSourcesSubject.assertThat(
            loadSourceFileObject("a.b.R", "a/b"),
            loadSourceFileObject(
                "a.b.secondreallyreallyreallyreallyreallyreally" +
                    "reallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                    "longpackage.R",
                "a/b/secondreallyreallyreallyreallyreallyreally" +
                    "reallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                    "longpackage"
            ),
            JavaFileObjects.forSourceString(
                "androidx.annotation.NonNull",
                "package androidx.annotation; public @interface NonNull {}"
            ),
            JavaFileObjects.forSourceString(
                "androidx.annotation.Nullable",
                "package androidx.annotation; public @interface Nullable {}"
            ),
            javaFileObject
        ).compilesWithoutError()
    }

    private fun JavaSourcesSubject.parsesAs(fullClassName: String) =
        this.parsesAs(fullClassName, "expected/java_nav_writer_test")

    private fun compileFiles(vararg javaFileObject: JavaFileObject) = javac()
        .compile(
            loadSourceFileObject("a.b.R", "a/b"),
            JavaFileObjects.forSourceString(
                "androidx.annotation.NonNull",
                "package androidx.annotation; public @interface NonNull {}"
            ),
            JavaFileObjects.forSourceString(
                "androidx.annotation.Nullable",
                "package androidx.annotation; public @interface Nullable {}"
            ),
            *javaFileObject
        )

    @Test
    fun testDirectionClassGeneration() {
        val actionSpec = generateDirectionsTypeSpec(
            Action(
                id("next"), id("destA"),
                listOf(
                    Argument("main", StringType),
                    Argument("mainInt", IntType),
                    Argument("optional", StringType, StringValue("bla")),
                    Argument("optionalInt", IntType, IntValue("239")),
                    Argument(
                        "optionalParcelable",
                        ObjectType("android.content.pm.ActivityInfo"),
                        NullValue,
                        true
                    ),
                    Argument(
                        "parcelable",
                        ObjectType("android.content.pm.ActivityInfo")
                    ),
                    Argument(
                        "innerData",
                        ObjectType("android.content.pm.ActivityInfo\$WindowLayout")
                    )
                )
            ),
            true
        )
        val actual = toJavaFileObject(actionSpec)
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.Next")
        // actions spec must be inner class to be compiled, because of static modifier on class
        assertCompilesWithoutError(wrappedInnerClass(actionSpec))
    }

    @Test
    fun testDirectionNoIdClassGeneration() {
        val actionSpec = generateDirectionsTypeSpec(Action(id("finish"), null, emptyList()), true)
        val actual = toJavaFileObject(actionSpec)
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.Finish")
        // actions spec must be inner class to be compiled, because of static modifier on class
        assertCompilesWithoutError(wrappedInnerClass(actionSpec))
    }

    @Test
    fun testDirectionsClassGeneration() {
        val nextAction = Action(
            id("next"), id("destA"),
            listOf(
                Argument("main", StringType),
                Argument("optional", StringType, StringValue("bla"))
            )
        )

        val prevAction = Action(
            id("previous"), id("destB"),
            listOf(
                Argument("arg1", StringType),
                Argument("arg2", StringType)
            )
        )

        val dest = Destination(
            null, ClassName.get("a.b", "MainFragment"), "fragment", listOf(),
            listOf(prevAction, nextAction)
        )

        val actual = generateDirectionsCodeFile(dest, emptyList(), true).toJavaFileObject()
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.MainFragmentDirections")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testDirectionsClassGeneration_longPackage() {
        val funAction = Action(
            ResReference(
                "a.b.secondreallyreallyreallyreally" +
                    "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                    "longpackage",
                "id", "next"
            ),
            id("destA"),
            listOf()
        )

        val dest = Destination(
            null,
            ClassName.get(
                "a.b.reallyreallyreallyreally" +
                    "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                    "longpackage",
                "LongPackageFragment"
            ),
            "fragment", listOf(),
            listOf(funAction)
        )

        val actual = generateDirectionsCodeFile(dest, emptyList(), true).toJavaFileObject()
        JavaSourcesSubject.assertThat(actual).parsesAs(
            "a.b.reallyreallyreallyreallyreally" +
                "reallyreallyreallyreallyreallyreallyreallyreallyreallyreallyreally" +
                "longpackage.LongPackageFragmentDirections"
        )
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testDirectionsClassGeneration_sanitizedNames() {
        val nextAction = Action(
            id("next_action"), id("destA"),
            listOf(
                Argument("main_arg", StringType),
                Argument("optional.arg", StringType, StringValue("bla"))
            )
        )

        val prevAction = Action(
            id("previous_action"), id("destB"),
            listOf(
                Argument("arg_1", StringType),
                Argument("arg.2", StringType)
            )
        )

        val dest = Destination(
            null, ClassName.get("a.b", "SanitizedMainFragment"),
            "fragment", listOf(), listOf(prevAction, nextAction)
        )

        val actual = generateDirectionsCodeFile(dest, emptyList(), true).toJavaFileObject()
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.SanitizedMainFragmentDirections")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testArgumentsClassGeneration() {
        val dest = Destination(
            null, ClassName.get("a.b", "MainFragment"), "fragment",
            listOf(
                Argument("main", StringType),
                Argument("optional", IntType, IntValue("-1")),
                Argument(
                    "reference", ReferenceType,
                    ReferenceValue(
                        ResReference(
                            "a.b", "drawable",
                            "background"
                        )
                    )
                ),
                Argument("referenceZeroDefaultValue", ReferenceType, IntValue("0")),
                Argument("floatArg", FloatType, FloatValue("1")),
                Argument("floatArrayArg", FloatArrayType),
                Argument(
                    "objectArrayArg",
                    ObjectArrayType(
                        "android.content.pm.ActivityInfo"
                    )
                ),
                Argument("boolArg", BoolType, BooleanValue("true")),
                Argument(
                    "optionalParcelable",
                    ObjectType("android.content.pm.ActivityInfo"),
                    NullValue,
                    true
                ),
                Argument(
                    "enumArg",
                    ObjectType("java.nio.file.AccessMode"),
                    EnumValue(ObjectType("java.nio.file.AccessMode"), "READ"),
                    false
                )
            ),
            listOf()
        )

        val actual = generateArgsCodeFile(dest, true).toJavaFileObject()
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.MainFragmentArgs")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testArgumentsClassGeneration_sanitizedNames() {
        val dest = Destination(
            null, ClassName.get("a.b", "SanitizedMainFragment"),
            "fragment",
            listOf(
                Argument("name.with.dot", IntType),
                Argument("name_with_underscore", IntType),
                Argument("name with spaces", IntType)
            ),
            listOf()
        )

        val actual = generateArgsCodeFile(dest, true).toJavaFileObject()
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.SanitizedMainFragmentArgs")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testArgumentsClassGeneration_innerClassName() {
        val dest = Destination(
            null, ClassName.get("a.b", "MainFragment\$InnerFragment"),
            "fragment",
            listOf(
                Argument("mainArg", StringType)
            ),
            listOf()
        )

        val actual = generateArgsCodeFile(dest, true).toJavaFileObject()
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.MainFragment\$InnerFragmentArgs")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testGeneratedDirectionEqualsImpl() {
        val nextAction = Action(id("next"), id("destA"), listOf(Argument("main", StringType)))
        val dest = Destination(
            null, ClassName.get("a.b", "MainFragment"), "fragment", listOf(),
            listOf(nextAction)
        )

        val actual = generateDirectionsCodeFile(dest, emptyList(), true).toJavaFileObject()

        val generatedFiles = compileFiles(actual).generatedFiles()
        val loader = InMemoryGeneratedClassLoader(generatedFiles)

        fun createNextObj(mainArgValue: String) = loader.loadClass("a.b.MainFragmentDirections")
            .getDeclaredMethod("next", String::class.java)
            .invoke(null, mainArgValue)

        val nextObjectA = createNextObj("data")
        val nextObjectB = createNextObj("data")
        val nextObjectC = createNextObj("different data")

        assertThat(nextObjectA, `is`(nextObjectB))
        assertThat(nextObjectA, not(`is`(nextObjectC)))
    }

    @Test
    fun testGeneratedDirectionHashCodeImpl() {
        val nextAction = Action(id("next"), id("destA"), listOf(Argument("main", StringType)))
        val dest = Destination(
            null, ClassName.get("a.b", "MainFragment"), "fragment", listOf(),
            listOf(nextAction)
        )

        val actual = generateDirectionsCodeFile(dest, emptyList(), true).toJavaFileObject()

        val generatedFiles = compileFiles(actual).generatedFiles()
        val loader = InMemoryGeneratedClassLoader(generatedFiles)

        fun createNextObj(mainArgValue: String): Any? = loader.loadClass(
            "a.b" +
                ".MainFragmentDirections"
        )
            .getDeclaredMethod("next", String::class.java)
            .invoke(null, mainArgValue)

        val nextObjectA = createNextObj("data")
        val nextObjectB = createNextObj("data")
        val nextObjectC = createNextObj("different data")

        assertThat(nextObjectA.hashCode(), `is`(nextObjectB.hashCode()))
        assertThat(nextObjectA.hashCode(), not(`is`(nextObjectC.hashCode())))
    }

    /**
     * Class loader that allows us to load classes from compile testing generated classes.
     */
    class InMemoryGeneratedClassLoader(
        private val generatedFiles: ImmutableList<JavaFileObject>
    ) : ClassLoader(ClassLoader.getSystemClassLoader()) {
        override fun findClass(name: String): Class<*> {
            val simpleName = name.let { it.substring(it.lastIndexOf('.') + 1, it.length) }
            val match = generatedFiles.firstOrNull {
                it.isNameCompatible(simpleName, JavaFileObject.Kind.CLASS)
            }
            if (match != null) {
                val data = match.openInputStream().use { inputStream ->
                    ByteArray(inputStream.available()).apply {
                        inputStream.read(this)
                    }
                }
                return super.defineClass(name, data, 0, data.size)
            }
            return super.findClass(name)
        }
    }
}
