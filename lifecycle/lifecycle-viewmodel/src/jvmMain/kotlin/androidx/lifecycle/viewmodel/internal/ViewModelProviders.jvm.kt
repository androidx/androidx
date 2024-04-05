/*
<<<<<<<< HEAD:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/InvokeOnCanvas.kt
 * Copyright 2021 The Android Open Source Project
========
 * Copyright 2024 The Android Open Source Project
>>>>>>>> 5d516369f99416bd39919bc73974c857cadede07:lifecycle/lifecycle-viewmodel/src/jvmMain/kotlin/androidx/lifecycle/viewmodel/internal/ViewModelProviders.jvm.kt
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

package androidx.lifecycle.viewmodel.internal

import kotlin.reflect.KClass

internal actual val <T : Any> KClass<T>.canonicalName: String?
    get() = qualifiedName
