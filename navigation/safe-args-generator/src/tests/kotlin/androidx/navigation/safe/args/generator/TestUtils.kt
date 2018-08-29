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

import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubject
import com.squareup.javapoet.JavaFile
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.charset.Charset
import javax.tools.JavaFileObject

fun JavaFile.toJavaFileObject(workingDir: TemporaryFolder): JavaFileObject {
    val destination = workingDir.newFolder()
    this.writeTo(destination)
    val path = this.packageName.replace('.', '/')
    val generated = File(destination, "$path/${this.typeSpec.name}.java")
    MatcherAssert.assertThat(generated.exists(), CoreMatchers.`is`(true))
    return JavaFileObjects.forResource(generated.toURI().toURL())
}

fun JavaSourcesSubject.parsesAs(fullClassName: String, folder: String = "expected") =
        this.parsesAs(loadSourceFile(fullClassName, folder))

fun loadSourceFile(fullClassName: String, folder: String): JavaFileObject {
    val folderPath = "src/tests/test-data/${if (folder.isEmpty()) "" else "$folder/"}"
    val split = fullClassName.split(".")
    val code = File("$folderPath/${split.last()}.java").readText(Charset.defaultCharset())
    return JavaFileObjects.forSourceString(fullClassName, code)
}