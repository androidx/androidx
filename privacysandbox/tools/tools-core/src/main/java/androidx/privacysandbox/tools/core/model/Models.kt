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

package androidx.privacysandbox.tools.core.model

fun ParsedApi.getOnlyService(): AnnotatedInterface {
    check(services.size == 1) {
        "Expected to find one annotated service, but found ${services.size}."
    }
    return services.first()
}

fun ParsedApi.hasSuspendFunctions(): Boolean {
    val annotatedInterfaces = services + interfaces + callbacks
    return annotatedInterfaces.flatMap(AnnotatedInterface::methods).any(Method::isSuspend)
}

fun ParsedApi.containsSdkActivityLauncher(): Boolean {
    return values.any { it.containsSdkActivityLauncher() } ||
        interfaces.any { it.containsSdkActivityLauncher() } ||
        callbacks.any { it.containsSdkActivityLauncher() } ||
        services.any { it.containsSdkActivityLauncher() }
}

private fun AnnotatedInterface.containsSdkActivityLauncher(): Boolean {
    val isInReturns =
        methods.any { it.returnType.qualifiedName == Types.sdkActivityLauncher.qualifiedName }
    val isInParams =
        methods
            .flatMap { it.parameters }
            .any { it.type.qualifiedName == Types.sdkActivityLauncher.qualifiedName }

    return isInReturns || isInParams
}

private fun AnnotatedValue.containsSdkActivityLauncher(): Boolean =
    when (this) {
        is AnnotatedEnumClass -> false
        is AnnotatedDataClass ->
            properties.any { it.type.qualifiedName == Types.sdkActivityLauncher.qualifiedName }
    }

object Types {
    val unit = Type(packageName = "kotlin", simpleName = "Unit")
    val boolean = Type(packageName = "kotlin", simpleName = "Boolean")
    val int = Type(packageName = "kotlin", simpleName = "Int")
    val long = Type(packageName = "kotlin", simpleName = "Long")
    val float = Type(packageName = "kotlin", simpleName = "Float")
    val double = Type(packageName = "kotlin", simpleName = "Double")
    val string = Type(packageName = "kotlin", simpleName = "String")
    val char = Type(packageName = "kotlin", simpleName = "Char")
    val short = Type(packageName = "kotlin", simpleName = "Short")
    val byte = Type(packageName = "kotlin", simpleName = "Byte")
    val primitiveTypes = setOf(unit, boolean, int, long, float, double, string, char, short, byte)

    val any = Type("kotlin", simpleName = "Any")
    val bundle = Type("android.os", "Bundle")
    val sandboxedUiAdapter =
        Type(packageName = "androidx.privacysandbox.ui.core", simpleName = "SandboxedUiAdapter")
    val sdkActivityLauncher =
        Type(
            packageName = "androidx.privacysandbox.activity.core",
            simpleName = "SdkActivityLauncher"
        )

    fun list(elementType: Type) =
        Type(
            packageName = "kotlin.collections",
            simpleName = "List",
            typeParameters = listOf(elementType)
        )

    fun Type.asNullable(): Type {
        if (isNullable) return this
        return copy(isNullable = true)
    }

    fun Type.asNonNull(): Type {
        if (isNullable) return copy(isNullable = false)
        return this
    }
}
