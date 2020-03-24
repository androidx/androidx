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

package androidx.lifecycle.hilt

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType.ISOLATING
import javax.annotation.processing.Processor
import javax.lang.model.element.Element

/**
 * Should generate:
 * ```
 * @Module
 * @InstallIn(ActivityComponent.class)
 * public abstract class $_HiltModule {
 *   @Binds
 *   @IntoMap
 *   @ViewModelKey($.class)
 *   public bind($_AssistedFactory f): ViewModelAssistedFactory<?>
 * }
 * ```
 * and
 * ```
 * class $_AssistedFactory extends ViewModelAssistedFactory<$> {
 *
 *   private final Provider<Dep1> dep1;
 *   private final Provider<Dep2> dep2;
 *   ...
 *
 *   @Inject
 *   $_AssistedFactory(Provider<Dep1> dep1, Provider<Dep2> dep2, ...) {
 *     this.dep1 = dep1;
 *     this.dep2 = dep2;
 *     ...
 *   }
 *
 *   @Overrides
 *   @NonNull
 *   public $ create(@NonNull SavedStateHandle handle) {
 *     return new $(dep1.get(), dep2.get(), ..., handle);
 *   }
 * }
 * ```
 */
@AutoService(Processor::class)
@IncrementalAnnotationProcessor(ISOLATING)
class HiltViewModelProcessor : BasicAnnotationProcessor() {
    override fun initSteps() = listOf(ViewModelInjectStep())

    class ViewModelInjectStep : ProcessingStep {

        override fun annotations() = setOf(ViewModelInject::class.java)

        override fun process(
            elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
        ): MutableSet<out Element> {
            // TODO(danysantiago): Implement this...
            return mutableSetOf()
        }
    }
}