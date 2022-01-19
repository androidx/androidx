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

import androidx.room.compiler.processing.util.sanitizeAsJavaParameterName
import kotlinx.metadata.ClassName
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmClassVisitor
import kotlinx.metadata.KmConstructorExtensionVisitor
import kotlinx.metadata.KmConstructorVisitor
import kotlinx.metadata.KmExtensionType
import kotlinx.metadata.KmFunctionExtensionVisitor
import kotlinx.metadata.KmFunctionVisitor
import kotlinx.metadata.KmPropertyExtensionVisitor
import kotlinx.metadata.KmPropertyVisitor
import kotlinx.metadata.KmTypeExtensionVisitor
import kotlinx.metadata.KmTypeParameterVisitor
import kotlinx.metadata.KmTypeVisitor
import kotlinx.metadata.KmValueParameterVisitor
import kotlinx.metadata.KmVariance
import kotlinx.metadata.jvm.JvmConstructorExtensionVisitor
import kotlinx.metadata.jvm.JvmFieldSignature
import kotlinx.metadata.jvm.JvmFunctionExtensionVisitor
import kotlinx.metadata.jvm.JvmMethodSignature
import kotlinx.metadata.jvm.JvmPropertyExtensionVisitor
import kotlinx.metadata.jvm.JvmTypeExtensionVisitor
import kotlinx.metadata.jvm.KotlinClassMetadata

// represents a function or constructor
internal interface KmExecutable {
    val parameters: List<KmValueParameter>
}

/**
 * Represents the kotlin metadata of a function
 */
internal data class KmFunction(
    /**
     * Name of the function in byte code
     */
    val jvmName: String,
    /**
     * Name of the function in source code
     */
    val name: String,
    val descriptor: String,
    private val flags: Flags,
    override val parameters: List<KmValueParameter>,
    val returnType: KmType,
    val receiverType: KmType?
) : KmExecutable {
    fun isSuspend() = Flag.Function.IS_SUSPEND(flags)
    fun isExtension() = receiverType != null
}

/**
 * Represents the kotlin metadata of a constructor
 */
internal data class KmConstructor(
    val descriptor: String,
    private val flags: Flags,
    override val parameters: List<KmValueParameter>
) : KmExecutable {
    fun isPrimary() = !Flag.Constructor.IS_SECONDARY(flags)
}

internal data class KmProperty(
    val name: String,
    val type: KmType,
    val getter: KmFunction?,
    val setter: KmFunction?
) {
    val typeParameters
        get() = type.typeArguments

    fun isNullable() = Flag.Type.IS_NULLABLE(type.flags)
}

internal data class KmType(
    val flags: Flags,
    val typeArguments: List<KmType>,
    val extendsBound: KmType?,
    val isExtensionType: Boolean
) {
    fun isNullable() = Flag.Type.IS_NULLABLE(flags)
    fun erasure(): KmType = KmType(flags, emptyList(), extendsBound?.erasure(), isExtensionType)
}

private data class KmTypeParameter(
    val name: String,
    val flags: Flags,
    val extendsBound: KmType?
) {
    fun asKmType() = KmType(
        flags = flags,
        typeArguments = emptyList(),
        extendsBound = extendsBound,
        isExtensionType = false
    )
}

/**
 * Represents the kotlin metadata of a parameter
 */
internal data class KmValueParameter(
    val name: String,
    val type: KmType,
    private val flags: Int
) {
    fun isNullable() = type.isNullable()
    fun hasDefault() = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(flags)
}

internal data class KmClassTypeInfo(
    val kmType: KmType,
    val superType: KmType?
)

internal fun KotlinClassMetadata.Class.readFunctions(): List<KmFunction> =
    mutableListOf<KmFunction>().apply { accept(FunctionReader(this)) }

private class FunctionReader(val result: MutableList<KmFunction>) : KmClassVisitor() {
    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor {
        return object : KmFunctionVisitor() {

            lateinit var methodSignature: JvmMethodSignature
            val parameters = mutableListOf<KmValueParameter>()
            lateinit var returnType: KmType
            var receiverType: KmType? = null

            override fun visitValueParameter(
                flags: Flags,
                name: String
            ): KmValueParameterVisitor {
                return ValueParameterReader(name, flags) {
                    parameters.add(it)
                }
            }

            override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? {
                return TypeReader(flags) {
                    receiverType = it
                }
            }

            override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor {
                if (type != JvmFunctionExtensionVisitor.TYPE) {
                    error("Unsupported extension type: $type")
                }
                return object : JvmFunctionExtensionVisitor() {
                    override fun visit(signature: JvmMethodSignature?) {
                        methodSignature = signature!!
                    }
                }
            }

            override fun visitReturnType(flags: Flags): KmTypeVisitor {
                return TypeReader(flags) {
                    returnType = it
                }
            }

            override fun visitEnd() {
                result.add(
                    KmFunction(
                        name = name,
                        jvmName = methodSignature.name,
                        descriptor = methodSignature.asString(),
                        flags = flags,
                        parameters = parameters,
                        returnType = returnType,
                        receiverType = receiverType
                    )
                )
            }
        }
    }
}

