/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build

import java.io.File
import java.io.IOException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * JUnit 4 [TestRule]s are traditionally added to a test class as public JVM fields
 * with a @[org.junit.Rule] annotation.  This works decently in Java, but has drawbacks,
 * such as requiring all methods in a test class to be subject to the same [TestRule]s, and
 * making it difficult to configure [TestRule]s in different ways between test methods.
 * With lambdas, objects that have been built as [TestRule] can use this extension function
 * to allow per-method custom application.
 */
fun <T : TestRule> T.wrap(fn: (T) -> Unit) = apply(object : Statement() {
    override fun evaluate() = fn(this@wrap)
}, Description.EMPTY).evaluate()

fun File.filesInFolder() = list()!!.toList()

fun File.assertExists(): File {
    if (exists()) {
        return this
    }
    var youngestMissingSoFar = this
    while (!youngestMissingSoFar.parentFile.exists()) {
        youngestMissingSoFar = youngestMissingSoFar.parentFile
    }
    val missing = youngestMissingSoFar
    val actualFiles = missing.parentFile.filesInFolder()
    throw IOException(
        "Could not read $this.  ${missing.parent} has $actualFiles but not ${missing.name}"
    )
}