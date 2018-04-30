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

import static org.junit.Assert.assertEquals;

import androidx.webkit.internal.codegen.representations.ClassRepr;
import androidx.webkit.internal.codegen.representations.MethodRepr;

import com.android.tools.lint.LintCoreProjectEnvironment;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.squareup.javapoet.JavaFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class BoundaryInterfaceTest {
    private LintCoreProjectEnvironment mProjectEnv;

    @Before
    public void setUp() throws Exception {
        mProjectEnv = PsiProjectSetup.sProjectEnvironment;
        // Add files required to resolve dependencies in the tests in this class. This is needed for
        // example to identify a class as being an android.webkit class (and turn it into an
        // InvocationHandler).
        mProjectEnv.registerPaths(Arrays.asList(TestUtils.getTestDepsDir()));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testSingleClassAndMethod() {
        testBoundaryInterfaceGeneration("SingleClassAndMethod");
    }

    @Ignore
    @Test public void testWebkitReturnTypeGeneratesInvocationHandler() {
        testBoundaryInterfaceGeneration("WebKitTypeAsMethodParameter");
    }

    @Ignore
    @Test public void testWebkitMethodParameterTypeGeneratesInvocationHandler() {
        testBoundaryInterfaceGeneration("WebKitTypeAsMethodReturn");
    }

    /**
     * Ensures methods are filtered correctly so only explicitly added methods are added to the
     * boundary interface.
     */
    @Test public void testFilterMethods() {
        PsiFile inputFile =
                TestUtils.getTestFile(mProjectEnv.getProject(), "FilterMethods");
        PsiClass psiClass = TestUtils.getSingleClassFromFile(inputFile);
        MethodRepr method2 =
                MethodRepr.fromPsiMethod(psiClass.findMethodsByName("method2", false)[0]);
        ClassRepr classRepr = new ClassRepr(Arrays.asList(method2), psiClass);
        JavaFile actualBoundaryInterface = BoundaryGeneration.createBoundaryInterface(classRepr);
        assertBoundaryInterfaceCorrect(psiClass.getName(), actualBoundaryInterface);
    }

    // TODO(gsennton) add test case including a (static) inner class which should create a
    // separate boundary interface file.

    /**
     * Generates a boundary interface from the test-file with name {@param className}.java, and
     * compares the result to the test-file {@param className}BoundaryInterface.java.
     */
    private void testBoundaryInterfaceGeneration(String className) {
        PsiFile inputFile = TestUtils.getTestFile(mProjectEnv.getProject(), className);
        ClassRepr classRepr = ClassRepr.fromPsiClass(TestUtils.getSingleClassFromFile(inputFile));

        JavaFile actualBoundaryInterface = BoundaryGeneration.createBoundaryInterface(classRepr);
        assertBoundaryInterfaceCorrect(className, actualBoundaryInterface);
    }

    private void assertBoundaryInterfaceCorrect(String className,
            JavaFile actualBoundaryInterface) {
        PsiJavaFile expectedBoundaryFile = TestUtils.getExpectedTestFile(mProjectEnv.getProject(),
                className + "BoundaryInterface");
        assertEquals(expectedBoundaryFile.getText(), actualBoundaryInterface.toString());
    }
}
