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

package androidx.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that can be attached to a component such as an
 * androidx.activity.ComponentActivity or {@link androidx.fragment.app.Fragment}
 * to denote what layout the component intends to inflate and set as its content.
 * <p>
 * This annotation is marked as {@link Inherited} and will therefore apply to subclasses
 * automatically.
 * <p>
 * It is strongly recommended that components that support this annotation specifically call
 * it out in their documentation.
 *
 * @see androidx.activity.ComponentActivity#onCreate(android.os.Bundle)
 * @see androidx.fragment.app.Fragment#onCreateView
 */
@Retention(RUNTIME)
@Target({TYPE})
@Inherited
public @interface ContentView {
    @LayoutRes
    int value();
}
