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

package androidx.car.app.annotations;

import androidx.car.app.CarContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the minimum API level required to be able to use this model, field or method.
 * <p>
 * Before using any of this elements, application must check that
 * {@link CarContext#getCarAppApiLevel()} is equal or greater than the value of this annotation.
 * <p>
 * For example, if an application wants to use a newer template "Foo" marked with
 * <code>@RequiresHostApiLevel(2)</code> while maintain backwards compatibility with older hosts
 * by using an older template "Bar" (<code>@RequiresHostApiLevel(1)</code>), they can do:
 *
 * <pre>
 * if (getCarContext().getCarApiLevel() >= 2) {
 *     // Use new feature
 *     return Foo.Builder()....;
 * } else {
 *     // Use supported fallback
 *     return Bar.Builder()....;
 * }
 * </pre>
 *
 * If a certain model or method has no {@link RequiresCarApi} annotation, it is assumed to
 * be available in all car API levels.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
public @interface RequiresCarApi {

    /**
     * The minimum API level required to be able to use this model, field or method. Applications
     * shouldn't use any elements annotated with a {@link RequiresCarApi} greater than
     * {@link CarContext#getCarAppApiLevel()}
     */
    int value();
}
