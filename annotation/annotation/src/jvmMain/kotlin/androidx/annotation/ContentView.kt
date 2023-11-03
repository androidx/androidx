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
package androidx.annotation

/**
 * Annotation that can be attached to a constructor with a single [LayoutRes] parameter to denote
 * what layout the component intends to inflate and set as its content.
 *
 * It is strongly recommended that components that support this annotation specifically call it out
 * in their documentation.
 *
 * ```
 * public class MainFragment extends Fragment {
 *     public MainFragment() {
 *         // This constructor is annotated with @ContentView
 *         super(R.layout.main);
 *     }
 * }
 * ```
 *
 * @see androidx.activity.ComponentActivity.ComponentActivity
 * @see androidx.fragment.app.Fragment.Fragment
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CONSTRUCTOR)
public annotation class ContentView
