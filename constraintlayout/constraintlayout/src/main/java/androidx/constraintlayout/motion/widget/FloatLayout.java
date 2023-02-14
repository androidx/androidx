/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.constraintlayout.motion.widget;

/**
 * Add support to views that do floating point layout.
 * This can be useful to allow objects within the view to animate smoothly
 */
public interface FloatLayout {
    /**
     * To convert to regular layout
     * l = (int)(0.5f + lf);
     * You are expected to do your own measure if you need it.
     * This will be called only during animation.
     */
    void layout(float lf, float tf, float rf, float bf);
}
