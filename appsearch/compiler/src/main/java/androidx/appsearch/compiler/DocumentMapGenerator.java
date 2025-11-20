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

import static androidx.appsearch.compiler.IntrospectionHelper.RESTRICT_TO_ANNOTATION_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.RESTRICT_TO_SCOPE_CLASS;
import static androidx.room.compiler.codegen.compat.XConverters.toJavaPoet;

import com.google.auto.common.GeneratedAnnotationSpecs;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

/**
 * A class that wraps a static method that generates a java class of
 * {@link androidx.appsearch.app.AppSearchDocumentClassMap}.
 */
public class DocumentMapGenerator {
    /**
     * Returns the generated {@link androidx.appsearch.app.AppSearchDocumentClassMap}, based on
     * the provided document class map.
     *
     * @param documentClassMap                 The map from schema type names to the list of the
     *                                         fully qualified
     *                                         names of the corresponding document classes, so
     *                                         that the
     *                                         {@code getMap} method of the generated class can
     *                                         return this map.
     * @param restrictGeneratedCodeToLib Whether to annotate the generated class with
     *                                   {@code RestrictTo(LIBRARY)}.
     */
    public static @NonNull JavaFile generate(
            @NonNull ProcessingEnvironment processingEnv,
            @NonNull String packageName,
            @NonNull String classSuffix,
            @NonNull Map<String, List<String>> documentClassMap,
            boolean restrictGeneratedCodeToLib) {
        ClassName superClassName = ClassName.get(
                IntrospectionHelper.APPSEARCH_PKG, "AppSearchDocumentClassMap");
        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(IntrospectionHelper.GEN_CLASS_PREFIX + "DocumentClassMap" + "_"
                        + classSuffix)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .superclass(superClassName)
                .addAnnotation(AnnotationSpec.builder(AutoService.class)
                        .addMember("value", "$T.class", superClassName)
                        .build());

        if (restrictGeneratedCodeToLib) {
            // Add @RestrictTo(LIBRARY_GROUP) to the generated class
            genClass.addAnnotation(
                    AnnotationSpec.builder(toJavaPoet(RESTRICT_TO_ANNOTATION_CLASS))
                            .addMember(
                                    /* name= */"value",
                                    "$T.LIBRARY",
                                    toJavaPoet(RESTRICT_TO_SCOPE_CLASS))
                            .build());
        }

        // Add the @Generated annotation to avoid static analysis running on these files
        GeneratedAnnotationSpecs.generatedAnnotationSpec(
                processingEnv.getElementUtils(),
                processingEnv.getSourceVersion(),
                AppSearchCompiler.class
        ).ifPresent(genClass::addAnnotation);

        // The type of the map is Map<String, List<String>>.
        TypeName returnType = ParameterizedTypeName.get(ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)));

        genClass.addMethod(MethodSpec.methodBuilder("getMap")
                .addModifiers(Modifier.PROTECTED)
                .returns(returnType)
                .addAnnotation(NonNull.class)
                .addAnnotation(Override.class)
                .addStatement("$T result = new $T<>()", returnType,
                        ClassName.get(HashMap.class))
                .addCode(getMapConstructionCode(documentClassMap))
                .addStatement("return result")
                .build());

        return JavaFile.builder(packageName, genClass.build()).build();
    }

    private static CodeBlock getMapConstructionCode(
            @NonNull Map<String, List<String>> documentClassMap) {
        CodeBlock.Builder mapContentBuilder = CodeBlock.builder();

        documentClassMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(entry -> {
                    String valueString = entry.getValue().stream().map(
                            value -> "\"" + value + "\"").collect(Collectors.joining(", "));
                    mapContentBuilder.addStatement("result.put($S, $T.asList($L))", entry.getKey(),
                            ClassName.get(Arrays.class), valueString);
                });
        return mapContentBuilder.build();
    }

    private DocumentMapGenerator() {
    }
}
