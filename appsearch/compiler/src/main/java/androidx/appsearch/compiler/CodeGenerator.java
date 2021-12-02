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

import com.google.auto.common.GeneratedAnnotationSpecs;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.File;
import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

/**
 * Generates java code for an {@link androidx.appsearch.app.AppSearchSchema} and a translator
 * between the document class and a {@link androidx.appsearch.app.GenericDocument}.
 */
class CodeGenerator {
    private final ProcessingEnvironment mEnv;
    private final IntrospectionHelper mHelper;
    private final DocumentModel mModel;

    private final String mOutputPackage;
    private final TypeSpec mOutputClass;

    public static CodeGenerator generate(
            @NonNull ProcessingEnvironment env, @NonNull DocumentModel model)
            throws ProcessingException {
        return new CodeGenerator(env, model);
    }

    private CodeGenerator(
            @NonNull ProcessingEnvironment env, @NonNull DocumentModel model)
            throws ProcessingException {
        // Prepare constants needed for processing
        mEnv = env;
        mHelper = new IntrospectionHelper(env);
        mModel = model;

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

    /**
     * Creates factory class for any class annotated with
     * {@link androidx.appsearch.annotation.Document}
     * <p>Class Example 1:
     *   For a class Foo annotated with @Document, we will generated a
     *   $$__AppSearch__Foo.class under the output package.
     * <p>Class Example 2:
     *   For an inner class Foo.Bar annotated with @Document, we will generated a
     *   $$__AppSearch__Foo$$__Bar.class under the output package.
     */
    private TypeSpec createClass() throws ProcessingException {
        // Gets the full name of target class.
        String qualifiedName = mModel.getClassElement().getQualifiedName().toString();
        String className = qualifiedName.substring(mOutputPackage.length() + 1);
        ClassName genClassName = mHelper.getDocumentClassFactoryForClass(mOutputPackage, className);

        TypeName genClassType = TypeName.get(mModel.getClassElement().asType());
        TypeName factoryType = ParameterizedTypeName.get(
                mHelper.getAppSearchClass("DocumentClassFactory"),
                genClassType);

        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(genClassName)
                .addOriginatingElement(mModel.getClassElement())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(factoryType);

        // Add the @Generated annotation to avoid static analysis running on these files
        GeneratedAnnotationSpecs.generatedAnnotationSpec(
                mEnv.getElementUtils(),
                mEnv.getSourceVersion(),
                AppSearchCompiler.class
        ).ifPresent(genClass::addAnnotation);

        SchemaCodeGenerator.generate(mEnv, mModel, genClass);
        ToGenericDocumentCodeGenerator.generate(mEnv, mModel, genClass);
        FromGenericDocumentCodeGenerator.generate(mEnv, mModel, genClass);
        return genClass.build();
    }
}
