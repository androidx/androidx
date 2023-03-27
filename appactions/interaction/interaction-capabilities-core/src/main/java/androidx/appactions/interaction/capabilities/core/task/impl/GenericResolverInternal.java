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

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.converters.ParamValueConverter;
import androidx.appactions.interaction.capabilities.core.impl.converters.SlotTypeConverter;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.task.AppEntityListResolver;
import androidx.appactions.interaction.capabilities.core.task.AppEntityResolver;
import androidx.appactions.interaction.capabilities.core.task.EntitySearchResult;
import androidx.appactions.interaction.capabilities.core.task.InventoryListResolver;
import androidx.appactions.interaction.capabilities.core.task.InventoryResolver;
import androidx.appactions.interaction.capabilities.core.task.ValidationResult;
import androidx.appactions.interaction.capabilities.core.task.ValueListListener;
import androidx.appactions.interaction.capabilities.core.task.ValueListener;
import androidx.appactions.interaction.capabilities.core.task.impl.exceptions.InvalidResolverException;
import androidx.appactions.interaction.capabilities.core.values.SearchAction;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.auto.value.AutoOneOf;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

/**
 * A wrapper around all types of slot resolvers (value listeners + disambig resolvers).
 *
 * <p>This allows one type of resolver to be bound for each slot, and abstracts the details of the
 * individual resolvers. It is also the place where repeated fields are handled.
 *
 * @param <ValueTypeT>
 */
@AutoOneOf(GenericResolverInternal.Kind.class)
public abstract class GenericResolverInternal<ValueTypeT> {
    @NonNull
    public static <ValueTypeT> GenericResolverInternal<ValueTypeT> fromValueListener(
            @NonNull ValueListener<ValueTypeT> valueListener) {
        return AutoOneOf_GenericResolverInternal.value(valueListener);
    }

    @NonNull
    public static <ValueTypeT> GenericResolverInternal<ValueTypeT> fromValueListListener(
            @NonNull ValueListListener<ValueTypeT> valueListListener) {
        return AutoOneOf_GenericResolverInternal.valueList(valueListListener);
    }

    @NonNull
    public static <ValueTypeT> GenericResolverInternal<ValueTypeT> fromAppEntityResolver(
            @NonNull AppEntityResolver<ValueTypeT> appEntityResolver) {
        return AutoOneOf_GenericResolverInternal.appEntityResolver(appEntityResolver);
    }

    @NonNull
    public static <ValueTypeT> GenericResolverInternal<ValueTypeT> fromAppEntityListResolver(
            @NonNull AppEntityListResolver<ValueTypeT> appEntityListResolver) {
        return AutoOneOf_GenericResolverInternal.appEntityListResolver(appEntityListResolver);
    }

    @NonNull
    public static <ValueTypeT> GenericResolverInternal<ValueTypeT> fromInventoryResolver(
            @NonNull InventoryResolver<ValueTypeT> inventoryResolver) {
        return AutoOneOf_GenericResolverInternal.inventoryResolver(inventoryResolver);
    }

    @NonNull
    public static <ValueTypeT> GenericResolverInternal<ValueTypeT> fromInventoryListResolver(
            @NonNull InventoryListResolver<ValueTypeT> inventoryListResolverListener) {
        return AutoOneOf_GenericResolverInternal.inventoryListResolver(
                inventoryListResolverListener);
    }

    /** Returns the Kind of this resolver */
    @NonNull
    public abstract Kind getKind();

    abstract ValueListener<ValueTypeT> value();

    abstract ValueListListener<ValueTypeT> valueList();

    abstract AppEntityResolver<ValueTypeT> appEntityResolver();

    abstract AppEntityListResolver<ValueTypeT> appEntityListResolver();

    abstract InventoryResolver<ValueTypeT> inventoryResolver();

    abstract InventoryListResolver<ValueTypeT> inventoryListResolver();

    /** Wrapper which should invoke the `lookupAndRender` provided by the developer. */
    @NonNull
    public ListenableFuture<EntitySearchResult<ValueTypeT>> invokeLookup(
            @NonNull SearchAction<ValueTypeT> searchAction) throws InvalidResolverException {
        switch (getKind()) {
            case APP_ENTITY_RESOLVER:
                return appEntityResolver().lookupAndRender(searchAction);
            case APP_ENTITY_LIST_RESOLVER:
                return appEntityListResolver().lookupAndRender(searchAction);
            default:
                throw new InvalidResolverException(
                        String.format(
                                "invokeLookup is not supported on this resolver of type %s",
                                getKind().name()));
        }
    }

    /**
     * Wrapper which should invoke the EntityRender#renderEntities method when the Assistant is
     * prompting for disambiguation.
     */
    @NonNull
    public ListenableFuture<Void> invokeEntityRender(@NonNull List<String> entityIds)
            throws InvalidResolverException {
        switch (getKind()) {
            case INVENTORY_RESOLVER:
                return inventoryResolver().renderChoices(entityIds);
            case INVENTORY_LIST_RESOLVER:
                return inventoryListResolver().renderChoices(entityIds);
            default:
                throw new InvalidResolverException(
                        String.format(
                                "invokeEntityRender is not supported on this resolver of type %s",
                                getKind().name()));
        }
    }

    /**
     * Notifies the app that a new value for this argument has been set by Assistant. This method
     * should only be called with completely grounded values.
     */
    @NonNull
    public ListenableFuture<ValidationResult> notifyValueChange(
            @NonNull List<ParamValue> paramValues,
            @NonNull ParamValueConverter<ValueTypeT> converter)
            throws StructConversionException {
        SlotTypeConverter<ValueTypeT> singularConverter = SlotTypeConverter.ofSingular(converter);
        SlotTypeConverter<List<ValueTypeT>> repeatedConverter = SlotTypeConverter.ofRepeated(
                converter);

        switch (getKind()) {
            case VALUE:
                return value().onReceived(singularConverter.convert(paramValues));
            case VALUE_LIST:
                return valueList().onReceived(repeatedConverter.convert(paramValues));
            case APP_ENTITY_RESOLVER:
                return appEntityResolver().onReceived(singularConverter.convert(paramValues));
            case APP_ENTITY_LIST_RESOLVER:
                return appEntityListResolver().onReceived(repeatedConverter.convert(paramValues));
            case INVENTORY_RESOLVER:
                return inventoryResolver().onReceived(singularConverter.convert(paramValues));
            case INVENTORY_LIST_RESOLVER:
                return inventoryListResolver().onReceived(repeatedConverter.convert(paramValues));
        }
        throw new IllegalStateException("unreachable");
    }

    /** The kind of resolver. */
    public enum Kind {
        VALUE,
        VALUE_LIST,
        APP_ENTITY_RESOLVER,
        APP_ENTITY_LIST_RESOLVER,
        INVENTORY_RESOLVER,
        INVENTORY_LIST_RESOLVER
    }
}
