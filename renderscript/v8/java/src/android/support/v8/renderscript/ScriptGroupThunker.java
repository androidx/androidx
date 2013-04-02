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

import java.lang.reflect.Method;
import java.util.ArrayList;

class ScriptGroupThunker extends ScriptGroup {
    android.renderscript.ScriptGroup mN;

    android.renderscript.ScriptGroup getNObj() {
        return mN;
    }

    ScriptGroupThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public void setInput(Script.KernelID s, Allocation a) {
        AllocationThunker at = (AllocationThunker) a;
        mN.setInput(s.mN, at.getNObj());
    }

    public void setOutput(Script.KernelID s, Allocation a) {
        AllocationThunker at = (AllocationThunker) a;
        mN.setOutput(s.mN, at.getNObj());
    }

    public void execute() {
        mN.execute();
    }


    public static final class Builder {

        android.renderscript.ScriptGroup.Builder bN;
        RenderScript mRS;

        Builder(RenderScript rs) {
            RenderScriptThunker rst = (RenderScriptThunker) rs;
            mRS = rs;
            bN = new android.renderscript.ScriptGroup.Builder(rst.mN);
        }

        public Builder addKernel(Script.KernelID k) {
            bN.addKernel(k.mN);
            return this;
        }

        public Builder addConnection(Type t, Script.KernelID from, Script.FieldID to) {
            TypeThunker tt = (TypeThunker) t;
            bN.addConnection(tt.getNObj(), from.mN, to.mN);
            return this;
        }

        public Builder addConnection(Type t, Script.KernelID from, Script.KernelID to) {
            TypeThunker tt = (TypeThunker) t;
            bN.addConnection(tt.getNObj(), from.mN, to.mN);
            return this;
        }



        public ScriptGroupThunker create() {
            ScriptGroupThunker sg = new ScriptGroupThunker(0, mRS);
            sg.mN = bN.create();
            return sg;
        }
    }


}


