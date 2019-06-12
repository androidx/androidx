/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.lifecycle.utils

import androidx.lifecycle.LifecycleProcessor
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubject
import java.io.File
import java.nio.charset.Charset
import javax.tools.JavaFileObject

fun load(fullClassName: String, folder: String): JavaFileObject {
    val folderPath = "src/tests/test-data/${if (folder.isEmpty()) "" else folder + "/"}"
    val split = fullClassName.split(".")
    val code = File("$folderPath/${split.last()}.java").readText(Charset.defaultCharset())
    return JavaFileObjects.forSourceString(fullClassName, code)
}

fun processClass(className: String, vararg fullClassNames: String): CompileTester {
    val javaFiles = fullClassNames.map { load(it, "") }.toTypedArray()
    val processedWith = JavaSourcesSubject.assertThat(load(className, ""), *javaFiles)
            .processedWith(LifecycleProcessor())
    return checkNotNull(processedWith)
}
