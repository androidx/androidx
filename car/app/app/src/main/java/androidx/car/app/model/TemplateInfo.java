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

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Stores information about {@link Template} returned from a {@link
 * androidx.car.app.Screen}.
 */
public final class TemplateInfo {
    @Keep
    @Nullable
    private final Class<? extends Template> mTemplateClass;
    @Keep
    @Nullable
    private final String mTemplateId;

    public TemplateInfo(@NonNull Template template, @NonNull String templateId) {
        this.mTemplateClass = template.getClass();
        this.mTemplateId = templateId;
    }

    private TemplateInfo() {
        this.mTemplateClass = null;
        this.mTemplateId = null;
    }

    @NonNull
    public Class<? extends Template> getTemplateClass() {
        return requireNonNull(mTemplateClass);
    }

    @NonNull
    public String getTemplateId() {
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
}
