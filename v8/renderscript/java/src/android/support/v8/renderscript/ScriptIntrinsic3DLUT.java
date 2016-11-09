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

import android.util.Log;

/**
 *
 * Intrinsic for converting RGB to RGBA by using a 3D lookup table.  The
 * incoming r,g,b values are use as normalized x,y,z coordinates into a 3D
 * allocation.  The 8 nearest values are sampled and linearly interpolated.  The
 * result is placed in the output.
 *
 **/
public class ScriptIntrinsic3DLUT extends ScriptIntrinsic {
    private Allocation mLUT;
    private Element mElement;
    // API level for the intrinsic
    private static final int INTRINSIC_API_LEVEL = 19;

    protected ScriptIntrinsic3DLUT(long id, RenderScript rs, Element e) {
        super(id, rs);
        mElement = e;
    }

    /**
     * Supported elements types are {@link Element#U8_4}
     *
     * The defaults tables are identity.
     *
     * @param rs The RenderScript context
     * @param e Element type for intputs and outputs
     *
     * @return ScriptIntrinsic3DLUT
     */
    public static ScriptIntrinsic3DLUT create(RenderScript rs, Element e) {
        if (!e.isCompatible(Element.U8_4(rs))) {
            throw new RSIllegalArgumentException("Element must be compatible with uchar4.");
        }
        long id;
        boolean mUseIncSupp = rs.isUseNative() &&
                              android.os.Build.VERSION.SDK_INT < INTRINSIC_API_LEVEL;

        id = rs.nScriptIntrinsicCreate(8, e.getID(rs), mUseIncSupp);

        ScriptIntrinsic3DLUT si = new ScriptIntrinsic3DLUT(id, rs, e);
        si.setIncSupp(mUseIncSupp);
        return si;
    }

    /**
     * Sets the {@link android.support.v8.renderscript.Allocation} to be used as
     * the lookup table.
     *
     * The lookup table must use the same
     * {@link android.support.v8.renderscript.Element} as the intrinsic.
     *
     */

    public void setLUT(Allocation lut) {
        final Type t = lut.getType();

        if (t.getZ() == 0) {
            throw new RSIllegalArgumentException("LUT must be 3d.");
        }

        if (!t.getElement().isCompatible(mElement)) {
            throw new RSIllegalArgumentException("LUT element type must match.");
        }

        mLUT = lut;
        setVar(0, mLUT);
    }


    /**
     * Invoke the kernel and apply the lookup to each cell of ain
     * and copy to aout.
     *
     * @param ain Input allocation
     * @param aout Output allocation
     */
    public void forEach(Allocation ain, Allocation aout) {
        forEach(0, ain, aout, null);
    }

    /**
     * Get a KernelID for this intrinsic kernel.
     *
     * @return Script.KernelID The KernelID object.
     */
    public Script.KernelID getKernelID() {
        return createKernelID(0, 3, null, null);
    }
}

