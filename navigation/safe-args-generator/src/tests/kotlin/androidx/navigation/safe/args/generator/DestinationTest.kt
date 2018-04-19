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

import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.ResReference
import com.squareup.javapoet.ClassName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DestinationTest {

    @Test
    fun fullName() {
        val name = Destination.createName(id("main"), "foo.sub.ClassName", "some.app")
        assertThat(name, `is`(ClassName.get("foo.sub", "ClassName")))
    }

    @Test
    fun relativeWithSubpackage() {
        val name = Destination.createName(id("main"), ".sub.ClassName", "some.app")
        assertThat(name, `is`(ClassName.get("some.app.sub", "ClassName")))
    }

    @Test
    fun fullNameNoPackage() {
        val name = Destination.createName(id("main"), "ClassName", "some.app")
        assertThat(name, `is`(ClassName.get("", "ClassName")))
    }

    @Test
    fun idOnly() {
        val name = Destination.createName(id("main"), "", "some.app")
        assertThat(name, `is`(ClassName.get("foo.bar", "Main")))
    }
}

private fun id(name: String) = ResReference("foo.bar", "id", name)