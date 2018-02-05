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

import android.arch.navigation.safe.args.generator.ext.S
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.TypeName

sealed class NavType {
    companion object {
        fun parse(name: String): NavType? = when (name) {
            "integer" -> IntegerType
            "string" -> StringType
            else -> null
        }
    }

    abstract fun typeName(): TypeName

    abstract fun verify(value: String): Boolean

    abstract fun write(value: String): CodeBlock

    abstract fun bundlePutMethod(): String
}

object IntegerType : NavType() {
    override fun bundlePutMethod() = "putInt"

    override fun write(value: String): CodeBlock = CodeBlock.of(value)

    override fun typeName(): TypeName = TypeName.INT

    override fun verify(value: String): Boolean {
        try {
            Integer.parseInt(value)
            return true
        } catch (ex: NumberFormatException) {
            return false
        }
    }
}

object StringType : NavType() {

    override fun bundlePutMethod() = "putString"

    override fun typeName(): TypeName = ClassName.get(String::class.java)

    override fun verify(value: String) = true

    override fun write(value: String) = CodeBlock.of(S, value)
}