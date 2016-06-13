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

package android.support.v8.renderscript;

import android.util.SparseArray;

/**
 * The parent class for all executable scripts. This should not be used by
 * applications.
 **/
public class Script extends BaseObj {
    /**
     * Determine if Incremental Intrinsic Support is needed
     *
     */
    private boolean mUseIncSupp;
    protected void setIncSupp(boolean useInc) {
        mUseIncSupp = useInc;
    }
    protected boolean isIncSupp() {
        return mUseIncSupp;
    }
    /**
     * An allocation for the compat context will be created when needed
     * e.g. foreach(ain, aout), setVar(ain);
     *
     */
    long getDummyAlloc(Allocation ain) {
        long dInElement = 0;
        long dInType = 0;
        long dummyAlloc = 0;
        if (ain != null) {
            Type inType = ain.getType();
            dInElement = inType.getElement().getDummyElement(mRS);
            dInType = inType.getDummyType(mRS, dInElement);
            int xBytesSize = inType.getX() * inType.getElement().getBytesSize();
            dummyAlloc = mRS.nIncAllocationCreateTyped(ain.getID(mRS), dInType, xBytesSize);
            ain.setIncAllocID(dummyAlloc);
        }

        return dummyAlloc;
    }
    /**
     * KernelID is an identifier for a Script + root function pair. It is used
     * as an identifier for ScriptGroup creation.
     *
     * This class should not be directly created. Instead use the method in the
     * reflected or intrinsic code "getKernelID_funcname()".
     *
     */
    public static final class KernelID extends BaseObj {
        android.renderscript.Script.KernelID mN;
        Script mScript;
        int mSlot;
        int mSig;
        KernelID(long id, RenderScript rs, Script s, int slot, int sig) {
            super(id, rs);
            mScript = s;
            mSlot = slot;
            mSig = sig;
        }
    }

    private final SparseArray<KernelID> mKIDs = new SparseArray<KernelID>();
    /**
     * Only to be used by generated reflected classes.
     *
     *
     * @param slot
     * @param sig
     * @param ein
     * @param eout
     *
     * @return KernelID
     */
    protected KernelID createKernelID(int slot, int sig, Element ein, Element eout) {
        KernelID k = mKIDs.get(slot);
        if (k != null) {
            return k;
        }

        long id = mRS.nScriptKernelIDCreate(getID(mRS), slot, sig, mUseIncSupp);
        if (id == 0) {
            throw new RSDriverException("Failed to create KernelID");
        }

        k = new KernelID(id, mRS, this, slot, sig);

        mKIDs.put(slot, k);
        return k;
    }

    /**
     * InvokeID is an identifier for a invoke function. It is used
     * as an identifier for ScriptGroup creation.
     *
     * This class should not be directly created. Instead use the method in the
     * reflected or intrinsic code "getInvokeID_funcname()".
     *
     */
    public static final class InvokeID extends BaseObj {
        Script mScript;
        int mSlot;
        InvokeID(long id, RenderScript rs, Script s, int slot) {
            super(id, rs);
            mScript = s;
            mSlot = slot;
        }
    }

    private final SparseArray<InvokeID> mIIDs = new SparseArray<InvokeID>();
    /**
     * Only to be used by generated reflected classes.
     */
    protected InvokeID createInvokeID(int slot) {
        InvokeID i = mIIDs.get(slot);
        if (i != null) {
            return i;
        }

        long id = mRS.nScriptInvokeIDCreate(getID(mRS), slot);
        if (id == 0) {
            throw new RSDriverException("Failed to create KernelID");
        }

        i = new InvokeID(id, mRS, this, slot);
        mIIDs.put(slot, i);
        return i;
    }

    /**
     * FieldID is an identifier for a Script + exported field pair. It is used
     * as an identifier for ScriptGroup creation.
     *
     * This class should not be directly created. Instead use the method in the
     * reflected or intrinsic code "getFieldID_funcname()".
     *
     */
    public static final class FieldID extends BaseObj {
        android.renderscript.Script.FieldID mN;
        Script mScript;
        int mSlot;
        FieldID(long id, RenderScript rs, Script s, int slot) {
            super(id, rs);
            mScript = s;
            mSlot = slot;
        }
    }

