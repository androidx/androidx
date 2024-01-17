/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.safe.args.generator.java

import androidx.navigation.safe.args.generator.CodeFile
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import java.io.File

data class JavaCodeFile(internal val wrapped: JavaFile) : CodeFile {
    override fun writeTo(directory: File) {
        wrapped.writeTo(directory)
    }

    override fun fileName() = "${wrapped.packageName}.${wrapped.typeSpec.name}"

    fun toClassName() = ClassName.get(wrapped.packageName, wrapped.typeSpec.name)

    override fun toString() = wrapped.toString()
}

fun JavaFile.toCodeFile() = JavaCodeFile(this)
