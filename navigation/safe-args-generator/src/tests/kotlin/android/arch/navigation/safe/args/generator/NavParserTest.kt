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

package android.arch.navigation.safe.args.generator

import android.arch.navigation.safe.args.generator.models.Action
import android.arch.navigation.safe.args.generator.models.Argument
import android.arch.navigation.safe.args.generator.models.Destination
import android.arch.navigation.safe.args.generator.models.Id
import android.arch.navigation.safe.args.generator.models.Type
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File

@RunWith(JUnit4::class)
class NavParserTest {

    @Test
    fun test() {
        val id: (String) -> Id = { id -> Id("a.b", id) }
        val navGraph = parseNavigationFile(File("src/tests/test-data/naive_test.xml"), "a.b")

        val nameFirst = "android.arch.navigation.testapp.MainFragment"
        val nameNext = "android.arch.navigation.testapp.NextFragment"

        val expectedFirst = Destination("fragment", nameFirst,
                listOf(Argument("myarg1", Type.STRING, "one")),
                listOf(Action(id("next"), id("next_fragment"), listOf(
                        Argument("myarg2", Type.STRING),
                        Argument("randomArgument", Type.STRING)))))

        val expectedNext = Destination("fragment", nameNext,
                listOf(Argument("myarg2", Type.STRING)),
                listOf(Action(id("next"), id("first_screen"))))

        val expectedGraph = Destination("navigation", "", emptyList(), emptyList(),
                listOf(expectedFirst, expectedNext))
        assertThat(navGraph, `is`(expectedGraph))
    }

    @Test
    fun testIdParsing() {
        assertThat(parseId("@+id/next", "a.b"), `is`(Id("a.b", "next")))
        assertThat(parseId("@id/next", "a.b"), `is`(Id("a.b", "next")))
        assertThat(parseId("@android:id/text", "a.b"), `is`(Id("android", "text")))
        assertThat(parseId("@android:id/text", "a.b"), `is`(Id("android", "text")))
        assertThat(parseId("@not.android:id/text", "a.b"), `is`(Id("not.android", "text")))
    }
}