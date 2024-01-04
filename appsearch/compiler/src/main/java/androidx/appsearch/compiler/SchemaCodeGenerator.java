/*
 * Copyright 2020 The Android Open Source Project
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

import static androidx.appsearch.compiler.IntrospectionHelper.APPSEARCH_EXCEPTION_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.APPSEARCH_SCHEMA_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.PROPERTY_CONFIG_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.getDocumentClassFactoryForClass;

import androidx.annotation.NonNull;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.DocumentPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.LongPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.StringPropertyAnnotation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.util.Collections;
import java.util.LinkedHashSet;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

/** Generates java code for an {@link androidx.appsearch.app.AppSearchSchema}. */
class SchemaCodeGenerator {
    private final DocumentModel mModel;
    private final LinkedHashSet<TypeElement> mDependencyDocumentClasses;

    public static void generate(
            @NonNull ProcessingEnvironment env,
            @NonNull DocumentModel model,
            @NonNull TypeSpec.Builder classBuilder) throws ProcessingException {
        new SchemaCodeGenerator(model, env).generate(classBuilder);
    }

    private SchemaCodeGenerator(@NonNull DocumentModel model, @NonNull ProcessingEnvironment env) {
        mModel = model;
        mDependencyDocumentClasses = computeDependencyClasses(model, env);
    }

    @NonNull
    private static LinkedHashSet<TypeElement> computeDependencyClasses(
            @NonNull DocumentModel model,
            @NonNull ProcessingEnvironment env) {
        LinkedHashSet<TypeElement> dependencies = new LinkedHashSet<>(model.getParentTypes());
        for (AnnotatedGetterOrField getterOrField : model.getAnnotatedGettersAndFields()) {
            if (!(getterOrField.getAnnotation() instanceof DocumentPropertyAnnotation)) {
                continue;
            }

            TypeMirror documentClass = getterOrField.getComponentType();
            dependencies.add((TypeElement) env.getTypeUtils().asElement(documentClass));
        }
        return dependencies;
    }

    private void generate(@NonNull TypeSpec.Builder classBuilder) throws ProcessingException {
        classBuilder.addField(
                FieldSpec.builder(String.class, "SCHEMA_NAME")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", mModel.getSchemaName())
                        .build());

        classBuilder.addMethod(
                MethodSpec.methodBuilder("getSchemaName")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addAnnotation(Override.class)
                        .addStatement("return SCHEMA_NAME")
                        .build());

        classBuilder.addMethod(
                MethodSpec.methodBuilder("getSchema")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(APPSEARCH_SCHEMA_CLASS)
                        .addAnnotation(Override.class)
                        .addException(APPSEARCH_EXCEPTION_CLASS)
                        .addStatement("return $L", createSchemaInitializerGetDocumentTypes())
                        .build());

        classBuilder.addMethod(createDependencyClassesMethod());
    }

    @NonNull
    private MethodSpec createDependencyClassesMethod() {
        TypeName listOfClasses = ParameterizedTypeName.get(ClassName.get("java.util", "List"),
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(Object.class)));

