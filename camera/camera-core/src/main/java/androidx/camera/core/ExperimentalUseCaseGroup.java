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

package androidx.camera.core;

import static java.lang.annotation.RetentionPolicy.CLASS;

import androidx.annotation.RequiresOptIn;

import java.lang.annotation.Retention;


/**
 * Denotes that the annotated classes and methods uses the experimental feature which provides
 * a grouping mechanism for {@link UseCase}s.
 *
 * <p> The {@link UseCaseGroup} is a class that groups {@link UseCase}s together. All the
 * {@link UseCase}s in the same group share certain properties. The {@link ViewPort} is a
 * collection of shared {@link UseCase} properties for synchronizing the visible rectangle across
 * all the use cases.
 */
@Retention(CLASS)
@RequiresOptIn
public @interface ExperimentalUseCaseGroup {
}
