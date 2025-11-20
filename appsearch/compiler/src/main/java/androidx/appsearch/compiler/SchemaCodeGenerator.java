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
import static androidx.room.compiler.codegen.compat.XConverters.toJavaPoet;

import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.DocumentPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.EmbeddingPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.LongPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.StringPropertyAnnotation;
import androidx.room.compiler.codegen.XClassName;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.XType;
import androidx.room.compiler.processing.XTypeElement;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.lang.model.element.Modifier;

/** Generates java code for an {@link androidx.appsearch.app.AppSearchSchema}. */
class SchemaCodeGenerator {
    private final DocumentModel mModel;
    private final IntrospectionHelper mHelper;
    private final LinkedHashSet<XTypeElement> mDependencyDocumentClasses;

    public static void generate(
            @NonNull XProcessingEnv env,
            @NonNull DocumentModel model,
            TypeSpec.@NonNull Builder classBuilder) throws XProcessingException {
        new SchemaCodeGenerator(model, env).generate(classBuilder);
    }

    private SchemaCodeGenerator(@NonNull DocumentModel model, @NonNull XProcessingEnv env) {
        mModel = model;
        mHelper = new IntrospectionHelper(env);
        mDependencyDocumentClasses = computeDependencyClasses(model);
    }

    private static @NonNull LinkedHashSet<XTypeElement> computeDependencyClasses(
            @NonNull DocumentModel model) {
        LinkedHashSet<XTypeElement> dependencies = new LinkedHashSet<>(model.getParentTypes());
        for (AnnotatedGetterOrField getterOrField : model.getAnnotatedGettersAndFields()) {
            if (!(getterOrField.getAnnotation() instanceof DocumentPropertyAnnotation)) {
                continue;
            }

            XType documentClass = getterOrField.getComponentType();
            dependencies.add(documentClass.getTypeElement());
        }
        return dependencies;
    }

    private void generate(TypeSpec.@NonNull Builder classBuilder) throws XProcessingException {
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
                        .returns(toJavaPoet(APPSEARCH_SCHEMA_CLASS))
                        .addAnnotation(Override.class)
                        .addException(toJavaPoet(APPSEARCH_EXCEPTION_CLASS))
                        .addStatement("return $L", createSchemaInitializerGetDocumentTypes())
                        .build());

