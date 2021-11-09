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

import static javax.lang.model.util.ElementFilter.typesIn;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;

/**
 * Processes {@code androidx.appsearch.annotation.Document} annotations.
 *
 * <p>Only plain Java objects and AutoValue Document classes without builders are supported.
 */
@SupportedAnnotationTypes({IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({AppSearchCompiler.OUTPUT_DIR_OPTION})
public class AppSearchCompiler extends BasicAnnotationProcessor {
    /**
     * This property causes us to write output to a different folder instead of the usual filer
     * location. It should only be used for testing.
     */
    @VisibleForTesting
    static final String OUTPUT_DIR_OPTION = "AppSearchCompiler.OutputDir";

    @Override
    @NonNull
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    protected Iterable<? extends Step> steps() {
        return ImmutableList.of(new AppSearchCompileStep(processingEnv));
    }

    private static final class AppSearchCompileStep implements Step {
        private final ProcessingEnvironment mProcessingEnv;
        private final Messager mMessager;

        AppSearchCompileStep(ProcessingEnvironment processingEnv) {
            mProcessingEnv = processingEnv;
            mMessager = processingEnv.getMessager();
        }

        @Override
        public ImmutableSet<String> annotations() {
            return ImmutableSet.of(IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS);
        }

        @Override
        public ImmutableSet<Element> process(
                ImmutableSetMultimap<String, Element> elementsByAnnotation) {
            Set<TypeElement> documentElements =
                    typesIn(elementsByAnnotation.get(
                            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS));
            for (TypeElement document : documentElements) {
                try {
                    processDocument(document);
                } catch (MissingTypeException e) {
                    // Save it for next round to wait for the AutoValue annotation processor to
                    // be run first.
                    return ImmutableSet.of(e.getTypeName());
                } catch (ProcessingException e) {
                    // Prints error message.
                    e.printDiagnostic(mMessager);
                }
            }
            // No elements will be passed to next round of processing.
            return ImmutableSet.of();
        }

        private void processDocument(@NonNull TypeElement element)
                throws ProcessingException, MissingTypeException {
            if (element.getKind() != ElementKind.CLASS) {
                throw new ProcessingException(
                        "@Document annotation on something other than a class", element);
            }

            DocumentModel model;
            if (element.getAnnotation(AutoValue.class) != null) {
                // Document class is annotated as AutoValue class. For processing the AutoValue
                // class, we also need the generated class from AutoValue annotation processor.
                TypeElement generatedElement =
                        mProcessingEnv.getElementUtils().getTypeElement(
                                getAutoValueGeneratedClassName(element));
                if (generatedElement == null) {
                    // Generated class is not found.
                    throw new MissingTypeException(element);
                } else {
                    model = DocumentModel.createAutoValueModel(mProcessingEnv, element,
                            generatedElement);
                }
            } else {
                // Non-AutoValue AppSearch Document class.
                model = DocumentModel.createPojoModel(mProcessingEnv, element);
            }
            CodeGenerator generator = CodeGenerator.generate(mProcessingEnv, model);
            String outputDir = mProcessingEnv.getOptions().get(OUTPUT_DIR_OPTION);
            try {
                if (outputDir == null || outputDir.isEmpty()) {
                    generator.writeToFiler();
                } else {
                    mMessager.printMessage(
                            Kind.NOTE,
                            "Writing output to \"" + outputDir
                                    + "\" due to the presence of -A" + OUTPUT_DIR_OPTION);
                    generator.writeToFolder(new File(outputDir));
                }
            } catch (IOException e) {
                ProcessingException pe =
                        new ProcessingException("Failed to write output", model.getClassElement());
                pe.initCause(e);
                throw pe;
            }
        }

        /**
         * Gets the generated class name of an AutoValue annotated class.
         *
         * <p>This is the same naming strategy used by AutoValue's processor.
         */
        private String getAutoValueGeneratedClassName(TypeElement element) {
            TypeElement type = element;
            String name = type.getSimpleName().toString();
            while (type.getEnclosingElement() instanceof TypeElement) {
                type = (TypeElement) type.getEnclosingElement();
                name = type.getSimpleName().toString() + "_" + name;
            }
            String pkg = MoreElements.getPackage(type).getQualifiedName().toString();
            String dot = pkg.isEmpty() ? "" : ".";
            return pkg + dot + "AutoValue_" + name;
        }
    }
}
