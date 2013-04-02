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

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

class ScriptIntrinsicBlurThunker extends ScriptIntrinsicBlur {

    android.renderscript.ScriptIntrinsicBlur mN;

    android.renderscript.ScriptIntrinsicBlur getNObj() {
        return mN;
    }

    protected ScriptIntrinsicBlurThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicBlurThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;

        ScriptIntrinsicBlurThunker blur = new ScriptIntrinsicBlurThunker(0, rs);
        blur.mN = android.renderscript.ScriptIntrinsicBlur.create(rst.mN, et.getNObj());
        return blur;
    }

    public void setInput(Allocation ain) {
        AllocationThunker aint = (AllocationThunker) ain;
        mN.setInput(aint.getNObj());
    }

    public void setRadius(float radius) {
        mN.setRadius(radius);
    }

    public void forEach(Allocation aout) {
        AllocationThunker aoutt = (AllocationThunker) aout;
        if (aoutt != null) {
            mN.forEach(aoutt.getNObj());
        }
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

