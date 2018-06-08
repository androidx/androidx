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

import androidx.navigation.safe.args.generator.NavType.BOOLEAN
import androidx.navigation.safe.args.generator.NavType.FLOAT
import androidx.navigation.safe.args.generator.NavType.INT
import androidx.navigation.safe.args.generator.NavType.STRING
import androidx.navigation.safe.args.generator.NavType.REFERENCE

import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.ResReference
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubject
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.nio.charset.Charset
import javax.tools.JavaFileObject

@RunWith(JUnit4::class)
class WriterTest {

    @get:Rule
    @Suppress("MemberVisibilityCanBePrivate")
    val workingDir = TemporaryFolder()

    private fun load(fullClassName: String, folder: String): JavaFileObject {
        val folderPath = "src/tests/test-data/${if (folder.isEmpty()) "" else folder + "/"}"
        val split = fullClassName.split(".")
        val code = File("$folderPath/${split.last()}.java").readText(Charset.defaultCharset())
        return JavaFileObjects.forSourceString(fullClassName, code)
    }

    private fun id(id: String) = ResReference("a.b", "id", id)

    private fun wrappedInnerClass(spec: TypeSpec): JavaFileObject {
        val wrappedSpec = TypeSpec.classBuilder("BoringWrapper").addType(spec).build()
        return toJavaFileObject(JavaFile.builder("a.b", wrappedSpec).build())
    }

    private fun toJavaFileObject(javaFile: JavaFile): JavaFileObject {
        val destination = workingDir.newFolder()
        javaFile.writeTo(destination)
        val path = javaFile.packageName.replace('.', '/')
        val generated = File(destination, "$path/${javaFile.typeSpec.name}.java")
        MatcherAssert.assertThat(generated.exists(), CoreMatchers.`is`(true))
        return JavaFileObjects.forResource(generated.toURI().toURL())
    }

    private fun toJavaFileObject(spec: TypeSpec) =
            toJavaFileObject(JavaFile.builder("a.b", spec).build())

    private fun assertCompilesWithoutError(javaFileObject: JavaFileObject) {
        JavaSourcesSubject.assertThat(load("a.b.R", "a/b"), javaFileObject).compilesWithoutError()
    }

    private fun JavaSourcesSubject.parsesAs(fullClassName: String) =
            this.parsesAs(load(fullClassName, "expected"))

    @Test
    fun testDirectionClassGeneration() {
        val actionSpec = generateDirectionsTypeSpec(Action(id("next"), id("destA"),
                listOf(
                        Argument("main", STRING),
                        Argument("mainInt", INT),
                        Argument("optional", STRING, StringValue("bla")),
                        Argument("optionalInt", INT, IntValue("239")))))
        val actual = toJavaFileObject(actionSpec)
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.Next")
        // actions spec must be inner class to be compiled, because of static modifier on class
        assertCompilesWithoutError(wrappedInnerClass(actionSpec))
    }

    @Test
    fun testDirectionNoIdClassGeneration() {
        val actionSpec = generateDirectionsTypeSpec(Action(id("finish"), null, emptyList()))
        val actual = toJavaFileObject(actionSpec)
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.Finish")
        // actions spec must be inner class to be compiled, because of static modifier on class
        assertCompilesWithoutError(wrappedInnerClass(actionSpec))
    }

    @Test
    fun testDirectionsClassGeneration() {
        val nextAction = Action(id("next"), id("destA"),
                listOf(
                        Argument("main", STRING),
                        Argument("optional", STRING, StringValue("bla"))))

        val prevAction = Action(id("previous"), id("destB"),
                listOf(
                        Argument("arg1", STRING),
                        Argument("arg2", STRING)))

        val dest = Destination(null, ClassName.get("a.b", "MainFragment"), "fragment", listOf(),
                listOf(prevAction, nextAction))

        val actual = toJavaFileObject(generateDirectionsJavaFile(dest))
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.MainFragmentDirections")
        assertCompilesWithoutError(actual)
    }

    @Test
    fun testArgumentsClassGeneration() {
        val dest = Destination(null, ClassName.get("a.b", "MainFragment"), "fragment", listOf(
                Argument("main", STRING),
                Argument("optional", INT, IntValue("-1")),
                Argument("reference", REFERENCE, ReferenceValue(ResReference("a.b", "drawable",
                        "background"))),
                Argument("floatArg", FLOAT, FloatValue("1")),
                Argument("boolArg", BOOLEAN, BooleanValue("true"))),
                listOf())

        val actual = toJavaFileObject(generateArgsJavaFile(dest))
        JavaSourcesSubject.assertThat(actual).parsesAs("a.b.MainFragmentArgs")
        assertCompilesWithoutError(actual)
    }
}