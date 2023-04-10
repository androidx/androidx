/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.utils

object Fixtures {
    const val CLASS_1 = "Lcom/sample/Activity;"
    const val CLASS_1_METHOD_1 = "HSPLcom/sample/Activity;-><init>()V"
    const val CLASS_1_METHOD_2 = "HSPLcom/sample/Activity;->onCreate(Landroid/os/Bundle;)V"
    const val CLASS_2 = "Lcom/sample/Utils;"
    const val CLASS_2_METHOD_1 = "HSLcom/sample/Utils;-><init>()V"
    const val CLASS_2_METHOD_2 = "HLcom/sample/Utils;->someMethod()V"
    const val CLASS_2_METHOD_3 = "HLcom/sample/Utils;->someOtherMethod()V"
    const val CLASS_2_METHOD_4 = "HSLcom/sample/Utils;->someOtherMethod()V"
    const val CLASS_2_METHOD_5 = "HSPLcom/sample/Utils;->someOtherMethod()V"
    const val CLASS_3 = "Lcom/sample/Fragment;"
    const val CLASS_3_METHOD_1 = "HSPLcom/sample/Fragment;-><init>()V"
    const val CLASS_4 = "Lcom/sample/SomeOtherClass;"
    const val CLASS_4_METHOD_1 = "HSPLcom/sample/SomeOtherClass;-><init>()V"
}
