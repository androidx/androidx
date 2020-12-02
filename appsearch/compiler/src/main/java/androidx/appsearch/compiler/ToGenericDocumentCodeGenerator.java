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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Generates java code for a translator from a data class to a
 * {@link androidx.appsearch.app.GenericDocument}.
 */
class ToGenericDocumentCodeGenerator {
    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mHelper;
    private final AppSearchDocumentModel mModel;

    public static void generate(
            @NonNull ProcessingEnvironment env,
            @NonNull AppSearchDocumentModel model,
            @NonNull TypeSpec.Builder classBuilder) throws ProcessingException {
        new ToGenericDocumentCodeGenerator(env, model).generate(classBuilder);
    }

    private ToGenericDocumentCodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull AppSearchDocumentModel model) {
        mEnv = env;
        mHelper = new IntrospectionHelper(env);
        mModel = model;
    }

    private void generate(TypeSpec.Builder classBuilder) throws ProcessingException {
        classBuilder.addMethod(createToGenericDocumentMethod());
    }

    private MethodSpec createToGenericDocumentMethod() throws ProcessingException {
        // Method header
        TypeName classType = TypeName.get(mModel.getClassElement().asType());
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toGenericDocument")
                .addModifiers(Modifier.PUBLIC)
                .returns(mHelper.getAppSearchClass("GenericDocument"))
                .addAnnotation(Override.class)
                .addParameter(classType, "dataClass")
                .addException(mHelper.getAppSearchExceptionClass());

        // Construct a new GenericDocument.Builder with the schema type and URI
        methodBuilder.addStatement("$T builder =\nnew $T<>($L, SCHEMA_TYPE)",
                ParameterizedTypeName.get(
                        mHelper.getAppSearchClass("GenericDocument", "Builder"),
                        WildcardTypeName.subtypeOf(Object.class)),
                mHelper.getAppSearchClass("GenericDocument", "Builder"),
                createAppSearchFieldRead(
                        mModel.getSpecialFieldName(AppSearchDocumentModel.SpecialField.URI)));

        setSpecialFields(methodBuilder);

        // Set properties
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
            @NonNull MethodSpec.Builder method,
            @NonNull String fieldName,
            @NonNull VariableElement property) throws ProcessingException {
        // Scenario 1: field is a Collection
        //   1a: CollectionForLoopAssign
        //       Collection contains boxed Long, Integer, Double, Float, Boolean or byte[].
        //       We have to pack it into a primitive array of type long[], double[], boolean[],
        //       or byte[][] by reading each element one-by-one and assigning it. The compiler takes
        //       care of unboxing.
        //
        //   1b: CollectionCallToArray
        //       Collection contains String or GenericDocument.
        //       We have to convert this into an array of String[] or GenericDocument[], but no
        //       conversion of the collection elements is needed. We can use Collection#toArray for
        //       this.
        //
        //   1c: CollectionForLoopCallToGenericDocument
        //       Collection contains a class which is annotated with @AppSearchDocument.
        //       We have to convert this into an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   1x: Collection contains any other kind of class. This unsupported and compilation
        //       fails.
        //       Note: Set<Byte[]>, Set<Byte>, and Set<Set<Byte>> are in this category. We don't
        //       support such conversions currently, but in principle they are possible and could
        //       be implemented.

        // Scenario 2: field is an Array
        //   2a: ArrayForLoopAssign
        //       Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[],
        //       or Byte[].
        //       We have to pack it into a primitive array of type long[], double[], boolean[] or
        //       byte[] by reading each element one-by-one and assigning it. The compiler takes care
        //       of unboxing.
        //
        //   2b: ArrayUseDirectly
        //       Array is of type String[], long[], double[], boolean[], byte[][] or
        //       GenericDocument[].
        //       We can directly use this field with no conversion.
        //
        //   2c: ArrayForLoopCallToGenericDocument
        //       Array is of a class which is annotated with @AppSearchDocument.
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
        //   3a: FieldUseDirectlyWithNullCheck
        //       Field is of type String, Long, Integer, Double, Float, Boolean, byte[] or
        //       GenericDocument.
        //       We can use this field directly, after testing for null. The java compiler will box
        //       or unbox as needed.
        //
        //   3b: FieldUseDirectlyWithoutNullCheck
        //       Field is of type long, int, double, float, or boolean.
        //       We can use this field directly without testing for null.
        //
        //   3c: FieldCallToGenericDocument
        //       Field is of a class which is annotated with @AppSearchDocument.
        //       We have to convert this into a GenericDocument through the standard conversion
        //       machinery.
        //
        //   3x: Field is of any other kind of class. This is unsupported and compilation fails.
        String propertyName = mModel.getPropertyName(property);
        if (tryConvertFromCollection(method, fieldName, propertyName, property)) {
            return;
        }
        if (tryConvertFromArray(method, fieldName, propertyName, property)) {
            return;
        }
        convertFromField(method, fieldName, propertyName, property);
    }

    /**
     * If the given field is a Collection, generates code to read it and convert it into a form
     * suitable for GenericDocument and returns true. If the field is not a Collection, returns
     * false.
     */
    private boolean tryConvertFromCollection(
            @NonNull MethodSpec.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull VariableElement property) throws ProcessingException {
        Types typeUtil = mEnv.getTypeUtils();
        if (!typeUtil.isAssignable(typeUtil.erasure(property.asType()), mHelper.mCollectionType)) {
            return false;  // This is not a scenario 1 collection
        }

        // Copy the field into a local variable to make it easier to refer to it repeatedly.
        CodeBlock.Builder body = CodeBlock.builder()
                .addStatement(
                        "$T $NCopy = $L",
                        property.asType(),
                        fieldName,
                        createAppSearchFieldRead(fieldName));

        List<? extends TypeMirror> genericTypes =
                ((DeclaredType) property.asType()).getTypeArguments();
        TypeMirror propertyType = genericTypes.get(0);

        if (!tryCollectionForLoopAssign(body, fieldName, propertyName, propertyType)           // 1a
                && !tryCollectionCallToArray(body, fieldName, propertyName, propertyType)      // 1b
                && !tryCollectionForLoopCallToGenericDocument(
                        body, fieldName, propertyName, propertyType)) {                        // 1c
            // Scenario 1x
            throw new ProcessingException(
                    "Unhandled out property type (1x): " + property.asType().toString(), property);
        }

        method.addCode(body.build());
        return true;
    }

    //   1a: CollectionForLoopAssign
    //       Collection contains boxed Long, Integer, Double, Float, Boolean or byte[].
    //       We have to pack it into a primitive array of type long[], double[], boolean[],
    //       or byte[][] by reading each element one-by-one and assigning it. The compiler takes
    //       care of unboxing.
    private boolean tryCollectionForLoopAssign(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        String setPropertyMethod;
        if (typeUtil.isSameType(propertyType, mHelper.mLongBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mIntegerBoxType)) {
            setPropertyMethod = "setPropertyLong";
            body.addStatement(
                    "long[] $NConv = new long[$NCopy.size()]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mHelper.mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mFloatBoxType)) {
            setPropertyMethod = "setPropertyDouble";
            body.addStatement(
                    "double[] $NConv = new double[$NCopy.size()]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mHelper.mBooleanBoxType)) {
            setPropertyMethod = "setPropertyBoolean";
            body.addStatement(
                    "boolean[] $NConv = new boolean[$NCopy.size()]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mHelper.mBytePrimitiveArrayType)) {
            setPropertyMethod = "setPropertyBytes";
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
                .addStatement("builder.$N($S, $NConv)", setPropertyMethod, propertyName, fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    //   1b: CollectionCallToArray
    //       Collection contains String. We have to convert this into an array of String[] or but no
    //       conversion of the collection elements is needed. We can use Collection#toArray for
    //       this.
    private boolean tryCollectionCallToArray(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        if (typeUtil.isSameType(propertyType, mHelper.mStringType)) {
            body.addStatement(
                    "String[] $NConv = $NCopy.toArray(new String[0])", fieldName, fieldName);

        } else {
            // This is not a type 1b collection.
            return false;
        }

        body.addStatement(
                "builder.setPropertyString($S, $NConv)", propertyName, fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    //   1c: CollectionForLoopCallToGenericDocument
    //       Collection contains a class which is annotated with @AppSearchDocument.
    //       We have to convert this into an array of GenericDocument[], by reading each element
    //       one-by-one and converting it through the standard conversion machinery.
    private boolean tryCollectionForLoopCallToGenericDocument(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        Element element = typeUtil.asElement(propertyType);
        if (element == null) {
            // The propertyType is not an element, this is not a type 1c list.
            return false;
        }
        try {
            mHelper.getAnnotation(element, IntrospectionHelper.APP_SEARCH_DOCUMENT_CLASS);
        } catch (ProcessingException e) {
            // The propertyType doesn't have @AppSearchDocument annotation, this is not a type 1c
            // list.
            return false;
        }

        body.addStatement("GenericDocument[] $NConv = new GenericDocument[$NCopy.size()]",
                fieldName, fieldName);
        body.addStatement("$T factory = $T.getInstance().getOrCreateFactory($T.class)",
                ParameterizedTypeName.get(mHelper.getAppSearchClass("DataClassFactory"),
                        TypeName.get(propertyType)),
                mHelper.getAppSearchClass("DataClassFactoryRegistry"), propertyType);

        body.addStatement("int i = 0");
        body.add("for ($T item : $NCopy) {\n", propertyType, fieldName).indent();
        body.addStatement("$NConv[i++] = factory.toGenericDocument(item)", fieldName);

        body.unindent().add("}\n");

        body.addStatement("builder.setPropertyDocument($S, $NConv)", propertyName, fieldName)
                .unindent().add("}\n");   //  if ($NCopy != null) {

        method.add(body.build());
        return true;
    }

    /**
     * If the given field is an array, generates code to read it and convert it into a form suitable
     * for GenericDocument and returns true. If the field is not an array, returns false.
     */
    private boolean tryConvertFromArray(
            @NonNull MethodSpec.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull VariableElement property) throws ProcessingException {
        Types typeUtil = mEnv.getTypeUtils();
        if (property.asType().getKind() != TypeKind.ARRAY
                // Byte arrays have a native representation in Icing, so they are not considered a
                // "repeated" type
                || typeUtil.isSameType(property.asType(), mHelper.mBytePrimitiveArrayType)) {
            return false;  // This is not a scenario 2 array
        }

        // Copy the field into a local variable to make it easier to refer to it repeatedly.
        CodeBlock.Builder body = CodeBlock.builder()
                .addStatement(
                        "$T $NCopy = $L",
                        property.asType(),
                        fieldName,
                        createAppSearchFieldRead(fieldName));

        TypeMirror propertyType = ((ArrayType) property.asType()).getComponentType();

        if (!tryArrayForLoopAssign(body, fieldName, propertyName, propertyType)                // 2a
                && !tryArrayUseDirectly(body, fieldName, propertyName, propertyType)           // 2b
                && !tryArrayForLoopCallToGenericDocument(
                        body, fieldName, propertyName, propertyType)) {                        // 2c
            // Scenario 2x
            throw new ProcessingException(
                    "Unhandled out property type (2x): " + property.asType().toString(), property);
        }

        method.addCode(body.build());
        return true;
    }

    //   2a: ArrayForLoopAssign
    //       Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[],
    //       or Byte[].
    //       We have to pack it into a primitive array of type long[], double[], boolean[] or
    //       byte[] by reading each element one-by-one and assigning it. The compiler takes care
    //       of unboxing.
    private boolean tryArrayForLoopAssign(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        String setPropertyMethod;
        if (typeUtil.isSameType(propertyType, mHelper.mLongBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mIntegerBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mIntPrimitiveType)) {
            setPropertyMethod = "setPropertyLong";
            body.addStatement(
                    "long[] $NConv = new long[$NCopy.length]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mHelper.mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mFloatBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mFloatPrimitiveType)) {
            setPropertyMethod = "setPropertyDouble";
            body.addStatement(
                    "double[] $NConv = new double[$NCopy.length]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mHelper.mBooleanBoxType)) {
            setPropertyMethod = "setPropertyBoolean";
            body.addStatement(
                    "boolean[] $NConv = new boolean[$NCopy.length]", fieldName, fieldName);

        } else if (typeUtil.isSameType(propertyType, mHelper.mByteBoxType)) {
            setPropertyMethod = "setPropertyBytes";
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
                .addStatement("builder.$N($S, $NConv)", setPropertyMethod, propertyName, fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    //   2b: ArrayUseDirectly
    //       Array is of type String[], long[], double[], boolean[], byte[][].
    //       We can directly use this field with no conversion.
    private boolean tryArrayUseDirectly(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        String setPropertyMethod;
        if (typeUtil.isSameType(propertyType, mHelper.mStringType)) {
            setPropertyMethod = "setPropertyString";
        } else if (typeUtil.isSameType(propertyType, mHelper.mLongPrimitiveType)) {
            setPropertyMethod = "setPropertyLong";
        } else if (typeUtil.isSameType(propertyType, mHelper.mDoublePrimitiveType)) {
            setPropertyMethod = "setPropertyDouble";
        } else if (typeUtil.isSameType(propertyType, mHelper.mBooleanPrimitiveType)) {
            setPropertyMethod = "setPropertyBoolean";
        } else if (typeUtil.isSameType(propertyType, mHelper.mBytePrimitiveArrayType)) {
            setPropertyMethod = "setPropertyBytes";
        } else {
            // This is not a type 2b array.
            return false;
        }

        body.addStatement(
                "builder.$N($S, $NCopy)", setPropertyMethod, propertyName, fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    //   2c: ArrayForLoopCallToGenericDocument
    //       Array is of a class which is annotated with @AppSearchDocument.
    //       We have to convert this into an array of GenericDocument[], by reading each element
    //       one-by-one and converting it through the standard conversion machinery.
    private boolean tryArrayForLoopCallToGenericDocument(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        CodeBlock.Builder body = CodeBlock.builder()
                .add("if ($NCopy != null) {\n", fieldName).indent();

        Element element = typeUtil.asElement(propertyType);
        if (element == null) {
            // The propertyType is not an element, this is not a type 1c list.
            return false;
        }
        try {
            mHelper.getAnnotation(element, IntrospectionHelper.APP_SEARCH_DOCUMENT_CLASS);
        } catch (ProcessingException e) {
            // The propertyType doesn't have @AppSearchDocument annotation, this is not a type 1c
            // list.
            return false;
        }

        body.addStatement("GenericDocument[] $NConv = new GenericDocument[$NCopy.length]",
                fieldName, fieldName);
        body.addStatement("$T factory = $T.getInstance().getOrCreateFactory($T.class)",
                ParameterizedTypeName.get(mHelper.getAppSearchClass("DataClassFactory"),
                        TypeName.get(propertyType)),
                mHelper.getAppSearchClass("DataClassFactoryRegistry"), propertyType);
        body.add("for (int i = 0; i < $NConv.length; i++) {\n", fieldName).indent();
        body.addStatement("$NConv[i] = factory.toGenericDocument($NCopy[i])",
                fieldName, fieldName);
        body.unindent().add("}\n");

        body.addStatement("builder.setPropertyDocument($S, $NConv)", propertyName, fieldName)
                .unindent().add("}\n");    //  if ($NCopy != null) {

        method.add(body.build());
        return true;
    }

    /**
     * Given a field which is a single element (non-collection), generates code to read it and
     * convert it into a form suitable for GenericDocument.
     */
    private void convertFromField(
            @NonNull MethodSpec.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull VariableElement property) throws ProcessingException {
        // TODO(b/156296904): Handle scenario 3c (FieldCallToGenericDocument)
        CodeBlock.Builder body = CodeBlock.builder();
        if (!tryFieldUseDirectlyWithNullCheck(
                body, fieldName, propertyName, property.asType())  // 3a
                && !tryFieldUseDirectlyWithoutNullCheck(
                        body, fieldName, propertyName, property.asType())  // 3b
                && !tryFieldCallToGenericDocument(
                        body, fieldName, propertyName, property.asType())) {  // 3c
            // Scenario 3x
            throw new ProcessingException(
                    "Unhandled out property type (3x): " + property.asType().toString(), property);
        }
        method.addCode(body.build());
    }

    //   3a: FieldUseDirectlyWithNullCheck
    //       Field is of type String, Long, Integer, Double, Float, Boolean, byte[].
    //       We can use this field directly, after testing for null. The java compiler will box
    //       or unbox as needed.
    private boolean tryFieldUseDirectlyWithNullCheck(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        // Copy the field into a local variable to make it easier to refer to it repeatedly.
        CodeBlock.Builder body = CodeBlock.builder()
                .addStatement(
                        "$T $NCopy = $L",
                        propertyType,
                        fieldName,
                        createAppSearchFieldRead(fieldName))
                .add("if ($NCopy != null) {\n", fieldName).indent();

        String setPropertyMethod;
        if (typeUtil.isSameType(propertyType, mHelper.mStringType)) {
            setPropertyMethod = "setPropertyString";
        } else if (typeUtil.isSameType(propertyType, mHelper.mLongBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mIntegerBoxType)) {
            setPropertyMethod = "setPropertyLong";
        } else if (typeUtil.isSameType(propertyType, mHelper.mDoubleBoxType)
                || typeUtil.isSameType(propertyType, mHelper.mFloatBoxType)) {
            setPropertyMethod = "setPropertyDouble";
        } else if (typeUtil.isSameType(propertyType, mHelper.mBooleanBoxType)) {
            setPropertyMethod = "setPropertyBoolean";
        } else if (typeUtil.isSameType(propertyType, mHelper.mBytePrimitiveArrayType)) {
            setPropertyMethod = "setPropertyBytes";
        } else {
            // This is not a type 3a field
            return false;
        }

        body.addStatement(
                "builder.$N($S, $NCopy)", setPropertyMethod, propertyName, fieldName)
                .unindent().add("}\n");

        method.add(body.build());
        return true;
    }

    //   3b: FieldUseDirectlyWithoutNullCheck
    //       Field is of type long, int, double, float, or boolean.
    //       We can use this field directly without testing for null.
    private boolean tryFieldUseDirectlyWithoutNullCheck(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();
        String setPropertyMethod;
        if (typeUtil.isSameType(propertyType, mHelper.mLongPrimitiveType)
                || typeUtil.isSameType(propertyType, mHelper.mIntPrimitiveType)) {
            setPropertyMethod = "setPropertyLong";
        } else if (typeUtil.isSameType(propertyType, mHelper.mDoublePrimitiveType)
                || typeUtil.isSameType(propertyType, mHelper.mFloatPrimitiveType)) {
            setPropertyMethod = "setPropertyDouble";
        } else if (typeUtil.isSameType(propertyType, mHelper.mBooleanPrimitiveType)) {
            setPropertyMethod = "setPropertyBoolean";
        } else {
            // This is not a type 3b field
            return false;
        }

        method.addStatement(
                "builder.$N($S, $L)",
                setPropertyMethod,
                propertyName,
                createAppSearchFieldRead(fieldName));
        return true;
    }

    //   3c: FieldCallToGenericDocument
    //       Field is of a class which is annotated with @AppSearchDocument.
    //       We have to convert this into a GenericDocument through the standard conversion
    //       machinery.
    private boolean tryFieldCallToGenericDocument(
            @NonNull CodeBlock.Builder method,
            @NonNull String fieldName,
            @NonNull String propertyName,
            @NonNull TypeMirror propertyType) {
        Types typeUtil = mEnv.getTypeUtils();

        Element element = typeUtil.asElement(propertyType);
        if (element == null) {
            // The propertyType is not an element, this is not a type 3c field.
            return false;
        }
        try {
            mHelper.getAnnotation(element, IntrospectionHelper.APP_SEARCH_DOCUMENT_CLASS);
        } catch (ProcessingException e) {
            // The propertyType doesn't have @AppSearchDocument annotation, this is not a type 3c
            // field.
            return false;
        }
        method.addStatement("$T $NCopy = $L", propertyType, propertyName,
                createAppSearchFieldRead(fieldName));

        method.add("if ($NCopy != null) {\n", propertyName).indent();

        method.addStatement("GenericDocument $NConv = $T.getInstance().getOrCreateFactory($T.class)"
                        + ".toGenericDocument($NCopy)", fieldName,
                mHelper.getAppSearchClass("DataClassFactoryRegistry"), propertyType,
                propertyName);
        method.addStatement("builder.setPropertyDocument($S, $NConv)", propertyName, fieldName);

        method.unindent().add("}\n");
        return true;
    }

    private void setSpecialFields(MethodSpec.Builder method) {
        for (AppSearchDocumentModel.SpecialField specialField :
                AppSearchDocumentModel.SpecialField.values()) {
            String fieldName = mModel.getSpecialFieldName(specialField);
            if (fieldName == null) {
                continue;  // The data class doesn't have this field, so no need to set it.
            }
            switch (specialField) {
                case URI:
                    break;  // Always provided to builder constructor; cannot be set separately.
                case NAMESPACE:
                    method.addCode(CodeBlock.builder()
                            .addStatement(
                                    "String $NCopy = $L",
                                    fieldName, createAppSearchFieldRead(fieldName))
                            .add("if ($NCopy != null) {\n", fieldName).indent()
                            .addStatement("builder.setNamespace($NCopy)", fieldName)
                            .unindent().add("}\n")
                            .build());
                    break;
                case CREATION_TIMESTAMP_MILLIS:
                    method.addStatement(
                            "builder.setCreationTimestampMillis($L)",
                            createAppSearchFieldRead(fieldName));
                    break;
                case TTL_MILLIS:
                    method.addStatement(
                            "builder.setTtlMillis($L)", createAppSearchFieldRead(fieldName));
                    break;
                case SCORE:
                    method.addStatement(
                            "builder.setScore($L)", createAppSearchFieldRead(fieldName));
                    break;
            }
        }
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
}
