/*
 * Copyright 2025 The Android Open Source Project
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

// ***********************************************************************************
// MAINTAINED AND TESTED IN THE R8 REPO. PLEASE MAKE CHANGES THERE AND REPLICATE.
// ***********************************************************************************

package androidx.annotation.keep

/**
 * Used to define an unspecified class value in the Keep annotations.
 *
 * For example, when a method return class isn't important to specify in
 * [UsesReflectionToAccessMethod.returnClass], it defaults to `Unspecified::class`, which signifies
 * that any class is accepted.
 *
 * This is used as `null` cannot be used as a default in annotations.
 */
public class Unspecified