internal fun KotlinClassMetadata.Class.readConstructors(): List<KmConstructor> =
    mutableListOf<KmConstructor>().apply { accept(ConstructorReader(this)) }

private class ConstructorReader(val result: MutableList<KmConstructor>) : KmClassVisitor() {
    override fun visitConstructor(flags: Flags): KmConstructorVisitor {
        return object : KmConstructorVisitor() {

            lateinit var descriptor: String
            val parameters = mutableListOf<KmValueParameter>()

            override fun visitValueParameter(
                flags: Flags,
                name: String
            ): KmValueParameterVisitor {
                return ValueParameterReader(name, flags) {
                    parameters.add(it)
                }
            }

            override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor {
                if (type != JvmConstructorExtensionVisitor.TYPE) {
                    error("Unsupported extension type: $type")
                }
                return object : JvmConstructorExtensionVisitor() {
                    override fun visit(signature: JvmMethodSignature?) {
                        descriptor = signature!!.asString()
                    }
                }
            }

            override fun visitEnd() {
                result.add(KmConstructor(descriptor, flags, parameters))
            }
        }
    }
}

internal class KotlinMetadataClassFlags(val classMetadata: KotlinClassMetadata.Class) {

    private val flags: Flags by lazy {
        var theFlags: Flags = 0
        classMetadata.accept(object : KmClassVisitor() {
            override fun visit(flags: Flags, name: ClassName) {
                theFlags = flags
                super.visit(flags, name)
            }
        })
        return@lazy theFlags
    }

    fun isObject(): Boolean = Flag.Class.IS_OBJECT(flags)

    fun isCompanionObject(): Boolean = Flag.Class.IS_COMPANION_OBJECT(flags)

    fun isAnnotationClass(): Boolean = Flag.Class.IS_ANNOTATION_CLASS(flags)

    fun isInterface(): Boolean = Flag.Class.IS_INTERFACE(flags)

    fun isClass(): Boolean = Flag.Class.IS_CLASS(flags)

    fun isDataClass(): Boolean = Flag.Class.IS_DATA(flags)

    fun isValueClass(): Boolean = Flag.Class.IS_VALUE(flags)

    fun isFunctionalInterface(): Boolean = Flag.Class.IS_FUN(flags)

    fun isExpect(): Boolean = Flag.Class.IS_EXPECT(flags)
}

internal fun KotlinClassMetadata.Class.readProperties(): List<KmProperty> =
    mutableListOf<KmProperty>().apply { accept(PropertyReader(this)) }

/**
 * Reads the properties of a class declaration
 */
private class PropertyReader(
    val result: MutableList<KmProperty>
) : KmClassVisitor() {
    override fun visitProperty(
        flags: Flags,
        name: String,
        getterFlags: Flags,
        setterFlags: Flags
    ): KmPropertyVisitor {
        var setterParam: KmValueParameter? = null
        var getter: JvmMethodSignature? = null
        var setter: JvmMethodSignature? = null
        return object : KmPropertyVisitor() {
            lateinit var returnType: KmType
            override fun visitEnd() {
                result.add(
                    KmProperty(
                        type = returnType,
                        name = name,
                        setter = setter?.let { setterSignature ->
                            // setter parameter visitor may not be invoked when not declared
                            // explicitly
                            val param = setterParam ?: KmValueParameter(
                                // kotlinc will set this to set-? but it is better to not expose
                                // it here since it is not valid name
                                name = "set-?".sanitizeAsJavaParameterName(0),
                                type = returnType,
                                flags = 0
                            )
                            KmFunction(
                                jvmName = setterSignature.name,
                                name = JvmAbi.computeSetterName(name),
                                descriptor = setterSignature.asString(),
                                flags = 0,
                                parameters = listOf(param),
                                returnType = KM_VOID_TYPE,
                                receiverType = null
                            )
                        },
                        getter = getter?.let { getterSignature ->
                            KmFunction(
                                jvmName = getterSignature.name,
                                name = JvmAbi.computeGetterName(name),
                                descriptor = getterSignature.asString(),
                                flags = flags,
                                parameters = emptyList(),
                                returnType = returnType,
                                receiverType = null
                            )
                        }
                    )
                )
            }

            override fun visitReturnType(flags: Flags): KmTypeVisitor {
                return TypeReader(flags) {
                    returnType = it
                }
            }

            override fun visitSetterParameter(
                flags: Flags,
                name: String
            ): KmValueParameterVisitor {
                return ValueParameterReader(
                    name = name,
                    flags = flags
                ) {
                    setterParam = it
                }
            }

            override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? {
                if (type != JvmPropertyExtensionVisitor.TYPE) {
                    return null
                }
                return object : JvmPropertyExtensionVisitor() {
                    override fun visit(
                        jvmFlags: Flags,
                        fieldSignature: JvmFieldSignature?,
                        getterSignature: JvmMethodSignature?,
                        setterSignature: JvmMethodSignature?
                    ) {
                        getter = getterSignature
                        setter = setterSignature
                    }
                }
            }
        }
    }
}

