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

package androidx.webkit.internal.codegen.representations;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Modifier;

/**
 * Representation of a class for the support library to implement. Note that the class should not
 * necessarily contain all of the members of the class we are providing a compatibility version of.
 */
public class ClassRepr {
    /**
     * List of methods to implement in the support library version of this class.
     */
    public final List<MethodRepr> methods;

    /**
     * Reference to the original class we want to supplement.
     */
    public final PsiClass psiClass;

    public ClassRepr(List<MethodRepr> methods, PsiClass psiClass) {
        this.methods = methods;
        this.psiClass = psiClass;
    }

    /**
     * Helper method for creating a ClassRepr containing all of the methods in the original Android
     * class {@param psiClass}.
     */
    public static ClassRepr fromPsiClass(PsiClass psiClass) {
        List<MethodRepr> methods = new ArrayList<>();
        for (PsiMethod psiMethod : psiClass.getMethods()) {
            methods.add(MethodRepr.fromPsiMethod(psiMethod));
        }
        return new ClassRepr(methods, psiClass);
    }


    /**
     * Create a Boundary Interface from this ClassRepr.
     */
    public TypeSpec createBoundaryInterface() {
        TypeSpec.Builder interfaceBuilder =
                TypeSpec.interfaceBuilder(this.psiClass.getName() + "BoundaryInterface")
                        .addModifiers(Modifier.PUBLIC);
        for (MethodRepr methodRepr : this.methods) {
            interfaceBuilder.addMethod(methodRepr.createBoundaryInterfaceMethodDeclaration());
        }
        return interfaceBuilder.build();
    }
}
