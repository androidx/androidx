/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.renderscript;


import java.lang.reflect.Field;
import java.lang.Class;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import android.util.Log;

class RSReflect {
    Class mElement;
    Class mElementBuilder;
    Class mElementDataType;
    Class mElementDataKind;
    Method mElement_createUser;
    Method mElement_createVector;
    Method mElement_createPixel;
    Constructor mElementBuilder_constructor;
    Method mElementBuilder_add;
    Method mElementBuilder_create;

    Class mType;
    Class mTypeBuilder;
    Class mTypeCubemapFace;
    Constructor mTypeBuilder_constructor;
    Method mTypeBuilder_setX;
    Method mTypeBuilder_setY;
    Method mTypeBuilder_setMipmaps;
    Method mTypeBuilder_setFaces;

    Class mAllocation;
    Class mAllocationMipmapControl;
    Method mAllocation_syncAll;
    Method mAllocation_copyFrom_O;
    Method mAllocation_copyFromUnchecked_I;
    Method mAllocation_copyFromUnchecked_S;
    Method mAllocation_copyFromUnchecked_B;
    Method mAllocation_copyFromUnchecked_F;
    Method mAllocation_copyFrom_I;
    Method mAllocation_copyFrom_S;
    Method mAllocation_copyFrom_B;
    Method mAllocation_copyFrom_F;
    Method mAllocation_setFromFieldPacker;
    Method mAllocation_setFromFieldPacker_component;
    Method mAllocation_generateMipmaps;
    Method mAllocation_copy1DRangeFromUnchecked;


    Class mBaseObj;
    Class mRenderScript;
    Class mSampler;
    Class mScript;
    Class mScriptC;
    Class mScriptGroup;


    private RSReflect() {
    }

    private boolean init() {
        try {
            Method m[];

            mElement = Class.forName("android.renderscript.Element");
            mElementBuilder = Class.forName("android.renderscript.Element$Builder");
            mElementDataType = Class.forName("android.renderscript.Element$DataType");
            mElementDataKind = Class.forName("android.renderscript.Element$DataKind");

            mType = Class.forName("android.renderscript.Type");
            mTypeBuilder = Class.forName("android.renderscript.Type$Builder");
            mTypeCubemapFace = Class.forName("android.renderscript.Type$CubemapFace");

            mAllocation = Class.forName("android.renderscript.Allocation");
            mBaseObj = Class.forName("android.renderscript.BaseObj");
            mRenderScript = Class.forName("android.renderscript.RenderScript");
            mSampler = Class.forName("android.renderscript.Sampler");
            mScript = Class.forName("android.renderscript.Script");
            mScriptC = Class.forName("android.renderscript.ScriptC");

            mElement_createUser = mElement.getDeclaredMethod("createUser",
                        new Class[] { mRenderScript, mElementDataType });
            mElement_createVector = mElement.getDeclaredMethod("createVector",
                        new Class[] { mRenderScript, mElementDataType, Integer.TYPE });
            mElement_createPixel = mElement.getDeclaredMethod("createPixel",
                        new Class[] { mRenderScript, mElementDataType, mElementDataKind });
            mElementBuilder_constructor = mElementBuilder.getDeclaredConstructor(
                            new Class[] { mRenderScript });
            mElementBuilder_add = mElementBuilder.getDeclaredMethod("add",
                            new Class[] { mElement, String.class, Integer.TYPE });
            mElementBuilder_create = mElementBuilder.getDeclaredMethod("create",
                            new Class[] {});

            mTypeBuilder_constructor = mTypeBuilder.getDeclaredConstructor(
                            new Class[] { mRenderScript, mElement });
            mTypeBuilder_setX = mTypeBuilder.getDeclaredMethod("setX",
                            new Class[] { Integer.TYPE });
            mTypeBuilder_setY = mTypeBuilder.getDeclaredMethod("setY",
                            new Class[] { Integer.TYPE });
            mTypeBuilder_setMipmaps = mTypeBuilder.getDeclaredMethod("setMipmaps",
                            new Class[] { Boolean.TYPE });
            mTypeBuilder_setFaces = mTypeBuilder.getDeclaredMethod("setFaces",
                            new Class[] { Boolean.TYPE });





            //mScriptGroup = Class.forName("android.renderscript.Element");


        } catch (Throwable e) {
            android.util.Log.w("RSR", "Using native RS failed. " + e);
            return false;
        }
        return true;
    }

    static RSReflect create() {
        android.util.Log.v("RSR", "create");
        RSReflect r = new RSReflect();
        if (r.init()) {
            android.util.Log.v("RSR", "create ok");
            return r;
        }
        android.util.Log.v("RSR", "create fail");
        return null;
    }

    private Method findMethod(Method m[], String name) {
        for (int ct=0; ct < m.length; ct++) {
            if (m[ct].getName().equals(name)) {
                return m[ct];
            }
        }
        return null;
    }


    //Class c = Class.forName("java.lang.String");

}
