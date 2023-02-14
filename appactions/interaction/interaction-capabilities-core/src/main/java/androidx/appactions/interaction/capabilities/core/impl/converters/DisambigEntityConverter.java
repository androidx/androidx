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

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.proto.Entity;

/**
 * Converter from {@code ValueTypeT} to the app-driven disambig entity i.e. {@code Entity} proto.
 * The ValueTypeT instance is usually a value object provided by the app.
 *
 * @param <ValueTypeT>
 */
@FunctionalInterface
public interface DisambigEntityConverter<ValueTypeT> {
    @NonNull
    Entity convert(ValueTypeT type) throws StructConversionException;
}
