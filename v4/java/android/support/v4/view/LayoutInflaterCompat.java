/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.view;

import android.os.Build;
import android.view.LayoutInflater;

/**
 * Helper for accessing features in {@link android.view.LayoutInflater}
 * introduced after API level 4 in a backwards compatible fashion.
 */
public class LayoutInflaterCompat {

    interface LayoutInflaterCompatImpl {
        public void setFactory(LayoutInflater layoutInflater, LayoutInflaterFactory factory);
    }

    static class LayoutInflaterCompatImplBase implements LayoutInflaterCompatImpl {
        @Override
        public void setFactory(LayoutInflater layoutInflater, LayoutInflaterFactory factory) {
            LayoutInflaterCompatBase.setFactory(layoutInflater, factory);
        }
    }

    static class LayoutInflaterCompatImplV11 extends LayoutInflaterCompatImplBase {
        @Override
        public void setFactory(LayoutInflater layoutInflater, LayoutInflaterFactory factory) {
            LayoutInflaterCompatHC.setFactory(layoutInflater, factory);
        }
    }

    static final LayoutInflaterCompatImpl IMPL;
    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 11) {
            IMPL = new LayoutInflaterCompatImplV11();
        } else {
            IMPL = new LayoutInflaterCompatImplBase();
        }
    }

    /*
     * Hide the constructor.
     */
    private LayoutInflaterCompat() {
    }

    /**
     * Attach a custom Factory interface for creating views while using
     * this LayoutInflater. This must not be null, and can only be set once;
     * after setting, you can not change the factory.
     *
     * @see LayoutInflater#setFactory(android.view.LayoutInflater.Factory)
     */
    public static void setFactory(LayoutInflater inflater, LayoutInflaterFactory factory) {
        IMPL.setFactory(inflater, factory);
    }

}
