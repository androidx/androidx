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

class ScriptIntrinsicConvolve3x3Thunker extends ScriptIntrinsicConvolve3x3 {
    android.renderscript.ScriptIntrinsicConvolve3x3 mN;

    android.renderscript.ScriptIntrinsicConvolve3x3 getNObj() {
        return mN;
    }


    ScriptIntrinsicConvolve3x3Thunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicConvolve3x3Thunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;

        ScriptIntrinsicConvolve3x3Thunker si = new ScriptIntrinsicConvolve3x3Thunker(0, rs);
        si.mN = android.renderscript.ScriptIntrinsicConvolve3x3.create(rst.mN, et.getNObj());
        return si;
    }

    public void setInput(Allocation ain) {
        AllocationThunker aint = (AllocationThunker)ain;
        mN.setInput(aint.getNObj());
    }

    public void setCoefficients(float v[]) {
        mN.setCoefficients(v);
    }

    public void forEach(Allocation aout) {
        AllocationThunker aoutt = (AllocationThunker)aout;
        mN.forEach(aoutt.getNObj());

    }

    public Script.KernelID getKernelID() {
        Script.KernelID k = createKernelID(0, 2, null, null);
        k.mN = mN.getKernelID();
        return k;
    }

    public Script.FieldID getFieldID_Input() {
        Script.FieldID f = createFieldID(1, null);
        f.mN = mN.getFieldID_Input();
        return f;
    }

}

