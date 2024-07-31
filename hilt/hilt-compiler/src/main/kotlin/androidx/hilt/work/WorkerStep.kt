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

package androidx.hilt.work

import androidx.hilt.ClassNames
import androidx.room.compiler.codegen.toJavaPoet
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingEnvConfig
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.XTypeElement
import javax.tools.Diagnostic

/** Processing step that generates code enabling assisted injection of Workers using Hilt. */
class WorkerStep : XProcessingStep {

    override fun annotations() = setOf(ClassNames.HILT_WORKER.canonicalName())

    override fun process(
        env: XProcessingEnv,
        elementsByAnnotation: Map<String, Set<XElement>>,
        isLastRound: Boolean
    ): Set<XElement> {
        elementsByAnnotation[ClassNames.HILT_WORKER.canonicalName()]
            ?.filterIsInstance<XTypeElement>()
            ?.mapNotNull { element -> parse(env, element) }
            ?.forEach { worker -> WorkerGenerator(env, worker).generate() }
        return emptySet()
    }

    // usage of findTypeElement and requireType with -Pandroidx.maxDepVersions=true
    @Suppress("DEPRECATION")
    private fun parse(env: XProcessingEnv, workerTypeElement: XTypeElement): WorkerElement? {
        var valid = true

        if (env.findTypeElement(ClassNames.WORKER_ASSISTED_FACTORY) == null) {
            env.error(
                "To use @HiltWorker you must add the 'work' artifact. " +
                    "androidx.hilt:hilt-work:<version>"
            )
            valid = false
        }

        val workerType = workerTypeElement.type
        val listenableWorkerType = env.requireType(ClassNames.LISTENABLE_WORKER)
        if (!listenableWorkerType.isAssignableFrom(workerType)) {
            env.error(
                "@HiltWorker is only supported on types that subclass " +
                    "${ClassNames.LISTENABLE_WORKER}.",
                workerTypeElement
            )
            valid = false
        }

        val constructors =
            workerTypeElement.getConstructors().filter {
                if (it.hasAnnotation(ClassNames.INJECT)) {
                    env.error(
                        "Worker constructor should be annotated with @AssistedInject instead of " +
                            "@Inject.",
                        it
                    )
                    valid = false
                }
                it.hasAnnotation(ClassNames.ASSISTED_INJECT)
            }
        if (constructors.size != 1) {
            env.error(
                "@HiltWorker annotated class should contain exactly one @AssistedInject " +
                    "annotated constructor.",
                workerTypeElement
            )
            valid = false
        }
        constructors
            .filter { it.isPrivate() }
            .forEach {
                env.error("@AssistedInject annotated constructors must not be private.", it)
                valid = false
            }

        if (workerTypeElement.isNested() && !workerTypeElement.isStatic()) {
            env.error(
                "@HiltWorker may only be used on inner classes if they are static.",
                workerTypeElement
            )
            valid = false
        }

        if (!valid) return null

        val injectConstructor = constructors.first()
        var contextIndex = -1
        var workerParametersIndex = -1
        injectConstructor.parameters.forEachIndexed { index, param ->
            if (param.type.asTypeName().toJavaPoet() == ClassNames.CONTEXT) {
                if (!param.hasAnnotation(ClassNames.ASSISTED)) {
                    env.error("Missing @Assisted annotation in param '${param.name}'.", param)
                    valid = false
                }
                contextIndex = index
            }
            if (param.type.asTypeName().toJavaPoet() == ClassNames.WORKER_PARAMETERS) {
                if (!param.hasAnnotation(ClassNames.ASSISTED)) {
                    env.error("Missing @Assisted annotation in param '${param.name}'.", param)
                    valid = false
                }
                workerParametersIndex = index
            }
        }
        if (contextIndex > workerParametersIndex) {
            env.error(
                "The 'Context' parameter must be declared before the 'WorkerParameters' in the " +
                    "@AssistedInject constructor of a @HiltWorker annotated class.",
                injectConstructor
            )
            valid = false
        }

        if (!valid) return null

        return WorkerElement(workerTypeElement, injectConstructor)
    }

    private fun XProcessingEnv.error(message: String, element: XElement? = null) {
        if (element != null) {
            messager.printMessage(Diagnostic.Kind.ERROR, message, element)
        } else {
            messager.printMessage(Diagnostic.Kind.ERROR, message)
        }
    }

    companion object {
        val ENV_CONFIG = XProcessingEnvConfig.DEFAULT.copy(disableAnnotatedElementValidation = true)
    }
}
