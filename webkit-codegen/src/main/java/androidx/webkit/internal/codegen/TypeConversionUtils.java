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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

/**
 * A mix of utilities related to type conversion/recognition.
 */
public class TypeConversionUtils {
    private static final String INVOCATION_HANDLER_CLASS = "java.lang.reflect.InvocationHandler";
    private static final PsiElementFactory sPsiFactory = JavaPsiFacade.getElementFactory(
            PsiProjectSetup.sProjectEnvironment.getProject());

    /**
     * Return a type suitable for use across the boundary - i.e. an InvocationHandler if
     * the type @param psiType is an android.webkit type.
     */
    public static TypeName getBoundaryType(PsiType psiType) {
        String typeName = psiType.getCanonicalText();
        if (typeName.startsWith("android.webkit.")) {
            return typeSpecFromString(
                    sPsiFactory.createTypeByFQClassName(
                            INVOCATION_HANDLER_CLASS).getCanonicalText());
        }
        return typeSpecFromString(typeName);
    }

    /**
     * Return the corresponding TypeName for the String {@param type}.
     */
    private static TypeName typeSpecFromString(String type) {
        switch (type) {
            case "void": return TypeName.VOID;
            case "boolean": return TypeName.BOOLEAN;
            case "byte": return TypeName.BYTE;
            case "short": return TypeName.SHORT;
            case "int": return TypeName.INT;
            case "long": return TypeName.LONG;
            case "char": return TypeName.CHAR;
            case "float": return TypeName.FLOAT;
            case "double": return TypeName.DOUBLE;
            default: return ClassName.bestGuess(type);
        }
    }
}