/**
 * Reads a type description and calls the output with the read value
 */
private class TypeReader(
    private val flags: Flags,
    private val output: (KmType) -> Unit
) : KmTypeVisitor() {
    private val typeArguments = mutableListOf<KmType>()
    private var extendsBound: KmType? = null
    private var isExtensionType = false
    override fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor {
        return TypeReader(flags) {
            typeArguments.add(it)
        }
    }

    override fun visitFlexibleTypeUpperBound(
        flags: Flags,
        typeFlexibilityId: String?
    ): KmTypeVisitor {
        return TypeReader(flags) {
            extendsBound = it
        }
    }

    override fun visitExtensions(type: KmExtensionType): KmTypeExtensionVisitor? {
        if (type != JvmTypeExtensionVisitor.TYPE) return null
        return object : JvmTypeExtensionVisitor() {
            override fun visitAnnotation(annotation: KmAnnotation) {
                if (annotation.className == "kotlin/ExtensionFunctionType") {
                    isExtensionType = true
                }
            }
        }
    }

    override fun visitEnd() {
        output(
            KmType(
                flags = flags,
                typeArguments = typeArguments,
                extendsBound = extendsBound,
                isExtensionType = isExtensionType
            )
        )
    }
}

/**
 * Reads the value parameter of a function or constructor and calls the output with the read value
 */
private class ValueParameterReader(
    val name: String,
    val flags: Flags,
    val output: (KmValueParameter) -> Unit
) : KmValueParameterVisitor() {
    lateinit var type: KmType
    override fun visitType(flags: Flags): KmTypeVisitor {
        return TypeReader(flags) {
            type = it
        }
    }

    override fun visitEnd() {
        output(
            KmValueParameter(
                name = name,
                type = type,
                flags = flags
            )
        )
    }
}

/**
 * Reads a class declaration and turns it into a KmType for both itself and its super type
 */
internal class ClassAsKmTypeReader(
    val output: (KmClassTypeInfo) -> Unit
) : KmClassVisitor() {
    private var flags: Flags = 0
    private val typeParameters = mutableListOf<KmTypeParameter>()
    private var superType: KmType? = null
    override fun visit(flags: Flags, name: ClassName) {
        this.flags = flags
    }

    override fun visitTypeParameter(
        flags: Flags,
        name: String,
        id: Int,
        variance: KmVariance
    ): KmTypeParameterVisitor {
        return TypeParameterReader(name, flags) {
            typeParameters.add(it)
        }
    }

    override fun visitSupertype(flags: Flags): KmTypeVisitor {
        return TypeReader(flags) {
            superType = it
        }
    }

    override fun visitEnd() {
        output(
            KmClassTypeInfo(
                kmType = KmType(
                    flags = flags,
                    typeArguments = typeParameters.map {
                        it.asKmType()
                    },
                    extendsBound = null,
                    isExtensionType = false
                ),
                superType = superType
            )
        )
    }
}

private class TypeParameterReader(
    private val name: String,
    private val flags: Flags,
    private val output: (KmTypeParameter) -> Unit
) : KmTypeParameterVisitor() {
    private var upperBound: KmType? = null
    override fun visitEnd() {
        output(
            KmTypeParameter(
                name = name,
                flags = flags,
                extendsBound = upperBound
            )
        )
    }

    override fun visitUpperBound(flags: Flags): KmTypeVisitor {
        return TypeReader(flags) {
            upperBound = it
        }
    }
}

private val KM_VOID_TYPE = KmType(
    flags = 0,
    typeArguments = emptyList(),
    extendsBound = null,
    isExtensionType = false
)