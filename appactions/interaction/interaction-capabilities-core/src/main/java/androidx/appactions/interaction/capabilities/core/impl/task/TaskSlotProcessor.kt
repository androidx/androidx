/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.appactions.interaction.capabilities.core.impl.task

import androidx.appactions.interaction.capabilities.core.EntitySearchResult
import androidx.appactions.interaction.capabilities.core.ValidationResult
import androidx.appactions.interaction.capabilities.core.impl.converters.EntityConverter
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.InvalidResolverException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.MissingEntityConverterException
import androidx.appactions.interaction.capabilities.core.impl.task.exceptions.MissingSearchActionConverterException
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.DisambiguationData
import androidx.appactions.interaction.proto.ParamValue
import kotlin.String
import kotlin.Throws

/**
 * Contains static utility methods that handles processing argument slots for TaskCapabilityImpl.
 */
internal object TaskSlotProcessor {
    /**
     * Processes all ParamValue for a single slot.
     *
     * @return a [SlotProcessingResult] object.
     */
    @Throws(
        MissingEntityConverterException::class,
        MissingSearchActionConverterException::class,
        StructConversionException::class,
        InvalidResolverException::class,
    )
    suspend fun processSlot(
        name: String,
        pendingArgs: List<CurrentValue>,
        taskParamMap: Map<String, TaskParamBinding<*>>,
    ): SlotProcessingResult {
        // TODO(b/234655571) use slot metadata to ensure that we never auto accept values
        // for reference slots.
        val taskParamBinding =
            taskParamMap[name]
                ?: return SlotProcessingResult(
                    true,
                    pendingArgs.map {
                        TaskCapabilityUtils.toCurrentValue(it.value, CurrentValue.Status.ACCEPTED)
                    },
                )
        val groundedValues = mutableListOf<ParamValue>()
        val ungroundedValues = mutableListOf<CurrentValue>()
        var groundingResult = AppGroundingResult.ofSuccess(ParamValue.getDefaultInstance())
        for (pendingValue in pendingArgs) {
            if (pendingValue.hasDisambiguationData()) {
                // assistant-driven disambiguation
                groundingResult =
                    consumeGroundingResult(
                        chainAssistantGrounding(groundingResult, pendingValue, taskParamBinding),
                        groundedValues,
                        ungroundedValues,
                    )
            } else if (taskParamBinding.groundingPredicate.invoke(pendingValue.value)) {
                // app-driven disambiguation
                groundingResult =
                    consumeGroundingResult(
                        chainAppGrounding(groundingResult, pendingValue, taskParamBinding),
                        groundedValues,
                        ungroundedValues,
                    )
            } else {
                groundedValues.add(pendingValue.value)
            }
        }
        return if (groundedValues.isEmpty()) {
            SlotProcessingResult(
                /* isSuccessful= */ false,
                ungroundedValues.toList(),
            )
        } else {
            processValidationResult(
                invokeValueChange(groundedValues, taskParamBinding),
                groundedValues,
                ungroundedValues,
            )
        }
    }

    /** enqueues processing of a pending value that requires assistant-driven grounding. */
    @Throws(InvalidResolverException::class)
    private suspend fun chainAssistantGrounding(
        groundingResult: AppGroundingResult,
        pendingValue: CurrentValue,
        taskParamBinding: TaskParamBinding<*>,
    ) =
        when (groundingResult.kind) {
            AppGroundingResult.Kind.SUCCESS -> {
                renderAssistantDisambigData(pendingValue.disambiguationData, taskParamBinding)
                AppGroundingResult.ofFailure(
                    CurrentValue.newBuilder(pendingValue)
                        .setStatus(CurrentValue.Status.DISAMBIG)
                        .build(),
                )
            }
            AppGroundingResult.Kind.FAILURE -> AppGroundingResult.ofFailure(pendingValue)
        }

    /** enqueues processing of a pending value that requires app-driven grounding. */
    @Throws(
        MissingEntityConverterException::class,
        MissingSearchActionConverterException::class,
        StructConversionException::class,
        InvalidResolverException::class,
    )
    private suspend fun chainAppGrounding(
        groundingResult: AppGroundingResult,
        pendingValue: CurrentValue,
        taskParamBinding: TaskParamBinding<*>,
    ) =
        when (groundingResult.kind) {
            AppGroundingResult.Kind.SUCCESS -> ground(pendingValue.value, taskParamBinding)
            AppGroundingResult.Kind.FAILURE -> AppGroundingResult.ofFailure(pendingValue)
        }

