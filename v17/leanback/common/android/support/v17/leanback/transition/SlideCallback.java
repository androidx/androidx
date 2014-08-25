/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.transition;

import android.view.View;

/**
 * Used by Slide to determine the slide edge and distance when it is about to
 * create animator.
 * @hide
 */
public interface SlideCallback {

    /**
     * Called when Slide is about to create animator for an appearing/disappearing view.
     * Callback returns true to ask Slide to create animator, edge is returned
     * in edge[0], distance in pixels is returned in distance[0].  Slide will not
     * create animator if callback returns false.
     */
    public boolean getSlide(View view, boolean appear, int[] edge, float[] distance);

}
