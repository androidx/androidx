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
 * BaseObj is the base class for all RenderScript objects owned by a RS context.
 * It is responsible for lifetime management and resource tracking. This class
 * should not be used by a user application.
 *
 **/
public class BaseObj {
    BaseObj(int id, RenderScript rs) {
        rs.validate();
        mRS = rs;
        mID = id;
        mDestroyed = false;
    }

    void setID(int id) {
        if (mID != 0) {
            throw new RSRuntimeException("Internal Error, reset of object ID.");
        }
        mID = id;
    }

    /**
     * Lookup the native object ID for this object.  Primarily used by the
     * generated reflected code.
     *
     * @param rs Context to verify against internal context for
     *           match.
     *
     * @return int
     */
    int getID(RenderScript rs) {
        mRS.validate();
        if (rs.isNative) {
            RenderScriptThunker rst = (RenderScriptThunker)rs;
            if (getNObj() != null) {
                return getNObj().hashCode();
            }
        }
        if (mDestroyed) {
            throw new RSInvalidStateException("using a destroyed object.");
        }
        if (mID == 0) {
            throw new RSRuntimeException("Internal error: Object id 0.");
        }
        if ((rs != null) && (rs != mRS)) {
            throw new RSInvalidStateException("using object with mismatched context.");
        }
        return mID;
    }

    android.renderscript.BaseObj getNObj() {
        return null;
    }

    void checkValid() {
        if ((mID == 0) && (getNObj() == null)) {
            throw new RSIllegalArgumentException("Invalid object.");
        }
    }

    private int mID;
    private boolean mDestroyed;
    RenderScript mRS;

    protected void finalize() throws Throwable {
        if (!mDestroyed) {
            if(mID != 0 && mRS.isAlive()) {
                mRS.nObjDestroy(mID);
            }
            mRS = null;
            mID = 0;
            mDestroyed = true;
            //Log.v(RenderScript.LOG_TAG, getClass() +
            // " auto finalizing object without having released the RS reference.");
        }
        super.finalize();
    }

    /**
     * Frees any native resources associated with this object.  The
     * primary use is to force immediate cleanup of resources when it is
     * believed the GC will not respond quickly enough.
     */
    synchronized public void destroy() {
        if(mDestroyed) {
            throw new RSInvalidStateException("Object already destroyed.");
        }
        mDestroyed = true;
        mRS.nObjDestroy(mID);
    }

    /**
     * Calculates the hash code value for a BaseObj.
     *
     * @return int
     */
    @Override
    public int hashCode() {
        return mID;
    }

    /**
     * Compare the current BaseObj with another BaseObj for equality.
     *
     * @param obj The object to check equality with.
     *
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        // Early-out check to see if both BaseObjs are actually the same
        if (this == obj)
            return true;

        if (getClass() != obj.getClass()) {
            return false;
        }

        BaseObj b = (BaseObj) obj;
        return mID == b.mID;
    }
}

