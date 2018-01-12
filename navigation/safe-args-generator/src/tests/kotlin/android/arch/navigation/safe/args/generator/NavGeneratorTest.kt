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

package android.arch.navigation.safe.args.generator

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

    @Suppress("MemberVisibilityCanPrivate")
    @get:Rule
    val workingDir = TemporaryFolder()
    @Test
    fun test() {
        generateSafeArgs("foo", File("src/tests/test-data/naive_test.xml"), workingDir.root)
        assertThat(File(workingDir.root, "foo/MainFragmentDirections.java").exists(), `is`(true))
        assertThat(File(workingDir.root, "foo/NextFragmentDirections.java").exists(), `is`(true))
    }
}