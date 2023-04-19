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

import androidx.annotation.NonNull;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/** Generates java code for an {@link androidx.appsearch.app.AppSearchSchema}. */
class SchemaCodeGenerator {
    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mHelper;
    private final DocumentModel mModel;
    private final Set<ClassName> mDocumentTypesAccumulator = new HashSet<>();

    public static void generate(
            @NonNull ProcessingEnvironment env,
            @NonNull DocumentModel model,
            @NonNull TypeSpec.Builder classBuilder) throws ProcessingException {
        new SchemaCodeGenerator(env, model).generate(classBuilder);
    }

    private SchemaCodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull DocumentModel model) {
        mEnv = env;
        mHelper = new IntrospectionHelper(env);
        mModel = model;
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
                        .returns(TypeName.get(mHelper.mStringType))
                        .addAnnotation(Override.class)
                        .addStatement("return SCHEMA_NAME")
                        .build());

        CodeBlock schemaInitializer = createSchemaInitializerGetDocumentTypes();

        classBuilder.addMethod(
                MethodSpec.methodBuilder("getSchema")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(mHelper.getAppSearchClass("AppSearchSchema"))
                        .addAnnotation(Override.class)
                        .addException(mHelper.getAppSearchExceptionClass())
                        .addStatement("return $L", schemaInitializer)
                        .build());

        classBuilder.addMethod(createNestedClassesMethod());
    }

    @NonNull
    private MethodSpec createNestedClassesMethod() {
        TypeName setOfClasses = ParameterizedTypeName.get(ClassName.get("java.util", "List"),
                ParameterizedTypeName.get(ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(Object.class)));

        TypeName arraySetOfClasses =
                ParameterizedTypeName.get(ClassName.get("java.util", "ArrayList"),
                        ParameterizedTypeName.get(ClassName.get(Class.class),
                                WildcardTypeName.subtypeOf(Object.class)));

        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getNestedDocumentClasses")
                .addModifiers(Modifier.PUBLIC)
                .returns(setOfClasses)
                .addAnnotation(Override.class)
                .addException(mHelper.getAppSearchExceptionClass());

        if (mDocumentTypesAccumulator.isEmpty()) {
            methodBuilder.addStatement("return $T.emptyList()",
                    ClassName.get("java.util", "Collections"));
        } else {
            methodBuilder.addStatement("$T classSet = new $T()", setOfClasses, arraySetOfClasses);
            for (ClassName className : mDocumentTypesAccumulator) {
                methodBuilder.addStatement("classSet.add($T.class)", className);
            }
            methodBuilder.addStatement("return classSet").build();
        }
        return methodBuilder.build();
    }

    /**
     * This method accumulates Document-type properties in mDocumentTypesAccumulator by calling
     * {@link #createPropertySchema}.
     */
    private CodeBlock createSchemaInitializerGetDocumentTypes() throws ProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T(SCHEMA_NAME)", mHelper.getAppSearchClass("AppSearchSchema", "Builder"))
                .indent();
        for (VariableElement property : mModel.getPropertyFields().values()) {
            codeBlock.add("\n.addProperty($L)", createPropertySchema(property));
        }
        codeBlock.add("\n.build()").unindent();
        return codeBlock.build();
    }

    /** This method accumulates Document-type properties in mDocumentTypesAccumulator. */
    private CodeBlock createPropertySchema(@NonNull VariableElement property)
            throws ProcessingException {
        AnnotationMirror annotation = mModel.getPropertyAnnotation(property);
        Map<String, Object> params = mHelper.getAnnotationParams(annotation);

        // Find the property type
        Types typeUtil = mEnv.getTypeUtils();
        TypeMirror propertyType;
        boolean repeated = false;
        boolean isPropertyString = false;
        boolean isPropertyDocument = false;
        boolean isPropertyLong = false;
        if (property.asType().getKind() == TypeKind.ERROR) {
            throw new ProcessingException("Property type unknown to java compiler", property);
        } else if (typeUtil.isAssignable(
                typeUtil.erasure(property.asType()), mHelper.mCollectionType)) {
            List<? extends TypeMirror> genericTypes =
                    ((DeclaredType) property.asType()).getTypeArguments();
            if (genericTypes.isEmpty()) {
                throw new ProcessingException(
                        "Property is repeated but has no generic type", property);
            }
            propertyType = genericTypes.get(0);
            repeated = true;
        } else if (property.asType().getKind() == TypeKind.ARRAY
                // Byte arrays have a native representation in Icing, so they are not considered a
                // "repeated" type
                && !typeUtil.isSameType(property.asType(), mHelper.mBytePrimitiveArrayType)
                && !typeUtil.isSameType(property.asType(), mHelper.mByteBoxArrayType)) {
            propertyType = ((ArrayType) property.asType()).getComponentType();
            repeated = true;

        } else {
            propertyType = property.asType();
        }
        ClassName propertyClass;
        if (typeUtil.isSameType(propertyType, mHelper.mStringType)) {
            propertyClass = mHelper.getAppSearchClass("AppSearchSchema", "StringPropertyConfig");
            isPropertyString = true;
        } else if (typeUtil.isSameType(propertyType, mHelper.mIntegerBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mIntPrimitiveType)
                || typeUtil.isSameType(propertyType, mHelper.mLongBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mLongPrimitiveType)) {
            propertyClass = mHelper.getAppSearchClass("AppSearchSchema", "LongPropertyConfig");
            isPropertyLong = true;
        } else if (typeUtil.isSameType(propertyType, mHelper.mFloatBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mFloatPrimitiveType)
                || typeUtil.isSameType(propertyType, mHelper.mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mDoublePrimitiveType)) {
            propertyClass = mHelper.getAppSearchClass("AppSearchSchema", "DoublePropertyConfig");
        } else if (typeUtil.isSameType(propertyType, mHelper.mBooleanBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mBooleanPrimitiveType)) {
            propertyClass = mHelper.getAppSearchClass("AppSearchSchema", "BooleanPropertyConfig");
        } else if (typeUtil.isSameType(propertyType, mHelper.mBytePrimitiveArrayType)
                || typeUtil.isSameType(propertyType, mHelper.mByteBoxArrayType)) {
            propertyClass = mHelper.getAppSearchClass("AppSearchSchema", "BytesPropertyConfig");
        } else {
            propertyClass = mHelper.getAppSearchClass("AppSearchSchema", "DocumentPropertyConfig");
            isPropertyDocument = true;
        }

        // Start the builder for the property
        String propertyName = mModel.getPropertyName(property);
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        if (isPropertyDocument) {
            ClassName documentClass = (ClassName) ClassName.get(propertyType);
            ClassName documentFactoryClass = mHelper.getDocumentClassFactoryForClass(documentClass);
            codeBlock.add(
                    "new $T($S, $T.SCHEMA_NAME)",
                    propertyClass.nestedClass("Builder"),
                    propertyName,
                    documentFactoryClass);
            mDocumentTypesAccumulator.add(documentClass);
        } else {
            codeBlock.add("new $T($S)", propertyClass.nestedClass("Builder"), propertyName);
        }
        codeBlock.indent();

        // Find property cardinality
        ClassName cardinalityEnum;
        if (repeated) {
            cardinalityEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "CARDINALITY_REPEATED");
        } else if (Boolean.parseBoolean(params.get("required").toString())) {
            cardinalityEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "CARDINALITY_REQUIRED");
        } else {
            cardinalityEnum = mHelper.getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "CARDINALITY_OPTIONAL");
        }
        codeBlock.add("\n.setCardinality($T)", cardinalityEnum);

        if (isPropertyString) {
            // Find tokenizer type
            int tokenizerType = Integer.parseInt(params.get("tokenizerType").toString());
            if (Integer.parseInt(params.get("indexingType").toString()) == 0) {
                //TODO(b/171857731) remove this hack after apply to Icing lib's change.
                tokenizerType = 0;
            }
            ClassName tokenizerEnum;
            if (tokenizerType == 0) {  // TOKENIZER_TYPE_NONE
                tokenizerEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "TOKENIZER_TYPE_NONE");
            } else if (tokenizerType == 1) {  // TOKENIZER_TYPE_PLAIN
                tokenizerEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "TOKENIZER_TYPE_PLAIN");
            } else if (tokenizerType == 2) {  // TOKENIZER_TYPE_VERBATIM
                tokenizerEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "TOKENIZER_TYPE_VERBATIM");
            } else if (tokenizerType == 3) { // TOKENIZER_TYPE_RFC822
                tokenizerEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "TOKENIZER_TYPE_RFC822");
            } else {
                throw new ProcessingException("Unknown tokenizer type " + tokenizerType, property);
            }
            codeBlock.add("\n.setTokenizerType($T)", tokenizerEnum);

            // Find indexing type
            int indexingType = Integer.parseInt(params.get("indexingType").toString());
            ClassName indexingEnum;
            if (indexingType == 0) {  // INDEXING_TYPE_NONE
                indexingEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "INDEXING_TYPE_NONE");
            } else if (indexingType == 1) {  // INDEXING_TYPE_EXACT_TERMS
                indexingEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "INDEXING_TYPE_EXACT_TERMS");
            } else if (indexingType == 2) {  // INDEXING_TYPE_PREFIXES
                indexingEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "INDEXING_TYPE_PREFIXES");
            } else {
                throw new ProcessingException("Unknown indexing type " + indexingType, property);
            }
            codeBlock.add("\n.setIndexingType($T)", indexingEnum);

            int joinableValueType = Integer.parseInt(params.get("joinableValueType").toString());
            ClassName joinableEnum;
            if (joinableValueType == 0) { // JOINABLE_VALUE_TYPE_NONE
                joinableEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig", "JOINABLE_VALUE_TYPE_NONE");

            } else if (joinableValueType == 1) { // JOINABLE_VALUE_TYPE_QUALIFIED_ID
                if (repeated) {
                    throw new ProcessingException(
                            "Joinable value type " + joinableValueType + " not allowed on repeated "
                                    + "properties.", property);

                }
                joinableEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "StringPropertyConfig",
                        "JOINABLE_VALUE_TYPE_QUALIFIED_ID");
            } else {
                throw new ProcessingException(
                        "Unknown joinable value type " + joinableValueType, property);
            }
            codeBlock.add("\n.setJoinableValueType($T)", joinableEnum);

        } else if (isPropertyDocument) {
            if (params.containsKey("indexNestedProperties")) {
                boolean indexNestedProperties = Boolean.parseBoolean(
                        params.get("indexNestedProperties").toString());

                codeBlock.add("\n.setShouldIndexNestedProperties($L)", indexNestedProperties);
            }
        } else if (isPropertyLong) {
            int indexingType = 0;  // INDEXING_TYPE_NONE
            if (params.containsKey("indexingType")) {
                indexingType = Integer.parseInt(params.get("indexingType").toString());
            }

            ClassName indexingEnum;
            if (indexingType == 0) {  // INDEXING_TYPE_NONE
                indexingEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "LongPropertyConfig", "INDEXING_TYPE_NONE");
            } else if (indexingType == 1) {  // INDEXING_TYPE_RANGE
                indexingEnum = mHelper.getAppSearchClass(
                        "AppSearchSchema", "LongPropertyConfig", "INDEXING_TYPE_RANGE");
            } else {
                throw new ProcessingException("Unknown indexing type " + indexingType, property);
            }
            codeBlock.add("\n.setIndexingType($T)", indexingEnum);
        }

        // Done!
        codeBlock.add("\n.build()");
        codeBlock.unindent();
        return codeBlock.build();
    }
}
