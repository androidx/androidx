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
import static androidx.appsearch.compiler.IntrospectionHelper.GENERIC_DOCUMENT_CLASS;

import androidx.annotation.NonNull;
import androidx.appsearch.compiler.AnnotatedGetterOrField.ElementTypeCategory;
import androidx.appsearch.compiler.annotationwrapper.DataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.DocumentPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.MetadataPropertyAnnotation;
import androidx.appsearch.compiler.annotationwrapper.PropertyAnnotation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Generates java code for a translator from an instance of a class annotated with
 * {@link androidx.appsearch.annotation.Document} into a
 * {@link androidx.appsearch.app.GenericDocument}.
 */
class ToGenericDocumentCodeGenerator {
    private final IntrospectionHelper mHelper;
    private final DocumentModel mModel;

    private ToGenericDocumentCodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull DocumentModel model) {
        mHelper = new IntrospectionHelper(env);
        mModel = model;
    }

    public static void generate(
            @NonNull ProcessingEnvironment env,
            @NonNull DocumentModel model,
            @NonNull TypeSpec.Builder classBuilder) {
        new ToGenericDocumentCodeGenerator(env, model).generate(classBuilder);
    }

    private void generate(TypeSpec.Builder classBuilder) {
        classBuilder.addMethod(createToGenericDocumentMethod());
    }

    private MethodSpec createToGenericDocumentMethod() {
        // Method header
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("toGenericDocument")
                .addModifiers(Modifier.PUBLIC)
                .returns(GENERIC_DOCUMENT_CLASS)
                .addAnnotation(Override.class)
                .addParameter(ClassName.get(mModel.getClassElement()), "document")
                .addException(APPSEARCH_EXCEPTION_CLASS);

        // Construct a new GenericDocument.Builder with the namespace, id, and schema type
        methodBuilder.addStatement("$T builder =\nnew $T<>($L, $L, SCHEMA_NAME)",
                ParameterizedTypeName.get(
                        GENERIC_DOCUMENT_CLASS.nestedClass("Builder"),
                        WildcardTypeName.subtypeOf(Object.class)),
                GENERIC_DOCUMENT_CLASS.nestedClass("Builder"),
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
        if (getterOrField.getJvmType() instanceof PrimitiveType) {
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
        //       Collection contains String or GenericDocument.
        //       We have to convert this into an array of String[] or GenericDocument[], but no
        //       conversion of the collection elements is needed. We can use Collection#toArray for
        //       this.
        //
        //   1c: CollectionForLoopCallToGenericDocument
        //       Collection contains a class which is annotated with @Document.
        //       We have to convert this into an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.

        // Scenario 2: field is an Array
        //   2a: ArrayForLoopAssign
        //       Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[].
        //       We have to pack it into a primitive array of type long[], double[], boolean[]
        //       by reading each element one-by-one and assigning it. The compiler takes care of
        //       unboxing and widening where necessary.
        //
        //   2b: ArrayUseDirectly
        //       Array is of type String[], long[], double[], boolean[], byte[][] or
        //       GenericDocument[].
        //       We can directly use this field with no conversion.
        //
        //   2c: ArrayForLoopCallToGenericDocument
        //       Array is of a class which is annotated with @Document.
        //       We have to convert this into an array of GenericDocument[], by reading each element
        //       one-by-one and converting it through the standard conversion machinery.
        //
        //   2d: Array is of class byte[]. This is actually a single-valued field as byte arrays are
        //       natively supported by Icing, and is handled as Scenario 3a.

        // Scenario 3: Single valued fields
        //   3a: FieldUseDirectlyWithNullCheck
        //       Field is of type String, Long, Integer, Double, Float, Boolean.
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
        ElementTypeCategory typeCategory = getterOrField.getElementTypeCategory();
        switch (annotation.getDataPropertyKind()) {
            case STRING_PROPERTY:
                switch (typeCategory) {
                    case COLLECTION: // List<String>: 1b
                        return collectionCallToArray(annotation, getterOrField);
                    case ARRAY: // String[]: 2b
                        return arrayUseDirectly(annotation, getterOrField);
                    case SINGLE: // String: 3a
                        return fieldUseDirectlyWithNullCheck(annotation, getterOrField);
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
                switch (typeCategory) {
                    case COLLECTION: // List<Long>|List<Integer>: 1a
                        return collectionForLoopAssign(
                                annotation,
                                getterOrField,
                                /* targetArrayComponentType= */mHelper.mLongPrimitiveType);
                    case ARRAY:
                        if (mHelper.isPrimitiveLongArray(getterOrField.getJvmType())) {
                            return arrayUseDirectly(annotation, getterOrField); // long[]: 2b
                        } else {
                            // Long[]|Integer[]|int[]: 2a
                            return arrayForLoopAssign(
                                    annotation,
                                    getterOrField,
                                    /* targetArrayComponentType= */mHelper.mLongPrimitiveType);
                        }
                    case SINGLE:
                        if (getterOrField.getJvmType() instanceof PrimitiveType) {
                            // long|int: 3b
                            return fieldUseDirectlyWithoutNullCheck(annotation, getterOrField);
                        } else {
                            // Long|Integer: 3a
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
                                /* targetArrayComponentType= */mHelper.mDoublePrimitiveType);
                    case ARRAY:
                        if (mHelper.isPrimitiveDoubleArray(getterOrField.getJvmType())) {
                            return arrayUseDirectly(annotation, getterOrField); // double[]: 2b
                        } else {
                            // Double[]|Float[]|float[]: 2a
                            return arrayForLoopAssign(
                                    annotation,
                                    getterOrField,
                                    /* targetArrayComponentType= */mHelper.mDoublePrimitiveType);
                        }
                    case SINGLE:
                        if (getterOrField.getJvmType() instanceof PrimitiveType) {
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
                                /* targetArrayComponentType= */mHelper.mBooleanPrimitiveType);
                    case ARRAY:
                        if (mHelper.isPrimitiveBooleanArray(getterOrField.getJvmType())) {
                            return arrayUseDirectly(annotation, getterOrField); // boolean[]: 2b
                        } else {
                            // Boolean[]: 2a
                            return arrayForLoopAssign(
                                    annotation,
                                    getterOrField,
                                    /* targetArrayComponentType= */mHelper.mBooleanPrimitiveType);
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
                        return collectionForLoopAssign(
                                annotation,
                                getterOrField,
                                /* targetArrayComponentType= */mHelper.mBytePrimitiveArrayType);
                    case ARRAY: // byte[][]: 2b
                        return arrayUseDirectly(annotation, getterOrField);
                    case SINGLE: // byte[]: 2d
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
    @NonNull
    private CodeBlock collectionForLoopAssign(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull TypeMirror targetArrayComponentType) {
        TypeMirror jvmType = getterOrField.getJvmType(); // e.g. List<Long>
        TypeMirror componentType = getterOrField.getComponentType(); // e.g. Long
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType, jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = $L",
                        targetArrayComponentType,
                        jvmName,
                        newArrayExpr(
                                targetArrayComponentType,
                                /* size= */CodeBlock.of("$NCopy.size()", jvmName)))
                .addStatement("int i = 0")
                .beginControlFlow("for ($T item : $NCopy)", componentType, jvmName)
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
    //     Collection contains String or GenericDocument.
    //     We have to convert this into an array of String[] or GenericDocument[], but no
    //     conversion of the collection elements is needed. We can use Collection#toArray for
    //     this.
    @NonNull
    private CodeBlock collectionCallToArray(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror collectionType = getterOrField.getJvmType(); // e.g. List<String>
        TypeMirror componentType = getterOrField.getComponentType(); // e.g. String
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        collectionType, jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = $NCopy.toArray(new $T[0])",
                        componentType, jvmName, jvmName, componentType)
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
    @NonNull
    private CodeBlock collectionForLoopCallToGenericDocument(
            @NonNull DocumentPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror collectionType = getterOrField.getJvmType(); // e.g. List<Person>
        TypeMirror documentClass = getterOrField.getComponentType(); // e.g. Person
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        collectionType, jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = new $T[$NCopy.size()]",
                        GENERIC_DOCUMENT_CLASS, jvmName, GENERIC_DOCUMENT_CLASS, jvmName)
                .addStatement("int i = 0")
                .beginControlFlow("for ($T item : $NCopy)", documentClass, jvmName)
                .addStatement("$NConv[i++] = $T.fromDocumentClass(item)",
                        jvmName, GENERIC_DOCUMENT_CLASS)
                .endControlFlow() // for (...) {
                .addStatement("builder.setPropertyDocument($S, $NConv)",
                        annotation.getName(), jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 2a: ArrayForLoopAssign
    //     Array is of type Long[], Integer[], int[], Double[], Float[], float[], Boolean[].
    //     We have to pack it into a primitive array of type long[], double[], boolean[]
    //     by reading each element one-by-one and assigning it. The compiler takes care of
    //     unboxing and widening where necessary.
    @NonNull
    private CodeBlock arrayForLoopAssign(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField,
            @NonNull TypeMirror targetArrayComponentType) {
        TypeMirror jvmType = getterOrField.getJvmType(); // e.g. Long[]
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType, jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = $L",
                        targetArrayComponentType,
                        jvmName,
                        newArrayExpr(
                                targetArrayComponentType,
                                /* size= */CodeBlock.of("$NCopy.length", jvmName)))
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
    //     Array is of type String[], long[], double[], boolean[], byte[][] or
    //     GenericDocument[].
    //     We can directly use this field with no conversion.
    @NonNull
    private CodeBlock arrayUseDirectly(
            @NonNull DataPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror jvmType = getterOrField.getJvmType(); // e.g. String[]
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType, jvmName, createReadExpr(getterOrField))
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
    @NonNull
    private CodeBlock arrayForLoopCallToGenericDocument(
            @NonNull DocumentPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror jvmType = getterOrField.getJvmType(); // e.g. Person[]
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType, jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T[] $NConv = new $T[$NCopy.length]",
                        GENERIC_DOCUMENT_CLASS, jvmName, GENERIC_DOCUMENT_CLASS, jvmName)
                .beginControlFlow("for (int i = 0; i < $NConv.length; i++)", jvmName)
                .addStatement("$NConv[i] = $T.fromDocumentClass($NCopy[i])",
                        jvmName, GENERIC_DOCUMENT_CLASS, jvmName)
                .endControlFlow() // for (...) {
                .addStatement("builder.setPropertyDocument($S, $NConv)",
                        annotation.getName(), jvmName)
                .endControlFlow() //  if ($NCopy != null) {
                .build();
    }

    // 3a: FieldUseDirectlyWithNullCheck
    //     Field is of type String, Long, Integer, Double, Float, Boolean.
    //     We can use this field directly, after testing for null. The java compiler will box
    //     or unbox as needed.
    @NonNull
    private CodeBlock fieldUseDirectlyWithNullCheck(
            @NonNull PropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror jvmType = getterOrField.getJvmType();
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        jvmType, jvmName, createReadExpr(getterOrField))
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
    @NonNull
    private CodeBlock fieldUseDirectlyWithoutNullCheck(
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
    @NonNull
    private CodeBlock fieldCallToGenericDocument(
            @NonNull DocumentPropertyAnnotation annotation,
            @NonNull AnnotatedGetterOrField getterOrField) {
        TypeMirror documentClass = getterOrField.getJvmType(); // Person
        String jvmName = getterOrField.getJvmName(); // e.g. mProp|getProp
        return CodeBlock.builder()
                .addStatement("$T $NCopy = $L",
                        documentClass, jvmName, createReadExpr(getterOrField))
                .beginControlFlow("if ($NCopy != null)", jvmName)
                .addStatement("$T $NConv = $T.fromDocumentClass($NCopy)",
                        GENERIC_DOCUMENT_CLASS, jvmName, GENERIC_DOCUMENT_CLASS, jvmName)
                .addStatement("builder.setPropertyDocument($S, $NConv)",
                        annotation.getName(), jvmName)
                .endControlFlow() // if ($NCopy != null) {
                .build();
    }

    private CodeBlock newArrayExpr(@NonNull TypeMirror componentType, @NonNull CodeBlock size) {
        // Component type itself may be an array type e.g. byte[]
        // Deduce the base component type e.g. byte
        TypeMirror baseComponentType = componentType;
        int dims = 1;
        while (baseComponentType.getKind() == TypeKind.ARRAY) {
            baseComponentType = ((ArrayType) baseComponentType).getComponentType();
            dims++;
        }
        CodeBlock.Builder codeBlock = CodeBlock.builder()
                .add("new $T[$L]", baseComponentType, size);
        for (int i = 1; i < dims; i++) {
            codeBlock.add("[]");
        }
        return codeBlock.build();
    }

    /**
     * Returns an expr that reading the annotated getter/fields from a document class var.
     *
     * <p>Assumes there is a document class var in-scope called {@code document}.
     */
    private CodeBlock createReadExpr(@NonNull AnnotatedGetterOrField annotatedGetterOrField) {
        PropertyAccessor accessor = mModel.getAccessor(annotatedGetterOrField);
        if (accessor.isField()) {
            return CodeBlock.of("document.$N", accessor.getElement().getSimpleName().toString());
        } else { // getter
            return CodeBlock.of("document.$N()", accessor.getElement().getSimpleName().toString());
        }
    }
}
