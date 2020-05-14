/*
 * Copyright 2018 The Android Open Source Project
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

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

/**
 * Processes AppSearchDocument annotations.
 */
@SupportedAnnotationTypes({DocumentProcessor.APP_SEARCH_DOCUMENT_CLASS})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class DocumentProcessor extends AbstractProcessor {
    static final String APP_SEARCH_DOCUMENT_CLASS =
            "androidx.appsearch.annotation.AppSearchDocument";

    private Messager mMessager;

    @Override
    @NonNull
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(@NonNull ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(
            @NonNull Set<? extends TypeElement> set,
            @NonNull RoundEnvironment roundEnvironment) {
        try {
            tryProcess(set, roundEnvironment);
        } catch (ProcessingException e) {
            e.printDiagnostic(mMessager);
        }
        // True means we claimed the annotations. This is true regardless of whether they were
        // used correctly.
        return true;
    }

    private void tryProcess(
            @NonNull Set<? extends TypeElement> set,
            @NonNull RoundEnvironment roundEnvironment) throws ProcessingException {
        if (set.isEmpty()) return;

        // Find the TypeElement corresponding to the @AppSearchDocument annotation. We can't use the
        // annotation class directly because the appsearch project compiles only on Android, but
        // this annotation processor runs on the host.
        TypeElement appSearchDocument = findAnnotation(set, APP_SEARCH_DOCUMENT_CLASS);

        for (Element element : roundEnvironment.getElementsAnnotatedWith(appSearchDocument)) {
            if (element.getKind() != ElementKind.CLASS) {
                throw new ProcessingException(
                        "@AppSearchDocument annotation on something other than a class", element);
            }
            processAppSearchDocument(element);
        }
    }

    private void processAppSearchDocument(@NonNull Element element) throws ProcessingException {
        // TODO(b/156296904): Use this model to produce output files
        AppSearchDocumentModel.create(element);
    }

    private TypeElement findAnnotation(Set<? extends TypeElement> set, String name) {
        for (TypeElement typeElement : set) {
            if (typeElement.getQualifiedName().contentEquals(name)) {
                return typeElement;
            }
        }
        return null;
    }
}