        classBuilder.addMethod(createDependencyClassesMethod());
    }

    private @NonNull MethodSpec createDependencyClassesMethod() {
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
                .addException(toJavaPoet(APPSEARCH_EXCEPTION_CLASS));

        if (mDependencyDocumentClasses.isEmpty()) {
            methodBuilder.addStatement("return $T.emptyList()", ClassName.get(Collections.class));
        } else {
            methodBuilder.addStatement("$T classSet = new $T()", listOfClasses, arrayListOfClasses);
            for (XTypeElement dependencyType : mDependencyDocumentClasses) {
                methodBuilder.addStatement(
                        "classSet.add($T.class)",
                        toJavaPoet(dependencyType.asClassName()));
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
    private CodeBlock createSchemaInitializerGetDocumentTypes() throws XProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add(
                        "new $T(SCHEMA_NAME)",
                        toJavaPoet(APPSEARCH_SCHEMA_CLASS.nestedClass("Builder")))
                .indent();
        for (XTypeElement parentType : mModel.getParentTypes()) {
            XClassName parentDocumentFactoryClass =
                    getDocumentClassFactoryForClass(parentType.asClassName());
            codeBlock.add(
                    "\n.addParentType($T.SCHEMA_NAME)", toJavaPoet(parentDocumentFactoryClass));
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
            @NonNull AnnotatedGetterOrField getterOrField) throws XProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        if (annotation.getDataPropertyKind() == DataPropertyAnnotation.Kind.DOCUMENT_PROPERTY) {
            XClassName documentClass =
                    getterOrField.getComponentType().getTypeElement().asClassName();
            XClassName documentFactoryClass = getDocumentClassFactoryForClass(documentClass);
            codeBlock.add("new $T.Builder($S, $T.SCHEMA_NAME)",
                    toJavaPoet(DocumentPropertyAnnotation.CONFIG_CLASS),
                    annotation.getName(),
                    toJavaPoet(documentFactoryClass));
        } else {
            // All other property configs have a single param constructor that just takes the
            // property's serialized name as input
            codeBlock.add("new $T.Builder($S)",
                    toJavaPoet(annotation.getConfigClassName()), annotation.getName());
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
                Set<String> indexableNestedProperties = getAllIndexableNestedProperties(
                        documentPropertyAnnotation);
                for (String propertyPath : indexableNestedProperties) {
                    codeBlock.add(
                            CodeBlock.of("\n.addIndexableNestedProperties($S)", propertyPath));
                }
                break;
            case LONG_PROPERTY:
                LongPropertyAnnotation longPropertyAnnotation = (LongPropertyAnnotation) annotation;
                codeBlock.add(createSetIndexingTypeExpr(longPropertyAnnotation, getterOrField));
                break;
            case EMBEDDING_PROPERTY:
                EmbeddingPropertyAnnotation embeddingPropertyAnnotation =
                        (EmbeddingPropertyAnnotation) annotation;
                codeBlock
                        .add(createSetIndexingTypeExpr(embeddingPropertyAnnotation, getterOrField))
                        .add(createSetQuantizationTypeExpr(embeddingPropertyAnnotation,
                                getterOrField));
                break;
            case DOUBLE_PROPERTY: // fall-through
            case BOOLEAN_PROPERTY: // fall-through
            case BYTES_PROPERTY: // fall-through
            case BLOB_HANDLE_PROPERTY:
                break;
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }
        return codeBlock.add("\n.build()")
                .unindent()
                .build();
    }


    /**
     * Finds all indexable nested properties for the given type class and document property
     * annotation. This includes indexable nested properties that should be inherited from the
     * type's parent.
     */
    private Set<String> getAllIndexableNestedProperties(
            @NonNull DocumentPropertyAnnotation documentPropertyAnnotation)
            throws XProcessingException {
        Set<String> indexableNestedProperties = new LinkedHashSet<>(
                documentPropertyAnnotation.getIndexableNestedPropertiesList());

        if (documentPropertyAnnotation.getShouldInheritIndexableNestedPropertiesFromSuperClass()) {
            // List of classes to expand into parent classes to search for the property annotation
            Queue<XTypeElement> classesToExpand = new ArrayDeque<>();
            Set<XTypeElement> visited = new LinkedHashSet<>();
            classesToExpand.add(mModel.getClassElement());
            while (!classesToExpand.isEmpty()) {
                XTypeElement currentClass = classesToExpand.poll();
                if (visited.contains(currentClass)) {
                    continue;
                }
                visited.add(currentClass);
                // Look for the document property annotation in the class's parent classes
                List<XType> parentTypes = new ArrayList<>();
                if (currentClass.getSuperClass() != null) {
                    parentTypes.add(currentClass.getSuperClass());
                }
                parentTypes.addAll(currentClass.getSuperInterfaces());
                for (XType parent : parentTypes) {
                    XTypeElement parentElement = parent.getTypeElement();
                    if (parentElement == null) {
                        continue;
                    }
                    DocumentPropertyAnnotation annotation = mHelper.getDocumentPropertyAnnotation(
                            parentElement, documentPropertyAnnotation.getName());
                    if (annotation == null) {
                        // The property is not found in this level. Continue searching in one level
                        // above as the property could still be defined for this level by class
                        // inheritance.
                        classesToExpand.add(parentElement);
                    } else {
                        indexableNestedProperties.addAll(
                                annotation.getIndexableNestedPropertiesList());
                        if (annotation.getShouldInheritIndexableNestedPropertiesFromSuperClass()) {
                            // Continue searching in the parent class's parents
                            classesToExpand.add(parentElement);
                        }
                    }
                }
            }
        }
        return indexableNestedProperties;
    }

    /**
     * Creates an expr like {@code .setCardinality(PropertyConfig.CARDINALITY_REPEATED)}.
     */
    private static @NonNull CodeBlock createSetCardinalityExpr(
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
        return CodeBlock.of(
                "\n.setCardinality($T.$N)", toJavaPoet(PROPERTY_CONFIG_CLASS), enumName);
    }

    /**
     * Creates an expr like {@code .setTokenizerType(StringPropertyConfig.TOKENIZER_TYPE_PLAIN)}.
     */
    private static @NonNull CodeBlock createSetTokenizerTypeExpr(
            @NonNull StringPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws XProcessingException {
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
                    throw new XProcessingException(
                            "Unknown tokenizer type " + annotation.getTokenizerType(),
                            getterOrField.getElement());
            }
        }
        return CodeBlock.of("\n.setTokenizerType($T.$N)",
                toJavaPoet(StringPropertyAnnotation.CONFIG_CLASS), enumName);
    }

    /**
     * Creates an expr like {@code .setIndexingType(StringPropertyConfig.INDEXING_TYPE_PREFIXES)}.
     */
    private static @NonNull CodeBlock createSetIndexingTypeExpr(
            @NonNull StringPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws XProcessingException {
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
                throw new XProcessingException(
                        "Unknown indexing type " + annotation.getIndexingType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setIndexingType($T.$N)",
                toJavaPoet(StringPropertyAnnotation.CONFIG_CLASS), enumName);
    }

    /**
     * Creates an expr like {@code .setShouldIndexNestedProperties(true)}.
     */
    private static @NonNull CodeBlock createSetShouldIndexNestedPropertiesExpr(
            @NonNull DocumentPropertyAnnotation annotation) {
        return CodeBlock.of("\n.setShouldIndexNestedProperties($L)",
                annotation.getShouldIndexNestedProperties());
    }

    /**
     * Creates an expr like {@code .setIndexingType(LongPropertyConfig.INDEXING_TYPE_RANGE)}.
     */
    private static @NonNull CodeBlock createSetIndexingTypeExpr(
            @NonNull LongPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws XProcessingException {
        String enumName;
        switch (annotation.getIndexingType()) {
            case 0:
                enumName = "INDEXING_TYPE_NONE";
                break;
            case 1:
                enumName = "INDEXING_TYPE_RANGE";
                break;
            default:
                throw new XProcessingException(
                        "Unknown indexing type " + annotation.getIndexingType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setIndexingType($T.$N)",
                toJavaPoet(LongPropertyAnnotation.CONFIG_CLASS), enumName);
    }

    /**
     * Creates an expr like
     * {@code .setIndexingType(EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)}.
     */
    private static @NonNull CodeBlock createSetIndexingTypeExpr(
            @NonNull EmbeddingPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws XProcessingException {
        String enumName;
        switch (annotation.getIndexingType()) {
            case 0:
                enumName = "INDEXING_TYPE_NONE";
                break;
            case 1:
                enumName = "INDEXING_TYPE_SIMILARITY";
                break;
            default:
                throw new XProcessingException(
                        "Unknown indexing type " + annotation.getIndexingType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setIndexingType($T.$N)",
                toJavaPoet(EmbeddingPropertyAnnotation.CONFIG_CLASS), enumName);
    }

    /**
     * Creates an expr like
     * {@code .setQuantizationType(EmbeddingPropertyConfig.QUANTIZATION_TYPE_8_BIT)}.
     */
    private static @NonNull CodeBlock createSetQuantizationTypeExpr(
            @NonNull EmbeddingPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws XProcessingException {
        String enumName;
        switch (annotation.getQuantizationType()) {
            case 0:
                enumName = "QUANTIZATION_TYPE_NONE";
                break;
            case 1:
                enumName = "QUANTIZATION_TYPE_8_BIT";
                break;
            default:
                throw new XProcessingException(
                        "Unknown quantization type " + annotation.getQuantizationType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setQuantizationType($T.$N)",
                toJavaPoet(EmbeddingPropertyAnnotation.CONFIG_CLASS), enumName);
    }

    /**
     * Creates an expr like
     * {@code .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)}.
     */
    private static @NonNull CodeBlock createSetJoinableValueTypeExpr(
            @NonNull StringPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) throws XProcessingException {
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
                        throw new XProcessingException(
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
                throw new XProcessingException(
                        "Unknown joinable value type " + annotation.getJoinableValueType(),
                        getterOrField.getElement());
        }
        return CodeBlock.of("\n.setJoinableValueType($T.$N)",
                toJavaPoet(StringPropertyAnnotation.CONFIG_CLASS), enumName);
    }
}
