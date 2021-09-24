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

package androidx.room.compiler.processing

import javax.tools.Diagnostic

/**
 * Common interface for basic annotation processors.
 *
 * A processor should not implement this interface directly and instead should extend
 * [androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor] or
 * [androidx.room.compiler.processing.ksp.KspBasicAnnotationProcessor].
 *
 * The XProcessing Javac and KSP implementations of this interface will automatically validate and
 * defer annotated elements for the steps. If no valid annotated element is found for a
 * [XProcessingStep] then its [XProcessingStep.process] function will not be invoked, except for
 * the last round in which [XProcessingStep.processOver] is invoked regardless if the annotated
 * elements are valid or not. If there were invalid annotated elements until the last round, then
 * the XProcessing implementations will report an error for each invalid element.
 *
 * Be aware that even though the similarity in name, the Javac implementation of this interface
 * is not 1:1 with [com.google.auto.common.BasicAnnotationProcessor]. Specifically, validation is
 * done for each annotated element as opposed to the enclosing type element of the annotated
 * elements for the [XProcessingStep].
 */
interface XBasicAnnotationProcessor {

    /**
     * Returns the [XProcessingEnv].
     */
    val xProcessingEnv: XProcessingEnv

    /**
     * The list of processing steps to execute.
     */
    fun processingSteps(): Iterable<XProcessingStep>

    /**
     * Called at the end of a processing round after all [processingSteps] have been executed.
     */
    fun postRound(env: XProcessingEnv, round: XRoundEnv) { }
}

/**
 * Common code for implementations of [XBasicAnnotationProcessor] offered by XProcessing.
 */
