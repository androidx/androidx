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

import static androidx.remotecallback.compiler.RemoteCallbackProcessor.EXTERNAL_INPUT;
import static androidx.remotecallback.compiler.RemoteCallbackProcessor.REMOTE_CALLABLE;

import androidx.annotation.NonNull;

import java.util.HashMap;
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
 * Processes annotations from RemoteCallbacks.
 */
@SupportedAnnotationTypes({REMOTE_CALLABLE, EXTERNAL_INPUT})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class RemoteCallbackProcessor extends AbstractProcessor {

    @NonNull
    static final String REMOTE_CALLABLE = "androidx.remotecallback.RemoteCallable";

    @NonNull
    static final String EXTERNAL_INPUT = "androidx.remotecallback.ExternalInput";

    private HashMap<Element, CallbackReceiver> mMap = new HashMap<>();
    private ProcessingEnvironment mEnv;
    private Messager mMessager;

    @Override
    public synchronized void init(@NonNull ProcessingEnvironment processingEnvironment) {
        mEnv = processingEnvironment;
        mMessager = processingEnvironment.getMessager();
    }

    @Override
    public boolean process(
            @NonNull Set<? extends TypeElement> set,
            @NonNull RoundEnvironment roundEnvironment
    ) {
        if (set.isEmpty()) return true;
        TypeElement remoteCallable = findAnnotation(set, REMOTE_CALLABLE);

        for (Element element : roundEnvironment.getElementsAnnotatedWith(remoteCallable)) {
            Element cls = findClass(element);
            mMap.computeIfAbsent(cls, (c) -> new CallbackReceiver(c, mEnv, mMessager))
                    .addMethod(element);
        }
        for (CallbackReceiver receiver: mMap.values()) {
            receiver.finish(mEnv, mMessager);
        }
        return true;
    }

    private Element findClass(Element element) {
        if (element != null && element.getKind() != ElementKind.CLASS) {
            return findClass(element.getEnclosingElement());
        }
        return element;
    }

    private TypeElement findAnnotation(Set<? extends TypeElement> set, String name) {
        for (TypeElement typeElement : set) {
            if (String.valueOf(typeElement).equals(name)) {
                return typeElement;
            }
        }
        return null;
    }
}
