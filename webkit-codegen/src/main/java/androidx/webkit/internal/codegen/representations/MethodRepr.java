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

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import javax.lang.model.element.Modifier;

import androidx.webkit.internal.codegen.TypeConversionUtils;

/**
 * Representation of a method for the support library to implement.
 */
public class MethodRepr {
    public final PsiMethod psiMethod;

    private MethodRepr(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }

    /**
     * Generate a MethodRepr from a PsiMethod.
     */
    public static MethodRepr fromPsiMethod(PsiMethod psiMethod) {
        return new MethodRepr(psiMethod);
    }

    /**
     * Generate a method declaration, to be put in a boundary interface, from this MethodRepr
     * representing an android.webkit method.
     */
    public MethodSpec createBoundaryInterfaceMethodDeclaration() {
        MethodSpec.Builder builder = MethodSpec.methodBuilder(this.psiMethod.getName())
                .returns(TypeConversionUtils.getBoundaryType(this.psiMethod.getReturnType()))
                // The ABSTRACT modifier here ensures the method doesn't have a body.
                .addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC);
        for (PsiParameter param : this.psiMethod.getParameterList().getParameters()) {
            builder.addParameter(
                    ParameterSpec.builder(
                            TypeConversionUtils.getBoundaryType(param.getType()),
                            param.getName()
                    ).build());
        }
        return builder.build();
    }
}
