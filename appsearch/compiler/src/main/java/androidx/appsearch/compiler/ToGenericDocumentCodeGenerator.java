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
import static androidx.room.compiler.codegen.compat.XConverters.toJavaPoet;
import static androidx.room.compiler.processing.compat.XConverters.toJavac;

import androidx.appsearch.compiler.AnnotatedGetterOrField.ElementTypeCategory;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.DocumentPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.LongPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.SerializerClass;
import androidx.appsearch.compiler.annotationwrapper.StringPropertyAnnotation;
import androidx.room.compiler.codegen.XTypeName;
import androidx.room.compiler.processing.XProcessingEnv;
import androidx.room.compiler.processing.XType;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import org.jspecify.annotations.NonNull;

import javax.lang.model.element.Modifier;

/**
 * Generates java code for a translator from an instance of a class annotated with
 * {@link androidx.appsearch.annotation.Document} into a
 * {@link androidx.appsearch.app.GenericDocument}.
 */
class ToGenericDocumentCodeGenerator {
    private final XProcessingEnv mEnv;
    private final IntrospectionHelper mHelper;
    private final DocumentModel mModel;

    private ToGenericDocumentCodeGenerator(
            @NonNull XProcessingEnv env, @NonNull DocumentModel model) {
        mEnv = env;
        mHelper = new IntrospectionHelper(env);
        mModel = model;
    }

    public static void generate(
            @NonNull XProcessingEnv env,
            @NonNull DocumentModel model,
            TypeSpec.@NonNull Builder classBuilder) {
        new ToGenericDocumentCodeGenerator(env, model).generate(classBuilder);
    }

    private void generate(TypeSpec.Builder classBuilder) {
        classBuilder.addMethod(createToGenericDocumentMethod());
    }

    private MethodSpec createToGenericDocumentMethod() {
        // Method header
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toGenericDocument")
                .addModifiers(Modifier.PUBLIC)
                .returns(toJavaPoet(GENERIC_DOCUMENT_CLASS))
                .addAnnotation(Override.class)
                .addParameter(toJavaPoet(mModel.getClassElement().asClassName()), "document")
                .addException(toJavaPoet(APPSEARCH_EXCEPTION_CLASS));

        // Construct a new GenericDocument.Builder with the namespace, id, and schema type
        methodBuilder.addStatement("$T builder =\nnew $T<>($L, $L, SCHEMA_NAME)",
                toJavaPoet(
                        GENERIC_DOCUMENT_CLASS.nestedClass("Builder")
                                .parametrizedBy(XTypeName.ANY_WILDCARD)),
                toJavaPoet(GENERIC_DOCUMENT_CLASS.nestedClass("Builder")),
                createReadExpr(mModel.getNamespaceAnnotatedGetterOrField()),
                createReadExpr(mModel.getIdAnnotatedGetterOrField()));

        // Set metadata properties
        for (AnnotatedGetterOrField getterOrField : mModel.getAnnotatedGettersAndFields()) {
            PropertyAnnotation annotation = getterOrField.getAnnotation();
            if (annotation.getPropertyKind() != PropertyAnnotation.Kind.METADATA_PROPERTY
                    // Already set in the generated constructor above
                    || annotation == MetadataPropertyAnnotation.ID
                    || annotation == MetadataPropertyAnnotation.NAMESPACE) {
                continue;
            }

            methodBuilder.addCode(codeToCopyIntoGenericDoc(
                    (MetadataPropertyAnnotation) annotation, getterOrField));
        }

        // Set data properties
        for (AnnotatedGetterOrField getterOrField : mModel.getAnnotatedGettersAndFields()) {
            PropertyAnnotation annotation = getterOrField.getAnnotation();
            if (annotation.getPropertyKind() != PropertyAnnotation.Kind.DATA_PROPERTY) {
                continue;
            }
            methodBuilder.addCode(codeToCopyIntoGenericDoc(
                    (DataPropertyAnnotation) annotation, getterOrField));
        }

        methodBuilder.addStatement("return builder.build()");
        return methodBuilder.build();
    }

