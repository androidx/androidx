/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.XProcessingConfig
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Origin
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * When a compiled kotlin class is loaded from a `.class` file, its fields are not ordered in the
 * same way they are declared in code.
 * This particularly hurts Room where we generate the table structure in that order.
 *
 * This class implements a port of https://github.com/google/ksp/pull/260 via reflection until KSP
 * (or kotlin compiler) fixes the problem. As this uses reflection, it is fail safe such that if it
 * cannot find the correct order, it will just return in the order KSP returned instead of crashing.
 */
internal object KspFieldOrdering {
    /**
     * Sorts the given fields in the order they are declared in the backing class declaration.
     */
    fun orderFields(
        owner: KSClassDeclaration,
        fields: List<KspFieldElement>
    ): List<KspFieldElement> {
        // no reason to try to load .class if we don't have any fields to sort
        if (fields.isEmpty()) return fields
        val comparator = getFieldNamesComparator(owner)
        return if (comparator == null) {
            fields
        } else {
            fields.forEach {
                // make sure each name gets registered so that if we didn't find it in .class for
                // whatever reason, we keep the order given from KSP.
                comparator.register(it.name)
            }
            fields.sortedWith(comparator)
        }
    }

    /**
     * Builds a field names comparator from the given class declaration if and only if its origin
     * is CLASS.
     * If it fails to find the order, returns null.
     */
    @Suppress("BanUncheckedReflection")
    private fun getFieldNamesComparator(
        ksClassDeclaration: KSClassDeclaration
    ): FieldNameComparator? {
        return try {
            if (ksClassDeclaration.origin != Origin.CLASS) return null
            val typeReferences = ReflectionReferences.getInstance(ksClassDeclaration) ?: return null
            val descriptor = typeReferences.getDescriptorMethod.invoke(ksClassDeclaration)
                ?: return null
            if (!typeReferences.deserializedClassDescriptor.isInstance(descriptor)) {
                return null
            }
            val descriptorSrc = typeReferences.descriptorSourceMethod.invoke(descriptor)
                ?: return null
            if (!typeReferences.kotlinJvmBinarySourceElement.isInstance(descriptorSrc)) {
                return null
            }
            val binarySource = typeReferences.binaryClassMethod.invoke(descriptorSrc)
                ?: return null

            val fieldNameComparator = FieldNameComparator()
            val invocationHandler = InvocationHandler { _, method, args ->
                if (method.name == "visitField") {
                    val nameAsString = typeReferences.asStringMethod.invoke(args[0])
                    if (nameAsString is String) {
                        fieldNameComparator.register(nameAsString)
                    }
                }
                null
            }

            val proxy = Proxy.newProxyInstance(
                ksClassDeclaration.javaClass.classLoader,
                arrayOf(typeReferences.memberVisitor),
                invocationHandler
            )
            typeReferences.visitMembersMethod.invoke(binarySource, proxy, null)
            fieldNameComparator.seal()
            fieldNameComparator
        } catch (ignored: Throwable) {
            // this is best effort, if it failed, just ignore
            if (XProcessingConfig.STRICT_MODE) {
                throw RuntimeException("failed to get fields", ignored)
            }
            null
        }
    }

    /**
     * Holder object to keep references to class/method instances.
     */
    private class ReflectionReferences private constructor(
        classLoader: ClassLoader
    ) {

        val deserializedClassDescriptor: Class<*> = classLoader.loadClass(
            "org.jetbrains.kotlin.serialization.deserialization.descriptors" +
                ".DeserializedClassDescriptor"
        )

        val ksClassDeclarationDescriptorImpl: Class<*> = classLoader.loadClass(
            "com.google.devtools.ksp.symbol.impl.binary.KSClassDeclarationDescriptorImpl"
        )
        val kotlinJvmBinarySourceElement: Class<*> = classLoader.loadClass(
            "org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement"
        )

        val kotlinJvmBinaryClass: Class<*> = classLoader.loadClass(
            "org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass"
        )

        val memberVisitor: Class<*> = classLoader.loadClass(
            "org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass\$MemberVisitor"
        )

        val name: Class<*> = classLoader.loadClass(
            "org.jetbrains.kotlin.name.Name"
        )

        val getDescriptorMethod: Method = ksClassDeclarationDescriptorImpl
            .getDeclaredMethod("getDescriptor")

        val descriptorSourceMethod: Method = deserializedClassDescriptor.getMethod("getSource")

        val binaryClassMethod: Method = kotlinJvmBinarySourceElement.getMethod("getBinaryClass")

        val visitMembersMethod: Method = kotlinJvmBinaryClass.getDeclaredMethod(
            "visitMembers",
            memberVisitor, ByteArray::class.java
        )

        val asStringMethod: Method = name.getDeclaredMethod("asString")

        companion object {
            private val FAILED = Any()
            private var instance: Any? = null

            /**
             * Gets the cached instance or create a new one using the class loader of the given
             * [ref] parameter.
             */
            fun getInstance(ref: Any): ReflectionReferences? {
                if (instance == null) {
                    instance = try {
                        ReflectionReferences(ref::class.java.classLoader)
                    } catch (ignored: Throwable) {
                        FAILED
                    }
                }
                return instance as? ReflectionReferences
            }
        }
    }

    private class FieldNameComparator : Comparator<XFieldElement> {
        private var nextOrder: Int = 0
        private var sealed: Boolean = false
        private val orders = mutableMapOf<String, Int>()

        /**
         * Called when fields are read to lock the ordering.
         * This is only relevant in tests as at runtime, we just do a best effort (add a new id
         * for it) and continue.
         */
        fun seal() {
            sealed = true
        }

        /**
         * Registers the name with the next order id
         */
        fun register(name: String) {
            getOrder(name)
        }

        /**
         * Gets the order of the name. If it was not seen before, adds it to the list, giving it a
         * new ID.
         */
        private fun getOrder(name: String) = orders.getOrPut(name) {
            if (sealed && XProcessingConfig.STRICT_MODE) {
                error("expected to find field $name but it is non-existent")
            }
            nextOrder++
        }

        override fun compare(field1: XFieldElement, field2: XFieldElement): Int {
            return getOrder(field1.name).compareTo(getOrder(field2.name))
        }
    }
}
