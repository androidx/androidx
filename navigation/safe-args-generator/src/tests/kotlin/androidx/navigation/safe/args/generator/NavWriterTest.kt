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

    @Test
    fun testDirectionClassGeneration() {
        val destination = workingDir.newFolder()
        val actionSpec = generateDirectionsTypeSpec(Action(id("next"), id("destA"),
                listOf(
                        Argument("main", STRING),
                        Argument("mainInt", INT),
                        Argument("optional", STRING, StringValue("bla")),
                        Argument("optionalInt", INT, IntValue("239")))))
        JavaFile.builder("a.b", actionSpec).build().writeTo(destination)
        val expected = load("a.b.Next", "expected")
        val generated = File(destination, "/a/b/Next.java")
        MatcherAssert.assertThat(generated.exists(), CoreMatchers.`is`(true))
        val actual = JavaFileObjects.forResource(generated.toURI().toURL())
        JavaSourcesSubject.assertThat(actual).parsesAs(expected)
    }

    @Test
    fun testDirectionNoIdClassGeneration() {
        val destination = workingDir.newFolder()
        val actionSpec = generateDirectionsTypeSpec(Action(id("finish"), null, emptyList()))
        JavaFile.builder("a.b", actionSpec).build().writeTo(destination)
        val expected = load("a.b.Finish", "expected")
        val generated = File(destination, "/a/b/Finish.java")
        MatcherAssert.assertThat(generated.exists(), CoreMatchers.`is`(true))
        val actual = JavaFileObjects.forResource(generated.toURI().toURL())
        JavaSourcesSubject.assertThat(actual).parsesAs(expected)
    }

    @Test
    fun testDirectionsClassGeneration() {
        val destination = workingDir.newFolder()

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

        generateDirectionsJavaFile(dest).writeTo(destination)
        val expected = load("a.b.MainFragmentDirections", "expected")
        val rFile = load("a.b.R", "a/b")
        val generated = File(destination, "/a/b/MainFragmentDirections.java")
        MatcherAssert.assertThat(generated.exists(), CoreMatchers.`is`(true))
        val actual = JavaFileObjects.forResource(generated.toURI().toURL())
        JavaSourcesSubject.assertThat(actual).parsesAs(expected)
        JavaSourcesSubject.assertThat(actual, rFile).compilesWithoutError()
    }

    @Test
    fun testArgumentsClassGeneration() {
        val destination = workingDir.newFolder()

        val dest = Destination(null, ClassName.get("a.b", "MainFragment"), "fragment", listOf(
                Argument("main", STRING),
                Argument("optional", INT, IntValue("-1")),
                Argument("reference", REFERENCE, ReferenceValue(ResReference("a.b", "drawable",
                        "background")))),
                listOf())

        generateArgsJavaFile(dest).writeTo(destination)
        val expected = load("a.b.MainFragmentArgs", "expected")
        val rFile = load("a.b.R", "a/b")
        val generated = File(destination, "/a/b/MainFragmentArgs.java")
        MatcherAssert.assertThat(generated.exists(), CoreMatchers.`is`(true))
        val actual = JavaFileObjects.forResource(generated.toURI().toURL())
        JavaSourcesSubject.assertThat(actual).parsesAs(expected)
        JavaSourcesSubject.assertThat(rFile, actual).compilesWithoutError()
    }
}