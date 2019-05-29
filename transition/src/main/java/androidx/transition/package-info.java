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

/**
 * AndroidX Transition Library provides Transition API back to API level 14.
 * <p>
 * Transition API allows you to animate various kinds of layout changes automatically. For example,
 * you can present a change of {@link android.view.View#setVisibility(int) visibility} as fade in /
 * out animation like below:
 * </p>
 * <pre>
 *     TextView message = findViewById(R.id.message);
 *     ViewGroup container = findViewById(R.id.container);
 *     Fade fade = new Fade();
 *
 *     TransitionManager.beginDelayedTransition(container, fade);
 *     message.setVisibility(View.INVISIBLE);
 * </pre>
 * @see <a href="https://developer.android.com/training/transitions">Transition API Guide</a>
 */

package androidx.transition;
