/*
 * Copyright (C) 2016 The Android Open Source Project
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
package android.support.annotation;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;

/**
 * Typedef for the {@link VisibleForTesting#otherwise} attribute.
 *
 * @hide
 */
@IntDef({VisibleForTesting.PRIVATE,
         VisibleForTesting.PACKAGE_PRIVATE,
         VisibleForTesting.PROTECTED,
         VisibleForTesting.NONE}
// Important: If updating these constants, also update
// ../../../../external-annotations/android/support/annotation/annotations.xml
)
@Retention(SOURCE)
@interface ProductionVisibility {
}
