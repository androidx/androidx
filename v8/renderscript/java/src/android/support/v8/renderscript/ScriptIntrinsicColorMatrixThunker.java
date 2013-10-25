/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v8.renderscript;

import android.util.Log;

class ScriptIntrinsicColorMatrixThunker extends ScriptIntrinsicColorMatrix {
    android.renderscript.ScriptIntrinsicColorMatrix mN;

    android.renderscript.ScriptIntrinsicColorMatrix getNObj() {
        return mN;
    }

    private ScriptIntrinsicColorMatrixThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicColorMatrixThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker)e;

        ScriptIntrinsicColorMatrixThunker cm =  new ScriptIntrinsicColorMatrixThunker(0, rs);
        try {
            cm.mN = android.renderscript.ScriptIntrinsicColorMatrix.create(rst.mN, et.getNObj());
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
        return cm;

    }

    public void setColorMatrix(Matrix4f m) {
        try {
            mN.setColorMatrix(new android.renderscript.Matrix4f(m.getArray()));
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void setColorMatrix(Matrix3f m) {
        try {
            mN.setColorMatrix(new android.renderscript.Matrix3f(m.getArray()));
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void setGreyscale() {
        try {
            mN.setGreyscale();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void setYUVtoRGB() {
        try {
            mN.setYUVtoRGB();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void setRGBtoYUV() {
        try {
            mN.setRGBtoYUV();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }


    public void forEach(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;
        try {
            mN.forEach(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelID() {
        Script.KernelID k = createKernelID(0, 3, null, null);
        try {
            k.mN = mN.getKernelID();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

}

