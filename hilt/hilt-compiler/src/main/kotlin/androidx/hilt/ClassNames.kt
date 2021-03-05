/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.hilt

import com.squareup.javapoet.ClassName

internal object ClassNames {
    val ANDROIDX_ASSISTED = ClassName.get("androidx.hilt", "Assisted")
    val ASSISTED = ClassName.get("dagger.assisted", "Assisted")
    val ASSISTED_FACTORY = ClassName.get("dagger.assisted", "AssistedFactory")
    val ASSISTED_INJECT = ClassName.get("dagger.assisted", "AssistedInject")
    val BINDS = ClassName.get("dagger", "Binds")
    val CONTEXT = ClassName.get("android.content", "Context")
    val HILT_WORKER = ClassName.get("androidx.hilt.work", "HiltWorker")
    val NON_NULL = ClassName.get("androidx.annotation", "NonNull")
    val INJECT = ClassName.get("javax.inject", "Inject")
    val INSTALL_IN = ClassName.get("dagger.hilt", "InstallIn")
    val INTO_MAP = ClassName.get("dagger.multibindings", "IntoMap")
    val LISTENABLE_WORKER = ClassName.get("androidx.work", "ListenableWorker")
    val MODULE = ClassName.get("dagger", "Module")
    val ORIGINATING_ELEMENT = ClassName.get("dagger.hilt.codegen", "OriginatingElement")
    val PROVIDER = ClassName.get("javax.inject", "Provider")
    val SINGLETON_COMPONENT =
        ClassName.get("dagger.hilt.components", "SingletonComponent")
    val VIEW_MODEL_ASSISTED_FACTORY =
        ClassName.get("androidx.hilt.lifecycle", "ViewModelAssistedFactory")
    val STRING_KEY = ClassName.get("dagger.multibindings", "StringKey")
    val WORKER = ClassName.get("androidx.work", "Worker")
    val WORKER_ASSISTED_FACTORY = ClassName.get("androidx.hilt.work", "WorkerAssistedFactory")
    val WORKER_PARAMETERS = ClassName.get("androidx.work", "WorkerParameters")
}
