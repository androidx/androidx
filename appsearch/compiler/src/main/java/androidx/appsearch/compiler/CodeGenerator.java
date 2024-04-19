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

import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_CLASS_FACTORY_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.RESTRICT_TO_ANNOTATION_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.RESTRICT_TO_SCOPE_CLASS;
import static androidx.appsearch.compiler.IntrospectionHelper.getDocumentClassFactoryForClass;

import androidx.annotation.NonNull;

import com.google.auto.common.GeneratedAnnotationSpecs;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

/**
 * Generates java code for an {@link androidx.appsearch.app.AppSearchSchema} and a translator
 * between the document class and a {@link androidx.appsearch.app.GenericDocument}.
 */
class CodeGenerator {
    private final ProcessingEnvironment mEnv;
    private final DocumentModel mModel;
    private final boolean mRestrictGeneratedCodeToLib;
    private final String mOutputPackage;

    /**
     * Constructs a {@link CodeGenerator}.
     *
     * @param restrictGeneratedCodeToLib Whether to annotate the generated classes with
     *                                   {@code RestrictTo(LIBRARY)}.
     */
    CodeGenerator(
            @NonNull ProcessingEnvironment env,
            @NonNull DocumentModel model,
            boolean restrictGeneratedCodeToLib) {
        mEnv = env;
        mModel = model;
        mRestrictGeneratedCodeToLib = restrictGeneratedCodeToLib;
        mOutputPackage = mEnv.getElementUtils().getPackageOf(mModel.getClassElement()).toString();
    }

    public JavaFile createJavaFile() throws IOException, ProcessingException {
        TypeSpec outputClass = createClass();
        return JavaFile.builder(mOutputPackage, outputClass).build();
    }

    /**
     * Creates factory class for any class annotated with
     * {@link androidx.appsearch.annotation.Document}
     * <p>Class Example 1:
     * For a class Foo annotated with @Document, we will generated a
     * $$__AppSearch__Foo.class under the output package.
     * <p>Class Example 2:
     * For an inner class Foo.Bar annotated with @Document, we will generated a
     * $$__AppSearch__Foo$$__Bar.class under the output package.
     */
    private TypeSpec createClass() throws ProcessingException {
        // Gets the full name of target class.
        String qualifiedName = mModel.getQualifiedDocumentClassName();
        String className = qualifiedName.substring(mOutputPackage.length() + 1);
        ClassName genClassName = getDocumentClassFactoryForClass(mOutputPackage, className);

        TypeName genClassType = TypeName.get(mModel.getClassElement().asType());
        TypeName factoryType =
                ParameterizedTypeName.get(DOCUMENT_CLASS_FACTORY_CLASS, genClassType);

        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(genClassName)
                .addOriginatingElement(mModel.getClassElement())
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addSuperinterface(factoryType);

        // Add the @Generated annotation to avoid static analysis running on these files
        GeneratedAnnotationSpecs.generatedAnnotationSpec(
                mEnv.getElementUtils(),
                mEnv.getSourceVersion(),
                AppSearchCompiler.class
        ).ifPresent(genClass::addAnnotation);

        if (mRestrictGeneratedCodeToLib) {
            // Add @RestrictTo(LIBRARY_GROUP) to the generated class
            genClass.addAnnotation(
                    AnnotationSpec.builder(RESTRICT_TO_ANNOTATION_CLASS)
                            .addMember(/* name= */"value", "$T.LIBRARY", RESTRICT_TO_SCOPE_CLASS)
                            .build());
        }

        SchemaCodeGenerator.generate(mEnv, mModel, genClass);
        ToGenericDocumentCodeGenerator.generate(mEnv, mModel, genClass);
        FromGenericDocumentCodeGenerator.generate(mEnv, mModel, genClass);
        return genClass.build();
    }
}
