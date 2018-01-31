/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.webkit.internal.codegen;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiRecursiveElementVisitor;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;

public class TestUtils {
    static final String TEST_DATA_DIR = "codegen/";
    private static final String EXPECTED_TEST_DATA_DIR = "codegen-expected/";

    /**
     * Returns the directory containing test resource file dependencies.
     */
    static File getTestDepsDir() {
        return new File(TestUtils.getTestFilePath(TestUtils.TEST_DATA_DIR, "deps/"));
    }

    static PsiJavaFile getTestFile(Project project, String className) {
        String fileName = className + ".java";
        return createPsiFileFromFile(project, getTestFilePath(TEST_DATA_DIR, fileName));
    }

    static PsiJavaFile getExpectedTestFile(Project project, String className) {
        String fileName = className + ".java";
        return createPsiFileFromFile(project, getTestFilePath(EXPECTED_TEST_DATA_DIR, fileName));
    }

    private static String readEntireFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static String getTestFilePath(String testDataDir, String fileName) {
        URL resourceUrl = TestUtils.class.getClassLoader().getResource(testDataDir + fileName);
        try {
            return new File(resourceUrl.toURI()).getAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use the project {@param project} to parse the file with name {@param fileName} into a PsiFile
     * object.
     */
    private static PsiJavaFile createPsiFileFromFile(Project project, String fileName) {
        FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName);
        String code = readEntireFile(new File(fileName));
        return (PsiJavaFile)
                PsiFileFactory.getInstance(project).createFileFromText(fileName, type, code);
    }

    /**
     * Get a single class from a file - this should only be used as a utility method for testing.
     */
    static PsiClass getSingleClassFromFile(PsiFile classFile) {
        final PsiClass[] psiClass = {null};
        classFile.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PsiClass) {
                    psiClass[0] = (PsiClass) element;
                    return;
                }
                super.visitElement(element);
            }
        });
        return psiClass[0];
    }
}
