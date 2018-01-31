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

import com.squareup.javapoet.JavaFile;

import androidx.webkit.internal.codegen.representations.ClassRepr;

/**
 * Contains a bunch of utility methods for generating WebView Support Library boundary interfaces.
 */
public class BoundaryGeneration {
    private static final String BOUNDARY_INTERFACE_PACKAGE = "org.chromium.support_lib_boundary";
    private static final String INDENT = "    "; // 4 white spaces.

    /**
     * Create a WebView Support Library boundary interface given the class representation
     * {@classRepr}.
     */
    public static JavaFile createBoundaryInterface(ClassRepr classRepr) {
        return JavaFile.builder(BOUNDARY_INTERFACE_PACKAGE, classRepr.createBoundaryInterface())
                .indent(INDENT)
                .build();
    }
}
