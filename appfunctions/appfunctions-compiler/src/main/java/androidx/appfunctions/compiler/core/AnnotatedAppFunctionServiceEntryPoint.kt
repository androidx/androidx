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

package androidx.appfunctions.compiler.core

import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionServiceClass
import androidx.appfunctions.compiler.core.IntrospectionHelper.AppFunctionServiceEntryPointAnnotation
import androidx.appfunctions.compiler.core.IntrospectionHelper.ExtensionsAppFunctionServiceClass
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier

/** Represents a class annotated with @AppFunctionServiceEntryPoint. */
class AnnotatedAppFunctionServiceEntryPoint(
    val serviceDeclaration: KSClassDeclaration,
    val appFunctions: List<AnnotatedAppFunction>,
) {
    private val appFunctionServiceEntryPointAnnotation by lazy {
        serviceDeclaration.annotations.findAnnotation(
            AppFunctionServiceEntryPointAnnotation.CLASS_NAME
        )
            ?: throw ProcessingException(
                "Class not annotated with @AppFunctionServiceEntryPoint",
                serviceDeclaration,
            )
    }

    /** The name of the service class. */
    val serviceName: String by lazy {
        appFunctionServiceEntryPointAnnotation.requirePropertyValueOfType(
            AppFunctionServiceEntryPointAnnotation.PROPERTY_SERVICE_NAME,
            String::class,
        )
    }

    /** The name of the app function XML file. */
    val appFunctionXmlFileName: String by lazy {
        appFunctionServiceEntryPointAnnotation.requirePropertyValueOfType(
            AppFunctionServiceEntryPointAnnotation.PROPERTY_APP_FUNCTION_XML_FILE_NAME,
            String::class,
        )
    }

    /** Validates the app function entry point. */
    fun validate(): AnnotatedAppFunctionServiceEntryPoint {
        validateAnnotation()
        validateSuperClass()
        validateIsAbstract()
        validateExecuteFunctionIsNotImplemented()
        validateAppFunctions()
        return this
    }

    /**
     * Returns the set of files that need to be processed to obtain the complete information about
     * the app function entry point.
     */
    fun getSourceFiles(): Set<KSFile> {
        val sourceFileSet: MutableSet<KSFile> = mutableSetOf()

        serviceDeclaration.containingFile?.let { sourceFileSet.add(it) }

        for (appFunction in appFunctions) {
            sourceFileSet.addAll(appFunction.getSourceFiles())
        }
        return sourceFileSet
    }

    private fun validateAnnotation() {
        val entryPointAnnotation =
            serviceDeclaration.annotations.findAnnotation(
                AppFunctionServiceEntryPointAnnotation.CLASS_NAME
            )
        if (entryPointAnnotation == null) {
            throw ProcessingException(
                "Class must be annotated with @AppFunctionServiceEntryPoint",
                serviceDeclaration,
            )
        }
    }

    private fun validateSuperClass() {
        val isAppFunctionService =
            serviceDeclaration.getAllSuperTypes().any { it.declaration.isAppFunctionService() }
        val isExtensionAppFunctionService =
            serviceDeclaration.getAllSuperTypes().any {
                it.declaration.isExtensionAppFunctionService()
            }
        if (
            !isAppFunctionService &&
                !isExtensionAppFunctionService &&
                !isExtendingHiltGeneratedClass()
        ) {
            throw ProcessingException(
                "Class must extend either " +
                    "${AppFunctionServiceClass.CLASS_NAME.canonicalName} or " +
                    ExtensionsAppFunctionServiceClass.CLASS_NAME.canonicalName,
                serviceDeclaration,
            )
        }
    }

    /**
     * Check if the [serviceDeclaration] is overriding a Hilt generated class without bytecode
     * transformation.
     *
     * For build system that doesn't use bytecode transformation, the Hilt usage looks like this
     *
     * ```
     * @AndroidEntryPoint(AppFunctionService::class)
     * @AppFunctionServiceEntryPoint(...)
     * abstract class BaseAppFunctionService: Hilt_BaseAppFunctionService() { ... }
     * ```
     *
     * If the Hilt class is not processed by KSP, the multi-round feature would not work. Therefore,
     * the build would result in failure that AppFunction compiler is unable to resolve the
     * generated Hilt class to check its super types.
     *
     * To avoid this issue, this method is to detect such scenario and fallback to check the class
     * declared in AndroidEntryPoint.
     */
    private fun isExtendingHiltGeneratedClass(): Boolean {
        val androidEntryPointAnnotation =
            serviceDeclaration.annotations.findAnnotation(
                IntrospectionHelper.AndroidEntryPointAnnotation.CLASS_NAME
            )
        if (androidEntryPointAnnotation == null) {
            return false
        }
        var hasHiltGeneratedPrefix = false
        for (superType in serviceDeclaration.superTypes) {
            val simpleName = superType.toString().substringAfterLast('.')
            if (simpleName.startsWith(HILT_CLASS_PREFIX)) {
                hasHiltGeneratedPrefix = true
                break
            }
        }
        if (!hasHiltGeneratedPrefix) {
            return false
        }
        // Service annotated with AndroidEntryPoint and overrides a Hilt_ prefix class, it's
        // safe to assume that this is running on Hilt setup. Check the target class to see
        // if it is AppFunctionService or the subtype of AppFunctionService
        val overrideService =
            androidEntryPointAnnotation.requirePropertyValueOfType(
                IntrospectionHelper.AndroidEntryPointAnnotation.PROPERTY_VALUE,
                KSType::class,
            )
        val overrideServiceDeclaration = overrideService.declaration
        if (overrideServiceDeclaration !is KSClassDeclaration) {
            return false
        }
        if (
            overrideServiceDeclaration.isAppFunctionService() ||
                overrideServiceDeclaration.isExtensionAppFunctionService()
        ) {
            return true
        }
        for (superType in overrideServiceDeclaration.getAllSuperTypes()) {
            if (
                superType.declaration.isAppFunctionService() ||
                    superType.declaration.isExtensionAppFunctionService()
            ) {
                return true
            }
        }
        return false
    }

    private fun KSDeclaration.isAppFunctionService(): Boolean {
        return qualifiedName?.asString() == AppFunctionServiceClass.CLASS_NAME.canonicalName
    }

    private fun KSDeclaration.isExtensionAppFunctionService(): Boolean {
        return qualifiedName?.asString() ==
            ExtensionsAppFunctionServiceClass.CLASS_NAME.canonicalName
    }

    private fun validateIsAbstract() {
        if (Modifier.ABSTRACT !in serviceDeclaration.modifiers) {
            throw ProcessingException(
                "The class being annotated with AppFunctionServiceEntryPoint should be an abstract class",
                serviceDeclaration,
            )
        }
    }

    private fun validateExecuteFunctionIsNotImplemented() {
        val executeFunction =
            serviceDeclaration.declarations.filterIsInstance<KSFunctionDeclaration>().find {
                it.simpleName.asString() ==
                    AppFunctionServiceClass.ExecuteFunctionMethod.METHOD_NAME
            }
        if (executeFunction != null && Modifier.ABSTRACT !in executeFunction.modifiers) {
            throw ProcessingException(
                "The abstract class cannot implement executeFunction. This would be generated by the compiler",
                executeFunction,
            )
        }
    }

    private fun validateAppFunctions() {
        if (appFunctions.isEmpty()) {
            throw ProcessingException(
                "Class must have at least one AppFunction",
                serviceDeclaration,
            )
        }
        for (appFunction in appFunctions) {
            appFunction.validate(skipFirstParameterValidation = true)
        }
    }

    private companion object {
        const val HILT_CLASS_PREFIX = "Hilt_"
    }
}
