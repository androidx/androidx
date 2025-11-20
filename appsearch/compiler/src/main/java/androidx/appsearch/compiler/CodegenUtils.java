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

package androidx.appsearch.compiler;

import static androidx.room.compiler.processing.compat.XConverters.toJavac;

import androidx.room.compiler.processing.XArrayType;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.XType;

import com.squareup.javapoet.CodeBlock;

import org.jspecify.annotations.NonNull;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Utils for generating Java code.
 */
final class CodegenUtils {

    private CodegenUtils() {
    }

    /**
     * Returns an expr of the form {@code new ComponentType[size]}.
     *
     * <p>Gracefully handles the case where the component type itself is an array type.
     * For example, given component type is {@code byte[]} returns {@code new byte[size][]}.
     */
    static CodeBlock createNewArrayExpr(
            @NonNull XType componentType,
            @NonNull CodeBlock size,
            @NonNull XProcessingEnv env) {
        XArrayType arrayType = env.getArrayType(componentType);
        TypeMirror innerMostType = toJavac(arrayType.getComponentType());
        int dims = 1;
        while (innerMostType.getKind() == TypeKind.ARRAY) {
            innerMostType = ((ArrayType) innerMostType).getComponentType();
            dims++;
        }
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T[$L]", innerMostType, size);
        for (int i = 1; i < dims; i++) {
            codeBlock.add("[]");
        }
        return codeBlock.build();
    }
}
