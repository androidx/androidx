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
import androidx.annotation.VisibleForTesting;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Generates java code for an {@link androidx.appsearch.app.AppSearchSchema} and a translator
 * between the data class and a {@link androidx.appsearch.app.GenericDocument}.
 */
class CodeGenerator {
    @VisibleForTesting static final String GEN_CLASS_PREFIX = "$$__AppSearch__";
    private static final String APPSEARCH_PKG = "androidx.appsearch.app";

    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mIntrospectionHelper;
    private final AppSearchDocumentModel mModel;

    private final TypeMirror mCollectionType;
    private final TypeMirror mStringType;
    private final TypeMirror mIntegerBoxType;
    private final TypeMirror mLongBoxType;
    private final TypeMirror mFloatBoxType;
    private final TypeMirror mDoubleBoxType;
    private final TypeMirror mBooleanBoxType;
    private final TypeMirror mByteArrayType;

    private final String mOutputPackage;
    private final TypeSpec mOutputClass;

    public static CodeGenerator generate(
            @NonNull ProcessingEnvironment env, @NonNull AppSearchDocumentModel model)
            throws ProcessingException {
        return new CodeGenerator(env, model);
    }

    private CodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull AppSearchDocumentModel model)
            throws ProcessingException {
        // Prepare constants needed for processing
        mEnv = env;
        mIntrospectionHelper = new IntrospectionHelper(env);
        mModel = model;

        Elements elementUtil = env.getElementUtils();
        Types typeUtil = env.getTypeUtils();
        mCollectionType = elementUtil.getTypeElement(Collection.class.getName()).asType();
        mStringType = elementUtil.getTypeElement(String.class.getName()).asType();
        mIntegerBoxType = elementUtil.getTypeElement(Integer.class.getName()).asType();
        mLongBoxType = elementUtil.getTypeElement(Long.class.getName()).asType();
        mFloatBoxType = elementUtil.getTypeElement(Float.class.getName()).asType();
        mDoubleBoxType = elementUtil.getTypeElement(Double.class.getName()).asType();
        mBooleanBoxType = elementUtil.getTypeElement(Boolean.class.getName()).asType();
        mByteArrayType = typeUtil.getArrayType(typeUtil.getPrimitiveType(TypeKind.BYTE));

        // Perform the actual work of generating code
        mOutputPackage = mEnv.getElementUtils().getPackageOf(mModel.getClassElement()).toString();
        mOutputClass = createClass();
    }

    public void writeToFiler() throws IOException {
        JavaFile.builder(mOutputPackage, mOutputClass).build().writeTo(mEnv.getFiler());
    }

    public void writeToFolder(@NonNull File folder) throws IOException {
        JavaFile.builder(mOutputPackage, mOutputClass).build().writeTo(folder);
    }

    private TypeSpec createClass() throws ProcessingException {
        String genClassName = GEN_CLASS_PREFIX + mModel.getClassElement().getSimpleName();
        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(genClassName)
                .addOriginatingElement(mModel.getClassElement());

        genClass.addField(
                FieldSpec.builder(String.class, "SCHEMA_TYPE")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", mModel.getSchemaName())
                        .build());

        genClass.addField(
                FieldSpec.builder(ClassName.get(APPSEARCH_PKG, "AppSearchSchema"), "SCHEMA")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(createSchemaInitializer())
                        .build());

        return genClass.build();
    }

    private CodeBlock createSchemaInitializer() throws ProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T($L)",
                        ClassName.get(APPSEARCH_PKG, "AppSearchSchema", "Builder"),
                        "SCHEMA_TYPE")
                .indent();
        for (VariableElement property : mModel.getPropertyFields()) {
            codeBlock.add("\n.addProperty($L)", createPropertySchema(property));
        }
        codeBlock.add("\n.build()").unindent();
        return codeBlock.build();
    }

    private CodeBlock createPropertySchema(@NonNull VariableElement property)
            throws ProcessingException {
        // Find the property name
        AnnotationMirror annotation =
                mIntrospectionHelper.getAnnotation(property, IntrospectionHelper.PROPERTY_CLASS);
        Map<String, Object> params = mIntrospectionHelper.getAnnotationParams(annotation);
        String propertyName = params.get("name").toString();
        if (propertyName.isEmpty()) {
            propertyName = property.getSimpleName().toString();
        }
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T($S)",
                        ClassName.get(APPSEARCH_PKG, "PropertyConfig", "Builder"),
                        propertyName)
                .indent();

        // Find the property type
        Types typeUtil = mEnv.getTypeUtils();
        TypeMirror propertyType;
        boolean repeated = false;
        if (property.asType().getKind() == TypeKind.ERROR) {
            throw new ProcessingException("Property type unknown to java compiler", property);
        } else if (typeUtil.isAssignable(typeUtil.erasure(property.asType()), mCollectionType)) {
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
                && !typeUtil.isSameType(property.asType(), mByteArrayType)) {
            propertyType = ((ArrayType) property.asType()).getComponentType();
            repeated = true;

        } else {
            propertyType = property.asType();
        }
        ClassName propertyTypeEnum;
        if (typeUtil.isSameType(propertyType, mStringType)) {
            propertyTypeEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "DATA_TYPE_STRING");
        } else if (typeUtil.isSameType(propertyType, mIntegerBoxType)
                || typeUtil.isSameType(propertyType, typeUtil.unboxedType(mIntegerBoxType))
                || typeUtil.isSameType(propertyType, mLongBoxType)
                || typeUtil.isSameType(propertyType, typeUtil.unboxedType(mLongBoxType))) {
            propertyTypeEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "DATA_TYPE_INT64");
        } else if (typeUtil.isSameType(propertyType, mFloatBoxType)
                || typeUtil.isSameType(propertyType, typeUtil.unboxedType(mFloatBoxType))
                || typeUtil.isSameType(propertyType, mDoubleBoxType)
                || typeUtil.isSameType(propertyType, typeUtil.unboxedType(mDoubleBoxType))) {
            propertyTypeEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "DATA_TYPE_DOUBLE");
        } else if (typeUtil.isSameType(propertyType, mBooleanBoxType)
                || typeUtil.isSameType(propertyType, typeUtil.unboxedType(mBooleanBoxType))) {
            propertyTypeEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "DATA_TYPE_BOOLEAN");
        } else if (typeUtil.isSameType(propertyType, mByteArrayType)) {
            propertyTypeEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "DATA_TYPE_BYTES");
        } else {
            propertyTypeEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "DATA_TYPE_DOCUMENT");
        }
        codeBlock.add("\n.setDataType($T)", propertyTypeEnum);

        // Find property cardinality
        ClassName cardinalityEnum;
        if (repeated) {
            cardinalityEnum =
                    ClassName.get(APPSEARCH_PKG, "PropertyConfig", "CARDINALITY_REPEATED");
        } else if (Boolean.parseBoolean(params.get("required").toString())) {
            cardinalityEnum =
                    ClassName.get(APPSEARCH_PKG, "PropertyConfig", "CARDINALITY_REQUIRED");
        } else {
            cardinalityEnum =
                    ClassName.get(APPSEARCH_PKG, "PropertyConfig", "CARDINALITY_OPTIONAL");
        }
        codeBlock.add("\n.setCardinality($T)", cardinalityEnum);

        // Find tokenizer type
        int tokenizerType = Integer.parseInt(params.get("tokenizerType").toString());
        ClassName tokenizerEnum;
        if (tokenizerType == 0) {  // TOKENIZER_TYPE_NONE
            tokenizerEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "TOKENIZER_TYPE_NONE");
        } else if (tokenizerType == 1) {  // TOKENIZER_TYPE_PLAIN
            tokenizerEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "TOKENIZER_TYPE_PLAIN");
        } else {
            throw new ProcessingException("Unknown tokenizer type " + tokenizerType, property);
        }
        codeBlock.add("\n.setTokenizerType($T)", tokenizerEnum);

        // Find indexing type
        int indexingType = Integer.parseInt(params.get("indexingType").toString());
        ClassName indexingEnum;
        if (indexingType == 0) {  // INDEXING_TYPE_NONE
            indexingEnum = ClassName.get(APPSEARCH_PKG, "PropertyConfig", "INDEXING_TYPE_NONE");
        } else if (indexingType == 1) {  // INDEXING_TYPE_EXACT_TERMS
            indexingEnum = ClassName.get(
                    APPSEARCH_PKG, "PropertyConfig", "INDEXING_TYPE_EXACT_TERMS");
        } else if (indexingType == 2) {  // INDEXING_TYPE_PREFIXES
            indexingEnum = ClassName.get(
                    APPSEARCH_PKG, "PropertyConfig", "INDEXING_TYPE_PREFIXES");
        } else {
            throw new ProcessingException("Unknown indexing type " + indexingType, property);
        }
        codeBlock.add("\n.setIndexingType($T)", indexingEnum);

        // Done!
        codeBlock.add("\n.build()");
        codeBlock.unindent();
        return codeBlock.build();
    }
}
