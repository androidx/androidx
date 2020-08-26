/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XArrayType
import androidx.room.compiler.processing.XDeclaredType
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XType
import androidx.room.compiler.processing.XTypeElement
import org.jetbrains.kotlin.ksp.processing.CodeGenerator
import org.jetbrains.kotlin.ksp.processing.KSPLogger
import org.jetbrains.kotlin.ksp.processing.Resolver
import javax.annotation.processing.Filer

internal class KspProcessingEnv(
    override val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    val resolver: Resolver
) : XProcessingEnv {
    override val messager: XMessager
        get() = TODO("Not yet implemented")
    override val filer: Filer
        get() = TODO("Not yet implemented")

    override fun findTypeElement(qName: String): XTypeElement? {
        TODO("Not yet implemented")
    }

    override fun findType(qName: String): XType? {
        TODO("Not yet implemented")
    }

    override fun findGeneratedAnnotation(): XTypeElement? {
        TODO("Not yet implemented")
    }

    override fun getDeclaredType(type: XTypeElement, vararg types: XType): XDeclaredType {
        TODO("Not yet implemented")
    }

    override fun getArrayType(type: XType): XArrayType {
        TODO("Not yet implemented")
    }
}