    /**
     * Consumes the result of grounding.
     *
     * If grounding was successful (app-driven with 1 returned result) the grounded ParamValue is
     * added to groundedValues.
     *
     * otherwise the ungrounded CurrentValue is added to ungroundedValues.
     */
    private fun consumeGroundingResult(
        groundingResult: AppGroundingResult,
        groundedValues: MutableList<ParamValue>,
        ungroundedValues: MutableList<CurrentValue>,
    ): AppGroundingResult {
        when (groundingResult.kind) {
            AppGroundingResult.Kind.SUCCESS -> groundedValues.add(groundingResult.success!!)
            AppGroundingResult.Kind.FAILURE -> ungroundedValues.add(groundingResult.failure!!)
        }
        return groundingResult
    }

    /**
     * Applies "wildcard capture" technique. For more details see
     * [...](https://docs.oracle.com/javase/tutorial/java/generics/capture.html)
     */
    @Throws(StructConversionException::class)
    private suspend fun <T> invokeValueChange(
        updatedValue: List<ParamValue>,
        binding: TaskParamBinding<T>,
    ): ValidationResult {
        return binding.resolver.notifyValueChange(updatedValue, binding.converter)
    }

    @Throws(InvalidResolverException::class)
    private suspend fun renderAssistantDisambigData(
        disambiguationData: DisambiguationData,
        binding: TaskParamBinding<*>,
    ) {
        val entityIds = disambiguationData.entitiesList.map { it.identifier }
        binding.resolver.invokeEntityRender(entityIds)
    }

    /**
     * Processes the ValidationResult from sending argument updates to onReceived.
     *
     * @param validationResult the ValidationResult returned from value listener.
     * @param groundedValues a List of all grounded ParamValue.
     * @param ungroundedValues a List of all ungrounded CurrentValue.
     */
    private fun processValidationResult(
        validationResult: ValidationResult,
        groundedValues: List<ParamValue>,
        ungroundedValues: List<CurrentValue>,
    ): SlotProcessingResult {
        val combinedValues = mutableListOf<CurrentValue>()
        when (validationResult.kind) {
            ValidationResult.Kind.ACCEPTED ->
                combinedValues.addAll(
                    TaskCapabilityUtils.paramValuesToCurrentValue(
                        groundedValues,
                        CurrentValue.Status.ACCEPTED,
                    ),
                )
            ValidationResult.Kind.REJECTED ->
                combinedValues.addAll(
                    TaskCapabilityUtils.paramValuesToCurrentValue(
                        groundedValues,
                        CurrentValue.Status.REJECTED,
                    ),
                )
        }
        combinedValues.addAll(ungroundedValues)
        return SlotProcessingResult(
            /* isSuccessful= */ ungroundedValues.isEmpty() &&
                validationResult.kind === ValidationResult.Kind.ACCEPTED,
            combinedValues.toList(),
        )
    }

    /** perform an in-app search for an ungrounded ParamValue */
    @Throws(
        MissingEntityConverterException::class,
        MissingSearchActionConverterException::class,
        StructConversionException::class,
        InvalidResolverException::class,
    )
    private suspend fun <T> ground(
        ungroundedParamValue: ParamValue,
        binding: TaskParamBinding<T>,
    ): AppGroundingResult {
        if (binding.entityConverter == null) {
            throw MissingEntityConverterException("No entity converter found in the binding.")
        }
        if (binding.searchActionConverter == null) {
            throw MissingSearchActionConverterException(
                "No search action converter found in the binding.",
            )
        }
        val entityConverter = binding.entityConverter
        val searchActionConverter = binding.searchActionConverter
        val searchAction = searchActionConverter.toSearchAction(ungroundedParamValue)
        val entitySearchResult = binding.resolver.invokeLookup(searchAction)
        return processEntitySearchResult(entitySearchResult, entityConverter, ungroundedParamValue)
    }

    /**
     * Processes the EntitySearchResult from performing an entity search.
     *
     * @param entitySearchResult the EntitySearchResult returned from the app resolver.
     * @param ungroundedValue the original ungrounded ParamValue.
     */
    @Throws(StructConversionException::class)
    private fun <T> processEntitySearchResult(
        entitySearchResult: EntitySearchResult<T>,
        entityConverter: EntityConverter<T>,
        ungroundedValue: ParamValue,
    ): AppGroundingResult {
        return when (entitySearchResult.possibleValues.size) {
            0 ->
                AppGroundingResult.ofFailure(
                    TaskCapabilityUtils.toCurrentValue(
                        ungroundedValue,
                        CurrentValue.Status.REJECTED,
                    ),
                )
            1 -> {
                val groundedEntity = entityConverter.convert(entitySearchResult.possibleValues[0]!!)
                AppGroundingResult.ofSuccess(
                    TaskCapabilityUtils.groundedValueToParamValue(groundedEntity),
                )
            }
            else -> {
                val disambigEntities =
                    entitySearchResult.possibleValues.map { entityConverter.convert(it!!) }
                AppGroundingResult.ofFailure(
                    TaskCapabilityUtils.getCurrentValueForDisambiguation(
                        ungroundedValue,
                        disambigEntities,
                    ),
                )
            }
        }
    }
}
