/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.model.constraints;

import androidx.annotation.RestrictTo;
import androidx.car.app.model.GridSection;
import androidx.car.app.model.RowSection;
import androidx.car.app.model.Section;
import androidx.car.app.model.SectionedItemTemplate;
import androidx.car.app.navigation.model.MapWithContentTemplate;

import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;

/**
 * Encapsulates the constraints to apply when validating a {@link SectionedItemTemplate} under
 * different contexts.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SectionedItemTemplateConstraints {

    /**
     * Allowed {@link Section} types when {@link SectionedItemTemplate} is used as content within a
     * {@link MapWithContentTemplate}.
     */
    public static final @NonNull SectionedItemTemplateConstraints
            MAP_WITH_CONTENT_TEMPLATE_CONSTRAINTS =
            new SectionedItemTemplateConstraints(ImmutableSet.of(
                    RowSection.class,
                    GridSection.class
            ));

    private final ImmutableSet<Class<? extends Section<?>>> mAllowedSectionTypes;

    /**
     * Checks if the {@link SectionedItemTemplate} meets the constraint's requirement(s).
     *
     * @throws IllegalArgumentException if any section types are not allowed
     */
    public void validateOrThrow(@NonNull SectionedItemTemplate template) {
        for (Section<?> section : template.getSections()) {
            if (!mAllowedSectionTypes.contains(section.getClass())) {
                throw new IllegalArgumentException(
                        section.getClass().getSimpleName()
                                + " is not allowed in SectionedItemTemplate in this context");
            }
        }
    }

    private SectionedItemTemplateConstraints(
            ImmutableSet<Class<? extends Section<?>>> allowedSectionTypes) {
        this.mAllowedSectionTypes = allowedSectionTypes;
    }
}
