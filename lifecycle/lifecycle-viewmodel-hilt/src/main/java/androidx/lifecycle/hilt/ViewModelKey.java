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

package androidx.lifecycle.hilt;

import androidx.annotation.RestrictTo;
import androidx.lifecycle.ViewModel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dagger.MapKey;

/**
 * Dagger multibinding key for ViewModels
 *
 * @hide
 */
@MapKey
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public @interface ViewModelKey {
    /**
     * The ViewModel class used as key.
     * @return the class.
     */
    // TODO(danysantiago): Change to use strings and add optimizer rule to avoid class loading.
    Class<? extends ViewModel> value();
}
