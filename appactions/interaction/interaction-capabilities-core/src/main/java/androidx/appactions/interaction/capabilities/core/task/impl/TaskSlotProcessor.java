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

package androidx.appactions.interaction.capabilities.core.task.impl;

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;

import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.converters.DisambigEntityConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.SearchActionConverter;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.task.EntitySearchResult;
import androidx.appactions.interaction.capabilities.core.task.ValidationResult;
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.InvalidResolverException;
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.MissingEntityConverterException;
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.MissingSearchActionConverterException;
import androidx.appactions.interaction.capabilities.core.values.SearchAction;
import androidx.appactions.interaction.proto.CurrentValue;
import androidx.appactions.interaction.proto.CurrentValue.Status;
import androidx.appactions.interaction.proto.DisambiguationData;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Contains static utility methods that handles processing argument slots for TaskCapabilityImpl.
 */
final class TaskSlotProcessor {

    private TaskSlotProcessor() {
    }

    /** perform an in-app search for an ungrounded ParamValue */
    private static <T> ListenableFuture<AppGroundingResult> ground(
            ParamValue ungroundedParamValue, TaskParamBinding<T> binding, Executor executor) {
        GenericResolverInternal<T> fieldResolver = binding.resolver();
        if (!binding.entityConverter().isPresent()) {
            return Futures.immediateFailedFuture(
                    new MissingEntityConverterException(
                            "No entity converter found in the binding."));
        }
        if (!binding.searchActionConverter().isPresent()) {
            return Futures.immediateFailedFuture(
                    new MissingSearchActionConverterException(
                            "No search action converter found in the binding."));
        }
        DisambigEntityConverter<T> entityConverter = binding.entityConverter().get();
        SearchActionConverter<T> searchActionConverter = binding.searchActionConverter().get();
        try {
            SearchAction<T> searchAction = searchActionConverter.toSearchAction(
                    ungroundedParamValue);
            // Note, transformAsync is needed to catch checked exceptions. See
            // https://yaqs.corp.google.com/eng/q/2565415714299052032.
            return Futures.transformAsync(
                    fieldResolver.invokeLookup(searchAction),
                    (entitySearchResult) -> {
                        try {
                            return Futures.immediateFuture(
                                    processEntitySearchResult(
                                            entitySearchResult, entityConverter,
                                            ungroundedParamValue));
                        } catch (StructConversionException e) {
                            return Futures.immediateFailedFuture(e);
                        }
                    },
                    executor,
                    "processEntitySearchResult");
        } catch (InvalidResolverException | StructConversionException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * Applies "wildcard capture" technique. For more details see
     * https://docs.oracle.com/javase/tutorial/java/generics/capture.html
     */
    private static <T> ListenableFuture<ValidationResult> invokeValueChange(
            List<ParamValue> updatedValue, TaskParamBinding<T> binding) {
        try {
            return binding.resolver().notifyValueChange(updatedValue, binding.converter());
        } catch (StructConversionException e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    /**
     * Processes all ParamValue for a single slot.
     *
     * @return a {@code ListenableFuture<SlotProcessingResult>} object.
     */
    static ListenableFuture<SlotProcessingResult> processSlot(
            String name,
            List<CurrentValue> pendingArgs,
            TaskParamRegistry taskParamRegistry,
            Executor executor) {
        TaskParamBinding<?> taskParamBinding = taskParamRegistry.bindings().get(name);
        if (taskParamBinding == null) {
            // TODO(b/234655571) use slot metadata to ensure that we never auto accept values for
            // reference slots.
            return Futures.immediateFuture(
                    SlotProcessingResult.create(
                            Boolean.TRUE,
                            pendingArgs.stream()
                                    .map(
                                            pendingArg ->
                                                    TaskCapabilityUtils.toCurrentValue(
                                                            pendingArg.getValue(), Status.ACCEPTED))
                                    .collect(toImmutableList())));
        }
        List<ParamValue> groundedValues = Collections.synchronizedList(new ArrayList<>());
        List<CurrentValue> ungroundedValues = Collections.synchronizedList(new ArrayList<>());

        ListenableFuture<AppGroundingResult> groundingFuture =
                Futures.immediateFuture(
                        AppGroundingResult.ofSuccess(ParamValue.getDefaultInstance()));

        for (CurrentValue pendingValue : pendingArgs) {
            if (pendingValue.hasDisambiguationData()) {
                // assistant-driven disambiguation
                groundingFuture =
                        consumeGroundingResult(
                                chainAssistantGrounding(groundingFuture, pendingValue,
                                        taskParamBinding, executor),
                                groundedValues,
                                ungroundedValues,
                                executor);
            } else if (taskParamBinding.groundingPredicate().test(pendingValue.getValue())) {
                // app-driven disambiguation
                groundingFuture =
                        consumeGroundingResult(
                                chainAppGrounding(groundingFuture, pendingValue, taskParamBinding,
                                        executor),
                                groundedValues,
                                ungroundedValues,
                                executor);
            } else {
                groundedValues.add(pendingValue.getValue());
            }
        }
        return Futures.transformAsync(
                groundingFuture,
                (unused) -> {
                    if (groundedValues.isEmpty()) {
                        return Futures.immediateFuture(
                                SlotProcessingResult.create(
                                        /** isSuccessful= */
                                        false, Collections.unmodifiableList(ungroundedValues)));
                    }
                    return Futures.transform(
                            invokeValueChange(groundedValues, taskParamBinding),
                            validationResult ->
                                    processValidationResult(validationResult, groundedValues,
                                            ungroundedValues),
                            executor,
                            "validation");
                },
                executor,
                "slot processing result");
    }

    /**
     * Consumes the result of grounding.
     *
     * <p>If grounding was successful (app-driven with 1 returned result) the grounded ParamValue is
     * added to groundedValues.
     *
     * <p>otherwise the ungrounded CurrentValue is added to ungroundedValues.
     */
    static ListenableFuture<AppGroundingResult> consumeGroundingResult(
            ListenableFuture<AppGroundingResult> resultFuture,
            List<ParamValue> groundedValues,
            List<CurrentValue> ungroundedValues,
            Executor executor) {
        return Futures.transform(
                resultFuture,
                appGroundingResult -> {
                    switch (appGroundingResult.getKind()) {
                        case SUCCESS:
                            groundedValues.add(appGroundingResult.success());
                            break;
                        case FAILURE:
                            ungroundedValues.add(appGroundingResult.failure());
                    }
                    return appGroundingResult;
                },
                executor,
                "consume grounding result");
    }

    /** enqueues processing of a pending value that requires assistant-driven grounding. */
    static ListenableFuture<AppGroundingResult> chainAssistantGrounding(
            ListenableFuture<AppGroundingResult> groundingFuture,
            CurrentValue pendingValue,
            TaskParamBinding<?> taskParamBinding,
            Executor executor) {
        return Futures.transformAsync(
                groundingFuture,
                previousResult -> {
                    switch (previousResult.getKind()) {
                        case SUCCESS:
                            return Futures.transform(
                                    renderAssistantDisambigData(
                                            pendingValue.getDisambiguationData(), taskParamBinding),
                                    unused ->
                                            AppGroundingResult.ofFailure(
                                                    CurrentValue.newBuilder(pendingValue).setStatus(
                                                            Status.DISAMBIG).build()),
                                    executor,
                                    "renderAssistantDisambigData");
                        case FAILURE:
                            return Futures.immediateFuture(
                                    AppGroundingResult.ofFailure(pendingValue));
                    }
                    throw new IllegalStateException("unreachable");
                },
                executor,
                "assistant grounding");
    }

    /** enqueues processing of a pending value that requires app-driven grounding. */
    static ListenableFuture<AppGroundingResult> chainAppGrounding(
            ListenableFuture<AppGroundingResult> groundingFuture,
            CurrentValue pendingValue,
            TaskParamBinding<?> taskParamBinding,
            Executor executor) {
        return Futures.transformAsync(
                groundingFuture,
                previousResult -> {
                    switch (previousResult.getKind()) {
                        case SUCCESS:
                            return ground(pendingValue.getValue(), taskParamBinding, executor);
                        case FAILURE:
                            return Futures.immediateFuture(
                                    AppGroundingResult.ofFailure(pendingValue));
                    }
                    throw new IllegalStateException("unreachable");
                },
                executor,
                "app grounding");
    }

    /**
     * Processes the EntitySearchResult from performing an entity search.
     *
     * @param entitySearchResult the EntitySearchResult returned from the app resolver.
     * @param ungroundedValue    the original ungrounded ParamValue.
     */
    private static <T> AppGroundingResult processEntitySearchResult(
            EntitySearchResult<T> entitySearchResult,
            DisambigEntityConverter<T> entityConverter,
            ParamValue ungroundedValue)
            throws StructConversionException {
        switch (entitySearchResult.possibleValues().size()) {
            case 0:
                return AppGroundingResult.ofFailure(
                        TaskCapabilityUtils.toCurrentValue(ungroundedValue, Status.REJECTED));
            case 1:
                Entity groundedEntity =
                        entityConverter.convert(
                                Objects.requireNonNull(entitySearchResult.possibleValues().get(0)));
                return AppGroundingResult.ofSuccess(
                        TaskCapabilityUtils.groundedValueToParamValue(groundedEntity));
            default:
                List<Entity> disambigEntities =
                        getDisambigEntities(entitySearchResult.possibleValues(), entityConverter);
                return AppGroundingResult.ofFailure(
                        TaskCapabilityUtils.getCurrentValueForDisambiguation(
                                ungroundedValue, disambigEntities));
        }
    }

    private static <T> List<Entity> getDisambigEntities(
            List<T> possibleValues, DisambigEntityConverter<T> entityConverter)
            throws StructConversionException {
        List<Entity> disambigEntities = new ArrayList<>();
        for (T entity : possibleValues) {
            disambigEntities.add(entityConverter.convert(Objects.requireNonNull(entity)));
        }
        return Collections.unmodifiableList(disambigEntities);
    }

    /**
     * Processes the ValidationResult from sending argument updates to onReceived.
     *
     * @param validationResult the ValidationResult returned from value listener.
     * @param groundedValues   a List of all grounded ParamValue.
     * @param ungroundedValues a List of all ungrounded CurrentValue.
     */
    private static SlotProcessingResult processValidationResult(
            ValidationResult validationResult,
            List<ParamValue> groundedValues,
            List<CurrentValue> ungroundedValues) {
        List<CurrentValue> combinedValues = new ArrayList<>();
        switch (validationResult.getKind()) {
            case ACCEPTED:
                combinedValues.addAll(
                        TaskCapabilityUtils.paramValuesToCurrentValue(groundedValues,
                                Status.ACCEPTED));
                break;
            case REJECTED:
                combinedValues.addAll(
                        TaskCapabilityUtils.paramValuesToCurrentValue(groundedValues,
                                Status.REJECTED));
                break;
        }
        combinedValues.addAll(ungroundedValues);
        return SlotProcessingResult.create(
                /* isSuccessful= */ ungroundedValues.isEmpty()
                        && (validationResult.getKind() == ValidationResult.Kind.ACCEPTED),
                Collections.unmodifiableList(combinedValues));
    }

    private static ListenableFuture<Void> renderAssistantDisambigData(
            DisambiguationData disambiguationData, TaskParamBinding<?> binding) {
        List<String> entityIds =
                disambiguationData.getEntitiesList().stream()
                        .map(Entity::getIdentifier)
                        .collect(toImmutableList());
        try {
            return binding.resolver().invokeEntityRender(entityIds);
        } catch (InvalidResolverException e) {
            return Futures.immediateFailedFuture(e);
        }
    }
}
