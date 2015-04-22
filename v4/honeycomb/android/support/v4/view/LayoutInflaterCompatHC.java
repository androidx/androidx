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

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import java.lang.reflect.Field;
import java.util.ArrayList;

class LayoutInflaterCompatHC {
    private static final String TAG = "LayoutInflaterCompatHC";

    private static Field sLayoutInflaterFactory2Field;
    private static boolean sCheckedField;

    static class FactoryWrapperHC extends LayoutInflaterCompatBase.FactoryWrapper
            implements LayoutInflater.Factory2 {

        FactoryWrapperHC(LayoutInflaterFactory delegateFactory) {
            super(delegateFactory);
        }

        @Override
        public View onCreateView(View parent, String name, Context context,
                AttributeSet attributeSet) {
            return mDelegateFactory.onCreateView(parent, name, context, attributeSet);
        }
    }

    static void setFactory(LayoutInflater inflater, LayoutInflaterFactory factory) {
        final LayoutInflater.Factory2 factory2 = factory != null
                ? new FactoryWrapperHC(factory) : null;
        inflater.setFactory2(factory2);

        final LayoutInflater.Factory f = inflater.getFactory();
        if (f instanceof LayoutInflater.Factory2) {
            // The merged factory is now set to getFactory(), but not getFactory2() (pre-v21).
            // We will now try and force set the merged factory to mFactory2
            forceSetFactory2(inflater, (LayoutInflater.Factory2) f);
        } else {
            // Else, we will force set the original wrapped Factory2
            forceSetFactory2(inflater, factory2);
        }
    }

    /**
     * For APIs >= 11 && < 21, there was a framework bug that prevented a LayoutInflater's
     * Factory2 from being merged properly if set after a cloneInContext from a LayoutInflater
     * that already had a Factory2 registered. We work around that bug here. If we can't we
     * log an error.
     */
    static void forceSetFactory2(LayoutInflater inflater, LayoutInflater.Factory2 factory) {
        if (!sCheckedField) {
            try {
                sLayoutInflaterFactory2Field = LayoutInflater.class.getDeclaredField("mFactory2");
                sLayoutInflaterFactory2Field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "forceSetFactory2 Could not find field 'mFactory2' on class "
                        + LayoutInflater.class.getName()
                        + "; inflation may have unexpected results.", e);
            }
            sCheckedField = true;
        }
        if (sLayoutInflaterFactory2Field != null) {
            try {
                sLayoutInflaterFactory2Field.set(inflater, factory);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "forceSetFactory2 could not set the Factory2 on LayoutInflater "
                        + inflater + "; inflation may have unexpected results.", e);
            }
        }
    }
}
