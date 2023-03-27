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

import static androidx.appactions.interaction.capabilities.core.testing.ArgumentUtils.buildSearchActionParamValue;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures;
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters;
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver;
import androidx.appactions.interaction.capabilities.core.task.EntitySearchResult;
import androidx.appactions.interaction.capabilities.core.task.InventoryResolver;
import androidx.appactions.interaction.capabilities.core.task.ValidationResult;
import androidx.appactions.interaction.capabilities.core.task.ValueListListener;
import androidx.appactions.interaction.capabilities.core.task.ValueListener;
import androidx.appactions.interaction.capabilities.core.testing.spec.SettableFutureWrapper;
import androidx.appactions.interaction.capabilities.core.values.SearchAction;
import androidx.appactions.interaction.proto.CurrentValue;
import androidx.appactions.interaction.proto.CurrentValue.Status;
import androidx.appactions.interaction.proto.DisambiguationData;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@RunWith(JUnit4.class)
public final class TaskSlotProcessorTest {

    private <T> GenericResolverInternal<T> createAssistantDisambigResolver(
            ValidationResult validationResult,
            Consumer<T> valueConsumer,
            Consumer<List<String>> renderConsumer) {
        return GenericResolverInternal.fromInventoryResolver(
                new InventoryResolver<T>() {
                    @NonNull
                    @Override
                    public ListenableFuture<ValidationResult> onReceived(T value) {
                        valueConsumer.accept(value);
                        return Futures.immediateFuture(validationResult);
                    }

                    @NonNull
                    @Override
                    public ListenableFuture<Void> renderChoices(@NonNull List<String> entityIDs) {
                        renderConsumer.accept(entityIDs);
                        return Futures.immediateVoidFuture();
                    }
                });
    }

    private <T> GenericResolverInternal<T> createValueResolver(
            ValidationResult validationResult, Consumer<T> valueConsumer) {
        return GenericResolverInternal.fromValueListener(
                new ValueListener<T>() {
                    @NonNull
                    @Override
                    public ListenableFuture<ValidationResult> onReceived(T value) {
                        valueConsumer.accept(value);
                        return Futures.immediateFuture(validationResult);
                    }
                });
    }

    private <T> GenericResolverInternal<T> createValueResolver(ValidationResult validationResult) {
        return createValueResolver(validationResult, (unused) -> {
        });
    }

    private <T> GenericResolverInternal<T> createValueListResolver(
            ValidationResult validationResult, Consumer<List<T>> valueConsumer) {
        return GenericResolverInternal.fromValueListListener(
                new ValueListListener<T>() {
                    @NonNull
                    @Override
                    public ListenableFuture<ValidationResult> onReceived(List<T> value) {
                        valueConsumer.accept(value);
                        return Futures.immediateFuture(validationResult);
                    }
                });
    }

    private <T> GenericResolverInternal<T> createAppEntityResolver(
            ValidationResult validationResult,
            Consumer<T> valueConsumer,
            EntitySearchResult<T> appSearchResult,
            Consumer<SearchAction<T>> appSearchConsumer) {
        return GenericResolverInternal.fromAppEntityResolver(
                new AppEntityResolver<T>() {
                    @NonNull
                    @Override
                    public ListenableFuture<ValidationResult> onReceived(T value) {
                        valueConsumer.accept(value);
                        return Futures.immediateFuture(validationResult);
                    }

                    @Override
                    public ListenableFuture<EntitySearchResult<T>> lookupAndRender(
                            SearchAction<T> searchAction) {
                        appSearchConsumer.accept(searchAction);
                        return Futures.immediateFuture(appSearchResult);
                    }
                });
    }

