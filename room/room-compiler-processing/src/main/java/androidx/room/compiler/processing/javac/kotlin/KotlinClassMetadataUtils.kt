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

package androidx.room.compiler.processing.javac.kotlin

import androidx.room.compiler.processing.XArrayType
import androidx.room.compiler.processing.XEnumTypeElement
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XNullability
import androidx.room.compiler.processing.javac.JavacKmAnnotation
import androidx.room.compiler.processing.javac.JavacKmAnnotationValue
import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.util.sanitizeAsJavaParameterName
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic
import kotlin.metadata.ClassKind
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeParameter
import kotlin.metadata.KmValueParameter
import kotlin.metadata.Visibility
import kotlin.metadata.declaresDefaultValue
import kotlin.metadata.isData
import kotlin.metadata.isDelegated
import kotlin.metadata.isExpect
import kotlin.metadata.isFunInterface
import kotlin.metadata.isNullable
import kotlin.metadata.isSecondary
import kotlin.metadata.isSuspend
import kotlin.metadata.isValue
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.annotations
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.kind
import kotlin.metadata.visibility

internal interface KmData

internal interface KmVisibility : KmData {
    val visibility: Visibility

    fun isInternal() = visibility == Visibility.INTERNAL

    fun isPrivate() = visibility == Visibility.PRIVATE
}

internal interface KmBaseTypeContainer : KmData {
    val upperBounds: List<KmTypeContainer>
    val nullability: XNullability
}

internal class KmClassContainer(private val env: JavacProcessingEnv, private val kmClass: KmClass) :
    KmVisibility {
    override val visibility: Visibility
        get() = kmClass.visibility

    val type: KmTypeContainer by lazy {
        KmTypeContainer(
            kmType = KmType().apply { classifier = KmClassifier.Class(kmClass.name) },
            typeArguments =
                kmClass.typeParameters.map { kmTypeParameter ->
                    KmTypeContainer(
                        kmType =
                            KmType().apply {
                                classifier = KmClassifier.Class(kmTypeParameter.name)
                            },
                        typeArguments = emptyList(),
                        upperBounds = kmTypeParameter.upperBounds.map { it.asContainer() }
                    )
                }
        )
    }

    val superType: KmTypeContainer? by lazy { kmClass.supertypes.firstOrNull()?.asContainer() }

    val superTypes: List<KmTypeContainer> by lazy { kmClass.supertypes.map { it.asContainer() } }

    val typeParameters: List<KmTypeParameterContainer> by lazy {
        kmClass.typeParameters.map { it.asContainer() }
    }

    private val functionList: List<KmFunctionContainer> by lazy {
        kmClass.functions.map { it.asContainer() }
    }

    private val constructorList: List<KmConstructorContainer> by lazy {
        kmClass.constructors.map { it.asContainer(type) }
    }

    private val propertyList: List<KmPropertyContainer> by lazy {
        kmClass.properties.map { it.asContainer() }
    }

    val primaryConstructorSignature: String? by lazy {
        constructorList.firstOrNull { it.isPrimary() }?.descriptor
    }

    fun isObject() = kmClass.kind == ClassKind.OBJECT

    fun isCompanionObject() = kmClass.kind == ClassKind.COMPANION_OBJECT

    fun isAnnotationClass() = kmClass.kind == ClassKind.ANNOTATION_CLASS

    fun isClass() = kmClass.kind == ClassKind.CLASS

    fun isInterface() = kmClass.kind == ClassKind.INTERFACE

    fun isDataClass() = kmClass.isData

    fun isValueClass() = kmClass.isValue

    fun isFunctionalInterface() = kmClass.isFunInterface

    fun isExpect() = kmClass.isExpect

    fun getFunctionMetadata(method: ExecutableElement): KmFunctionContainer? {
        check(method.kind == ElementKind.METHOD) { "must pass an element type of method" }
        return functionByDescriptor[method.descriptor(env.delegate)]
    }

    private val functionByDescriptor: Map<String, KmFunctionContainer> by lazy {
        buildMap {
            functionList.forEach { function -> function.descriptor?.let { put(it, function) } }
            propertyList.forEach { property ->
                property.getter?.descriptor?.let { put(it, property.getter) }
                property.setter?.descriptor?.let { put(it, property.setter) }
                property.syntheticMethodForAnnotations?.descriptor?.let {
                    put(it, property.syntheticMethodForAnnotations)
                }
            }
        }
    }

    fun getConstructorMetadata(method: ExecutableElement): KmConstructorContainer? {
        check(method.kind == ElementKind.CONSTRUCTOR) { "must pass an element type of constructor" }
        val methodSignature = method.descriptor(env.delegate)
        return constructorList.firstOrNull { it.descriptor == methodSignature }
    }

    fun getPropertyMetadata(field: VariableElement): KmPropertyContainer? {
        check(field.kind == ElementKind.FIELD) { "must pass an element type of field" }
        val fieldName = field.simpleName.toString()
        return propertyList.firstOrNull { it.backingFieldName == fieldName || it.name == fieldName }
    }

    companion object {
        /**
         * Creates a [KmClassContainer] for the given element if it contains Kotlin metadata,
         * otherwise this method returns null.
         *
         * Usually the [element] passed must represent a class. For example, if Kotlin metadata is
         * desired for a method, then the containing class should be used as parameter.
         */
        fun createFor(env: JavacProcessingEnv, element: Element): KmClassContainer? {
            val metadataAnnotation = getMetadataAnnotation(element) ?: return null
            return when (val classMetadata = KotlinClassMetadata.readStrict(metadataAnnotation)) {
                is KotlinClassMetadata.Class -> KmClassContainer(env, classMetadata.kmClass)
                // Synthetic classes generated for various Kotlin features ($DefaultImpls,
                // $WhenMappings, etc) are ignored because the data contained does not affect
                // the metadata derived APIs. These classes are never referenced by user code but
                // could be discovered by processors when inspecting inner classes.
                is KotlinClassMetadata.SyntheticClass,
                // Multi file classes are also ignored, the elements contained in these might be
                // referenced by user code in method bodies but not part of the AST, however it
                // is possible for a processor to discover them by inspecting elements under a
                // package.
                is KotlinClassMetadata.FileFacade,
                is KotlinClassMetadata.MultiFileClassFacade,
                is KotlinClassMetadata.MultiFileClassPart -> null
                else -> {
                    env.delegate.messager.printMessage(
                        Diagnostic.Kind.ERROR,
                        "Unable to read Kotlin metadata due to unsupported metadata " +
                            "kind: $classMetadata.",
                        element
                    )
                    null
                }
            }
        }

        /** Search for Kotlin's Metadata annotation across the element's hierarchy. */
        private fun getMetadataAnnotation(element: Element?): Metadata? =
            if (element != null) {
                element.getAnnotation(Metadata::class.java)
                    ?: getMetadataAnnotation(element.enclosingElement)
            } else {
                null
            }
    }
}

