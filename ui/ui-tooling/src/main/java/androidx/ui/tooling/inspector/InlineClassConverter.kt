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

package androidx.ui.tooling.inspector

import androidx.compose.Composer
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import kotlinx.metadata.KmClassifier
import kotlinx.metadata.KmFunction
import kotlinx.metadata.KmType
import kotlinx.metadata.KmValueParameter
import kotlinx.metadata.flagsOf
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import java.lang.Exception
import java.lang.reflect.Method

/**
 * Converter for casting a parameter represented by its primitive value to its inline class type.
 *
 * For example: an androidx.ui.graphics.Color instance is often represented by a long
 */
internal class InlineClassConverter {
    // Map from qualified function name to parameter name to conversion lambda
    private val functionParameterMap = mutableMapOf<String, Map<String, (Any) -> Any>>()
    // Map from inline type name to inline class and conversion lambda
    private val typeMap = mutableMapOf<String, Pair<Class<*>?, (Any) -> Any>>()
    // A noop conversion lambda
    private val identity: (Any) -> Any = { it }
    // Return value used in functions
    private val notInlineType = Pair(null as Class<*>?, identity)

    /**
     * Clear any cached data.
     */
    fun clear() {
        functionParameterMap.clear()
        typeMap.clear()
    }

    /**
     * Cast the specified [value] into the original inline type if applicable.
     *
     * @param functionName fully qualified name including the inline part
     *                     example: androidx.compose.foundation.TextKt.Text-g1O9ZpA
     * @param parameterName name of the parameter which value to cast example: color
     * @param value the value from the group which typically is the primitive representation
     *              of the parameter type example: Long
     */
    fun castParameterValue(functionName: String, parameterName: String, value: Any?): Any? {
        if (value == null) {
            return null
        }
        val parameterMap = functionParameterMap[functionName] ?: loadParameterMap(functionName)
        return parameterMap[parameterName]?.invoke(value) ?: value
    }

    private fun loadParameterMap(qualifiedFunctionName: String): Map<String, (Any) -> Any> {
        val className = qualifiedFunctionName.substringBeforeLast(".")
        val javaClass = loadClassOrNull(className) ?: return emptyMap()
        val facade = loadKotlinMetadata(javaClass) as? KotlinClassMetadata.FileFacade
        val kmPackage = facade?.toKmPackage() ?: return emptyMap()
        for (function in kmPackage.functions) {
            val method = findMatchingMethod(javaClass, function)
            if (method != null) {
                val map = mutableMapOf<String, (Any) -> Any>()
                for (parameter in function.valueParameters) {
                    val typeName = parameter.type?.name ?: continue
                    if (!typeName.startsWith("kotlin/")) {
                        val (_, mapper) = typeMap[typeName] ?: continue
                        if (mapper != identity) {
                            map[parameter.name] = mapper
                        }
                    }
                }
                functionParameterMap["$className.${method.name}"] = map
            }
        }
        return functionParameterMap[qualifiedFunctionName] ?: emptyMap()
    }

    private fun loadKotlinMetadata(javaClass: Class<*>): KotlinClassMetadata? {
        val metadata = javaClass.annotations.firstOrNull { it is Metadata }
            as? Metadata ?: return null
        val header = KotlinClassHeader(
            metadata.kind,
            metadata.metadataVersion,
            metadata.bytecodeVersion,
            metadata.data1,
            metadata.data2,
            metadata.extraString,
            metadata.packageName,
            metadata.extraInt
        )
        return KotlinClassMetadata.read(header)
    }

    private fun findMatchingMethod(javaClass: Class<*>, function: KmFunction): Method? {
        val methods = javaClass.declaredMethods.filter {
            it.name.substringBefore('-', "<no-match>") == function.name
        }
        val functionParams = function.valueParameters
        for (method in methods) {
            val methodParams = method.parameterTypes
            if (methodParams.size < functionParams.size ||
                !functionParameterTypesMatch(methodParams, functionParams)) {
                continue
            }
            // The compiler can add extra parameters after the Composer parameter. Allow that:
            if (methodParams.size > functionParams.size) {
                if (methodParams[functionParams.size].name != Composer::class.java.name) {
                    continue
                }
            }
            return method
        }
        return null
    }

    private fun functionParameterTypesMatch(
        methodParams: Array<Class<*>>,
        functionParams: List<KmValueParameter>
    ): Boolean {
        for (index in functionParams.indices) {
            if (!typeMatch(methodParams[index], functionParams[index])) {
                return false
            }
        }
        return true
    }

    private fun typeMatch(javaClass: Class<*>, parameter: KmValueParameter): Boolean {
        val typeName = parameter.type?.name ?: return false
        if (toJavaRuntimeTypeName(typeName) == javaClass.name) {
            return true
        }
        if (typeName.startsWith("kotlin/Function") &&
            javaClass.name.startsWith("kotlin.jvm.functions.Function")
        ) {
            return true
        }
        return javaClass == typeMap.getOrPut(typeName) { loadTypeMapper(typeName) }.first
    }

    /**
     * Map a type name found in the metadata API to the jvm runtime type.
     *
     * Note: several of these input types do not exist at runtime.
     */
    private fun toJavaRuntimeTypeName(typeName: String): String {
        if (!typeName.startsWith("kotlin/")) {
            return typeName.replace('/', '.')
        }
        return when (typeName) {
            "kotlin/Boolean" -> java.lang.Boolean.TYPE.name
            "kotlin/Int",
            "kotlin/UInt" -> Integer.TYPE.name
            "kotlin/Long",
            "kotlin/ULong" -> java.lang.Long.TYPE.name
            "kotlin/Double" -> java.lang.Double.TYPE.name
            "kotlin/Float" -> java.lang.Float.TYPE.name
            "kotlin/String" -> java.lang.String::class.java.name
            "kotlin/collections/Collection" -> java.util.Collection::class.java.name
            "kotlin/collections/List" -> java.util.List::class.java.name
            "kotlin/collections/Map" -> java.util.Map::class.java.name
            else -> java.lang.Void::class.java.name
        }
    }

    private fun loadTypeMapper(className: String): Pair<Class<*>?, (Any) -> Any> {
        val javaClass = loadClassOrNull(className) ?: return notInlineType
        val create = javaClass.declaredConstructors.singleOrNull() ?: return notInlineType
        val parameterClass = create.parameterTypes.singleOrNull() ?: return notInlineType
        val facade = loadKotlinMetadata(javaClass) as? KotlinClassMetadata.Class
        val kmClass = facade?.toKmClass() ?: return notInlineType
        if (!kmClass.isInline) {
            return notInlineType
        }
        create.isAccessible = true
        return Pair(parameterClass, { value -> create.newInstance(value) })
    }

    private fun loadClassOrNull(className: String): Class<*>? =
        try {
            javaClass.classLoader!!.loadClass(className)
        } catch (ex: Exception) {
            null
        }

    private val KmClass.isInline: Boolean
        get() = flags and flagsOf(Flag.Class.IS_INLINE) != 0

    private val KmType.name: String?
        get() = (classifier as? KmClassifier.Class)?.name
}
