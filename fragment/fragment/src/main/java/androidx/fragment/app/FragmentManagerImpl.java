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

package androidx.fragment.app;

/**
 * Package private implementation of FragmentManager.
 * <p>
 * All of the logic that used to be in this class has moved
 * to {@link FragmentManager} itself.
 * <p>
 * Developers should never instantiate a FragmentManager
 * directly, but instead operate via the APIs on
 * {@link FragmentActivity}, {@link Fragment}, or
 * {@link FragmentController} to retrieve an instance.
 */
class FragmentManagerImpl extends FragmentManager {
}
