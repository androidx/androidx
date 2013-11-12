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

class ScriptIntrinsicBlendThunker extends ScriptIntrinsicBlend {
    android.renderscript.ScriptIntrinsicBlend mN;

    android.renderscript.ScriptIntrinsicBlend getNObj() {
        return mN;
    }

    ScriptIntrinsicBlendThunker(int id, RenderScript rs) {
        super(id, rs);
    }

    public static ScriptIntrinsicBlendThunker create(RenderScript rs, Element e) {
        RenderScriptThunker rst = (RenderScriptThunker) rs;
        ElementThunker et = (ElementThunker)e;

        ScriptIntrinsicBlendThunker blend = new ScriptIntrinsicBlendThunker(0, rs);
        try {
            blend.mN = android.renderscript.ScriptIntrinsicBlend.create(rst.mN, et.getNObj());
        } catch (android.renderscript.RSRuntimeException exc) {
            throw ExceptionThunker.convertException(exc);
        }
        return blend;
    }

    public void forEachClear(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachClear(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDClear() {
        Script.KernelID k = createKernelID(0, 3, null, null);
        try {
            k.mN = mN.getKernelIDClear();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachSrc(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachSrc(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDSrc() {
        Script.KernelID k = createKernelID(1, 3, null, null);
        try {
            k.mN = mN.getKernelIDSrc();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachDst(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachDst(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDDst() {
        Script.KernelID k = createKernelID(2, 3, null, null);
        try {
            k.mN = mN.getKernelIDDst();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachSrcOver(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachSrcOver(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDSrcOver() {
        Script.KernelID k = createKernelID(3, 3, null, null);
        try {
            k.mN = mN.getKernelIDSrcOver();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachDstOver(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachDstOver(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDDstOver() {
        Script.KernelID k = createKernelID(4, 3, null, null);
        try {
            k.mN = mN.getKernelIDDstOver();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachSrcIn(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachSrcIn(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDSrcIn() {
        Script.KernelID k = createKernelID(5, 3, null, null);
        try {
            k.mN = mN.getKernelIDSrcIn();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachDstIn(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachDstIn(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDDstIn() {
        Script.KernelID k = createKernelID(6, 3, null, null);
        try {
            k.mN = mN.getKernelIDDstIn();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachSrcOut(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachSrcOut(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDSrcOut() {
        Script.KernelID k = createKernelID(7, 3, null, null);
        try {
            k.mN = mN.getKernelIDSrcOut();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachDstOut(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachDstOut(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDDstOut() {
        Script.KernelID k = createKernelID(8, 3, null, null);
        try {
            k.mN = mN.getKernelIDDstOut();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachSrcAtop(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachSrcAtop(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDSrcAtop() {
        Script.KernelID k = createKernelID(9, 3, null, null);
        try {
            k.mN = mN.getKernelIDSrcAtop();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachDstAtop(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachDstAtop(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDDstAtop() {
        Script.KernelID k = createKernelID(10, 3, null, null);
        try {
            k.mN = mN.getKernelIDDstAtop();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachXor(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachXor(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDXor() {
        Script.KernelID k = createKernelID(11, 3, null, null);
        try {
            k.mN = mN.getKernelIDXor();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachMultiply(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachMultiply(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDMultiply() {
        Script.KernelID k = createKernelID(14, 3, null, null);
        try {
            k.mN = mN.getKernelIDMultiply();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachAdd(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachAdd(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDAdd() {
        Script.KernelID k = createKernelID(34, 3, null, null);
        try {
            k.mN = mN.getKernelIDAdd();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

    public void forEachSubtract(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        try {
            mN.forEachSubtract(aint.getNObj(), aoutt.getNObj());
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
    }

    public Script.KernelID getKernelIDSubtract() {
        Script.KernelID k = createKernelID(35, 3, null, null);
        try {
            k.mN = mN.getKernelIDSubtract();
        } catch (android.renderscript.RSRuntimeException e) {
            throw ExceptionThunker.convertException(e);
        }
        return k;
    }

}