        TypeName arrayListOfClasses =
                ParameterizedTypeName.get(ClassName.get("java.util", "ArrayList"),
                        ParameterizedTypeName.get(ClassName.get(Class.class),
                                WildcardTypeName.subtypeOf(Object.class)));

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getDependencyDocumentClasses")
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfClasses)
                .addAnnotation(Override.class)
                .addException(APPSEARCH_EXCEPTION_CLASS);

        if (mDependencyDocumentClasses.isEmpty()) {
            methodBuilder.addStatement("return $T.emptyList()", ClassName.get(Collections.class));
        } else {
            methodBuilder.addStatement("$T classSet = new $T()", listOfClasses, arrayListOfClasses);
            for (TypeElement dependencyType : mDependencyDocumentClasses) {
                methodBuilder.addStatement("classSet.add($T.class)", ClassName.get(dependencyType));
            }
            methodBuilder.addStatement("return classSet").build();
        }

        return methodBuilder.build();
    }

    /**
     * Creates an expr of type {@link androidx.appsearch.app.AppSearchSchema}.
     *
     * <p>The AppSearchSchema has parent types and various Document.*Properties set.
     */
    private CodeBlock createSchemaInitializerGetDocumentTypes() throws ProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T(SCHEMA_NAME)", APPSEARCH_SCHEMA_CLASS.nestedClass("Builder"))
                .indent();
        for (TypeElement parentType : mModel.getParentTypes()) {
            ClassName parentDocumentFactoryClass =
                    getDocumentClassFactoryForClass(ClassName.get(parentType));
            codeBlock.add("\n.addParentType($T.SCHEMA_NAME)", parentDocumentFactoryClass);
        }

        for (AnnotatedGetterOrField getterOrField : mModel.getAnnotatedGettersAndFields()) {
            if (!(getterOrField.getAnnotation() instanceof DataPropertyAnnotation)) {
                continue;
            }

            CodeBlock propertyConfigExpr = createPropertyConfig(
                    (DataPropertyAnnotation) getterOrField.getAnnotation(), getterOrField);
            codeBlock.add("\n.addProperty($L)", propertyConfigExpr);
        }

        codeBlock.add("\n.build()").unindent();
        return codeBlock.build();
    }

    /**
     * Produces an expr for the creating the property's config e.g.
     *
     * <pre>
     * {@code
     * new StringPropertyConfig.Builder("someProp")
     *   .setCardinality(StringPropertyConfig.CARDINALITY_REPEATED)
     *   .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
     *   .build()
     * }
     * </pre>
     */
    private CodeBlock createPropertyConfig(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        if (annotation.getDataPropertyKind() == DataPropertyAnnotation.Kind.DOCUMENT_PROPERTY) {
            ClassName documentClass = (ClassName) ClassName.get(getterOrField.getComponentType());
            ClassName documentFactoryClass = getDocumentClassFactoryForClass(documentClass);
            codeBlock.add("new $T.Builder($S, $T.SCHEMA_NAME)",
                    DocumentPropertyAnnotation.CONFIG_CLASS,
                    annotation.getName(),
                    documentFactoryClass);
        } else {
            // All other property configs have a single param constructor that just takes the
            // property's serialized name as input
            codeBlock.add("new $T.Builder($S)",
                    annotation.getConfigClassName(), annotation.getName());
        }
        codeBlock.indent().add(createSetCardinalityExpr(annotation, getterOrField));
        switch (annotation.getDataPropertyKind()) {
            case STRING_PROPERTY:
                StringPropertyAnnotation stringPropertyAnnotation =
                        (StringPropertyAnnotation) annotation;
                codeBlock.add(createSetTokenizerTypeExpr(stringPropertyAnnotation, getterOrField))
                        .add(createSetIndexingTypeExpr(stringPropertyAnnotation, getterOrField))
                        .add(createSetJoinableValueTypeExpr(
                                stringPropertyAnnotation, getterOrField));
                break;
            case DOCUMENT_PROPERTY:
                DocumentPropertyAnnotation documentPropertyAnnotation =
                        (DocumentPropertyAnnotation) annotation;
                codeBlock.add(createSetShouldIndexNestedPropertiesExpr(documentPropertyAnnotation));
                break;
            case LONG_PROPERTY:
                LongPropertyAnnotation longPropertyAnnotation = (LongPropertyAnnotation) annotation;
                codeBlock.add(createSetIndexingTypeExpr(longPropertyAnnotation, getterOrField));
                break;
            case DOUBLE_PROPERTY: // fall-through
            case BOOLEAN_PROPERTY: // fall-through
            case BYTES_PROPERTY:
                break;
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }
        return codeBlock.add("\n.build()")
                .unindent()
                .build();
    }

    /**
     * Creates an expr like {@code .setCardinality(PropertyConfig.CARDINALITY_REPEATED)}.
     */
    @NonNull
    private static CodeBlock createSetCardinalityExpr(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        AnnotatedGetterOrField.ElementTypeCategory typeCategory =
                getterOrField.getElementTypeCategory();
        String enumName;
        switch (typeCategory) {
            case COLLECTION: // fall-through
            case ARRAY:
                enumName = "CARDINALITY_REPEATED";
                break;
            case SINGLE:
                enumName = annotation.isRequired()
                        ? "CARDINALITY_REQUIRED"
                        : "CARDINALITY_OPTIONAL";
                break;
            default:
                throw new IllegalStateException("Unhandled type category: " + typeCategory);
        }
        return CodeBlock.of("\n.setCardinality($T.$N)", PROPERTY_CONFIG_CLASS, enumName);
    }

    /**
     * Creates an expr like {@code .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)}.
     */
    @NonNull
    private static CodeBlock createSetTokenizerTypeExpr(
            @NonNull StringPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
        String enumName;
        if (annotation.getIndexingType() == 0) { // INDEXING_TYPE_NONE
            //TODO(b/171857731) remove this hack after apply to Icing lib's change.
            enumName = "TOKENIZER_TYPE_NONE";
        } else {
            switch (annotation.getTokenizerType()) {
                case 0:
                    enumName = "TOKENIZER_TYPE_NONE";
                    break;
                case 1:
                    enumName = "TOKENIZER_TYPE_PLAIN";
                    break;
                case 2:
                    enumName = "TOKENIZER_TYPE_VERBATIM";
                    break;
                case 3:
                    enumName = "TOKENIZER_TYPE_RFC822";
                    break;
                default:
                    throw new ProcessingException(
                            "Unknown tokenizer type " + annotation.getTokenizerType(),
                            getterOrField.getElement());
            }
        }
        return CodeBlock.of("\n.setTokenizerType($T.$N)",
                StringPropertyAnnotation.CONFIG_CLASS, enumName);
    }

    /**
     * Creates an expr like {@code .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)}.
     */
    @NonNull
    private static CodeBlock createSetIndexingTypeExpr(
            @NonNull StringPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
        String enumName;
        switch (annotation.getIndexingType()) {
            case 0:
                enumName = "INDEXING_TYPE_NONE";
                break;
            case 1:
                enumName = "INDEXING_TYPE_EXACT_TERMS";
                break;
            case 2:
                enumName = "INDEXING_TYPE_PREFIXES";
                break;
            default:
                throw new ProcessingException(
                        "Unknown indexing type " + annotation.getIndexingType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setIndexingType($T.$N)",
                StringPropertyAnnotation.CONFIG_CLASS, enumName);
    }

    /**
     * Creates an expr like {@code .setShouldIndexNestedProperties(true)}.
     */
    @NonNull
    private static CodeBlock createSetShouldIndexNestedPropertiesExpr(
            @NonNull DocumentPropertyAnnotation annotation) {
        return CodeBlock.of("\n.setShouldIndexNestedProperties($L)",
                annotation.shouldIndexNestedProperties());
    }

    /**
     * Creates an expr like {@code .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)}.
     */
    @NonNull
    private static CodeBlock createSetIndexingTypeExpr(
            @NonNull LongPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
        String enumName;
        switch (annotation.getIndexingType()) {
            case 0:
                enumName = "INDEXING_TYPE_NONE";
                break;
            case 1:
                enumName = "INDEXING_TYPE_RANGE";
                break;
            default:
                throw new ProcessingException(
                        "Unknown indexing type " + annotation.getIndexingType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setIndexingType($T.$N)",
                LongPropertyAnnotation.CONFIG_CLASS, enumName);
    }

    /**
     * Creates an expr like
     * {@code .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)}.
     */
    @NonNull
    private static CodeBlock createSetJoinableValueTypeExpr(
            @NonNull StringPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws ProcessingException {
        String enumName;
        AnnotatedGetterOrField.ElementTypeCategory typeCategory =
                getterOrField.getElementTypeCategory();
        switch (annotation.getJoinableValueType()) {
            case 0:
                enumName = "JOINABLE_VALUE_TYPE_NONE";
                break;
            case 1:
                switch (typeCategory) {
                    case COLLECTION: // fall-through
                    case ARRAY:
                        throw new ProcessingException(
                                "Joinable value type 1 not allowed on repeated properties.",
                                getterOrField.getElement());
                    case SINGLE: // fall-through
                        break;
                    default:
                        throw new IllegalStateException("Unhandled cardinality: " + typeCategory);
                }
                enumName = "JOINABLE_VALUE_TYPE_QUALIFIED_ID";
                break;
            default:
                throw new ProcessingException(
                        "Unknown joinable value type " + annotation.getJoinableValueType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setJoinableValueType($T.$N)",
                StringPropertyAnnotation.CONFIG_CLASS, enumName);
    }
}
