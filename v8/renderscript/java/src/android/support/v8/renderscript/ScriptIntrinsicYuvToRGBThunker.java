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


public class ScriptIntrinsicYuvToRGBThunker extends ScriptIntrinsicYuvToRGB {
    android.renderscript.ScriptIntrinsicYuvToRGB mN;

    android.renderscript.ScriptIntrinsicYuvToRGB getNObj() {
        return mN;
    }


    private ScriptIntrinsicYuvToRGBThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicYuvToRGBThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker) e;

        ScriptIntrinsicYuvToRGBThunker si = new ScriptIntrinsicYuvToRGBThunker(0, rs);
        try {
            si.mN = android.renderscript.ScriptIntrinsicYuvToRGB.create(rst.mN, et.getNObj());
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
        return si;
    }


    public void setInput(Allocation ain) {
        AllocationThunker aint = (AllocationThunker)ain;
        try {
            mN.setInput(aint.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public void forEach(Allocation aout) {
        AllocationThunker aoutt = (AllocationThunker)aout;
        try {
            mN.forEach(aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelID() {
        Script.KernelID k = createKernelID(0, 2, null, null);
        try {
            k.mN = mN.getKernelID();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public Script.FieldID getFieldID_Input() {
        Script.FieldID f = createFieldID(0, null);
        try {
            f.mN = mN.getFieldID_Input();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return f;
    }
}
