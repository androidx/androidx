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

import static androidx.appsearch.compiler.CodegenUtils.createNewArrayExpr;
import static androidx.appsearch.compiler.IntrospectionHelper.APPSEARCH_EXCEPTION_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.GENERIC_DOCUMENT_CLASS;

import androidx.annotation.NonNull;
import androidx.appsearch.compiler.AnnotatedGetterOrField.ElementTypeCategory;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.LongPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.SerializerClass;
import androidx.appsearch.compiler.annotationwrapper.StringPropertyAnnotation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;

/**
 * Generates java code for a translator from a {@code androidx.appsearch.app.GenericDocument} to
 * an instance of a class annotated with {@code androidx.appsearch.annotation.Document}.
 */
class FromGenericDocumentCodeGenerator {
    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mHelper;
    private final DocumentModel mModel;

    private FromGenericDocumentCodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull DocumentModel model) {
        mEnv = env;
        mHelper = new IntrospectionHelper(env);
        mModel = model;
    }

    public static void generate(
            @NonNull ProcessingEnvironment env,
            @NonNull DocumentModel model,
            @NonNull TypeSpec.Builder classBuilder) {
        new FromGenericDocumentCodeGenerator(env, model).generate(classBuilder);
    }

    private void generate(TypeSpec.Builder classBuilder) {
        classBuilder.addMethod(createFromGenericDocumentMethod());
    }

    private MethodSpec createFromGenericDocumentMethod() {
        // Method header
        TypeName documentClass = TypeName.get(mModel.getClassElement().asType());
        // The type of documentClassMap is Map<String, List<String>>.
        TypeName documentClassMapType = ParameterizedTypeName.get(ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get(String.class)));
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("fromGenericDocument")
                .addModifiers(Modifier.PUBLIC)
                .returns(documentClass)
                .addAnnotation(Override.class)
                .addParameter(GENERIC_DOCUMENT_CLASS, "genericDoc")
                .addParameter(documentClassMapType, "documentClassMap")
                .addException(APPSEARCH_EXCEPTION_CLASS);

        // Unpack properties from the GenericDocument into the format desired by the document class.
        // Unpack metadata properties first, then data properties.
        for (AnnotatedGetterOrField getterOrField : mModel.getAnnotatedGettersAndFields()) {
            if (getterOrField.getAnnotation().getPropertyKind()
                    != PropertyAnnotation.Kind.METADATA_PROPERTY) {
                continue;
            }
            methodBuilder.addCode(createCodeToExtractFromGenericDoc(
                    (MetadataPropertyAnnotation) getterOrField.getAnnotation(), getterOrField));
        }
        for (AnnotatedGetterOrField getterOrField : mModel.getAnnotatedGettersAndFields()) {
            if (getterOrField.getAnnotation().getPropertyKind()
                    != PropertyAnnotation.Kind.DATA_PROPERTY) {
                continue;
            }
            methodBuilder.addCode(createCodeToExtractFromGenericDoc(
                    (DataPropertyAnnotation) getterOrField.getAnnotation(), getterOrField));
        }

        // Create an instance of the document class/builder via the chosen create method.
        DocumentClassCreationInfo documentClassCreationInfo = mModel.getDocumentClassCreationInfo();
        CreationMethod creationMethod = documentClassCreationInfo.getCreationMethod();
        String variableName = creationMethod.returnsBuilder() ? "builder" : "document";
        List<CodeBlock> params = creationMethod.getParamAssociations().stream()
                .map(annotatedGetterOrField ->
                        CodeBlock.of("$NConv", annotatedGetterOrField.getJvmName()))
                .toList();
        if (creationMethod.isConstructor()) {
            methodBuilder.addStatement("$T $N = new $T($L)",
                    creationMethod.getReturnType(),
                    variableName,
                    creationMethod.getReturnType(),
                    CodeBlock.join(params, /* separator= */", "));
        } else {
            // static method
            methodBuilder.addStatement("$T $N = $T.$N($L)",
                    creationMethod.getReturnType(),
                    variableName,
                    creationMethod.getEnclosingClass(),
                    creationMethod.getJvmName(),
                    CodeBlock.join(params, /* separator= */", "));
        }

        // Assign all fields which weren't set in the creation method
        for (Map.Entry<AnnotatedGetterOrField, SetterOrField> entry :
                documentClassCreationInfo.getSettersAndFields().entrySet()) {
            AnnotatedGetterOrField getterOrField = entry.getKey();
            SetterOrField setterOrField = entry.getValue();
            if (setterOrField.isSetter()) {
                methodBuilder.addStatement("$N.$N($NConv)",
                        variableName, setterOrField.getJvmName(), getterOrField.getJvmName());
            } else {
                // field
                methodBuilder.addStatement("$N.$N = $NConv",
                        variableName, setterOrField.getJvmName(), getterOrField.getJvmName());
            }
        }

        if (creationMethod.returnsBuilder()) {
            methodBuilder.addStatement("return $N.build()", variableName);
        } else {
            methodBuilder.addStatement("return $N", variableName);
        }

        return methodBuilder.build();
    }

    /**
     * Returns code that copies the metadata property out of a generic document document.
     *
     * <p>Assumes there is a generic document var in-scope called {@code genericDoc}.
     *
     * <p>Leaves a variable in the scope with the name {@code {JVM_NAME}Conv} with the final result.
     * This variable is guaranteed to be of the same type as the {@link AnnotatedGetterOrField}'s
     * JVM type.
     */
    private CodeBlock createCodeToExtractFromGenericDoc(
            @NonNull MetadataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        // All metadata properties in a GenericDocument either use primitives or Strings and
        // their getters always return non-null values
        // e.g. genericDoc.getId() -> String, genericDoc.getTtlMillis() -> long
        return CodeBlock.builder()
                .addStatement("$T $NConv = $L",
                        getterOrField.getJvmType(),
                        getterOrField.getJvmName(),
                        maybeApplyNarrowingCast(
                                CodeBlock.of(
                                        "genericDoc.$N()", annotation.getGenericDocGetterName()),
                                /* exprType= */
                                annotation.getUnderlyingTypeWithinGenericDoc(mHelper),
                                /* targetType= */getterOrField.getJvmType()))
                .build();
    }

    /**
     * Returns code that copies the data property out of a generic document document.
     *
     * <p>Assumes there is a generic document var in-scope called {@code genericDoc}.
     *
     * <p>Leaves a variable in the scope with the name {@code {JVM_NAME}Conv} with the final result.
     * This variable is guaranteed to be of the same type as the {@link AnnotatedGetterOrField}'s
     * JVM type.
     */
    private CodeBlock createCodeToExtractFromGenericDoc(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        // Scenario 1: field is assignable from List
        //   1a: ListForLoopAssign
        //       List contains boxed Long, Integer, Double, Float, Boolean or byte[]. We have to
        //       unpack it from a primitive array of type long[], double[], boolean[], or byte[][]
        //       by reading each element one-by-one and assigning it. The compiler takes care of
        //       unboxing.
        //
        //   1b: ListCallArraysAsList
        //       List contains String or EmbeddingVector. We have to convert this from
        //       an array of String[] or EmbeddingVector[], but no conversion of the
        //       collection elements is needed. We can use Arrays#asList for this.
        //
        //   1c: ListForLoopCallFromGenericDocument
        //       List contains a class which is annotated with @Document.
        //       We have to convert this from an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   1d: ListForLoopCallDeserialize
        //       List contains a custom type for which we have a serializer.
        //       We have to convert this from an array of String[]|long[], by reading each element
        //       one-by-one and calling serializerClass.deserialize(element).

        // Scenario 2: field is an Array
        //   2a: ArrayForLoopAssign
        //       Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[],
        //       or Byte[].
        //       We have to unpack it from a primitive array of type long[], double[], boolean[] or
        //       byte[] by reading each element one-by-one and assigning it. The compiler takes care
        //       of unboxing.
        //
        //   2b: ArrayUseDirectly
        //       Array is of type String[], long[], double[], boolean[], byte[][] or
        //       EmbeddingVector[].
        //       We can directly use this field with no conversion.
        //
        //   2c: ArrayForLoopCallFromGenericDocument
        //       Array is of a class which is annotated with @Document.
        //       We have to convert this from an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   2d: ArrayForLoopCallDeserialize
        //       Array is of a custom type for which we have a serializer.
        //       We have to convert this from an array of String[]|long[], by reading each element
        //       one-by-one and calling serializerClass.deserialize(element).
        //
        //   2e: Array is of class byte[]. This is actually a single-valued field as byte arrays are
        //       natively supported by Icing, and is handled as Scenario 3a.

        // Scenario 3: Single valued fields
        //   3a: FieldUseDirectlyWithNullCheck
        //       Field is of type String, Long, Integer, Double, Float, Boolean, byte[] or
        //       EmbeddingVector.
        //       We can use this field directly, after testing for null. The java compiler will box
        //       or unbox as needed.
        //
        //   3b: FieldUseDirectlyWithoutNullCheck
        //       Field is of type long, int, double, float, or boolean.
        //       We can use this field directly. Since we cannot assign null, we must assign the
        //       default value if the field is not specified. The java compiler will box or unbox as
        //       needed
        //
        //   3c: FieldCallFromGenericDocument
        //       Field is of a class which is annotated with @Document.
        //       We have to convert this from a GenericDocument through the standard conversion
        //       machinery.
        //
        //   3d: FieldCallDeserialize
        //       Field is of a custom type for which we have a serializer.
        //       We have to convert this from a String|long by calling
        //       serializerClass.deserialize(value).
        ElementTypeCategory typeCategory = getterOrField.getElementTypeCategory();
        switch (annotation.getDataPropertyKind()) {
            case STRING_PROPERTY:
                SerializerClass stringSerializer =
                        ((StringPropertyAnnotation) annotation).getCustomSerializer();
                switch (typeCategory) {
                    case COLLECTION:
                        if (stringSerializer != null) { // List<CustomType>: 1d
                            return listForLoopCallDeserialize(
                                    annotation, getterOrField, stringSerializer);
                        } else { // List<String>: 1b
                            return listCallArraysAsList(annotation, getterOrField);
                        }
                    case ARRAY:
                        if (stringSerializer != null) { // CustomType[]: 2d
                            return arrayForLoopCallDeserialize(
                                    annotation, getterOrField, stringSerializer);
                        } else { // String[]: 2b
                            return arrayUseDirectly(annotation, getterOrField);
                        }
                    case SINGLE:
                        if (stringSerializer != null) { // CustomType: 3d
                            return fieldCallDeserialize(
                                    annotation, getterOrField, stringSerializer);
                        } else { // String: 3a
                            return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                        }
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case DOCUMENT_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<Person>: 1c
                        return listForLoopCallFromGenericDocument(annotation, getterOrField);
                    case ARRAY: // Person[]: 2c
                        return arrayForLoopCallFromGenericDocument(annotation, getterOrField);
                    case SINGLE: // Person: 3c
                        return fieldCallFromGenericDocument(annotation, getterOrField);
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case LONG_PROPERTY:
                SerializerClass longSerializer =
                        ((LongPropertyAnnotation) annotation).getCustomSerializer();
                switch (typeCategory) {
                    case COLLECTION:
                        if (longSerializer != null) { // List<CustomType>: 1d
                            return listForLoopCallDeserialize(
                                    annotation, getterOrField, longSerializer);
                        } else { // List<Long>|List<Integer>: 1a
                            return listForLoopAssign(annotation, getterOrField);
                        }
                    case ARRAY:
                        if (longSerializer != null) { // CustomType[]: 2d
                            return arrayForLoopCallDeserialize(
                                    annotation, getterOrField, longSerializer);
                        } else if (mHelper.isPrimitiveLongArray(getterOrField.getJvmType())) {
                            // long[]: 2b
                            return arrayUseDirectly(annotation, getterOrField);
                        } else { // int[]|Integer[]|Long[]: 2a
                            return arrayForLoopAssign(annotation, getterOrField);
                        }
                    case SINGLE:
                        if (longSerializer != null) { // CustomType: 3d
                            return fieldCallDeserialize(annotation, getterOrField, longSerializer);
                        } else if (getterOrField.getJvmType() instanceof PrimitiveType) {
                            // long|int: 3b
                            return fieldUseDirectlyWithoutNullCheck(annotation, getterOrField);
                        } else { // Long|Integer: 3a
                            return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                        }
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case DOUBLE_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<Double>|List<Float>: 1a
                        return listForLoopAssign(annotation, getterOrField);
                    case ARRAY:
                        if (mHelper.isPrimitiveDoubleArray(getterOrField.getJvmType())) {
                            // double[]: 2b
                            return arrayUseDirectly(annotation, getterOrField);
                        } else {
                            // float[]|Float[]|Double[]: 2a
                            return arrayForLoopAssign(annotation, getterOrField);
                        }
                    case SINGLE:
                        if (getterOrField.getJvmType() instanceof PrimitiveType) {
                            // double|float: 3b
                            return fieldUseDirectlyWithoutNullCheck(annotation, getterOrField);
                        } else {
                            // Double|Float: 3a
                            return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                        }
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case BOOLEAN_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<Boolean>: 1a
                        return listForLoopAssign(annotation, getterOrField);
                    case ARRAY:
                        if (mHelper.isPrimitiveBooleanArray(getterOrField.getJvmType())) {
                            // boolean[]: 2b
                            return arrayUseDirectly(annotation, getterOrField);
                        } else {
                            // Boolean[]
                            return arrayForLoopAssign(annotation, getterOrField);
                        }
                    case SINGLE:
                        if (getterOrField.getJvmType() instanceof PrimitiveType) {
                            // boolean: 3b
                            return fieldUseDirectlyWithoutNullCheck(annotation, getterOrField);
                        } else {
                            // Boolean: 3a
                            return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                        }
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case BYTES_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<byte[]>: 1a
                        return listForLoopAssign(annotation, getterOrField);
                    case ARRAY: // byte[][]: 2b
                        return arrayUseDirectly(annotation, getterOrField);
                    case SINGLE: // byte[]: 2e/3a
                        return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case EMBEDDING_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<EmbeddingVector>: 1b
                        return listCallArraysAsList(annotation, getterOrField);
                    case ARRAY:
                        // EmbeddingVector[]: 2b
                        return arrayUseDirectly(annotation, getterOrField);
                    case SINGLE:
                        // EmbeddingVector: 3a
                        return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }
    }

    // 1a: ListForLoopAssign
    //     List contains boxed Long, Integer, Double, Float, Boolean or byte[]. We have to
    //     unpack it from a primitive array of type long[], double[], boolean[], or byte[][]
    //     by reading each element one-by-one and assigning it. The compiler takes care of
    //     unboxing.
    @NonNull
    private CodeBlock listForLoopAssign(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror serializedType = annotation.getUnderlyingTypeWithinGenericDoc(mHelper);
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.$N($S)",
                        serializedType,
                        getterOrField.getJvmName(),
                        annotation.getGenericDocArrayGetterName(),
                        annotation.getName())
                .addStatement("$T<$T> $NConv = null",
                        List.class, getterOrField.getComponentType(), getterOrField.getJvmName())
                .beginControlFlow("if ($NCopy != null)", getterOrField.getJvmName())
                .addStatement("$NConv = new $T<>($NCopy.length)",
                        getterOrField.getJvmName(), ArrayList.class, getterOrField.getJvmName())
                .beginControlFlow("for (int i = 0; i < $NCopy.length; i++)",
                        getterOrField.getJvmName())
                .addStatement("$NConv.add($L)",
                        getterOrField.getJvmName(),
                        maybeApplyNarrowingCast(
                                CodeBlock.of("$NCopy[i]", getterOrField.getJvmName()),
                                /* exprType= */serializedType,
                                /* targetType= */getterOrField.getComponentType()))
                .endControlFlow() // for (...)
                .endControlFlow() // if (...)
                .build();
    }

    // 1b: ListCallArraysAsList
    //     List contains String or EmbeddingVector. We have to convert this from
    //     an array of String[] or EmbeddingVector[], but no conversion of the
    //     collection elements is needed. We can use Arrays#asList for this.
    @NonNull
    private CodeBlock listCallArraysAsList(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.$N($S)",
                        annotation.getUnderlyingTypeWithinGenericDoc(mHelper),
                        getterOrField.getJvmName(),
                        annotation.getGenericDocArrayGetterName(),
                        annotation.getName())
                .addStatement("$T<$T> $NConv = null",
                        List.class, getterOrField.getComponentType(), getterOrField.getJvmName())
                .beginControlFlow("if ($NCopy != null)", getterOrField.getJvmName())
                .addStatement("$NConv = $T.asList($NCopy)",
                        getterOrField.getJvmName(), Arrays.class, getterOrField.getJvmName())
                .endControlFlow() // if (...)
                .build();
    }

    // 1c: ListForLoopCallFromGenericDocument
    //     List contains a class which is annotated with @Document.
    //     We have to convert this from an array of GenericDocument[], by reading each element
    //     one-by-one and converting it through the standard conversion machinery.
    @NonNull
    private CodeBlock listForLoopCallFromGenericDocument(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.getPropertyDocumentArray($S)",
                        GENERIC_DOCUMENT_CLASS, getterOrField.getJvmName(), annotation.getName())
                .addStatement("$T<$T> $NConv = null",
                        List.class, getterOrField.getComponentType(), getterOrField.getJvmName())
                .beginControlFlow("if ($NCopy != null)", getterOrField.getJvmName())
                .addStatement("$NConv = new $T<>($NCopy.length)",
                        getterOrField.getJvmName(), ArrayList.class, getterOrField.getJvmName())
                .beginControlFlow("for (int i = 0; i < $NCopy.length; i++)",
                        getterOrField.getJvmName())
                .addStatement("$NConv.add($NCopy[i].toDocumentClass($T.class, documentClassMap))",
                        getterOrField.getJvmName(),
                        getterOrField.getJvmName(),
                        getterOrField.getComponentType())
                .endControlFlow() // for (...)
                .endControlFlow() // if (...)
                .build();
    }

    // 1d: ListForLoopCallDeserialize
    //     List contains a custom type for which we have a serializer.
    //     We have to convert this from an array of String[]|long[], by reading each element
    //     one-by-one and calling serializerClass.deserialize(element).
    @NonNull
    private CodeBlock listForLoopCallDeserialize(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull SerializerClass serializerClass) {
        TypeMirror customType = getterOrField.getComponentType();
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|prop
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.$N($S)",
                        annotation.getUnderlyingTypeWithinGenericDoc(mHelper),
                        jvmName,
                        annotation.getGenericDocArrayGetterName(),
                        annotation.getName())
                .addStatement("$T<$T> $NConv = null", List.class, customType, jvmName)
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$NConv = new $T<>($NCopy.length)", jvmName, ArrayList.class, jvmName)
                .addStatement("$T serializer = new $T()",
                        serializerClass.getElement(), serializerClass.getElement())
                .beginControlFlow("for (int i = 0; i < $NCopy.length; i++)", jvmName)
                .addStatement("$T elem = serializer.deserialize($NCopy[i])", customType, jvmName)
                .beginControlFlow("if (elem == null)")
                // Deserialization failed
                // Abort the whole transaction since we cannot preserve the same element indices
                // as the underlying data.
                .addStatement("$NConv = null", jvmName)
                .addStatement("break")
                .endControlFlow() // if (elem == null)
                .addStatement("$NConv.add(elem)", jvmName)
                .endControlFlow() // for (...)
                .endControlFlow() // if ($NCopy != null)
                .build();
    }

    // 2a: ArrayForLoopAssign
    //     Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[],
    //     or Byte[].
    //     We have to unpack it from a primitive array of type long[], double[], boolean[] or
    //     byte[] by reading each element one-by-one and assigning it. The compiler takes care
    //     of unboxing.
    @NonNull
    private CodeBlock arrayForLoopAssign(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror serializedType = annotation.getUnderlyingTypeWithinGenericDoc(mHelper);
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.$N($S)",
                        serializedType,
                        getterOrField.getJvmName(),
                        annotation.getGenericDocArrayGetterName(),
                        annotation.getName())
                .addStatement("$T[] $NConv = null",
                        getterOrField.getComponentType(), getterOrField.getJvmName())
                .beginControlFlow("if ($NCopy != null)", getterOrField.getJvmName())
                .addStatement("$NConv = $L",
                        getterOrField.getJvmName(),
                        createNewArrayExpr(
                                getterOrField.getComponentType(),
                                /* size= */
                                CodeBlock.of("$NCopy.length", getterOrField.getJvmName()),
                                mEnv))
                .beginControlFlow("for (int i = 0; i < $NCopy.length; i++)",
                        getterOrField.getJvmName())
                .addStatement("$NConv[i] = $L",
                        getterOrField.getJvmName(),
                        maybeApplyNarrowingCast(
                                CodeBlock.of("$NCopy[i]", getterOrField.getJvmName()),
                                /* exprType= */serializedType,
                                /* targetType= */getterOrField.getComponentType()))
                .endControlFlow() // for (...)
                .endControlFlow() // if (...)
                .build();
    }

    // 2b: ArrayUseDirectly
    //     Array is of type String[], long[], double[], boolean[], byte[][] or
    //     EmbeddingVector[].
    @NonNull
    private CodeBlock arrayUseDirectly(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        return CodeBlock.builder()
                .addStatement("$T[] $NConv = genericDoc.$N($S)",
                        annotation.getUnderlyingTypeWithinGenericDoc(mHelper),
                        getterOrField.getJvmName(),
                        annotation.getGenericDocArrayGetterName(),
                        annotation.getName())
                .build();
    }

    // 2c: ArrayForLoopCallFromGenericDocument
    //     Array is of a class which is annotated with @Document.
    //     We have to convert this from an array of GenericDocument[], by reading each element
    //     one-by-one and converting it through the standard conversion machinery.
    @NonNull
    private CodeBlock arrayForLoopCallFromGenericDocument(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.getPropertyDocumentArray($S)",
                        GENERIC_DOCUMENT_CLASS, getterOrField.getJvmName(), annotation.getName())
                .addStatement("$T[] $NConv = null",
                        getterOrField.getComponentType(), getterOrField.getJvmName())
                .beginControlFlow("if ($NCopy != null)", getterOrField.getJvmName())
                .addStatement("$NConv = new $T[$NCopy.length]",
                        getterOrField.getJvmName(),
                        getterOrField.getComponentType(),
                        getterOrField.getJvmName())
                .beginControlFlow("for (int i = 0; i < $NCopy.length; i++)",
                        getterOrField.getJvmName())
                .addStatement("$NConv[i] = $NCopy[i].toDocumentClass($T.class, documentClassMap)",
                        getterOrField.getJvmName(),
                        getterOrField.getJvmName(),
                        getterOrField.getComponentType())
                .endControlFlow() // for (...)
                .endControlFlow() // if (...)
                .build();
    }

    // 2d: ArrayForLoopCallDeserialize
    //     Array is of a custom type for which we have a serializer.
    //     We have to convert this from an array of String[]|long[], by reading each element
    //     one-by-one and calling serializerClass.deserialize(element).
    @NonNull
    private CodeBlock arrayForLoopCallDeserialize(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull SerializerClass serializerClass) {
        TypeMirror customType = getterOrField.getComponentType();
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|prop
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.$N($S)",
                        annotation.getUnderlyingTypeWithinGenericDoc(mHelper),
                        jvmName,
                        annotation.getGenericDocArrayGetterName(),
                        annotation.getName())
                .addStatement("$T[] $NConv = null", customType, jvmName)
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$NConv = $L",
                        jvmName,
                        createNewArrayExpr(
                                customType,
                                /* size= */CodeBlock.of("$NCopy.length", jvmName),
                                mEnv))
                .addStatement("$T serializer = new $T()",
                        serializerClass.getElement(), serializerClass.getElement())
                .beginControlFlow("for (int i = 0; i < $NCopy.length; i++)", jvmName)
                .addStatement("$T elem = serializer.deserialize($NCopy[i])", customType, jvmName)
                .beginControlFlow("if (elem == null)")
                // Deserialization failed
                // Abort the whole transaction since we cannot preserve the same element indices
                // as the underlying data.
                .addStatement("$NConv = null", jvmName)
                .addStatement("break")
                .endControlFlow() // if (elem == null)
                .addStatement("$NConv[i] = elem", jvmName)
                .endControlFlow() // for (...)
                .endControlFlow() // if ($NCopy != null)
                .build();
    }

    // 3a: FieldUseDirectlyWithNullCheck
    //     Field is of type String, Long, Integer, Double, Float, Boolean, byte[] or
    //     EmbeddingVector.
    //     We can use this field directly, after testing for null. The java compiler will box
    //     or unbox as needed.
    @NonNull
    private CodeBlock fieldUseDirectlyWithNullCheck(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror serializedType = annotation.getUnderlyingTypeWithinGenericDoc(mHelper);
        return CodeBlock.builder()
                .addStatement("$T[] $NCopy = genericDoc.$N($S)",
                        serializedType,
                        getterOrField.getJvmName(),
                        annotation.getGenericDocArrayGetterName(),
                        annotation.getName())
                .addStatement("$T $NConv = null",
                        getterOrField.getJvmType(), getterOrField.getJvmName())
                .beginControlFlow("if ($NCopy != null && $NCopy.length != 0)",
                        getterOrField.getJvmName(), getterOrField.getJvmName())
                .addStatement("$NConv = $L",
                        getterOrField.getJvmName(),
                        maybeApplyNarrowingCast(
                                CodeBlock.of("$NCopy[0]", getterOrField.getJvmName()),
                                /* exprType= */serializedType,
                                /* targetType= */getterOrField.getJvmType()))
                .endControlFlow() // if (...)
                .build();
    }

    // 3b: FieldUseDirectlyWithoutNullCheck
    //     Field is of type long, int, double, float, or boolean.
    //     We can use this field directly. Since we cannot assign null, we must assign the
    //     default value if the field is not specified. The java compiler will box or unbox as
    //     needed
    @NonNull
    private CodeBlock fieldUseDirectlyWithoutNullCheck(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        return CodeBlock.builder()
                .addStatement("$T $NConv = $L",
                        getterOrField.getJvmType(),
                        getterOrField.getJvmName(),
                        maybeApplyNarrowingCast(
                                CodeBlock.of("genericDoc.$N($S)",
                                        annotation.getGenericDocGetterName(), annotation.getName()),
                                /* exprType= */
                                annotation.getUnderlyingTypeWithinGenericDoc(mHelper),
                                /* targetType= */getterOrField.getJvmType()))
                .build();
    }

    // 3c: FieldCallFromGenericDocument
    //     Field is of a class which is annotated with @Document.
    //     We have to convert this from a GenericDocument through the standard conversion
    //     machinery.
    @NonNull
    private CodeBlock fieldCallFromGenericDocument(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        return CodeBlock.builder()
                .addStatement("$T $NCopy = genericDoc.getPropertyDocument($S)",
                        GENERIC_DOCUMENT_CLASS, getterOrField.getJvmName(), annotation.getName())
                .addStatement("$T $NConv = null",
                        getterOrField.getJvmType(), getterOrField.getJvmName())
                .beginControlFlow("if ($NCopy != null)", getterOrField.getJvmName())
                .addStatement("$NConv = $NCopy.toDocumentClass($T.class, documentClassMap)",
                        getterOrField.getJvmName(),
                        getterOrField.getJvmName(),
                        getterOrField.getJvmType())
                .endControlFlow() // if (...)
                .build();
    }

    // 3d: FieldCallDeserialize
    //     Field is of a custom type for which we have a serializer.
    //     We have to convert this from a String|long by calling
    //     serializerClass.deserialize(value).
    @NonNull
    private CodeBlock fieldCallDeserialize(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull SerializerClass serializerClass) {
        TypeMirror customType = getterOrField.getJvmType();
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|prop
        TypeMirror propType = annotation.getUnderlyingTypeWithinGenericDoc(mHelper); // e.g. long
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .addStatement("$T $NCopy = genericDoc.$N($S)",
                        propType,
                        jvmName,
                        annotation.getGenericDocGetterName(),
                        annotation.getName())
                .addStatement("$T $NConv = null", customType, jvmName);
        boolean nullCheckRequired = !(propType instanceof PrimitiveType);
        if (nullCheckRequired) {
            codeBlock.beginControlFlow("if ($NCopy != null)", jvmName);
        }
        codeBlock.addStatement("$NConv = new $T().deserialize($NCopy)",
                jvmName, serializerClass.getElement(), jvmName);
        if (nullCheckRequired) {
            codeBlock.endControlFlow();
        }
        return codeBlock.build();
    }

    /**
     * Prepends the expr with a cast so it may be coerced to the target type. For example,
     *
     * <pre>
     * {@code
     * // Given expr.ofTypeLong() and target type = int; returns:
     * (int) expr.ofTypeLong()
     * }
     * </pre>
     */
    @NonNull
    private CodeBlock maybeApplyNarrowingCast(
            @NonNull CodeBlock expr,
            @NonNull TypeMirror exprType,
            @NonNull TypeMirror targetType) {
        TypeMirror castType =
                mHelper.getNarrowingCastType(/* sourceType= */exprType, targetType);
        if (castType == null) {
            return expr;
        }
        return CodeBlock.of("($T) $L", castType, expr);
    }
}
