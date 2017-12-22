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

import com.android.tools.lint.LintCoreApplicationEnvironment;
import com.android.tools.lint.LintCoreProjectEnvironment;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;

/**
 * Utility class for setting up the PSI environment.
 * PSI is normally used within IntelliJ which means parts of the interface between PSI and the rest
 * of IntelliJ need to be initialized in a specific way, or mocked, when using PSI from the command
 * line.
 */
public class PsiProjectSetup {
    public static final LintCoreProjectEnvironment sProjectEnvironment = createProjectEnvironment();

    private static LintCoreProjectEnvironment createProjectEnvironment() {
        // Lint already uses PSI from the command line - so we copy their project configuration.
        LintCoreApplicationEnvironment appEnv = LintCoreApplicationEnvironment.get();

        Disposable parentDisposable = Disposer.newDisposable();
        LintCoreProjectEnvironment projectEnvironment =
                LintCoreProjectEnvironment.create(parentDisposable, appEnv);

        return projectEnvironment;
    }
}
