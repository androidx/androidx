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

package androidx.appactions.interaction.capabilities.core.impl.converters;

import static androidx.appactions.interaction.capabilities.core.impl.utils.ImmutableCollectors.toImmutableList;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.properties.EntityProperty;
import androidx.appactions.interaction.capabilities.core.properties.EnumProperty;
import androidx.appactions.interaction.capabilities.core.properties.IntegerProperty;
import androidx.appactions.interaction.capabilities.core.properties.ParamProperty;
import androidx.appactions.interaction.capabilities.core.properties.SimpleProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringOrEnumProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringProperty.PossibleValue;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.Entity;

import java.util.List;
import java.util.function.Function;

/** Contains utility functions that convert properties to IntentParameter proto. */
public final class PropertyConverter {

    private PropertyConverter() {}

    /** Create IntentParameter proto from a StringProperty. */
    @NonNull
    public static IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull StringProperty property) {
        IntentParameter.Builder builder = newIntentParameterBuilder(paramName, property);
        extractPossibleValues(property, PropertyConverter::possibleValueToProto).stream()
                .forEach(builder::addPossibleEntities);
        return builder.build();
    }

    /** Create IntentParameter proto from a StringOrEnumProperty. */
    @NonNull
    public static <EnumT extends Enum<EnumT>> IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull StringOrEnumProperty<EnumT> property) {
        IntentParameter.Builder builder = newIntentParameterBuilder(paramName, property);
        extractPossibleValues(property, PropertyConverter::possibleValueToProto).stream()
                .forEach(builder::addPossibleEntities);
        return builder.build();
    }

    /** Create IntentParameter proto from a EntityProperty. */
    @NonNull
    public static IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull EntityProperty property) {
        IntentParameter.Builder builder = newIntentParameterBuilder(paramName, property);
        extractPossibleValues(property, PropertyConverter::entityToProto).stream()
                .forEach(builder::addPossibleEntities);
        return builder.build();
    }

    /** Create IntentParameter proto from a EnumProperty. */
    @NonNull
    public static <EnumT extends Enum<EnumT>> IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull EnumProperty<EnumT> property) {
        IntentParameter.Builder builder = newIntentParameterBuilder(paramName, property);
        extractPossibleValues(property, PropertyConverter::enumToProto).stream()
                .forEach(builder::addPossibleEntities);
        return builder.build();
    }

    /** Create IntentParameter proto from a IntegerProperty. */
    @NonNull
    public static IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull IntegerProperty property) {
        return newIntentParameterBuilder(paramName, property).build();
    }

    /** Create IntentParameter proto from a SimpleProperty. */
    @NonNull
    public static IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull SimpleProperty property) {
        return newIntentParameterBuilder(paramName, property).build();
    }

    /** Create IntentParameter.Builder from a generic ParamProperty, fills in the common fields. */
    private static IntentParameter.Builder newIntentParameterBuilder(
            String paramName, ParamProperty<?> paramProperty) {
        return IntentParameter.newBuilder()
                .setName(paramName)
                .setIsRequired(paramProperty.isRequired())
                .setEntityMatchRequired(paramProperty.isValueMatchRequired())
                .setIsProhibited(paramProperty.isProhibited());
    }

    private static <T> List<Entity> extractPossibleValues(
            ParamProperty<T> property, Function<T, Entity> function) {
        return property.getPossibleValues().stream().map(function).collect(toImmutableList());
    }

    /** Converts a properties/Entity to a appactions Entity proto. */
    @NonNull
    public static Entity entityToProto(
            @NonNull androidx.appactions.interaction.capabilities.core.properties.Entity entity) {
        Entity.Builder builder =
                Entity.newBuilder()
                        .setName(entity.getName())
                        .addAllAlternateNames(entity.getAlternateNames());
        if (entity.getId() != null) {
            builder.setIdentifier(entity.getId());
        }
        return builder.build();
    }

    /**
     * Converts a capabilities library StringProperty.PossibleValue to a appactions Entity proto .
     */
    @NonNull
    public static Entity possibleValueToProto(@NonNull PossibleValue possibleValue) {
        return androidx.appactions.interaction.proto.Entity.newBuilder()
                .setIdentifier(possibleValue.getName())
                .setName(possibleValue.getName())
                .addAllAlternateNames(possibleValue.getAlternateNames())
                .build();
    }

    /**
     * Converts a capabilities library StringOrEnumProperty.PossibleValue to a appactions Entity
     * proto.
     */
    @NonNull
    public static <EnumT extends Enum<EnumT>> Entity possibleValueToProto(
            @NonNull StringOrEnumProperty.PossibleValue<EnumT> possibleValue) {

        switch (possibleValue.getKind()) {
            case STRING_VALUE:
                return possibleValueToProto(possibleValue.stringValue());
            case ENUM_VALUE:
                return enumToProto(possibleValue.enumValue());
        }
        throw new IllegalStateException("unreachable");
    }

    @NonNull
    public static <EnumT extends Enum<EnumT>> Entity enumToProto(EnumT enumValue) {
        return androidx.appactions.interaction.proto.Entity.newBuilder()
                .setIdentifier(enumValue.toString())
                .build();
    }
}
