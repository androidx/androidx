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

package androidx.car.app.navigation.model.constraints;

import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;

import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;

/**
 * Encapsulates the constraints to apply when creating a Content {@link Template} within a parent
 * template.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresCarApi(7)
public class ContentTemplateConstraints {
    /** Allowed templates for Map with Content Templates */
    public static final @NonNull ContentTemplateConstraints MAP_WITH_CONTENT_TEMPLATE_CONSTRAINTS =
            new ContentTemplateConstraints(ImmutableSet.of(
                    GridTemplate.class,
                    MessageTemplate.class,
                    ListTemplate.class,
                    PaneTemplate.class
            ));

    /** Allowed templates for TabContents */
    public static final @NonNull ContentTemplateConstraints TAB_CONTENTS_CONSTRAINTS =
            new ContentTemplateConstraints(ImmutableSet.of(
                    ListTemplate.class,
                    PaneTemplate.class,
                    GridTemplate.class,
                    MessageTemplate.class,
                    SearchTemplate.class
            ));
    private ImmutableSet<Class<? extends Template>> mAllowedTemplateTypes;

    /**
     * Checks if the {@link Template} meets the constraint's requirement(s).
     *
     * @throws IllegalArgumentException if any types are not allowed
     */
    public void validateOrThrow(@NonNull Template template) {
        if (!mAllowedTemplateTypes.contains(template.getClass())) {
            throw new IllegalArgumentException(template.getClass().getSimpleName()
                    + " is not allowed as content within the parent template");
        }
    }

    private ContentTemplateConstraints(ImmutableSet<Class<? extends Template>> allowedTypes) {
        this.mAllowedTemplateTypes = allowedTypes;
    }
}
