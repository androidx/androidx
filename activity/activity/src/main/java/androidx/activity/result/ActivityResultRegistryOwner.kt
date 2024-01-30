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
package androidx.activity.result

/**
 * A class that has an [ActivityResultRegistry] that allows you to register a
 * [ActivityResultCallback] for handling an
 * [androidx.activity.result.contract.ActivityResultContract].
 *
 * If it is not safe to call [ActivityResultRegistry.register]
 * in the constructor, it is strongly recommended to also implement [ActivityResultCaller].
 *
 * @see ActivityResultRegistry
 */
interface ActivityResultRegistryOwner {
    /**
     * Returns the ActivityResultRegistry of the provider.
     *
     * @return The activity result registry of the provider.
     */
    val activityResultRegistry: ActivityResultRegistry
}
