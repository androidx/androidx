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
        blend.mN = android.renderscript.ScriptIntrinsicBlend.create(rst.mN, et.getNObj());
        return blend;
    }

    public void forEachClear(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachClear(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDClear() {
        Script.KernelID k = createKernelID(0, 3, null, null);
        k.mN = mN.getKernelIDClear();
        return k;
    }

    public void forEachSrc(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachSrc(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDSrc() {
        Script.KernelID k = createKernelID(1, 3, null, null);
        k.mN = mN.getKernelIDSrc();
        return k;
    }

    public void forEachDst(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachDst(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDDst() {
        Script.KernelID k = createKernelID(2, 3, null, null);
        k.mN = mN.getKernelIDDst();
        return k;
    }

    public void forEachSrcOver(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachSrcOver(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDSrcOver() {
        Script.KernelID k = createKernelID(3, 3, null, null);
        k.mN = mN.getKernelIDSrcOver();
        return k;
    }

    public void forEachDstOver(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachDstOver(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDDstOver() {
        Script.KernelID k = createKernelID(4, 3, null, null);
        k.mN = mN.getKernelIDDstOver();
        return k;
    }

    public void forEachSrcIn(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachSrcIn(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDSrcIn() {
        Script.KernelID k = createKernelID(5, 3, null, null);
        k.mN = mN.getKernelIDSrcIn();
        return k;
    }

    public void forEachDstIn(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachDstIn(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDDstIn() {
        Script.KernelID k = createKernelID(6, 3, null, null);
        k.mN = mN.getKernelIDDstIn();
        return k;
    }

    public void forEachSrcOut(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachSrcOut(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDSrcOut() {
        Script.KernelID k = createKernelID(7, 3, null, null);
        k.mN = mN.getKernelIDSrcOut();
        return k;
    }

    public void forEachDstOut(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachDstOut(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDDstOut() {
        Script.KernelID k = createKernelID(8, 3, null, null);
        k.mN = mN.getKernelIDDstOut();
        return k;
    }

    public void forEachSrcAtop(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachSrcAtop(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDSrcAtop() {
        Script.KernelID k = createKernelID(9, 3, null, null);
        k.mN = mN.getKernelIDSrcAtop();
        return k;
    }

    public void forEachDstAtop(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachDstAtop(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDDstAtop() {
        Script.KernelID k = createKernelID(10, 3, null, null);
        k.mN = mN.getKernelIDDstAtop();
        return k;
    }

    public void forEachXor(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachXor(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDXor() {
        Script.KernelID k = createKernelID(11, 3, null, null);
        k.mN = mN.getKernelIDXor();
        return k;
    }

    public void forEachMultiply(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachMultiply(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDMultiply() {
        Script.KernelID k = createKernelID(14, 3, null, null);
        k.mN = mN.getKernelIDMultiply();
        return k;
    }

    public void forEachAdd(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachAdd(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDAdd() {
        Script.KernelID k = createKernelID(34, 3, null, null);
        k.mN = mN.getKernelIDAdd();
        return k;
    }

    public void forEachSubtract(Allocation ain, Allocation aout) {
        AllocationThunker aint = (AllocationThunker)ain;
        AllocationThunker aoutt = (AllocationThunker)aout;

        mN.forEachSubtract(aint.getNObj(), aoutt.getNObj());
    }

    public Script.KernelID getKernelIDSubtract() {
        Script.KernelID k = createKernelID(35, 3, null, null);
        k.mN = mN.getKernelIDSubtract();
        return k;
    }

}

