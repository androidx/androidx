/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.resourceinspection.annotation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a view within AppCompat that shadows platform attributes.
 * <p>
 * Many AppCompat views shadow a platform attribute in order to provide backwards compatibility
 * for older API levels. This means that a developer might set
 * {@code androidx.appcompat:backgroundTint} instead of {@code android:backgroundTint} to get a
 * background tint with support for API < 21. On more recent versions of the platform, this has
 * the effect of storing the resolution stack in the {@code androidx.appcompat} namespace but
 * reading the set value from the platform inspection companion in the {@code android} namespace,
 * causing the resolution stack to get lost in the inspector.
 * <p>
 * Ordinarily, this behavior could be overridden by an {@link Attribute} annotation on the getter,
 * but it is infeasible to override a platform getter that doesn't exist on older supported API
 * levels. It results in worse performance at load time on those devices.
 * <p>
 * This annotation instructs the processor to include a list of shadowed attributes with
 * API-level appropriate accessors in the view's inspection companion. It infers the attributes
 * to include from the interfaces on the annotated view. For example, a view that implements
 * {@link androidx.core.view.TintableBackgroundView} will report
 * {@code androidx.appcompat:backgroundTint} directly from the platform
 * {@link android.view.View.getBackgroundTintList()} getter. This approach allows views within
 * AppCompat to mix shadowed attributes and regular attribute annotations on the same view
 * without hand-written inspection companions.
 * <p>
 * The full list of supported interfaces is
 * {@link androidx.core.widget.AutoSizeableTextView},
 * {@link androidx.core.view.TintableBackgroundView},
 * {@link androidx.core.widget.TintableCheckedTextView},
 * {@link androidx.core.widget.TintableCompoundButton},
 * {@link androidx.core.widget.TintableCompoundDrawablesView}, and
 * {@link androidx.core.widget.TintableImageSourceView}.
 * Please see the mapping in {@link androidx.resourceinspection.processor} for full details.
 *
 * @hide
 */
@Target(TYPE)
@Retention(SOURCE)
@RestrictTo(LIBRARY_GROUP_PREFIX)
public @interface AppCompatShadowedAttributes {
}
