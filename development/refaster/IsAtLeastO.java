/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.os.Build.VERSION;
import androidx.core.os.BuildCompat;

import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.AlsoNegation;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

/**
 * Replace usages of BuildCompat.isAtLeastO() with SDK_INT check.
 */
public class IsAtLeastO {
    @BeforeTemplate
    boolean usingAtLeastO() {
        return BuildCompat.isAtLeastO();
    }

    @AfterTemplate
    @AlsoNegation
    boolean optimizedMethod() {
        return VERSION.SDK_INT >= 26;
    }
}
