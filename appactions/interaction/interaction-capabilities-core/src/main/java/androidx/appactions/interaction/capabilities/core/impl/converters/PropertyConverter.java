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
import androidx.appactions.interaction.capabilities.core.properties.ParamProperty;
import androidx.appactions.interaction.capabilities.core.properties.SimpleProperty;
import androidx.appactions.interaction.capabilities.core.properties.StringValue;
import androidx.appactions.interaction.capabilities.core.properties.TypeProperty;
import androidx.appactions.interaction.proto.AppActionsContext.IntentParameter;
import androidx.appactions.interaction.proto.Entity;

import java.util.List;

/** Contains utility functions that convert properties to IntentParameter proto. */
public final class PropertyConverter {

    private PropertyConverter() {}

    /** Create IntentParameter proto from a SimpleProperty. */
    @NonNull
    public static IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull SimpleProperty property) {
        return newIntentParameterBuilder(paramName, property).build();
    }

    /** Create IntentParameter proto from a TypeProperty. */
    @NonNull
    public static <T> IntentParameter getIntentParameter(
            @NonNull String paramName, @NonNull TypeProperty<T> property,
            @NonNull EntityConverter<T> entityConverter) {
        IntentParameter.Builder builder = newIntentParameterBuilder(paramName, property);
        extractPossibleValues(property, entityConverter).stream()
                .forEach(builder::addPossibleEntities);
        return builder.build();
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
            ParamProperty<T> property, EntityConverter<T> function) {
        return property.getPossibleValues().stream()
                .map(function::convert)
                .collect(toImmutableList());
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
     * Converts a capabilities library [PossibleStringValue] to a appactions Entity proto .
     */
    @NonNull
    public static Entity stringValueToProto(@NonNull StringValue possibleValue) {
        return Entity.newBuilder()
                .setIdentifier(possibleValue.getName())
                .setName(possibleValue.getName())
                .addAllAlternateNames(possibleValue.getAlternateNames())
                .build();
    }
}