internal interface KmFunctionContainer : KmVisibility {
    /** Name of the function in source code */
    val name: String
    /** Name of the function in byte code */
    val jvmName: String
    val descriptor: String?
    val typeParameters: List<KmTypeParameterContainer>
    val parameters: List<KmValueParameterContainer>
    val returnType: KmTypeContainer
    val propertyName: String?
    val isSuspend: Boolean

    fun isPropertySetter() = false

    fun isPropertyGetter() = false

    fun isSyntheticMethodForAnnotations() =
        (this as? KmPropertyFunctionContainerImpl)?.syntheticMethodForAnnotations == true

    fun isPropertyFunction() = this is KmPropertyFunctionContainerImpl

    fun isExtension() =
        (this as? KmFunctionContainerImpl)?.kmFunction?.receiverParameterType != null
}

private class KmFunctionContainerImpl(
    val kmFunction: KmFunction,
    override val returnType: KmTypeContainer,
) : KmFunctionContainer {
    override val visibility: Visibility
        get() = kmFunction.visibility

    override val name: String
        get() = kmFunction.name

    override val propertyName: String? = null
    override val jvmName: String
        get() = kmFunction.signature!!.name

    override val descriptor: String?
        get() {
            // This could be null due to https://youtrack.jetbrains.com/issue/KT-70600
            return kmFunction.signature?.toString()
        }

    override val typeParameters: List<KmTypeParameterContainer>
        get() = kmFunction.typeParameters.map { it.asContainer() }

    override val parameters: List<KmValueParameterContainer>
        get() = kmFunction.valueParameters.map { it.asContainer() }

    override val isSuspend: Boolean
        get() = kmFunction.isSuspend
}

private open class KmPropertyFunctionContainerImpl(
    override val visibility: Visibility,
    override val name: String,
    override val jvmName: String,
    override val descriptor: String,
    override val parameters: List<KmValueParameterContainer>,
    override val returnType: KmTypeContainer,
    override val propertyName: String?,
    val isSetterMethod: Boolean,
    val isGetterMethod: Boolean,
    val syntheticMethodForAnnotations: Boolean = false
) : KmFunctionContainer {
    override val typeParameters: List<KmTypeParameterContainer> = emptyList()
    override val isSuspend: Boolean = false

    override fun isPropertySetter() = isSetterMethod

    override fun isPropertyGetter() = isGetterMethod
}

