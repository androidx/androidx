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

package androidx.remotecallback.compiler;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;

/**
 * Holder class that is created for each class instance that is a
 * CallbackReceiver and has methods tagged with @RemoteCallable.
 */
class CallbackReceiver {

    @SuppressWarnings("unused")
    private static final String RESET = "reset";
    @SuppressWarnings("unused")
    private static final String GET_METHOD = "getMethod";
    @SuppressWarnings("unused")
    private static final String GET_ARGUMENTS = "getArguments";
    @SuppressWarnings("unused")
    private static final String GET_CLS_NAME = "getClsName";

    private final ProcessingEnvironment mEnv;
    private final Element mElement;
    private final String mClsName;
    private final ArrayList<CallableMethod> mMethods = new ArrayList<>();
    private final Messager mMessager;

    CallbackReceiver(Element c, ProcessingEnvironment env,
            Messager messager) {
        mEnv = env;
        mElement = c;
        mClsName = c.toString();
        mMessager = messager;
    }

    /**
     * Adds a method tagged with @RemoteCallable to this receiver.
     */
    void addMethod(Element element) {
        for (CallableMethod method: mMethods) {
            if (method.getName().equals(element.getSimpleName().toString())) {
                mMessager.printMessage(Diagnostic.Kind.ERROR,
                        "Multiple methods named " + element.getSimpleName());
                return;
            }
        }
        mMethods.add(new CallableMethod(mClsName, element, mEnv));
    }

    /**
     * Generates the code to handle creating and executing callbacks. The code
     * is assembled in one class that implements runnable that when run,
     * registers all of the CallbackHandlers.
     */
    void finish(ProcessingEnvironment env, Messager messager) {
        if (mMethods.size() == 0) {
            messager.printMessage(Diagnostic.Kind.ERROR, "No methods found for " + mClsName);
            return;
        }
        TypeSpec.Builder genClass = TypeSpec
                .classBuilder(findInitClass(mElement))
                .addOriginatingElement(mElement)
                .superclass(TypeName.get(mElement.asType()))
                .addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder runBuilder = MethodSpec
                .constructorBuilder()
                .addModifiers(Modifier.PUBLIC);
        for (CallableMethod method: mMethods) {
            method.addMethods(genClass, runBuilder, env, messager);
        }
        genClass.addMethod(runBuilder.build());
        try {
            TypeSpec typeSpec = genClass.build();
            String pkg = getPkg(mElement);
            JavaFile.builder(pkg, typeSpec)
                    .build()
                    .writeTo(mEnv.getFiler());
        } catch (IOException e) {
            messager.printMessage(Diagnostic.Kind.ERROR, "Exception writing " + e);
        }
    }

    private String findInitClass(Element element) {
        return String.format("%sInitializer", element.getSimpleName());
    }

    private String getPkg(Element s) {
        String pkg = mEnv.getElementUtils().getPackageOf(s).toString();
        return pkg;
    }
}
