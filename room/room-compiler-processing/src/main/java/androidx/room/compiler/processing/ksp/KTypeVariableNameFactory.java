/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing.ksp;

import com.squareup.kotlinpoet.TypeName;
import com.squareup.kotlinpoet.TypeVariableName;

import java.util.List;

final class KTypeVariableNameFactory {
    private KTypeVariableNameFactory() {
    }

    /**
     * Calls the internal companion object of KTypeVariableName which receives a list.
     * We use this in {@link KSTypeJavaPoetExtKt#createModifiableTypeVariableName(String, List)}
     * to create a {@link com.squareup.kotlinpoet.TypeVariableName} whose bounds can be modified
     * afterwards.
     */
    @SuppressWarnings("KotlinInternalInJava")
    static TypeVariableName newInstance(String name, List<TypeName> bounds) {
        return TypeVariableName.Companion.of$kotlinpoet(name, bounds, null);
    }
}