internal class KmConstructorContainer(
    private val kmConstructor: KmConstructor,
    override val returnType: KmTypeContainer,
) : KmFunctionContainer {
    override val visibility: Visibility
        get() = kmConstructor.visibility

    override val name: String = "<init>"
    override val propertyName: String? = null
    override val jvmName: String = name
    override val descriptor: String
        get() = checkNotNull(kmConstructor.signature).toString()

    override val typeParameters: List<KmTypeParameterContainer> = emptyList()
    override val parameters: List<KmValueParameterContainer> by lazy {
        kmConstructor.valueParameters.map { it.asContainer() }
    }
    override val isSuspend: Boolean
        get() = false

    fun isPrimary() = !kmConstructor.isSecondary
}

internal class KmPropertyContainer(
    private val kmProperty: KmProperty,
    val type: KmTypeContainer,
    val backingFieldName: String?,
    val getter: KmFunctionContainer?,
    val setter: KmFunctionContainer?,
    val syntheticMethodForAnnotations: KmFunctionContainer?,
) : KmVisibility {
    override val visibility: Visibility
        get() = kmProperty.visibility

    val name: String
        get() = kmProperty.name

    val typeParameters: List<KmTypeContainer>
        get() = type.typeArguments

    fun isNullable() = type.isNullable()

    fun isDelegated() = kmProperty.isDelegated
}

internal class KmTypeContainer(
    private val kmType: KmType,
    val typeArguments: List<KmTypeContainer>,
    /** The extends bounds are only non-null for wildcard (i.e. in/out variant) types. */
    val extendsBound: KmTypeContainer? = null,
    /** The upper bounds are only non-empty for type variable types with upper bounds. */
    override val upperBounds: List<KmTypeContainer> = emptyList()
) : KmBaseTypeContainer {
    fun isNullable() = kmType.isNullable

    val className: String? =
        kmType.classifier.let {
            when (it) {
                is KmClassifier.Class -> it.name.replace('/', '.')
                else -> null
            }
        }

    val annotations = kmType.annotations.map { it.asContainer() }

    fun isExtensionType() =
        kmType.annotations.any { it.className == "kotlin/ExtensionFunctionType" }

    fun erasure(): KmTypeContainer =
        KmTypeContainer(
            kmType = kmType,
            typeArguments = emptyList(),
            extendsBound = extendsBound?.erasure(),
            // The erasure of a type variable is equal to the erasure of the first upper bound.
            upperBounds = upperBounds.firstOrNull()?.erasure()?.let { listOf(it) } ?: emptyList(),
        )

    override val nullability: XNullability
        get() = computeTypeNullability(this.isNullable(), this.upperBounds, this.extendsBound)
}

internal class KmAnnotationContainer(private val kmAnnotation: KmAnnotation) {
    val className = kmAnnotation.className.replace('/', '.')

    fun getArguments(env: JavacProcessingEnv): Map<String, KmAnnotationArgumentContainer> {
        return kmAnnotation.arguments.mapValues { (_, arg) -> arg.asContainer(env) }
    }
}

internal class KmAnnotationArgumentContainer(
    private val env: JavacProcessingEnv,
    private val kmAnnotationArgument: KmAnnotationArgument
) {
    fun getValue(method: XMethodElement): Any? {
        return kmAnnotationArgument.let {
            when (it) {
                is KmAnnotationArgument.LiteralValue<*> -> it.value
                is KmAnnotationArgument.ArrayValue -> {
                    it.elements.map {
                        val valueType = (method.returnType as XArrayType).componentType
                        JavacKmAnnotationValue(method, valueType, it.asContainer(env))
                    }
                }
                is KmAnnotationArgument.EnumValue -> {
                    val enumTypeElement =
                        env.findTypeElement(it.enumClassName.replace('/', '.')) as XEnumTypeElement
                    enumTypeElement.entries.associateBy { it.name }[it.enumEntryName]
                }
                is KmAnnotationArgument.AnnotationValue -> {
                    val kmAnnotation =
                        KmAnnotation(it.annotation.className, it.annotation.arguments).asContainer()
                    JavacKmAnnotation(env, kmAnnotation)
                }
                is KmAnnotationArgument.KClassValue -> {
                    env.requireType(it.className.replace('/', '.'))
                }
                is KmAnnotationArgument.ArrayKClassValue -> {
                    val innerType = env.requireType(it.className.replace('/', '.'))
                    var arrayType = env.getArrayType(innerType)
                    repeat(it.arrayDimensionCount - 1) { arrayType = env.getArrayType(arrayType) }
                    arrayType
                }
            }
        }
    }
}

