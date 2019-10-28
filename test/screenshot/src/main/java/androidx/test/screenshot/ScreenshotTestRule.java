/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.test.screenshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ScreenshotTestRule implements TestRule {

    @Override
    public @NonNull Statement apply(@NonNull Statement base, @Nullable Description description) {
        return new ScreenshotTestStatement(base);
    }

    static class ScreenshotTestStatement extends Statement {

        final Statement mBase;

        ScreenshotTestStatement(Statement base) {
            super();
            mBase = base;
        }

        @Override
        public void evaluate() throws Throwable {
            Assume.assumeTrue(android.os.Build.MODEL.contains("Cuttlefish"));
            mBase.evaluate();
        }
    }
}
