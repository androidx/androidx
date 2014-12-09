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
import android.view.LayoutInflater;
import android.view.View;

class LayoutInflaterCompatHC {

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
        inflater.setFactory2(factory != null ? new FactoryWrapperHC(factory) : null);
    }

}
