/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.model.CarText;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.SearchTemplate;
import androidx.car.app.model.Template;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Encapsulates the constraints to apply when creating {@link TabContents}.
 *
 * @hide
 */
@RequiresCarApi(6)
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class TabContentsConstraints {

    /** Allow restricted set of templates as contents for a tab **/
    @NonNull
    public static final TabContentsConstraints DEFAULT =
            new TabContentsConstraints(Arrays.asList(
                    ListTemplate.class,
                    PaneTemplate.class,
                    GridTemplate.class,
                    MessageTemplate.class,
                    SearchTemplate.class
            ));
    private HashSet<Class<? extends Template>> mAllowedTemplateTypes;

    /**
     * Returns {@code true} if the {@link CarText} meets the constraints' requirement.
     *
     * @throws IllegalArgumentException if any span types are not allowed
     */
    public void validateOrThrow(@NonNull Template template) {
        if (!mAllowedTemplateTypes.contains(template.getClass())) {
            throw new IllegalArgumentException(
                    "Type is not allowed in tabs: " + template.getClass().getSimpleName());
        }
    }

    private TabContentsConstraints(List<Class<? extends Template>> allowedTypes) {
        mAllowedTemplateTypes = new HashSet<>(allowedTypes);
    }
}
