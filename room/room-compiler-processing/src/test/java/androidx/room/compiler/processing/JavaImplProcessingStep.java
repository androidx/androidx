/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.room.compiler.processing;

import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Not a JUnit test but a test nonetheless, it verifies that a Java source implementation of
 * XProcessingStep does not need to override interface methods with a body since Kotlin should
 * generate a default method, i.e. it verifies Kotlin's jvm-default is turned ON.
 */
@SuppressWarnings("deprecation") // On purpose overriding deprecated method.
public class JavaImplProcessingStep implements XProcessingStep {
    @Override
    public @NonNull Set<XElement> process(@NonNull XProcessingEnv env,
            @NonNull Map<String, ? extends Set<? extends XElement>> elementsByAnnotation) {
        return XProcessingStep.super.process(env, elementsByAnnotation);
    }

    @Override
    public void processOver(@NonNull XProcessingEnv env,
            @NonNull Map<String, ? extends Set<? extends XElement>> elementsByAnnotation) {
        XProcessingStep.super.processOver(env, elementsByAnnotation);
    }

    @Override
    public @NonNull Set<String> annotations() {
        return Collections.emptySet();
    }
}
