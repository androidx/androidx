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

import static androidx.appsearch.compiler.IntrospectionHelper.APPSEARCH_ANNOTATION_PKG;
import static androidx.appsearch.compiler.IntrospectionHelper.DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME;

import static javax.lang.model.util.ElementFilter.typesIn;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.squareup.javapoet.JavaFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
@SupportedAnnotationTypes({APPSEARCH_ANNOTATION_PKG + "." + DOCUMENT_ANNOTATION_SIMPLE_CLASS_NAME})
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
        private final Map<String, List<String>> mDocumentClassMap;
        // Annotation processing can be run in multiple rounds. This tracks the index of current
        // round starting from 0.
        private int mRoundIndex;

        AppSearchCompileStep(ProcessingEnvironment processingEnv) {
            mProcessingEnv = processingEnv;
            mMessager = processingEnv.getMessager();
            mDocumentClassMap = new HashMap<>();
            mRoundIndex = -1;
        }

        @Override
        public ImmutableSet<String> annotations() {
            return ImmutableSet.of(IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.canonicalName());
        }

        @Override
        public ImmutableSet<Element> process(
                ImmutableSetMultimap<String, Element> elementsByAnnotation) {
            mDocumentClassMap.clear();
            mRoundIndex += 1;

            Set<TypeElement> documentElements =
                    typesIn(elementsByAnnotation.get(
                            IntrospectionHelper.DOCUMENT_ANNOTATION_CLASS.canonicalName()));

            ImmutableSet.Builder<Element> nextRound = new ImmutableSet.Builder<>();
            String documentMapClassPackage = null;
            Set<String> classNames = new HashSet<>();
            for (TypeElement document : documentElements) {
                try {
                    processDocument(document);
                } catch (MissingTypeException e) {
                    // Save it for next round to wait for the AutoValue annotation processor to
                    // be run first.
                    nextRound.add(document);
                } catch (ProcessingException e) {
                    // Prints error message.
                    e.printDiagnostic(mMessager);
                }
                classNames.add(document.getQualifiedName().toString());
                String packageName =
                        mProcessingEnv.getElementUtils().getPackageOf(document).toString();
                // We must choose a deterministic package to place the generated document map
                // class. Given multiple packages, we have no real preference between them. So
                // for the sake of making a deterministic selection, we always choose to generate
                // the map in the lexicographically smallest package.
                if (documentMapClassPackage == null || packageName.compareTo(
                        documentMapClassPackage) < 0) {
                    documentMapClassPackage = packageName;
                }
            }

            try {
                if (!classNames.isEmpty()) {
                    // Append the hash code of classNames and the index of the current round as a
                    // suffix to the name of the generated document map class. This will prevent
                    // the generation of two classes with the same name, which could otherwise
                    // happen when there are two Java modules that contain classes in the same
                    // package name, or there are multiple rounds of annotation processing for some
                    // module.
                    String classSuffix = generateStringSetHash(
                            classNames, /* delimiter= */ ",") + "_" + mRoundIndex;
                    writeJavaFile(DocumentMapGenerator.generate(mProcessingEnv,
                            documentMapClassPackage, classSuffix, mDocumentClassMap));
                }
            } catch (NoSuchAlgorithmException | IOException e) {
                mProcessingEnv.getMessager().printMessage(Kind.ERROR,
                        "Failed to create the AppSearch document map class: " + e);
            }

            // Pass elements to next round of processing.
            return nextRound.build();
        }

        private void writeJavaFile(JavaFile javaFile) throws IOException {
            String outputDir = mProcessingEnv.getOptions().get(OUTPUT_DIR_OPTION);
            if (outputDir == null || outputDir.isEmpty()) {
                javaFile.writeTo(mProcessingEnv.getFiler());
            } else {
                mMessager.printMessage(
                        Kind.NOTE,
                        "Writing output to \"" + outputDir
                                + "\" due to the presence of -A" + OUTPUT_DIR_OPTION);
                javaFile.writeTo(new File(outputDir));
            }
        }

        /**
         * Process the document class by generating a factory class for it and properly update
         * {@link #mDocumentClassMap}.
         */
        private void processDocument(@NonNull TypeElement element)
                throws ProcessingException, MissingTypeException {
            if (element.getKind() != ElementKind.CLASS
                    && element.getKind() != ElementKind.INTERFACE) {
                throw new ProcessingException(
                        "@Document annotation on something other than a class or an interface",
                        element);
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
            try {
                writeJavaFile(generator.createJavaFile());
            } catch (IOException e) {
                ProcessingException pe =
                        new ProcessingException("Failed to write output", model.getClassElement());
                pe.initCause(e);
                throw pe;
            }

            List<String> documentClassList = mDocumentClassMap.computeIfAbsent(
                    model.getSchemaName(), k -> new ArrayList<>());
            documentClassList.add(
                    mProcessingEnv.getElementUtils().getBinaryName(element).toString());
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

        /**
         * Generate a SHA-256 hash for a given string set.
         *
         * @param set       The set of the strings.
         * @param delimiter The delimiter used to separate the strings, which should not have
         *                  appeared in any of the strings in the set.
         */
        @NonNull
        private static String generateStringSetHash(@NonNull Set<String> set,
                @NonNull String delimiter) throws NoSuchAlgorithmException {
            List<String> sortedList = new ArrayList<>(set);
            Collections.sort(sortedList);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String s : sortedList) {
                md.update(s.getBytes(StandardCharsets.UTF_8));
                md.update(delimiter.getBytes(StandardCharsets.UTF_8));
            }
            StringBuilder result = new StringBuilder();
            for (byte b : md.digest()) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    result.append('0');
                }
                result.append(hex);
            }
            return result.toString();
        }
    }
}
