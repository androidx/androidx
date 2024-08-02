/*
 * Copyright 2024 The Android Open Source Project
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

package com.example.androidx.webkit;

import androidx.annotation.IntDef;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
@IntDef({
        ContentType.SAFE_CONTENT,
        ContentType.MALICIOUS_CONTENT,
        ContentType.RESTRICTED_CONTENT,
})
public @interface ContentType {
    int SAFE_CONTENT = 0;
    int MALICIOUS_CONTENT = 1;
    int RESTRICTED_CONTENT = 2;
}
