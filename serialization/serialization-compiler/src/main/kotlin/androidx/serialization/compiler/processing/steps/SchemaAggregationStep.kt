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

package androidx.serialization.compiler.processing.steps

import androidx.serialization.Action
import androidx.serialization.EnumValue
import androidx.serialization.Field
import androidx.serialization.Reserved
import androidx.serialization.schema.Schema
import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep
import com.google.common.collect.ImmutableSet
import com.google.common.collect.SetMultimap
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element

/**
 * Processing step that aggregates schema annotations into a [Schema].
 */
class SchemaAggregationStep(
    private val processingEnv: ProcessingEnvironment
) : ProcessingStep {
    override fun process(
        elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>
    ): Set<Element> {
        // TODO: Implement schema collection
        return emptySet()
    }

    fun currentSchema(): Schema {
        TODO()
    }

    override fun annotations(): Set<Class<out Annotation>> = ANNOTATIONS

    private companion object {
        val ANNOTATIONS: Set<Class<out Annotation>> = ImmutableSet.of(
            Action::class.java,
            EnumValue::class.java,
            Field::class.java,
            Reserved::class.java,
            Reserved.IdRange::class.java
        )
    }
}