internal class KmTypeParameterContainer(
    private val kmTypeParameter: KmTypeParameter,
    override val upperBounds: List<KmTypeContainer>
) : KmBaseTypeContainer {
    val name: String
        get() = kmTypeParameter.name

    override val nullability: XNullability
        get() = computeTypeNullability(false, this.upperBounds, null)
}

internal class KmValueParameterContainer(
    private val kmValueParameter: KmValueParameter,
    val type: KmTypeContainer
) : KmData {
    val name: String
        get() = kmValueParameter.name

    fun isVarArgs() = kmValueParameter.varargElementType != null

    fun isNullable() = type.isNullable()

    fun hasDefault() = kmValueParameter.declaresDefaultValue
}

private fun computeTypeNullability(
    isNullable: Boolean,
    upperBounds: List<KmTypeContainer>,
    extendsBound: KmTypeContainer?
): XNullability {
    if (isNullable) {
        return XNullability.NULLABLE
    }
    // if there is an upper bound information, use its nullability (e.g. it might be T : Foo?)
    if (upperBounds.isNotEmpty() && upperBounds.all { it.nullability == XNullability.NULLABLE }) {
        return XNullability.NULLABLE
    }
    return extendsBound?.nullability ?: XNullability.NONNULL
}

private fun KmFunction.asContainer(): KmFunctionContainer =
    KmFunctionContainerImpl(kmFunction = this, returnType = this.returnType.asContainer())

private fun KmConstructor.asContainer(returnType: KmTypeContainer): KmConstructorContainer =
    KmConstructorContainer(kmConstructor = this, returnType = returnType)

private fun KmProperty.asContainer(): KmPropertyContainer =
    KmPropertyContainer(
        kmProperty = this,
        type = this.returnType.asContainer(),
        backingFieldName = fieldSignature?.name,
        getter =
            getterSignature?.let {
                KmPropertyFunctionContainerImpl(
                    visibility = this.visibility,
                    name = JvmAbi.computeGetterName(this.name),
                    jvmName = it.name,
                    descriptor = it.toString(),
                    parameters = emptyList(),
                    returnType = this.returnType.asContainer(),
                    propertyName = this.name,
                    isSetterMethod = false,
                    isGetterMethod = true,
                )
            },
        setter =
            setterSignature?.let {
                // setter parameter visitor may not be available when not declared explicitly
                val param =
                    this.setterParameter
                        ?: KmValueParameter(
                                // kotlinc will set this to set-? but it is better to not expose
                                // it here since it is not valid name
                                name = "set-?".sanitizeAsJavaParameterName(0)
                            )
                            .apply { type = this@asContainer.returnType }
                val returnType = KmType().apply { classifier = KmClassifier.Class("Unit") }
                KmPropertyFunctionContainerImpl(
                    visibility = this.visibility,
                    name = JvmAbi.computeSetterName(this.name),
                    jvmName = it.name,
                    descriptor = it.toString(),
                    parameters = listOf(param.asContainer()),
                    returnType = returnType.asContainer(),
                    propertyName = this.name,
                    isSetterMethod = true,
                    isGetterMethod = false,
                )
            },
        syntheticMethodForAnnotations =
            syntheticMethodForAnnotations?.let {
                val returnType = KmType().apply { classifier = KmClassifier.Class("Unit") }
                KmPropertyFunctionContainerImpl(
                    visibility = this.visibility,
                    name = JvmAbi.computeSyntheticMethodForAnnotationsName(this.name),
                    jvmName = it.name,
                    descriptor = it.toString(),
                    parameters = emptyList(),
                    returnType = returnType.asContainer(),
                    syntheticMethodForAnnotations = true,
                    propertyName = this.name,
                    isSetterMethod = false,
                    isGetterMethod = false,
                )
            },
    )

private fun KmType.asContainer(): KmTypeContainer =
    KmTypeContainer(
        kmType = this,
        typeArguments = this.arguments.mapNotNull { it.type?.asContainer() }
    )

private fun KmTypeParameter.asContainer(): KmTypeParameterContainer =
    KmTypeParameterContainer(
        kmTypeParameter = this,
        upperBounds = this.upperBounds.map { it.asContainer() }
    )

private fun KmValueParameter.asContainer(): KmValueParameterContainer =
    KmValueParameterContainer(kmValueParameter = this, type = this.type.asContainer())

private fun KmAnnotation.asContainer() = KmAnnotationContainer(this)

private fun KmAnnotationArgument.asContainer(env: JavacProcessingEnv) =
    KmAnnotationArgumentContainer(env, this)