    @Test
    public void processSlot_singleValue_accepted() throws Exception {
        TaskParamRegistry paramRegistry =
                TaskParamRegistry.builder()
                        .addTaskParameter(
                                "singularValue",
                                (paramValue) -> false,
                                createValueResolver(ValidationResult.newAccepted()),
                                Optional.empty(),
                                Optional.empty(),
                                TypeConverters::toStringValue)
                        .build();
        List<ParamValue> args =
                Collections.singletonList(
                        ParamValue.newBuilder().setIdentifier("testValue").build());

        SlotProcessingResult result =
                TaskSlotProcessor.processSlot(
                                "singularValue",
                                TaskCapabilityUtils.paramValuesToCurrentValue(args, Status.PENDING),
                                paramRegistry,
                                Runnable::run)
                        .get();

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.processedValues())
                .containsExactly(
                        CurrentValue.newBuilder().setValue(args.get(0)).setStatus(
                                Status.ACCEPTED).build());
    }

    @Test
    public void processSlot_singleValue_rejected() throws Exception {
        TaskParamRegistry paramRegistry =
                TaskParamRegistry.builder()
                        .addTaskParameter(
                                "singularValue",
                                (paramValue) -> false,
                                createValueResolver(ValidationResult.newRejected()),
                                Optional.empty(),
                                Optional.empty(),
                                TypeConverters::toStringValue)
                        .build();
        List<ParamValue> args =
                Collections.singletonList(
                        ParamValue.newBuilder().setIdentifier("testValue").build());

        SlotProcessingResult result =
                TaskSlotProcessor.processSlot(
                                "singularValue",
                                TaskCapabilityUtils.paramValuesToCurrentValue(args, Status.PENDING),
                                paramRegistry,
                                Runnable::run)
                        .get();

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.processedValues())
                .containsExactly(
                        CurrentValue.newBuilder().setValue(args.get(0)).setStatus(
                                Status.REJECTED).build());
    }

    @Test
    public void processSlot_repeatedValue_accepted() throws Exception {
        SettableFutureWrapper<List<String>> lastReceivedArgs = new SettableFutureWrapper<>();
        TaskParamRegistry paramRegistry =
                TaskParamRegistry.builder()
                        .addTaskParameter(
                                "repeatedValue",
                                (paramValue) -> false,
                                createValueListResolver(ValidationResult.newAccepted(),
                                        lastReceivedArgs::set),
                                Optional.empty(),
                                Optional.empty(),
                                TypeConverters::toStringValue)
                        .build();
        List<ParamValue> args =
                Arrays.asList(
                        ParamValue.newBuilder().setIdentifier("testValue1").build(),
                        ParamValue.newBuilder().setIdentifier("testValue2").build());

        SlotProcessingResult result =
                TaskSlotProcessor.processSlot(
                                "repeatedValue",
                                TaskCapabilityUtils.paramValuesToCurrentValue(args, Status.PENDING),
                                paramRegistry,
                                Runnable::run)
                        .get();

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.processedValues())
                .containsExactly(
                        CurrentValue.newBuilder().setValue(args.get(0)).setStatus(
                                Status.ACCEPTED).build(),
                        CurrentValue.newBuilder().setValue(args.get(1)).setStatus(
                                Status.ACCEPTED).build());
        assertThat(lastReceivedArgs.getFuture().get()).containsExactly("testValue1", "testValue2");
    }

    @Test
    public void processSlot_repeatedValue_rejected() throws Exception {
        SettableFutureWrapper<List<String>> lastReceivedArgs = new SettableFutureWrapper<>();
        TaskParamRegistry paramRegistry =
                TaskParamRegistry.builder()
                        .addTaskParameter(
                                "repeatedValue",
                                (paramValue) -> false,
                                createValueListResolver(ValidationResult.newRejected(),
                                        lastReceivedArgs::set),
                                Optional.empty(),
                                Optional.empty(),
                                TypeConverters::toStringValue)
                        .build();
        List<ParamValue> args =
                Arrays.asList(
                        ParamValue.newBuilder().setIdentifier("testValue1").build(),
                        ParamValue.newBuilder().setIdentifier("testValue2").build());

        SlotProcessingResult result =
                TaskSlotProcessor.processSlot(
                                "repeatedValue",
                                TaskCapabilityUtils.paramValuesToCurrentValue(args, Status.PENDING),
                                paramRegistry,
                                Runnable::run)
                        .get();

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.processedValues())
                .containsExactly(
                        CurrentValue.newBuilder().setValue(args.get(0)).setStatus(
                                Status.REJECTED).build(),
                        CurrentValue.newBuilder().setValue(args.get(1)).setStatus(
                                Status.REJECTED).build());
        assertThat(lastReceivedArgs.getFuture().get()).containsExactly("testValue1", "testValue2");
    }

    @Test
    public void listValues_oneAccepted_oneAssistantDisambig_invokesRendererAndOnReceived()
            throws Exception {
        SettableFutureWrapper<String> onReceivedCb = new SettableFutureWrapper<>();
        SettableFutureWrapper<List<String>> renderCb = new SettableFutureWrapper<>();
        TaskParamRegistry paramRegistry =
                TaskParamRegistry.builder()
                        .addTaskParameter(
                                "assistantDrivenSlot",
                                (paramValue) -> !paramValue.hasIdentifier(),
                                createAssistantDisambigResolver(
                                        ValidationResult.newAccepted(), onReceivedCb::set,
                                        renderCb::set),
                                Optional.empty(),
                                Optional.empty(),
                                TypeConverters::toStringValue)
                        .build();
        CurrentValue previouslyAccepted =
                CurrentValue.newBuilder()
                        .setStatus(Status.ACCEPTED)
                        .setValue(
                                ParamValue.newBuilder()
                                        .setIdentifier("id")
                                        .setStructValue(
                                                Struct.newBuilder()
                                                        .putFields("id",
                                                                Value.newBuilder().setStringValue(
                                                                        "1234").build())))
                        .build();
        List<CurrentValue> values =
                Arrays.asList(
                        previouslyAccepted,
                        CurrentValue.newBuilder()
                                .setStatus(Status.PENDING)
                                .setDisambiguationData(
                                        DisambiguationData.newBuilder()
                                                .addEntities(Entity.newBuilder().setIdentifier(
                                                        "entity-1"))
                                                .addEntities(Entity.newBuilder().setIdentifier(
                                                        "entity-2")))
                                .build());

        SlotProcessingResult result =
                TaskSlotProcessor.processSlot("assistantDrivenSlot", values, paramRegistry,
                                Runnable::run)
                        .get();

        assertThat(result.isSuccessful()).isFalse();
        assertThat(onReceivedCb.getFuture().get()).isEqualTo("id");
        assertThat(renderCb.getFuture().get()).containsExactly("entity-1", "entity-2");
        assertThat(result.processedValues())
                .containsExactly(
                        previouslyAccepted,
                        CurrentValue.newBuilder()
                                .setStatus(Status.DISAMBIG)
                                .setDisambiguationData(
                                        DisambiguationData.newBuilder()
                                                .addEntities(Entity.newBuilder().setIdentifier(
                                                        "entity-1"))
                                                .addEntities(Entity.newBuilder().setIdentifier(
                                                        "entity-2")))
                                .build());
    }

    @Test
    public void singularValue_appDisambigRejected_onReceivedNotCalled() throws Exception {
        SettableFutureWrapper<String> onReceivedCb = new SettableFutureWrapper<>();
        SettableFutureWrapper<SearchAction<String>> appSearchCb = new SettableFutureWrapper<>();
        EntitySearchResult<String> entitySearchResult = EntitySearchResult.empty();
        GenericResolverInternal<String> resolver =
                createAppEntityResolver(
                        ValidationResult.newAccepted(), // should not be invoked.
                        onReceivedCb::set,
                        entitySearchResult, // app-grounding returns REJECTED in all cases
                        appSearchCb::set);
        TaskParamRegistry paramRegistry =
                TaskParamRegistry.builder()
                        .addTaskParameter(
                                "appDrivenSlot",
                                (paramValue) -> true, // always invoke app-grounding in all cases
                                resolver,
                                Optional.of((unused) -> Entity.getDefaultInstance()),
                                Optional.of(
                                        (unused) ->
                                                SearchAction.<String>newBuilder()
                                                        .setQuery("A")
                                                        .setObject("nested")
                                                        .build()),
                                TypeConverters::toStringValue) // Not invoked
                        .build();
        List<CurrentValue> values =
                Arrays.asList(
                        CurrentValue.newBuilder()
                                .setStatus(Status.PENDING)
                                .setValue(buildSearchActionParamValue("A"))
                                .build());

        SlotProcessingResult result =
                TaskSlotProcessor.processSlot("appDrivenSlot", values, paramRegistry,
                        Runnable::run).get();

        assertThat(result.isSuccessful()).isFalse();
        assertThat(onReceivedCb.getFuture().isDone()).isFalse();
        assertThat(appSearchCb.getFuture().isDone()).isTrue();
        assertThat(appSearchCb.getFuture().get())
                .isEqualTo(SearchAction.<String>newBuilder().setQuery("A").setObject(
                        "nested").build());
        assertThat(result.processedValues())
                .containsExactly(
                        CurrentValue.newBuilder()
                                .setStatus(Status.REJECTED)
                                .setValue(buildSearchActionParamValue("A"))
                                .build());
    }
}
