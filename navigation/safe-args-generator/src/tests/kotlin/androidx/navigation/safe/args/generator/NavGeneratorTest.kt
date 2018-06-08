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

package androidx.navigation.safe.args.generator

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class NavGeneratorTest {

    @Suppress("MemberVisibilityCanBePrivate")
    @get:Rule
    val workingDir = TemporaryFolder()

    @Test
    fun test() {
        val output = generateSafeArgs("foo", "foo.flavor",
            testData("naive_test.xml"), workingDir.root)
        val javaNames = output.files
        val expectedSet = setOf(
                "androidx.navigation.testapp.MainFragmentDirections",
                "foo.flavor.NextFragmentDirections",
                "androidx.navigation.testapp.MainFragmentArgs",
                "foo.flavor.NextFragmentArgs"
                )
        assertThat(output.errors.isEmpty(), `is`(true))
        assertThat(javaNames.toSet(), `is`(expectedSet))
        javaNames.forEach { name ->
            val file = File(workingDir.root, "${name.replace('.', File.separatorChar)}.java")
            assertThat(file.exists(), `is`(true))
        }
    }
}