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

/**
 *
 * @hide
 **/
class ScriptIntrinsic3DLUTThunker extends ScriptIntrinsic3DLUT {
    android.renderscript.ScriptIntrinsic3DLUT mN;

    android.renderscript.ScriptIntrinsic3DLUT getNObj() {
        return mN;
    }

    private ScriptIntrinsic3DLUTThunker(int id, RenderScript rs, Element e) {
        super(id, rs, e);
    }

    public static ScriptIntrinsic3DLUTThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;

        ScriptIntrinsic3DLUTThunker lut = new ScriptIntrinsic3DLUTThunker(0, rs, e);
        try {
            lut.mN = android.renderscript.ScriptIntrinsic3DLUT.create(rst.mN, et.getNObj());
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
        return lut;
    }

    public void setLUT(Allocation lut) {
        AllocationThunker lutt = (AllocationThunker) lut;
        try {
            mN.setLUT(lutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }


    /**
     * Invoke the kernel and apply the lookup to each cell of ain
     * and copy to aout.
     *
     * @param ain Input allocation
     * @param aout Output allocation
     */
    public void forEach(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;
        try {
            mN.forEach(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    /**
     * Get a KernelID for this intrinsic kernel.
     *
     * @return Script.KernelID The KernelID object.
     */
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

