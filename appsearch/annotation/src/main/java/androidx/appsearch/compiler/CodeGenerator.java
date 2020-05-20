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
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
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
    @VisibleForTesting
    static final String GEN_CLASS_PREFIX = "$$__AppSearch__";
    private static final String APPSEARCH_PKG = "androidx.appsearch.app";

    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mIntrospectionHelper;
    private final AppSearchDocumentModel mModel;

    private final TypeMirror mCollectionType;
    private final TypeMirror mStringType;
    private final TypeMirror mIntegerBoxType;
    private final TypeMirror mIntPrimitiveType;
    private final TypeMirror mLongBoxType;
    private final TypeMirror mLongPrimitiveType;
    private final TypeMirror mFloatBoxType;
    private final TypeMirror mFloatPrimitiveType;
    private final TypeMirror mDoubleBoxType;
    private final TypeMirror mDoublePrimitiveType;
    private final TypeMirror mBooleanBoxType;
    private final TypeMirror mBooleanPrimitiveType;
    private final TypeMirror mByteBoxType;
    private final TypeMirror mByteArrayType;
    private final TypeMirror mByteArrayBoxType;

    private final String mOutputPackage;
    private final TypeSpec mOutputClass;

    // State accumulated during the parse
    private final Map<String, String> mFieldToPropertyName = new HashMap<>();

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
        mIntPrimitiveType = typeUtil.unboxedType(mIntegerBoxType);
        mLongBoxType = elementUtil.getTypeElement(Long.class.getName()).asType();
        mLongPrimitiveType = typeUtil.unboxedType(mLongBoxType);
        mFloatBoxType = elementUtil.getTypeElement(Float.class.getName()).asType();
        mFloatPrimitiveType = typeUtil.unboxedType(mFloatBoxType);
        mDoubleBoxType = elementUtil.getTypeElement(Double.class.getName()).asType();
        mDoublePrimitiveType = typeUtil.unboxedType(mDoubleBoxType);
        mBooleanBoxType = elementUtil.getTypeElement(Boolean.class.getName()).asType();
        mBooleanPrimitiveType = typeUtil.unboxedType(mBooleanBoxType);
        mByteBoxType = elementUtil.getTypeElement(Byte.class.getName()).asType();
        mByteArrayType = typeUtil.getArrayType(typeUtil.getPrimitiveType(TypeKind.BYTE));
        mByteArrayBoxType = typeUtil.getArrayType(mByteBoxType);

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
                FieldSpec.builder(getAppSearchClass("AppSearchSchema"), "SCHEMA")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(createSchemaInitializer())
                        .build());

        genClass.addMethod(createToGenericDocumentMethod());

        return genClass.build();
    }

    private CodeBlock createSchemaInitializer() throws ProcessingException {
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T(SCHEMA_TYPE)", getAppSearchClass("AppSearchSchema", "Builder"))
                .indent();
        for (Map.Entry<String, VariableElement> entry : mModel.getPropertyFields().entrySet()) {
            codeBlock.add(
                    "\n.addProperty($L)", createPropertySchema(entry.getKey(), entry.getValue()));
        }
        codeBlock.add("\n.build()").unindent();
        return codeBlock.build();
    }

    private CodeBlock createPropertySchema(
            @NonNull String fieldName, @NonNull VariableElement property)
            throws ProcessingException {
        // Find the property name
        AnnotationMirror annotation =
                mIntrospectionHelper.getAnnotation(property, IntrospectionHelper.PROPERTY_CLASS);
        Map<String, Object> params = mIntrospectionHelper.getAnnotationParams(annotation);
        String propertyName = params.get("name").toString();
        if (propertyName.isEmpty()) {
            propertyName = property.getSimpleName().toString();
        }
        mFieldToPropertyName.put(fieldName, propertyName);

        // Start the builder for that property
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T($S)",
                        getAppSearchClass("AppSearchSchema", "PropertyConfig", "Builder"),
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
                && !typeUtil.isSameType(property.asType(), mByteArrayType)
                && !typeUtil.isSameType(property.asType(), mByteArrayBoxType)) {
            propertyType = ((ArrayType) property.asType()).getComponentType();
            repeated = true;

        } else {
            propertyType = property.asType();
        }
        ClassName propertyTypeEnum;
        if (typeUtil.isSameType(propertyType, mStringType)) {
            propertyTypeEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "DATA_TYPE_STRING");
        } else if (typeUtil.isSameType(propertyType, mIntegerBoxType)
                || typeUtil.isSameType(propertyType, mIntPrimitiveType)
                || typeUtil.isSameType(propertyType, mLongBoxType)
                || typeUtil.isSameType(propertyType, mLongPrimitiveType)) {
            propertyTypeEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "DATA_TYPE_INT64");
        } else if (typeUtil.isSameType(propertyType, mFloatBoxType)
                || typeUtil.isSameType(propertyType, mFloatPrimitiveType)
                || typeUtil.isSameType(propertyType, mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mDoublePrimitiveType)) {
            propertyTypeEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "DATA_TYPE_DOUBLE");
        } else if (typeUtil.isSameType(propertyType, mBooleanBoxType)
                || typeUtil.isSameType(propertyType, mBooleanPrimitiveType)) {
            propertyTypeEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "DATA_TYPE_BOOLEAN");
        } else if (typeUtil.isSameType(propertyType, mByteArrayType)
                || typeUtil.isSameType(propertyType, mByteArrayBoxType)) {
            propertyTypeEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "DATA_TYPE_BYTES");
        } else {
            propertyTypeEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "DATA_TYPE_DOCUMENT");
        }
        codeBlock.add("\n.setDataType($T)", propertyTypeEnum);

        // Find property cardinality
        ClassName cardinalityEnum;
        if (repeated) {
            cardinalityEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "CARDINALITY_REPEATED");
        } else if (Boolean.parseBoolean(params.get("required").toString())) {
            cardinalityEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "CARDINALITY_REQUIRED");
        } else {
            cardinalityEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "CARDINALITY_OPTIONAL");
        }
        codeBlock.add("\n.setCardinality($T)", cardinalityEnum);

        // Find tokenizer type
        int tokenizerType = Integer.parseInt(params.get("tokenizerType").toString());
        ClassName tokenizerEnum;
        if (tokenizerType == 0) {  // TOKENIZER_TYPE_NONE
            tokenizerEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "TOKENIZER_TYPE_NONE");
        } else if (tokenizerType == 1) {  // TOKENIZER_TYPE_PLAIN
            tokenizerEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "TOKENIZER_TYPE_PLAIN");
        } else {
            throw new ProcessingException("Unknown tokenizer type " + tokenizerType, property);
        }
        codeBlock.add("\n.setTokenizerType($T)", tokenizerEnum);

        // Find indexing type
        int indexingType = Integer.parseInt(params.get("indexingType").toString());
        ClassName indexingEnum;
        if (indexingType == 0) {  // INDEXING_TYPE_NONE
            indexingEnum =
                    getAppSearchClass("AppSearchSchema", "PropertyConfig", "INDEXING_TYPE_NONE");
        } else if (indexingType == 1) {  // INDEXING_TYPE_EXACT_TERMS
            indexingEnum = getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "INDEXING_TYPE_EXACT_TERMS");
        } else if (indexingType == 2) {  // INDEXING_TYPE_PREFIXES
            indexingEnum = getAppSearchClass(
                    "AppSearchSchema", "PropertyConfig", "INDEXING_TYPE_PREFIXES");
        } else {
            throw new ProcessingException("Unknown indexing type " + indexingType, property);
        }
        codeBlock.add("\n.setIndexingType($T)", indexingEnum);

        // Done!
        codeBlock.add("\n.build()");
        codeBlock.unindent();
        return codeBlock.build();
    }

    private MethodSpec createToGenericDocumentMethod() throws ProcessingException {
        // Method header
        TypeName classType = TypeName.get(mModel.getClassElement().asType());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toGenericDocument")
                .addModifiers(Modifier.PUBLIC)
                .returns(getAppSearchClass("GenericDocument"))
                .addParameter(classType, "dataClass");
        methodBuilder.addStatement("$T builder =\nnew $T<>($L, SCHEMA_TYPE)",
                ParameterizedTypeName.get(
                        getAppSearchClass("GenericDocument", "Builder"),
                        WildcardTypeName.subtypeOf(Object.class)),
                getAppSearchClass("GenericDocument", "Builder"),
                createAppSearchFieldRead(
                        mModel.getSpecialFieldName(AppSearchDocumentModel.SpecialField.URI)));

        // TODO(b/156296904): Set score, ttl and other special fields

        for (Map.Entry<String, VariableElement> entry : mModel.getPropertyFields().entrySet()) {
            fieldToGenericDoc(methodBuilder, entry.getKey(), entry.getValue());
        }

        methodBuilder.addStatement("return builder.build()");
        return methodBuilder.build();
    }

    /**
     * Converts a field from a data class into a format suitable for one of the
     * {@link androidx.appsearch.app.GenericDocument.Builder#setProperty} methods.
     */
    private void fieldToGenericDoc(
            @NonNull MethodSpec.Builder builder,
            @NonNull String fieldName,
            @NonNull VariableElement property) throws ProcessingException {
        // Scenario 1: field is a Collection
        //   1a: Collection contains boxed Long, Integer, Double, Float, Boolean or byte[].
        //       We have to pack it into a primitive array of type long[], double[], boolean[],
        //       or byte[][] by reading each element one-by-one and assigning it. The compiler takes
        //       care of unboxing.
        //
        //   1b: Collection contains String or GenericDocument.
        //       We have to convert this into an array of String[] or GenericDocument[], but no
        //       conversion of the collection elements is needed. We can use Collection#toArray for
        //       this.
        //
        //   1c: Collection contains a class which is annotated with @AppSearchDocument.
        //       We have to convert this into an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   1x: Collection contains any other kind of class. This unsupported and compilation
        //       fails.
        //       Note: Set<Byte[]>, Set<Byte>, and Set<Set<Byte>> are in this category. We don't
        //       support such conversions currently, but in principle they are possible and could
        //       be implemented.

        // Scenario 2: field is an Array
        //   2a: Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[],
        //       or Byte[].
        //       We have to pack it into a primitive array of type long[], double[], boolean[] or
        //       byte[] by reading each element one-by-one and assigning it. The compiler takes care
        //       of unboxing.
        //
        //   2b: Array is of type String[], long[], double[], boolean[], byte[][] or
        //       GenericDocument[].
        //       We can directly use this field with no conversion.
        //
        //   2c: Array is of a class which is annotated with @AppSearchDocument.
        //       We have to convert this into an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   2d: Array is of class byte[]. This is actually a single-valued field as byte arrays are
        //       natively supported by Icing, and is handled as Scenario 3a.
        //
        //   2x: Array is of any other kind of class. This unsupported and compilation fails.
        //       Note: Byte[][] is in this category. We don't support such conversions
        //       currently, but in principle they are possible and could be implemented.

        // Scenario 3: Single valued fields
        //   3a: Field is of type String, Long, Integer, Double, Float, Boolean, byte[] or
        //       GenericDocument.
        //       We can use this field directly, after testing for null. The java compiler will box
        //       or unbox as needed.
        //
        //   3b: Field is of type long, int, double, float, or boolean.
        //       We can use this field directly without testing for null.
        //
        //   3c: Field is of a class which is annotated with @AppSearchDocument.
        //       We have to convert this into a GenericDocument through the standard conversion
        //       machinery.
        //
        //   3x: Field is of any other kind of class. This is unsupported and compilation fails.

        if (tryScenario1Collection(builder, fieldName, property)) {
            return;
        }
        if (tryScenario2Array(builder, fieldName, property)) {
            return;
        }
        tryScenario3SingleField(builder, fieldName, property);
    }

    /**
     * If the given field is a Collection, generates code to read it and convert it into a form
     * suitable for GenericDocument and returns true. If the field is not a Collection, returns
     * false.
     */
    private boolean tryScenario1Collection(
            @NonNull MethodSpec.Builder method,
            @NonNull String fieldName,
            @NonNull VariableElement property) throws ProcessingException {
        Types typeUtil = mEnv.getTypeUtils();
        if (!typeUtil.isAssignable(typeUtil.erasure(property.asType()), mCollectionType)) {
            return false;  // This is not a scenario 1 collection
        }

        // Copy the field into a local variable to make it easier to refer to it repeatedly.
        CodeBlock.Builder builder = CodeBlock.builder()
                .addStatement(
                        "$T $NCopy = $L",
                        property.asType(),
                        fieldName,
                        createAppSearchFieldRead(fieldName));

        List<? extends TypeMirror> genericTypes =
                ((DeclaredType) property.asType()).getTypeArguments();
        TypeMirror propertyType = genericTypes.get(0);

        // TODO(b/156296904): Handle scenario 1c
        if (!tryType1a(builder, fieldName, propertyType)
                && !tryType1b(builder, fieldName, propertyType)) {
            // Scenario 1x
            throw new ProcessingException(
                    "Unhandled property type " + property.asType().toString(), property);
        }

        method.addCode(builder.build());
        return true;
    }

    //   1a: Collection contains boxed Long, Integer, Double, Float, Boolean or byte[].
    //       We have to pack it into a primitive array of type long[], double[], boolean[],
    //       or byte[][] by reading each element one-by-one and assigning it. The compiler takes
    //       care of unboxing.
    private boolean tryType1a(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        if (typeUtil.isSameType(propertyType, mLongBoxType)
                || typeUtil.isSameType(propertyType, mIntegerBoxType)) {
            body.addStatement(
                    "long[] $NConv = new long[$NCopy.size()]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mFloatBoxType)) {
            body.addStatement(
                    "double[] $NConv = new double[$NCopy.size()]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mBooleanBoxType)) {
            body.addStatement(
                    "boolean[] $NConv = new boolean[$NCopy.size()]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mByteArrayType)) {
            body.addStatement(
                    "byte[][] $NConv = new byte[$NCopy.size()][]", fieldName, fieldName);

        } else {
            // This is not a type 1a collection.
            return false;
        }

        // Iterate over each element of the collection, assigning it to the output array.
        body.addStatement("int i = 0")
                .add("for ($T item : $NCopy) {\n", propertyType, fieldName).indent()
                .addStatement("$NConv[i++] = item", fieldName)
                .unindent().add("}\n")
                .addStatement(
                        "builder.setProperty($S, $NConv)",
                        mFieldToPropertyName.get(fieldName),
                        fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    //   1b: Collection contains String or GenericDocument.
    //       We have to convert this into an array of String[] or GenericDocument[], but no
    //       conversion of the collection elements is needed. We can use Collection#toArray for
    //       this.
    private boolean tryType1b(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        // TODO(b/156296904): Handle GenericDocument
        if (typeUtil.isSameType(propertyType, mStringType)) {
            body.addStatement(
                    "String[] $NConv = $NCopy.toArray(new String[0])", fieldName, fieldName);

        } else {
            // This is not a type 1b collection.
            return false;
        }

        body.addStatement(
                "builder.setProperty($S, $NConv)", mFieldToPropertyName.get(fieldName), fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    /**
     * If the given field is an array, generates code to read it and convert it into a form suitable
     * for GenericDocument and returns true. If the field is not an array, returns false.
     */
    private boolean tryScenario2Array(
            @NonNull MethodSpec.Builder method,
            @NonNull String fieldName,
            @NonNull VariableElement property) throws ProcessingException {
        Types typeUtil = mEnv.getTypeUtils();
        if (property.asType().getKind() != TypeKind.ARRAY
                // Byte arrays have a native representation in Icing, so they are not considered a
                // "repeated" type
                || typeUtil.isSameType(property.asType(), mByteArrayType)) {
            return false;  // This is not a scenario 2 array
        }

        // Copy the field into a local variable to make it easier to refer to it repeatedly.
        CodeBlock.Builder builder = CodeBlock.builder()
                .addStatement(
                        "$T $NCopy = $L",
                        property.asType(),
                        fieldName,
                        createAppSearchFieldRead(fieldName));

        TypeMirror propertyType = ((ArrayType) property.asType()).getComponentType();

        // TODO(b/156296904): Handle scenario 2c
        if (!tryType2a(builder, fieldName, propertyType)
                && !tryType2b(builder, fieldName, propertyType)) {
            // Scenario 2x
            throw new ProcessingException(
                    "Unhandled property type " + property.asType().toString(), property);
        }

        method.addCode(builder.build());
        return true;
    }

    //   2a: Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[],
    //       or Byte[].
    //       We have to pack it into a primitive array of type long[], double[], boolean[] or
    //       byte[] by reading each element one-by-one and assigning it. The compiler takes care
    //       of unboxing.
    private boolean tryType2a(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        if (typeUtil.isSameType(propertyType, mLongBoxType)
                || typeUtil.isSameType(propertyType, mIntegerBoxType)
                || typeUtil.isSameType(propertyType, mIntPrimitiveType)) {
            body.addStatement(
                    "long[] $NConv = new long[$NCopy.length]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mFloatBoxType)
                || typeUtil.isSameType(propertyType, mFloatPrimitiveType)) {
            body.addStatement(
                    "double[] $NConv = new double[$NCopy.length]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mBooleanBoxType)) {
            body.addStatement(
                    "boolean[] $NConv = new boolean[$NCopy.length]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mByteBoxType)) {
            body.addStatement(
                    "byte[] $NConv = new byte[$NCopy.length]", fieldName, fieldName);

        } else {
            // This is not a type 2a array.
            return false;
        }

        // Iterate over each element of the array, assigning it to the output array.
        body.add("for (int i = 0 ; i < $NCopy.length ; i++) {\n", fieldName)
                .indent()
                .addStatement("$NConv[i] = $NCopy[i]", fieldName, fieldName)
                .unindent().add("}\n")
                .addStatement(
                        "builder.setProperty($S, $NConv)",
                        mFieldToPropertyName.get(fieldName),
                        fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    //   2b: Array is of type String[], long[], double[], boolean[], byte[][] or
    //       GenericDocument[].
    //       We can directly use this field with no conversion.
    private boolean tryType2b(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        // TODO(b/156296904): Handle GenericDocument
        if (!typeUtil.isSameType(propertyType, mStringType)
                && !typeUtil.isSameType(propertyType, mLongPrimitiveType)
                && !typeUtil.isSameType(propertyType, mDoublePrimitiveType)
                && !typeUtil.isSameType(propertyType, mBooleanPrimitiveType)
                && !typeUtil.isSameType(propertyType, mByteArrayType)) {
            // This is not a type 2b array.
            return false;
        }

        body.addStatement(
                "builder.setProperty($S, $NCopy)", mFieldToPropertyName.get(fieldName), fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    /**
     * Given a field which is a single element (non-collection), generates code to read it and
     * convert it into a form suitable for GenericDocument.
     */
    private void tryScenario3SingleField(
            @NonNull MethodSpec.Builder method,
            @NonNull String fieldName,
            @NonNull VariableElement property) throws ProcessingException {
        // TODO(b/156296904): Handle scenario 3c
        CodeBlock.Builder builder = CodeBlock.builder();
        if (!tryType3a(builder, fieldName, property.asType())
                && !tryType3b(builder, fieldName, property.asType())) {
            // Scenario 3x
            throw new ProcessingException(
                    "Unhandled property type " + property.asType().toString(), property);
        }
        method.addCode(builder.build());
    }

    //   3a: Field is of type String, Long, Integer, Double, Float, Boolean, byte[] or
    //       GenericDocument.
    //       We can use this field directly, after testing for null. The java compiler will box
    //       or unbox as needed.
    private boolean tryType3a(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        // Copy the field into a local variable to make it easier to refer to it repeatedly.
        CodeBlock.Builder builder = CodeBlock.builder()
                .addStatement(
                        "$T $NCopy = $L",
                        propertyType,
                        fieldName,
                        createAppSearchFieldRead(fieldName))
                .add("if ($NCopy != null) {\n", fieldName).indent();

        // TODO(b/156296904): Handle GenericDocument
        if (!typeUtil.isSameType(propertyType, mStringType)
                && !typeUtil.isSameType(propertyType, mLongBoxType)
                && !typeUtil.isSameType(propertyType, mIntegerBoxType)
                && !typeUtil.isSameType(propertyType, mDoubleBoxType)
                && !typeUtil.isSameType(propertyType, mFloatBoxType)
                && !typeUtil.isSameType(propertyType, mBooleanBoxType)
                && !typeUtil.isSameType(propertyType, mByteArrayType)) {
            // This is not a type 3a field
            return false;
        }

        builder.addStatement(
                "builder.setProperty($S, $NCopy)", mFieldToPropertyName.get(fieldName), fieldName)
                .unindent().add("}\n");

        method.add(builder.build());
        return true;
    }

    //   3b: Field is of type long, int, double, float, or boolean.
    //       We can use this field directly without testing for null.
    private boolean tryType3b(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        if (!typeUtil.isSameType(propertyType, mLongPrimitiveType)
                && !typeUtil.isSameType(propertyType, mIntPrimitiveType)
                && !typeUtil.isSameType(propertyType, mDoublePrimitiveType)
                && !typeUtil.isSameType(propertyType, mFloatPrimitiveType)
                && !typeUtil.isSameType(propertyType, mBooleanPrimitiveType)) {
            // This is not a type 3b field
            return false;
        }

        method.addStatement(
                "builder.setProperty($S, $L)",
                mFieldToPropertyName.get(fieldName),
                createAppSearchFieldRead(fieldName));
        return true;
    }

    private CodeBlock createAppSearchFieldRead(@NonNull String fieldName) {
        switch (mModel.getFieldReadKind(fieldName)) {
            case FIELD:
                return CodeBlock.of("dataClass.$N", fieldName);
            case GETTER:
                String getter = mModel.getAccessorName(fieldName, /*get=*/ true);
                return CodeBlock.of("dataClass.$N()", getter);
        }
        return null;
    }

    private ClassName getAppSearchClass(String clazz, String... nested) {
        return ClassName.get(APPSEARCH_PKG, clazz, nested);
    }
}
