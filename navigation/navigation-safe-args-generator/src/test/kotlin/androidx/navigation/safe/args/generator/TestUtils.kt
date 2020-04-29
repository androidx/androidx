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

import androidx.navigation.safe.args.generator.java.JavaCodeFile
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubject
import java.io.File
import java.nio.charset.Charset

fun JavaCodeFile.toJavaFileObject() = this.wrapped.toJavaFileObject()

fun JavaSourcesSubject.parsesAs(fullClassName: String, folder: String = "expected") =
        this.parsesAs(loadSourceFileObject(fullClassName, folder))

fun loadSourceString(fullClassName: String, folder: String, fileExtension: String): String {
    val folderPath = "src/test/test-data/${if (folder.isEmpty()) "" else "$folder/"}"
    val split = fullClassName.split(".")
    return File("$folderPath/${split.last()}.$fileExtension").readText(Charset.defaultCharset())
}

fun loadSourceFileObject(fullClassName: String, folder: String) =
    JavaFileObjects.forSourceString(fullClassName, loadSourceString(fullClassName, folder, "java"))