    private final SparseArray<FieldID> mFIDs = new SparseArray();
    /**
     * Only to be used by generated reflected classes.
     *
     * @param slot
     * @param e
     *
     * @return FieldID
     */
    protected FieldID createFieldID(int slot, Element e) {
        FieldID f = mFIDs.get(slot);
        if (f != null) {
            return f;
        }

        long id = mRS.nScriptFieldIDCreate(getID(mRS), slot, mUseIncSupp);
        if (id == 0) {
            throw new RSDriverException("Failed to create FieldID");
        }

        f = new FieldID(id, mRS, this, slot);
        mFIDs.put(slot, f);
        return f;
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param slot
     */
    protected void invoke(int slot) {
        mRS.nScriptInvoke(getID(mRS), slot, mUseIncSupp);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param slot
     * @param v
     */
    protected void invoke(int slot, FieldPacker v) {
        if (v != null) {
            mRS.nScriptInvokeV(getID(mRS), slot, v.getData(), mUseIncSupp);
        } else {
            mRS.nScriptInvoke(getID(mRS), slot, mUseIncSupp);
        }
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param va
     * @param slot
     */
    public void bindAllocation(Allocation va, int slot) {
        mRS.validate();
        if (va != null) {
            mRS.nScriptBindAllocation(getID(mRS), va.getID(mRS), slot, mUseIncSupp);
        } else {
            mRS.nScriptBindAllocation(getID(mRS), 0, slot, mUseIncSupp);
        }
    }

    public void setTimeZone(String timeZone) {
        mRS.validate();
        try {
            mRS.nScriptSetTimeZone(getID(mRS), timeZone.getBytes("UTF-8"), mUseIncSupp);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Only intended for use by generated reflected code.
     *
     * @param slot
     * @param ain
     * @param aout
     * @param v
     */
    protected void forEach(int slot, Allocation ain, Allocation aout, FieldPacker v) {
        if (ain == null && aout == null) {
            throw new RSIllegalArgumentException(
                "At least one of ain or aout is required to be non-null.");
        }
        long in_id = 0;
        long out_id = 0;
        if (ain != null) {
            in_id = ain.getID(mRS);
        }
        if (aout != null) {
            out_id = aout.getID(mRS);
        }

        byte[] params = null;
        if (v != null) {
            params = v.getData();
        }

        if (mUseIncSupp) {
            long ainInc = getDummyAlloc(ain);
            long aoutInc = getDummyAlloc(aout);
            mRS.nScriptForEach(getID(mRS), slot, ainInc, aoutInc, params, mUseIncSupp);
        } else {
            mRS.nScriptForEach(getID(mRS), slot, in_id, out_id, params, mUseIncSupp);
        }
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param slot
     * @param ain
     * @param aout
     * @param v
     * @param sc
     */
    protected void forEach(int slot, Allocation ain, Allocation aout, FieldPacker v, LaunchOptions sc) {
        if (ain == null && aout == null) {
            throw new RSIllegalArgumentException(
                "At least one of ain or aout is required to be non-null.");
        }

        if (sc == null) {
            forEach(slot, ain, aout, v);
            return;
        }
        long in_id = 0;
        long out_id = 0;
        if (ain != null) {
            in_id = ain.getID(mRS);
        }
        if (aout != null) {
            out_id = aout.getID(mRS);
        }

        byte[] params = null;
        if (v != null) {
            params = v.getData();
        }
        if (mUseIncSupp) {
            long ainInc = getDummyAlloc(ain);
            long aoutInc = getDummyAlloc(aout);
            mRS.nScriptForEachClipped(getID(mRS), slot, ainInc, aoutInc, params, sc.xstart, sc.xend, sc.ystart, sc.yend, sc.zstart, sc.zend, mUseIncSupp);        
        } else {
            mRS.nScriptForEachClipped(getID(mRS), slot, in_id, out_id, params, sc.xstart, sc.xend, sc.ystart, sc.yend, sc.zstart, sc.zend, mUseIncSupp);
        }
    }

    Script(long id, RenderScript rs) {
        super(id, rs);
        mUseIncSupp = false;
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @hide
     */
    protected void forEach(int slot, Allocation[] ains, Allocation aout,
                           FieldPacker v) {
        forEach(slot, ains, aout, v, null);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @hide
     */
    protected void forEach(int slot, Allocation[] ains, Allocation aout,
                           FieldPacker v, LaunchOptions sc) {
        // TODO: Is this necessary if nScriptForEach calls validate as well?
        mRS.validate();
        if (ains != null) {
            for (Allocation ain : ains) {
                mRS.validateObject(ain);
            }
        }
        mRS.validateObject(aout);

        if (ains == null && aout == null) {
            throw new RSIllegalArgumentException(
                "At least one of ain or aout is required to be non-null.");
        }

        long[] in_ids;
        if (ains != null) {
            in_ids = new long[ains.length];
            for (int index = 0; index < ains.length; ++index) {
                in_ids[index] = ains[index].getID(mRS);
            }
        } else {
            in_ids = null;
        }

        long out_id = 0;
        if (aout != null) {
            out_id = aout.getID(mRS);
        }

        byte[] params = null;
        if (v != null) {
            params = v.getData();
        }

        int[] limits = null;
        if (sc != null) {
            limits = new int[6];

            limits[0] = sc.xstart;
            limits[1] = sc.xend;
            limits[2] = sc.ystart;
            limits[3] = sc.yend;
            limits[4] = sc.zstart;
            limits[5] = sc.zend;
        }

        mRS.nScriptForEach(getID(mRS), slot, in_ids, out_id, params, limits);
    }

    /**
     * Only intended for use by generated reflected code.  (General reduction)
     *
     * @hide
     */
    protected void reduce(int slot, Allocation[] ains, Allocation aout, LaunchOptions sc) {
        mRS.validate();
        if (ains == null || ains.length < 1) {
            throw new RSIllegalArgumentException(
                "At least one input is required.");
        }
        if (aout == null) {
            throw new RSIllegalArgumentException(
                "aout is required to be non-null.");
        }
        for (Allocation ain : ains) {
            mRS.validateObject(ain);
        }

        long[] in_ids = new long[ains.length];
        for (int index = 0; index < ains.length; ++index) {
            in_ids[index] = ains[index].getID(mRS);
        }
        long out_id = aout.getID(mRS);

        int[] limits = null;
        if (sc != null) {
            limits = new int[6];

            limits[0] = sc.xstart;
            limits[1] = sc.xend;
            limits[2] = sc.ystart;
            limits[3] = sc.yend;
            limits[4] = sc.zstart;
            limits[5] = sc.zend;
        }

        mRS.nScriptReduce(getID(mRS), slot, in_ids, out_id, limits);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param v
     */
    public void setVar(int index, float v) {
        mRS.nScriptSetVarF(getID(mRS), index, v, mUseIncSupp);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param v
     */
    public void setVar(int index, double v) {
        mRS.nScriptSetVarD(getID(mRS), index, v, mUseIncSupp);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param v
     */
    public void setVar(int index, int v) {
        mRS.nScriptSetVarI(getID(mRS), index, v, mUseIncSupp);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param v
     */
    public void setVar(int index, long v) {
        mRS.nScriptSetVarJ(getID(mRS), index, v, mUseIncSupp);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param v
     */
    public void setVar(int index, boolean v) {
        mRS.nScriptSetVarI(getID(mRS), index, v ? 1 : 0, mUseIncSupp);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param o
     */
    public void setVar(int index, BaseObj o) {
        if (mUseIncSupp) {
            long oInc = getDummyAlloc((Allocation)o);
            mRS.nScriptSetVarObj(getID(mRS), index, (o == null) ? 0 : oInc, mUseIncSupp);            
        } else {
            mRS.nScriptSetVarObj(getID(mRS), index, (o == null) ? 0 : o.getID(mRS), mUseIncSupp);
        }
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param v
     */
    public void setVar(int index, FieldPacker v) {
        mRS.nScriptSetVarV(getID(mRS), index, v.getData(), mUseIncSupp);
    }

    /**
     * Only intended for use by generated reflected code.
     *
     * @param index
     * @param v
     * @param e
     * @param dims
     */
    public void setVar(int index, FieldPacker v, Element e, int[] dims) {
        if (mUseIncSupp) {
            long dElement = e.getDummyElement(mRS);
            mRS.nScriptSetVarVE(getID(mRS), index, v.getData(), dElement, dims, mUseIncSupp);
        } else {
            mRS.nScriptSetVarVE(getID(mRS), index, v.getData(), e.getID(mRS), dims, mUseIncSupp);
        }
    }

    /**
     * Only intended for use by generated reflected code.
     *
     */
    public static class Builder {
        RenderScript mRS;

        Builder(RenderScript rs) {
            mRS = rs;
        }
    }


    /**
     * Only intended for use by generated reflected code.
     *
     */
    public static class FieldBase {
        protected Element mElement;
        protected Allocation mAllocation;

        protected void init(RenderScript rs, int dimx) {
            mAllocation = Allocation.createSized(rs, mElement, dimx, Allocation.USAGE_SCRIPT);
        }

        protected void init(RenderScript rs, int dimx, int usages) {
            mAllocation = Allocation.createSized(rs, mElement, dimx, Allocation.USAGE_SCRIPT | usages);
        }

        protected FieldBase() {
        }

        public Element getElement() {
            return mElement;
        }

        public Type getType() {
            return mAllocation.getType();
        }

        public Allocation getAllocation() {
            return mAllocation;
        }

        //@Override
        public void updateAllocation() {
        }
    }


    /**
     * Class for specifying the specifics about how a kernel will be
     * launched.
     *
     * This class can specify a potential range of cells on which to
     * run a kernel.  If no set is called for a dimension then this
     * class will have no impact on that dimension when the kernel
     * is executed.
     *
     * The forEach kernel launch will operate over the intersection of
     * the dimensions.
     *
     * Example:
     * LaunchOptions with setX(5, 15)
     * Allocation with dimension X=10, Y=10
     * The resulting forEach run would execute over:
     * x = 5 to 9 (inclusive) and
     * y = 0 to 9 (inclusive).
     *
     */
    public static final class LaunchOptions {
        private int xstart = 0;
        private int ystart = 0;
        private int xend = 0;
        private int yend = 0;
        private int zstart = 0;
        private int zend = 0;
        private int strategy;

        /**
         * Set the X range. xstartArg is the lowest coordinate of the range,
         * and xendArg-1 is the highest coordinate of the range.
         *
         * @param xstartArg Must be >= 0
         * @param xendArg Must be > xstartArg
         *
         * @return LaunchOptions
         */
        public LaunchOptions setX(int xstartArg, int xendArg) {
            if (xstartArg < 0 || xendArg <= xstartArg) {
                throw new RSIllegalArgumentException("Invalid dimensions");
            }
            xstart = xstartArg;
            xend = xendArg;
            return this;
        }

        /**
         * Set the Y range. ystartArg is the lowest coordinate of the range,
         * and yendArg-1 is the highest coordinate of the range.
         *
         * @param ystartArg Must be >= 0
         * @param yendArg Must be > ystartArg
         *
         * @return LaunchOptions
         */
        public LaunchOptions setY(int ystartArg, int yendArg) {
            if (ystartArg < 0 || yendArg <= ystartArg) {
                throw new RSIllegalArgumentException("Invalid dimensions");
            }
            ystart = ystartArg;
            yend = yendArg;
            return this;
        }

        /**
         * Set the Z range. zstartArg is the lowest coordinate of the range,
         * and zendArg-1 is the highest coordinate of the range.
         *
         * @param zstartArg Must be >= 0
         * @param zendArg Must be > zstartArg
         *
         * @return LaunchOptions
         */
        public LaunchOptions setZ(int zstartArg, int zendArg) {
            if (zstartArg < 0 || zendArg <= zstartArg) {
                throw new RSIllegalArgumentException("Invalid dimensions");
            }
            zstart = zstartArg;
            zend = zendArg;
            return this;
        }


        /**
         * Returns the current X start
         *
         * @return int current value
         */
        public int getXStart() {
            return xstart;
        }
        /**
         * Returns the current X end
         *
         * @return int current value
         */
        public int getXEnd() {
            return xend;
        }
        /**
         * Returns the current Y start
         *
         * @return int current value
         */
        public int getYStart() {
            return ystart;
        }
        /**
         * Returns the current Y end
         *
         * @return int current value
         */
        public int getYEnd() {
            return yend;
        }
        /**
         * Returns the current Z start
         *
         * @return int current value
         */
        public int getZStart() {
            return zstart;
        }
        /**
         * Returns the current Z end
         *
         * @return int current value
         */
        public int getZEnd() {
            return zend;
        }

    }
}
