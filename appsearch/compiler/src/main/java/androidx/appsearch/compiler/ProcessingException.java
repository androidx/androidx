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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/**
 * An exception thrown from the appsearch annotation processor to indicate something went wrong.
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
final class ProcessingException extends Exception {
    @Nullable
    private final Element mCulprit;

    /**
     * Warnings associated with this error which should be reported alongside it at a lower level.
     */
    private final List<ProcessingException> mWarnings = new ArrayList<>();

    ProcessingException(@NonNull String message, @Nullable Element culprit) {
        super(message);
        mCulprit = culprit;
    }

    public void addWarning(@NonNull ProcessingException warning) {
        mWarnings.add(warning);
    }

    public void addWarnings(@NonNull Collection<ProcessingException> warnings) {
        mWarnings.addAll(warnings);
    }

    public void printDiagnostic(Messager messager) {
        printDiagnostic(messager, Diagnostic.Kind.ERROR);
    }

    private void printDiagnostic(Messager messager, Diagnostic.Kind level) {
        messager.printMessage(level, getMessage(), mCulprit);
        for (ProcessingException warning : mWarnings) {
            warning.printDiagnostic(messager, Diagnostic.Kind.WARNING);
        }
    }
}
