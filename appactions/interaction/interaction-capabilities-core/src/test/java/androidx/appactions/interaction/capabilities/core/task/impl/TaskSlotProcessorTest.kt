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
package androidx.appactions.interaction.capabilities.core.task.impl

import androidx.appactions.interaction.capabilities.core.AppEntityListener
import androidx.appactions.interaction.capabilities.core.EntitySearchResult
import androidx.appactions.interaction.capabilities.core.InventoryListener
import androidx.appactions.interaction.capabilities.core.ValidationResult
import androidx.appactions.interaction.capabilities.core.ValueListener
import androidx.appactions.interaction.capabilities.core.impl.concurrent.Futures
import androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters
import androidx.appactions.interaction.capabilities.core.values.SearchAction
import androidx.appactions.interaction.capabilities.testing.internal.ArgumentUtils
import androidx.appactions.interaction.capabilities.testing.internal.SettableFutureWrapper
import androidx.appactions.interaction.proto.CurrentValue
import androidx.appactions.interaction.proto.DisambiguationData
import androidx.appactions.interaction.proto.Entity
import androidx.appactions.interaction.proto.ParamValue
import androidx.appactions.interaction.protobuf.Struct
import androidx.appactions.interaction.protobuf.Value
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TaskSlotProcessorTest {
    private fun <T> createAssistantDisambigResolver(
        validationResult: ValidationResult,
        valueConsumer: (T) -> Unit,
        renderConsumer: (List<String>) -> Unit,
    ): GenericResolverInternal<T> {
        return GenericResolverInternal.fromInventoryListener(
            object : InventoryListener<T> {
                override fun onReceivedAsync(value: T): ListenableFuture<ValidationResult> {
                    valueConsumer.invoke(value)
                    return Futures.immediateFuture(validationResult)
                }

                override fun renderChoicesAsync(entityIDs: List<String>): ListenableFuture<Void> {
                    renderConsumer.invoke(entityIDs)
                    return Futures.immediateVoidFuture()
                }
            },
        )
    }

    private fun <T> createValueResolver(
        validationResult: ValidationResult,
        valueConsumer: (T) -> Unit,
    ): GenericResolverInternal<T> {
        return GenericResolverInternal.fromValueListener(
            object : ValueListener<T> {
                override fun onReceivedAsync(value: T): ListenableFuture<ValidationResult> {
                    valueConsumer.invoke(value)
                    return Futures.immediateFuture(validationResult)
                }
            },
        )
    }

    private fun <T> createValueResolver(
        validationResult: ValidationResult,
    ): GenericResolverInternal<T> {
        return createValueResolver(validationResult) { _: T -> }
    }

    private fun <T> createValueListResolver(
        validationResult: ValidationResult,
        valueConsumer: (List<T>) -> Unit,
    ): GenericResolverInternal<T> {
        return GenericResolverInternal.fromValueListListener(
            object : ValueListener<List<T>> {
                override fun onReceivedAsync(value: List<T>): ListenableFuture<ValidationResult> {
                    valueConsumer.invoke(value)
                    return Futures.immediateFuture(validationResult)
                }
            },
        )
    }

    private fun <T> createAppEntityListener(
        validationResult: ValidationResult,
        valueConsumer: (T) -> Unit,
        appSearchResult: EntitySearchResult<T>,
        appSearchConsumer: (SearchAction<T>) -> Unit,
    ): GenericResolverInternal<T> {
        return GenericResolverInternal.fromAppEntityListener(
            object : AppEntityListener<T> {
                override fun onReceivedAsync(value: T): ListenableFuture<ValidationResult> {
                    valueConsumer.invoke(value)
                    return Futures.immediateFuture(validationResult)
                }

                override fun lookupAndRenderAsync(
                    searchAction: SearchAction<T>,
                ): ListenableFuture<EntitySearchResult<T>> {
                    appSearchConsumer.invoke(searchAction)
                    return Futures.immediateFuture(appSearchResult)
                }
            },
        )
    }

    @Test
    @Throws(Exception::class)
    fun processSlot_singleValue_accepted(): Unit = runBlocking {
        val binding =
            TaskParamBinding(
                "singularValue",
                { false },
                createValueResolver(ValidationResult.newAccepted()),
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                null,
                null,
            )
        val taskParamMap: MutableMap<String, TaskParamBinding<*>> = HashMap()
        taskParamMap["singularValue"] = binding
        val args = listOf(ParamValue.newBuilder().setIdentifier("testValue").build())
        val (isSuccessful, processedValues) =
            TaskSlotProcessor.processSlot(
                "singularValue",
                TaskCapabilityUtils.paramValuesToCurrentValue(args, CurrentValue.Status.PENDING),
                taskParamMap,
            )
        assertThat(isSuccessful).isTrue()
        assertThat(processedValues)
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(args[0])
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
    }

    @Test
    @Throws(Exception::class)
    fun processSlot_singleValue_rejected(): Unit = runBlocking {
        val binding =
            TaskParamBinding(
                "singularValue",
                { false },
                createValueResolver(ValidationResult.newRejected()),
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                null,
                null,
            )
        val taskParamMap: MutableMap<String, TaskParamBinding<*>> = HashMap()
        taskParamMap["singularValue"] = binding
        val args = listOf(ParamValue.newBuilder().setIdentifier("testValue").build())
        val (isSuccessful, processedValues) =
            TaskSlotProcessor.processSlot(
                "singularValue",
                TaskCapabilityUtils.paramValuesToCurrentValue(args, CurrentValue.Status.PENDING),
                taskParamMap,
            )
        assertThat(isSuccessful).isFalse()
        assertThat(processedValues)
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(args[0])
                    .setStatus(CurrentValue.Status.REJECTED)
                    .build(),
            )
    }

    @Test
    @Throws(Exception::class)
    fun processSlot_repeatedValue_accepted(): Unit = runBlocking {
        val lastReceivedArgs = SettableFutureWrapper<List<String?>>()
        val binding =
            TaskParamBinding(
                "repeatedValue",
                { false },
                createValueListResolver(
                    ValidationResult.newAccepted(),
                ) { lastReceivedArgs.set(it) },
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                null,
                null,
            )
        val taskParamMap: MutableMap<String, TaskParamBinding<*>> = HashMap()
        taskParamMap["repeatedValue"] = binding
        val args =
            listOf(
                ParamValue.newBuilder().setIdentifier("testValue1").build(),
                ParamValue.newBuilder().setIdentifier("testValue2").build(),
            )
        val (isSuccessful, processedValues) =
            TaskSlotProcessor.processSlot(
                "repeatedValue",
                TaskCapabilityUtils.paramValuesToCurrentValue(args, CurrentValue.Status.PENDING),
                taskParamMap,
            )
        assertThat(isSuccessful).isTrue()
        assertThat(processedValues)
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(args[0])
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
                CurrentValue.newBuilder()
                    .setValue(args[1])
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .build(),
            )
        assertThat(lastReceivedArgs.getFuture().get()).isEqualTo(
            listOf(
                "testValue1",
                "testValue2",
            ),
        )
    }

    @Test
    @Throws(Exception::class)
    fun processSlot_repeatedValue_rejected(): Unit = runBlocking {
        val lastReceivedArgs = SettableFutureWrapper<List<String>>()
        val binding =
            TaskParamBinding(
                "repeatedValue",
                { false },
                createValueListResolver(
                    ValidationResult.newRejected(),
                ) { lastReceivedArgs.set(it) },
                TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                null,
                null,
            )
        val taskParamMap: MutableMap<String, TaskParamBinding<*>> = HashMap()
        taskParamMap["repeatedValue"] = binding
        val args =
            listOf(
                ParamValue.newBuilder().setIdentifier("testValue1").build(),
                ParamValue.newBuilder().setIdentifier("testValue2").build(),
            )
        val (isSuccessful, processedValues) =
            TaskSlotProcessor.processSlot(
                "repeatedValue",
                TaskCapabilityUtils.paramValuesToCurrentValue(args, CurrentValue.Status.PENDING),
                taskParamMap,
            )
        assertThat(isSuccessful).isFalse()
        assertThat(processedValues)
            .containsExactly(
                CurrentValue.newBuilder()
                    .setValue(args[0])
                    .setStatus(CurrentValue.Status.REJECTED)
                    .build(),
                CurrentValue.newBuilder()
                    .setValue(args[1])
                    .setStatus(CurrentValue.Status.REJECTED)
                    .build(),
            )
        assertThat(lastReceivedArgs.getFuture().get()).isEqualTo(
            listOf(
                "testValue1",
                "testValue2",
            ),
        )
    }

    @Test
    @Throws(Exception::class)
    fun listValues_oneAccepted_oneAssistantDisambig_invokesRendererAndOnReceived(): Unit =
        runBlocking {
            val onReceivedCb = SettableFutureWrapper<String>()
            val renderCb = SettableFutureWrapper<List<String?>>()
            val binding =
                TaskParamBinding(
                    "assistantDrivenSlot",
                    { !it.hasIdentifier() },
                    createAssistantDisambigResolver(
                        ValidationResult.newAccepted(),
                        { onReceivedCb.set(it) },
                    ) {
                        renderCb.set(it)
                    },
                    TypeConverters.STRING_PARAM_VALUE_CONVERTER,
                    null,
                    null,
                )
            val taskParamMap: MutableMap<String, TaskParamBinding<*>> = HashMap()
            taskParamMap["assistantDrivenSlot"] = binding
            val previouslyAccepted =
                CurrentValue.newBuilder()
                    .setStatus(CurrentValue.Status.ACCEPTED)
                    .setValue(
                        ParamValue.newBuilder()
                            .setIdentifier("id")
                            .setStructValue(
                                Struct.newBuilder()
                                    .putFields(
                                        "id",
                                        Value.newBuilder().setStringValue("1234").build(),
                                    ),
                            ),
                    )
                    .build()
            val values =
                listOf(
                    previouslyAccepted,
                    CurrentValue.newBuilder()
                        .setStatus(CurrentValue.Status.PENDING)
                        .setDisambiguationData(
                            DisambiguationData.newBuilder()
                                .addEntities(Entity.newBuilder().setIdentifier("entity-1"))
                                .addEntities(Entity.newBuilder().setIdentifier("entity-2")),
                        )
                        .build(),
                )
            val (isSuccessful, processedValues) =
                TaskSlotProcessor.processSlot("assistantDrivenSlot", values, taskParamMap)
            assertThat(isSuccessful).isFalse()
            assertThat(onReceivedCb.getFuture().get()).isEqualTo("id")
            assertThat(renderCb.getFuture().get()).isEqualTo(listOf("entity-1", "entity-2"))
            assertThat(processedValues)
                .containsExactly(
                    previouslyAccepted,
                    CurrentValue.newBuilder()
                        .setStatus(CurrentValue.Status.DISAMBIG)
                        .setDisambiguationData(
                            DisambiguationData.newBuilder()
                                .addEntities(Entity.newBuilder().setIdentifier("entity-1"))
                                .addEntities(Entity.newBuilder().setIdentifier("entity-2")),
                        )
                        .build(),
                )
        }

    @Test
    @Throws(Exception::class)
    fun singularValue_appDisambigRejected_onReceivedNotCalled(): Unit = runBlocking {
        val onReceivedCb = SettableFutureWrapper<String>()
        val appSearchCb = SettableFutureWrapper<SearchAction<String>>()
        val entitySearchResult = EntitySearchResult.Builder<String>().build()
        val resolver =
            createAppEntityListener(
                ValidationResult.newAccepted(),
                { result: String -> onReceivedCb.set(result) },
                entitySearchResult,
            ) { result: SearchAction<String> ->
                appSearchCb.set(result)
            }
        val binding =
            TaskParamBinding(
                "appDrivenSlot",
                { true }, // always invoke app-grounding in all cases
                resolver,
                TypeConverters.STRING_PARAM_VALUE_CONVERTER, // Not invoked
                { Entity.getDefaultInstance() },
            ) {
                SearchAction.newBuilder<String>().setQuery("A").setObject("nested").build()
            }
        val taskParamMap: MutableMap<String, TaskParamBinding<*>> = HashMap()
        taskParamMap["appDrivenSlot"] = binding
        val values =
            listOf(
                CurrentValue.newBuilder()
                    .setStatus(CurrentValue.Status.PENDING)
                    .setValue(ArgumentUtils.buildSearchActionParamValue("A"))
                    .build(),
            )
        val (isSuccessful, processedValues) =
            TaskSlotProcessor.processSlot("appDrivenSlot", values, taskParamMap)
        assertThat(isSuccessful).isFalse()

        assertThat(onReceivedCb.getFuture().isDone).isFalse()
        assertThat(appSearchCb.getFuture().isDone).isTrue()
        assertThat(appSearchCb.getFuture().get())
            .isEqualTo(SearchAction.newBuilder<String>().setQuery("A").setObject("nested").build())
        assertThat(processedValues)
            .containsExactly(
                CurrentValue.newBuilder()
                    .setStatus(CurrentValue.Status.REJECTED)
                    .setValue(ArgumentUtils.buildSearchActionParamValue("A"))
                    .build(),
            )
    }
}
