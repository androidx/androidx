/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.inspection.inspector

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.ui.inspection.SPAM_LOG_TAG
import java.lang.reflect.Field
import java.lang.reflect.Member
import java.lang.reflect.Method
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.javaClass
import kotlin.metadata.ClassKind
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.Modality
import kotlin.metadata.isLateinit
import kotlin.metadata.isVar
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.kind
import kotlin.metadata.modality

private const val INSTANCE_FIELD = "INSTANCE"

private val WRAPPERS =
    setOf(
        Boolean::class.javaObjectType,
        Char::class.javaObjectType,
        Byte::class.javaObjectType,
        Short::class.javaObjectType,
        Int::class.javaObjectType,
        Long::class.javaObjectType,
        Float::class.javaObjectType,
        Double::class.javaObjectType,
    )

val Class<*>.allSuperclasses: Collection<Class<*>>
    get() {
        val result = mutableSetOf<Class<*>>()
        var klass: Class<*>? = this
        while (klass != null && klass != Any::class.java) {
            result.add(klass)
            result.addAll(klass.interfaces)
            klass = klass.superclass
        }
        return result
    }

/** A property returned by this API */
internal interface KotlinProperty {
    val isFinal: Boolean
    val isLateInit: Boolean
    val isVar: Boolean
    val hasNoParameters: Boolean
    val name: String
}

/** Utility functions for providing access to kotlin properties via Java reflection. */
internal class ReflectionSupport(private val inlineClassConverter: InlineClassConverter) {
    private val classCache = mutableMapOf<Class<*>, MappedClass>()

    /** Return the kotlin member properties of a given class. */
    fun declaredMemberProperties(javaClass: Class<*>): Collection<KotlinProperty> {
        val mappedClass = findMappedClass(javaClass)
        val kmClass = mappedClass?.kmClass ?: return emptyList()
        return kmClass.properties.map { KotlinPropertyImpl(it, javaClass) }
    }

    /**
     * Return the kotlin member properties of a given package class e.g. the file File.kt will have
     * a FileKt class holding the package definitions of the file.
     */
    fun declaredPackageProperties(packageClass: Class<*>): Collection<KotlinProperty> {
        val mappedClass = findMappedClass(packageClass)
        val kmPackage = mappedClass?.kmPackage ?: return emptyList()
        return kmPackage.properties.map { KotlinPropertyImpl(it, packageClass) }
    }

    /**
     * Return the object instance of a class. If the class is an object return the object instance,
     * otherwise return the companion object of the class.
     */
    fun objectInstance(javaClass: Class<*>): Any? {
        val mappedClass = findMappedClass(javaClass) ?: return null
        val kmClass = mappedClass.kmClass ?: return null
        val method =
            when {
                kmClass.kind == ClassKind.OBJECT -> javaClass.getDeclaredField(INSTANCE_FIELD)
                kmClass.companionObject != null ->
                    javaClass.getDeclaredField(kmClass.companionObject!!)
                else -> null
            }
        return method?.get(null)
    }

    /**
     * Return the value of a kotlin property. Convert the result to the property inline type if the
     * result from Java reflection is a primitive type but we expect a proper class instance.
     */
    @SuppressLint("BanUncheckedReflection")
    fun valueOf(property: KotlinProperty, instance: Any?): Any? {
        try {
            val prop = property as KotlinPropertyImpl
            val getter = prop.findGetter()
            val field = prop.findField()
            var value = getter?.invoke(instance) ?: field?.get(instance)
            if (
                value != null &&
                    value.javaClass in WRAPPERS &&
                    property.returnType?.contains('/') == true
            ) {
                value = inlineClassConverter.castParameterValue(property.returnType, value)
            }
            return value
        } catch (ex: CancellationException) {
            throw ex
        } catch (t: Throwable) {
            Log.w(SPAM_LOG_TAG, "Problem getting value from ${property.name}", t)
            return null
        }
    }

    /**
     * The kotlin metadata representation of a class. This can be either a Kotlin class or a kotlin
     * package file.
     */
    private class MappedClass(val kmClass: KmClass?, val kmPackage: KmPackage?)

    private fun findMappedClass(javaClass: Class<*>): MappedClass? {
        classCache[javaClass]?.let {
            return it
        }
        val metadataAnnotation = javaClass.getAnnotation(Metadata::class.java) ?: return null
        val metadata = KotlinClassMetadata.readStrict(metadataAnnotation)
        val mappedClass =
            when (metadata) {
                is KotlinClassMetadata.Class -> MappedClass(metadata.kmClass, null)
                is KotlinClassMetadata.FileFacade -> MappedClass(null, metadata.kmPackage)
                else -> null
            }
        mappedClass?.let { classCache[javaClass] = it }
        return mappedClass
    }

    private class KotlinPropertyImpl(
        private val property: KmProperty,
        private val ownerClass: Class<*>,
    ) : KotlinProperty {
        private var member: Member? = null

        override val name: String
            get() = property.name

        override val isFinal: Boolean
            get() = property.modality == Modality.FINAL

        override val isVar: Boolean
            get() = property.isVar

        override val isLateInit: Boolean
            get() = property.isLateinit

        override val hasNoParameters: Boolean
            get() = property.getterSignature?.descriptor?.startsWith("()") ?: true

        val returnType: String?
            get() = (property.returnType.classifier as? KmClassifier.Class)?.name

        fun findGetter(): Method? {
            if (member != null) {
                return member as? Method
            }
            val signature = property.getterSignature ?: return null
            try {
                val method = ownerClass.getDeclaredMethod(signature.name)
                method.isAccessible = true
                member = method
                return method
            } catch (_: NoSuchMethodException) {
                return null
            }
        }

        fun findField(): Field? {
            if (member != null) {
                return member as? Field
            }
            val signature = property.fieldSignature ?: return null
            try {
                val field = ownerClass.getDeclaredField(signature.name)
                field.isAccessible = true
                member = field
                return field
            } catch (_: NoSuchFieldException) {
                return null
            }
        }
    }
}
