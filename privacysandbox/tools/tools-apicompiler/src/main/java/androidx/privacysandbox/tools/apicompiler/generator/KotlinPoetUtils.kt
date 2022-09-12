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

package androidx.privacysandbox.tools.apicompiler.generator

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Type
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.OutputStream

/** KotlinPoet's [ClassName] for this class. */
internal fun AnnotatedInterface.specClassName() = ClassName(packageName, name)

/** KotlinPoet's [ClassName] for this type. */
internal fun Type.specClassName(): ClassName {
    val parts = name.split('.')
    val packageName = parts.dropLast(1).joinToString(".")
    val className = parts.last()
    return ClassName(packageName, className)
}

/** Convenience method to write [FileSpec]s to KSP-generated [OutputStream]s. */
internal fun OutputStream.write(spec: FileSpec) = bufferedWriter().use(spec::writeTo)

internal fun TypeSpec.Builder.primaryConstructor(vararg properties: PropertySpec):
    TypeSpec.Builder {
    val propertiesWithInitializer =
        properties.map {
            it.toBuilder().initializer(it.name)
                .build()
        }
    primaryConstructor(
        FunSpec.constructorBuilder()
            .addParameters(propertiesWithInitializer.map { ParameterSpec(it.name, it.type) })
            .addModifiers(KModifier.INTERNAL)
            .build()
    )
    addProperties(propertiesWithInitializer)
    return this
}