    /**
     * Returns code that copies the getter/field annotated with a {@link MetadataPropertyAnnotation}
     * from a document class into a {@code GenericDocument.Builder}.
     *
     * <p>Assumes:
     * <ol>
     *     <li>There is a document class var in-scope called {@code document}.</li>
     *     <li>There is {@code GenericDocument.Builder} var in-scope called {@code builder}.</li>
     *     <li>
     *         The annotation is not {@link MetadataPropertyAnnotation#ID} or
     *         {@link MetadataPropertyAnnotation#NAMESPACE}.
     *     </li>
     * </ol>
     */
    private CodeBlock codeToCopyIntoGenericDoc(
            @NonNull MetadataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        if (getterOrField.getJvmType().asTypeName().isPrimitive()) {
            // Directly set it
            return CodeBlock.builder()
                    .addStatement("builder.$N($L)",
                            annotation.getGenericDocSetterName(), createReadExpr(getterOrField))
                    .build();
        }
        // Boxed type. Need to guard against the case where the value is null at runtime.
        return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
    }

    /**
     * Returns code that copies the getter/field annotated with a {@link DataPropertyAnnotation}
     * from a document class into a {@code GenericDocument.Builder}.
     *
     * <p>Assumes:
     * <ol>
     *     <li>There is a document class var in-scope called {@code document}.</li>
     *     <li>There is {@code GenericDocument.Builder} var in-scope called {@code builder}.</li>
     * </ol>
     */
    private CodeBlock codeToCopyIntoGenericDoc(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        // Scenario 1: field is a Collection
        //   1a: CollectionForLoopAssign
        //       Collection contains boxed Long, Integer, Double, Float, Boolean, byte[].
        //       We have to pack it into a primitive array of type long[], double[], boolean[] or
        //       byte[][] by reading each element one-by-one and assigning it. The compiler takes
        //       care of unboxing and widening where necessary.
        //
        //   1b: CollectionCallToArray
        //       Collection contains String, GenericDocument, EmbeddingVector or
        //       AppSearchBlobHandle. We have to convert this into an array of String[],
        //       GenericDocument[], EmbeddingVector[] or AppSearchBlobHandle[], but no conversion of
        //       the collection elements is needed. We can use Collection#toArray for this.
        //
        //   1c: CollectionForLoopCallToGenericDocument
        //       Collection contains a class which is annotated with @Document.
        //       We have to convert this into an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   1d: CollectionForLoopCallSerialize
        //       Collection contains a custom type for which we have a serializer.
        //       We have to convert this into an array of String[]|long[], by reading each element
        //       one-by-one and passing it to serializerClass.serialize(customType).

        // Scenario 2: field is an Array
        //   2a: ArrayForLoopAssign
        //       Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[].
        //       We have to pack it into a primitive array of type long[], double[], boolean[]
        //       by reading each element one-by-one and assigning it. The compiler takes care of
        //       unboxing and widening where necessary.
        //
        //   2b: ArrayUseDirectly
        //       Array is of type String[], long[], double[], boolean[], byte[][],
        //       GenericDocument[], EmbeddingVector[] or AppSearchBlobHandle[].
        //       We can directly use this field with no conversion.
        //
        //   2c: ArrayForLoopCallToGenericDocument
        //       Array is of a class which is annotated with @Document.
        //       We have to convert this into an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   2d: ArrayForLoopCallSerialize
        //       Array is of a custom type for which we have a serializer.
        //       We have to convert this into an array of String[]|long[], by reading each element
        //       one-by-one and passing it to serializerClass.serialize(customType).
        //
        //   2e: Array is of class byte[]. This is actually a single-valued field as byte arrays are
        //       natively supported by Icing, and is handled as Scenario 3a.

        // Scenario 3: Single valued fields
        //   3a: FieldUseDirectlyWithNullCheck
        //       Field is of type String, Long, Integer, Double, Float, Boolean, EmbeddingVector or
        //       AppSearchBlobHandle.
        //       We can use this field directly, after testing for null. The java compiler will box
        //       or unbox as needed.
        //
        //   3b: FieldUseDirectlyWithoutNullCheck
        //       Field is of type long, int, double, float, or boolean.
        //       We can use this field directly without testing for null.
        //
        //   3c: FieldCallToGenericDocument
        //       Field is of a class which is annotated with @Document.
        //       We have to convert this into a GenericDocument through the standard conversion
        //       machinery.
        //
        //   3d: FieldCallSerialize
        //       Field is of a some custom type for which we have a serializer.
        //       We have to convert this into a String|long by calling
        //       serializeClass.serialize(customType).
        ElementTypeCategory typeCategory = getterOrField.getElementTypeCategory();
        switch (annotation.getDataPropertyKind()) {
            case STRING_PROPERTY:
                SerializerClass stringSerializer =
                        ((StringPropertyAnnotation) annotation).getCustomSerializer();
                switch (typeCategory) {
                    case COLLECTION:
                        if (stringSerializer != null) { // List<CustomType>: 1d
                            return collectionForLoopCallSerialize(
                                    annotation, getterOrField, stringSerializer);
                        } else { // List<String>: 1b
                            return collectionCallToArray(annotation, getterOrField);
                        }
                    case ARRAY:
                        if (stringSerializer != null) { // CustomType[]: 2d
                            return arrayForLoopCallToSerialize(
                                    annotation, getterOrField, stringSerializer);
                        } else { // String[]: 2b
                            return arrayUseDirectly(annotation, getterOrField);
                        }
                    case SINGLE:
                        if (stringSerializer != null) { // CustomType: 3d
                            return fieldCallSerialize(annotation, getterOrField, stringSerializer);
                        } else { // String: 3a
                            return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                        }
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case DOCUMENT_PROPERTY:
                DocumentPropertyAnnotation docPropAnnotation =
                        (DocumentPropertyAnnotation) annotation;
                switch (typeCategory) {
                    case COLLECTION: // List<Person>: 1c
                        return collectionForLoopCallToGenericDocument(
                                docPropAnnotation, getterOrField);
                    case ARRAY: // Person[]: 2c
                        return arrayForLoopCallToGenericDocument(docPropAnnotation, getterOrField);
                    case SINGLE: // Person: 3c
                        return fieldCallToGenericDocument(docPropAnnotation, getterOrField);
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case LONG_PROPERTY:
                SerializerClass longSerializer =
                        ((LongPropertyAnnotation) annotation).getCustomSerializer();
                switch (typeCategory) {
                    case COLLECTION:
                        if (longSerializer != null) { // List<CustomType>: 1d
                            return collectionForLoopCallSerialize(
                                    annotation, getterOrField, longSerializer);
                        } else { // List<Long>|List<Integer>: 1a
                            return collectionForLoopAssign(
                                    annotation,
                                    getterOrField,
                                    /* targetArrayComponentType= */mHelper.longPrimitiveType);
                        }
                    case ARRAY:
                        if (longSerializer != null) { // CustomType[]: 2d
                            return arrayForLoopCallToSerialize(
                                    annotation, getterOrField, longSerializer);
                        } else if (mHelper.isPrimitiveLongArray(getterOrField.getJvmType())) {
                            return arrayUseDirectly(annotation, getterOrField); // long[]: 2b
                        } else { // Long[]|Integer[]|int[]: 2a
                            return arrayForLoopAssign(
                                    annotation,
                                    getterOrField,
                                    /* targetArrayComponentType= */mHelper.longPrimitiveType);
                        }
                    case SINGLE:
                        if (longSerializer != null) { // CustomType: 3d
                            return fieldCallSerialize(annotation, getterOrField, longSerializer);
                        } else if (getterOrField.getJvmType().asTypeName().isPrimitive()) {
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
                        return collectionForLoopAssign(
                                annotation,
                                getterOrField,
                                /* targetArrayComponentType= */mHelper.doublePrimitiveType);
                    case ARRAY:
                        if (mHelper.isPrimitiveDoubleArray(getterOrField.getJvmType())) {
                            return arrayUseDirectly(annotation, getterOrField); // double[]: 2b
                        } else {
                            // Double[]|Float[]|float[]: 2a
                            return arrayForLoopAssign(
                                    annotation,
                                    getterOrField,
                                    /* targetArrayComponentType= */mHelper.doublePrimitiveType);
                        }
                    case SINGLE:
                        if (getterOrField.getJvmType().asTypeName().isPrimitive()) {
                            // double|float: 3b
                            return fieldUseDirectlyWithoutNullCheck(annotation, getterOrField);
                        } else {
                            // Double|Float: 3b
                            return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                        }
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case BOOLEAN_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<Boolean>: 1a
                        return collectionForLoopAssign(
                                annotation,
                                getterOrField,
                                /* targetArrayComponentType= */mHelper.booleanPrimitiveType);
                    case ARRAY:
                        if (mHelper.isPrimitiveBooleanArray(getterOrField.getJvmType())) {
                            return arrayUseDirectly(annotation, getterOrField); // boolean[]: 2b
                        } else {
                            // Boolean[]: 2a
                            return arrayForLoopAssign(
                                    annotation,
                                    getterOrField,
                                    /* targetArrayComponentType= */mHelper.booleanPrimitiveType);
                        }
                    case SINGLE:
                        if (getterOrField.getJvmType().asTypeName().isPrimitive()) {
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
                        return collectionForLoopAssign(
                                annotation,
                                getterOrField,
                                /* targetArrayComponentType= */mHelper.bytePrimitiveArrayType);
                    case ARRAY: // byte[][]: 2b
                        return arrayUseDirectly(annotation, getterOrField);
                    case SINGLE: // byte[]: 2e
                        return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case EMBEDDING_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION:
                        // List<EmbeddingVector>: 1b
                        return collectionCallToArray(annotation, getterOrField);
                    case ARRAY:
                        // EmbeddingVector[]: 2b
                        return arrayUseDirectly(annotation, getterOrField);
                    case SINGLE:
                        // EmbeddingVector: 3a
                        return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            case BLOB_HANDLE_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<AppSearchBlobHandle[]>: 1b
                        return collectionCallToArray(annotation, getterOrField);
                    case ARRAY: // AppSearchBlobHandle[]: 2b
                        return arrayUseDirectly(annotation, getterOrField);
                    case SINGLE: // AppSearchBlobHandle: 3a
                        return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
                    default:
                        throw new IllegalStateException("Unhandled type-category: " + typeCategory);
                }
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }
    }

    // 1a: CollectionForLoopAssign
    //     Collection contains boxed Long, Integer, Double, Float, Boolean, byte[].
    //     We have to pack it into a primitive array of type long[], double[], boolean[] or
    //     byte[][] by reading each element one-by-one and assigning it. The compiler takes
    //     care of unboxing and widening where necessary.
    private @NonNull CodeBlock collectionForLoopAssign(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull XType targetArrayComponentType) {
        XType jvmType = getterOrField.getJvmType(); // e.g. List<Long>
        XType componentType = getterOrField.getComponentType(); // e.g. Long
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = $L",
                        targetArrayComponentType.getTypeName(),
                        jvmName,
                        createNewArrayExpr(
                                targetArrayComponentType,
                                /* size= */CodeBlock.of("$NCopy.size()", jvmName),
                                mEnv))
                .addStatement("int i = 0")
                .beginControlFlow("for ($T item : $NCopy)", componentType.getTypeName(), jvmName)
                .addStatement("$NConv[i++] = item", jvmName)
                .endControlFlow() // for (...) {
                .addStatement("builder.$N($S, $NConv)",
                        getterOrField.getAnnotation().getGenericDocSetterName(),
                        annotation.getName(),
                        jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 1b: CollectionCallToArray
    //     Collection contains String, GenericDocument or EmbeddingVector.
    //     We have to convert this into an array of String[], GenericDocument[]
    //     EmbeddingVector[] or AppSearchBlobHandle[], but no conversion of the
    //     collection elements is needed. We can use Collection#toArray for this.
    private @NonNull CodeBlock collectionCallToArray(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        XType collectionType = getterOrField.getJvmType(); // e.g. List<String>
        XType componentType = getterOrField.getComponentType(); // e.g. String
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        collectionType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = $NCopy.toArray(new $T[0])",
                        componentType.getTypeName(), jvmName, jvmName, componentType.getTypeName())
                .addStatement("builder.$N($S, $NConv)",
                        getterOrField.getAnnotation().getGenericDocSetterName(),
                        annotation.getName(),
                        jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 1c: CollectionForLoopCallToGenericDocument
    //     Collection contains a class which is annotated with @Document.
    //     We have to convert this into an array of GenericDocument[], by reading each element
    //     one-by-one and converting it through the standard conversion machinery.
    private @NonNull CodeBlock collectionForLoopCallToGenericDocument(
            @NonNull DocumentPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        XType collectionType = getterOrField.getJvmType(); // e.g. List<Person>
        XType documentClass = getterOrField.getComponentType(); // e.g. Person
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        collectionType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = new $T[$NCopy.size()]",
                        toJavaPoet(GENERIC_DOCUMENT_CLASS), jvmName,
                        toJavaPoet(GENERIC_DOCUMENT_CLASS), jvmName)
                .addStatement("int i = 0")
                .beginControlFlow("for ($T item : $NCopy)", documentClass.getTypeName(), jvmName)
                .addStatement("$NConv[i++] = $T.fromDocumentClass(item)",
                        jvmName, toJavaPoet(GENERIC_DOCUMENT_CLASS))
                .endControlFlow() // for (...) {
                .addStatement("builder.setPropertyDocument($S, $NConv)",
                        annotation.getName(), jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 1d: CollectionForLoopCallSerialize
    //     Collection contains a custom type for which we have a serializer.
    //     We have to convert this into an array of String[]|long[], by reading each element
    //     one-by-one and passing it to serializerClass.serialize(customType).
    private @NonNull CodeBlock collectionForLoopCallSerialize(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull SerializerClass serializerClass) {
        XType jvmType = getterOrField.getJvmType(); // e.g. List<CustomType>
        XType customType = getterOrField.getComponentType(); // e.g. CustomType
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        XType propType = annotation.getUnderlyingTypeWithinGenericDoc(mHelper); // e.g. String
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = new $T[$NCopy.size()]",
                        propType.getTypeName(), jvmName, propType.getTypeName(), jvmName)
                .addStatement(
                        "$T serializer = new $T()",
                        toJavac(serializerClass.getElement()),
                        toJavac(serializerClass.getElement()))
                .addStatement("int i = 0")
                .beginControlFlow("for ($T item : $NCopy)", customType.getTypeName(), jvmName)
                .addStatement("$NConv[i++] = serializer.serialize(item)", jvmName)
                .endControlFlow() // for (...) {
                .addStatement("builder.$N($S, $NConv)",
                        annotation.getGenericDocSetterName(), annotation.getName(), jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 2a: ArrayForLoopAssign
    //     Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[].
    //     We have to pack it into a primitive array of type long[], double[], boolean[]
    //     by reading each element one-by-one and assigning it. The compiler takes care of
    //     unboxing and widening where necessary.
    private @NonNull CodeBlock arrayForLoopAssign(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull XType targetArrayComponentType) {
        XType jvmType = getterOrField.getJvmType(); // e.g. Long[]
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = $L",
                        targetArrayComponentType.getTypeName(),
                        jvmName,
                        createNewArrayExpr(
                                targetArrayComponentType,
                                /* size= */CodeBlock.of("$NCopy.length", jvmName),
                                mEnv))
                .beginControlFlow("for (int i = 0; i < $NCopy.length; i++)", jvmName)
                .addStatement("$NConv[i] = $NCopy[i]", jvmName, jvmName)
                .endControlFlow() // for (...) {
                .addStatement("builder.$N($S, $NConv)",
                        getterOrField.getAnnotation().getGenericDocSetterName(),
                        annotation.getName(),
                        jvmName)
                .endControlFlow() // if ($NCopy != null)
                .build();
    }

    // 2b: ArrayUseDirectly
    //     Array is of type String[], long[], double[], boolean[], byte[][],
    //     GenericDocument[], EmbeddingVector[] or AppSearchBlobHandle[].
    //     We can directly use this field with no conversion.
    private @NonNull CodeBlock arrayUseDirectly(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        XType jvmType = getterOrField.getJvmType(); // e.g. String[]
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("builder.$N($S, $NCopy)",
                        getterOrField.getAnnotation().getGenericDocSetterName(),
                        annotation.getName(),
                        jvmName)
                .endControlFlow() // if ($NCopy != null)
                .build();
    }

    // 2c: ArrayForLoopCallToGenericDocument
    //     Array is of a class which is annotated with @Document.
    //     We have to convert this into an array of GenericDocument[], by reading each element
    //     one-by-one and converting it through the standard conversion machinery.
    private @NonNull CodeBlock arrayForLoopCallToGenericDocument(
            @NonNull DocumentPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        XType jvmType = getterOrField.getJvmType(); // e.g. Person[]
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = new $T[$NCopy.length]",
                        toJavaPoet(GENERIC_DOCUMENT_CLASS), jvmName,
                        toJavaPoet(GENERIC_DOCUMENT_CLASS), jvmName)
                .beginControlFlow("for (int i = 0; i < $NConv.length; i++)", jvmName)
                .addStatement("$NConv[i] = $T.fromDocumentClass($NCopy[i])",
                        jvmName, toJavaPoet(GENERIC_DOCUMENT_CLASS), jvmName)
                .endControlFlow() // for (...) {
                .addStatement("builder.setPropertyDocument($S, $NConv)",
                        annotation.getName(), jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 2d: ArrayForLoopCallSerialize
    //     Array is of a custom type for which we have a serializer.
    //     We have to convert this into an array of String[]|long[], by reading each element
    //     one-by-one and passing it to serializerClass.serialize(customType).
    private @NonNull CodeBlock arrayForLoopCallToSerialize(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull SerializerClass serializerClass) {
        XType jvmType = getterOrField.getJvmType(); // e.g. CustomType[]
        XType propType = annotation.getUnderlyingTypeWithinGenericDoc(mHelper); // e.g. String
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = new $T[$NCopy.length]",
                        propType.getTypeName(), jvmName, propType.getTypeName(), jvmName)
                .addStatement(
                        "$T serializer = new $T()",
                        toJavac(serializerClass.getElement()),
                        toJavac(serializerClass.getElement()))
                .beginControlFlow("for (int i = 0; i < $NConv.length; i++)", jvmName)
                .addStatement("$NConv[i] = serializer.serialize($NCopy[i])", jvmName, jvmName)
                .endControlFlow() // for (...) {
                .addStatement("builder.$N($S, $NConv)",
                        annotation.getGenericDocSetterName(), annotation.getName(), jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 3a: FieldUseDirectlyWithNullCheck
    //     Field is of type String, Long, Integer, Double, Float, Boolean,
    //     EmbeddingVector or AppSearchBlobHandle.
    //     We can use this field directly, after testing for null. The java compiler will box
    //     or unbox as needed.
    private @NonNull CodeBlock fieldUseDirectlyWithNullCheck(
            @NonNull PropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        XType jvmType = getterOrField.getJvmType();
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName);

        switch (annotation.getPropertyKind()) {
            case METADATA_PROPERTY:
                codeBlock.addStatement("builder.$N($NCopy)",
                        getterOrField.getAnnotation().getGenericDocSetterName(), jvmName);
                break;
            case DATA_PROPERTY:
                codeBlock.addStatement("builder.$N($S, $NCopy)",
                        getterOrField.getAnnotation().getGenericDocSetterName(),
                        ((DataPropertyAnnotation) annotation).getName(),
                        jvmName);
                break;
            default:
                throw new IllegalStateException("Unhandled annotation: " + annotation);
        }

        return codeBlock.endControlFlow() // if ($NCopy != null)
                .build();
    }

    // 3b: FieldUseDirectlyWithoutNullCheck
    //     Field is of type long, int, double, float, or boolean.
    //     We can use this field directly without testing for null.
    private @NonNull CodeBlock fieldUseDirectlyWithoutNullCheck(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        return CodeBlock.builder()
                .addStatement("builder.$N($S, $L)",
                        getterOrField.getAnnotation().getGenericDocSetterName(),
                        annotation.getName(),
                        createReadExpr(getterOrField))
                .build();
    }

    // 3c: FieldCallToGenericDocument
    //     Field is of a class which is annotated with @Document.
    //     We have to convert this into a GenericDocument through the standard conversion
    //     machinery.
    private @NonNull CodeBlock fieldCallToGenericDocument(
            @NonNull DocumentPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        XType documentClass = getterOrField.getJvmType(); // Person
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        documentClass.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T $NConv = $T.fromDocumentClass($NCopy)",
                        toJavaPoet(GENERIC_DOCUMENT_CLASS), jvmName,
                        toJavaPoet(GENERIC_DOCUMENT_CLASS), jvmName)
                .addStatement("builder.setPropertyDocument($S, $NConv)",
                        annotation.getName(), jvmName)
                .endControlFlow() // if ($NCopy != null) {
                .build();
    }

    // 3d: FieldCallSerialize
    //     Field is of a some custom type for which we have a serializer.
    //     We have to convert this into a String|long by calling
    //     serializeClass.serialize(customType).
    private @NonNull CodeBlock fieldCallSerialize(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull SerializerClass serializerClass) {
        XType customType = getterOrField.getJvmType();
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement(
                        "$T $NCopy = $L",
                        customType.getTypeName(), jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement(
                        "$T serializer = new $T()",
                        toJavac(serializerClass.getElement()),
                        toJavac(serializerClass.getElement()))
                .addStatement(
                        "$T $NConv = serializer.serialize($NCopy)",
                        annotation.getUnderlyingTypeWithinGenericDoc(mHelper).getTypeName(),
                        jvmName, jvmName)
                .addStatement("builder.$N($S, $NConv)",
                        annotation.getGenericDocSetterName(), annotation.getName(), jvmName)
                .endControlFlow() // if ($NCopy != null)
                .build();
    }

    /**
     * Returns an expr that reading the annotated getter/fields from a document class var.
     *
     * <p>Assumes there is a document class var in-scope called {@code document}.
     */
    private CodeBlock createReadExpr(@NonNull AnnotatedGetterOrField annotatedGetterOrField) {
        PropertyAccessor accessor = mModel.getAccessor(annotatedGetterOrField);
        if (accessor.isField()) {
            return CodeBlock.of("document.$N", accessor.getElement().getName());
        } else { // getter
            return CodeBlock.of("document.$N()", accessor.getElement().getName());
        }
    }
}