internal class CommonProcessorDelegate(
    private val processorClass: Class<*>,
    private val env: XProcessingEnv,
    private val steps: List<XProcessingStep>,
) {
    // Type names of deferred elements from the processor.
    private val deferredElementNames = mutableSetOf<String>()
    // Type element names containing deferred elements from processing steps.
    private val elementsDeferredBySteps = mutableMapOf<XProcessingStep, Set<String>>()

    fun processRound(roundEnv: XRoundEnv) {
        val previousRoundDeferredElementNames = deferredElementNames.toMutableSet()
        deferredElementNames.clear()
        val currentElementsDeferredByStep = steps.associateWith { step ->
            // Previous round processor deferred elements, these need to be re-validated.
            val previousRoundDeferredElementsByAnnotation =
                getStepElementsByAnnotation(step, previousRoundDeferredElementNames)
                    .withDefault { emptySet() }
            // Previous round step deferred elements, these don't need to be re-validated.
            val stepDeferredElementsByAnnotation =
                getStepElementsByAnnotation(step, elementsDeferredBySteps[step] ?: emptySet())
                    .withDefault { emptySet() }
            val deferredElements = mutableSetOf<XElement>()
            val elementsByAnnotation = step.annotations().mapNotNull { annotation ->
                val annotatedElements = roundEnv.getElementsAnnotatedWith(annotation) +
                    previousRoundDeferredElementsByAnnotation.getValue(annotation)
                // Split between valid and invalid elements. Unlike auto-common, validation is only
                // done in the annotated element from the round and not in the closest enclosing
                // type element.
                val (validElements, invalidElements) = annotatedElements.partition { it.validate() }
                deferredElements.addAll(invalidElements)
                (validElements + stepDeferredElementsByAnnotation.getValue(annotation)).let {
                    if (it.isNotEmpty()) {
                        annotation to it.toSet()
                    } else {
                        null
                    }
                }
            }.toMap()
            // Store all processor deferred elements.
            deferredElementNames.addAll(
                deferredElements.mapNotNull { getClosestEnclosingTypeElement(it)?.qualifiedName }
            )
            // Only process the step if there are annotated elements found for this step.
            return@associateWith if (elementsByAnnotation.isNotEmpty()) {
                step.process(env, elementsByAnnotation)
                    .mapNotNull { getClosestEnclosingTypeElement(it)?.qualifiedName }.toSet()
            } else {
                emptySet()
            }
        }
        // Store elements deferred by steps.
        elementsDeferredBySteps.clear()
        elementsDeferredBySteps.putAll(currentElementsDeferredByStep)
    }

    fun processLastRound(): List<String> {
        steps.forEach { step ->
            val stepDeferredElementsByAnnotation = getStepElementsByAnnotation(
                step = step,
                typeElementNames =
                    deferredElementNames + elementsDeferredBySteps.getOrElse(step) { emptySet() }
            )
            val elementsByAnnotation = step.annotations().mapNotNull { annotation ->
                val annotatedElements = stepDeferredElementsByAnnotation[annotation] ?: emptySet()
                if (annotatedElements.isNotEmpty()) {
                    annotation to annotatedElements
                } else {
                    null
                }
            }.toMap()
            step.processOver(env, elementsByAnnotation)
        }
        // Return element names that were deferred until the last round, an error should be reported
        // for these, failing compilation. Sadly we currently don't have the mechanism to know if
        // the missing types were generated in the last round.
        return elementsDeferredBySteps.values.flatten()
    }

    /**
     * Get elements annotated with [step]'s annotations from the type element in [typeElementNames].
     *
     * Does not traverse type element members, so that if looking at `Outer` in the example
     * below, looking for `@X`, then `Outer`, `Outer.foo`, and `Outer.foo()` will be in the result,
     * but neither `Inner` nor its members will unless `Inner` is also in the [typeElementNames].
     * ```
     * @X class Outer {
     *   @X Object foo;
     *   @X void foo() {}
     *   @X static class Inner {
     *     @X Object bar;
     *     @X void bar() {}
     *   }
     * }
     * ```
     */
    private fun getStepElementsByAnnotation(
        step: XProcessingStep,
        typeElementNames: Set<String>
    ): Map<String, Set<XElement>> {
        if (typeElementNames.isEmpty()) {
            return emptyMap()
        }
        val stepAnnotations = step.annotations()
        val elementsByAnnotation = mutableMapOf<String, MutableSet<XElement>>()
        fun putStepAnnotatedElements(element: XElement) = element.getAllAnnotations()
            .map { it.qualifiedName }
            .forEach { annotationName ->
                if (stepAnnotations.contains(annotationName)) {
                    elementsByAnnotation.getOrPut(annotationName) { mutableSetOf() }.add(element)
                }
            }
        typeElementNames
            .mapNotNull { env.findTypeElement(it) }
            .forEach { typeElement ->
                typeElement.getEnclosedElements()
                    .filterNot { it.isTypeElement() }
                    .forEach { enclosedElement ->
                        if (enclosedElement is XExecutableElement) {
                            enclosedElement.parameters.forEach {
                                putStepAnnotatedElements(it)
                            }
                        }
                        putStepAnnotatedElements(enclosedElement)
                    }
                putStepAnnotatedElements(typeElement)
            }
        return elementsByAnnotation.withDefault { mutableSetOf() }
    }

    // TODO(b/201308409): Does not work with top-level KSP functions or properties whose
    //  container are synthetic.
    private fun getClosestEnclosingTypeElement(element: XElement): XTypeElement? {
        return when {
            element.isTypeElement() -> element
            element.isField() -> element.enclosingElement as? XTypeElement
            element.isMethod() -> element.enclosingElement as? XTypeElement
            element.isConstructor() -> element.enclosingElement
            element.isMethodParameter() ->
                element.enclosingMethodElement.enclosingElement as? XTypeElement
            element.isEnumEntry() -> element.enumTypeElement
            else -> {
                env.messager.printMessage(
                    kind = Diagnostic.Kind.WARNING,
                    msg = "Unable to defer element '$element': Don't know how to find " +
                        "closest enclosing type element."
                )
                null
            }
        }
    }

    fun reportMissingElements(missingElementNames: List<String>) {
        missingElementNames.forEach { missingElementName ->
            env.messager.printMessage(
                kind = Diagnostic.Kind.ERROR,
                msg = (
                    "%s was unable to process '%s' because not all of its dependencies " +
                        "could be resolved. Check for compilation errors or a circular " +
                        "dependency with generated code."
                    ).format(processorClass.canonicalName, missingElementName),
            )
        }
    }
}