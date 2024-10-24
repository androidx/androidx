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

package androidx.car.app.model;

import static java.util.Objects.requireNonNull;

import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Stores information about {@link Template} returned from a {@link
 * androidx.car.app.Screen}.
 *
 * <p><strong>This class is for use by host implementations and not by apps.</strong>
 */
@CarProtocol
@KeepFields
public final class TemplateInfo {
    private final @Nullable Class<? extends Template> mTemplateClass;
    private final @Nullable String mTemplateId;

    /**
     * Constructs the info for the given template information provided.
     *
     * @param templateClass the class of the template this info is for
     * @param templateId    the unique id for the template
     */
    public TemplateInfo(@NonNull Class<? extends Template> templateClass,
            @NonNull String templateId) {
        mTemplateClass = templateClass;
        mTemplateId = templateId;
    }

    /**
     * Returns the type of template this instance contains.
     *
     * @see TemplateInfo#TemplateInfo(Class, String)
     */
    public @NonNull Class<? extends Template> getTemplateClass() {
        // Intentionally kept as non-null because the library creates these classes internally after
        // the app returns a non-null template, a null-value should not be expected here.
        return requireNonNull(mTemplateClass);
    }

    /**
     * Returns the ID of the template.
     *
     * @see TemplateInfo#TemplateInfo(Class, String)
     */
    public @NonNull String getTemplateId() {
        // Intentionally kept as non-null because the library creates these classes internally after
        // the app returns a non-null template, a null-value should not be expected here.
        return requireNonNull(mTemplateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTemplateClass, mTemplateId);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TemplateInfo)) {
            return false;
        }
        TemplateInfo otherInfo = (TemplateInfo) other;

        return Objects.equals(mTemplateClass, otherInfo.mTemplateClass)
                && Objects.equals(mTemplateId, otherInfo.mTemplateId);
    }

    private TemplateInfo() {
        mTemplateClass = null;
        mTemplateId = null;
    }
}
