/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.operations.utilities.ArrayAccess;
import androidx.compose.remote.core.operations.utilities.CollectionsAccess;
import androidx.compose.remote.core.operations.utilities.DataMap;
import androidx.compose.remote.core.operations.utilities.IntFloatMap;
import androidx.compose.remote.core.operations.utilities.IntIntMap;
import androidx.compose.remote.core.operations.utilities.IntMap;
import androidx.compose.remote.core.operations.utilities.NanMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Represents runtime state for a RemoteCompose document State includes things like the value of
 * variables
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComposeState implements CollectionsAccess {
    public static final int START_ID = 42;
    //    private static final int MAX_FLOATS = 500;
    private static int sMaxColors = 200;

    /** Offset added to bitmap to cache bitmap textures */
    public static final int BITMAP_TEXTURE_ID_OFFSET = 2000;

    private static final int MAX_DATA = 1000;
    private final IntMap<Object> mIntDataMap = new IntMap<>();
    private final IntMap<Boolean> mIntWrittenMap = new IntMap<>();
    private final HashMap<Object, Integer> mDataIntMap = new HashMap<>();
    private final IntFloatMap mFloatMap = new IntFloatMap(); // efficient cache
    private final IntIntMap mIntegerMap = new IntIntMap(); // efficient cache
    private final IntIntMap mColorMap = new IntIntMap(); // efficient cache
    private final IntMap<DataMap> mDataMapMap = new IntMap<>();
    private final IntMap<Object> mObjectMap = new IntMap<>();

    // path information
    private final IntMap<Object> mPathMap = new IntMap<>();
    private final IntMap<float[]> mPathData = new IntMap<>();
    private final IntIntMap mPathWinding = new IntIntMap();

    private boolean[] mColorOverride = new boolean[sMaxColors];
    @NonNull private final IntMap<ArrayAccess> mCollectionMap = new IntMap<>();

    private final boolean[] mDataOverride = new boolean[MAX_DATA];
    private final boolean[] mIntegerOverride = new boolean[MAX_DATA];
    private final boolean[] mFloatOverride = new boolean[MAX_DATA];

    private int mNextId = START_ID;
    private final int @NonNull [] mIdMaps =
            new int[] {START_ID, NanMap.START_VAR, NanMap.START_ARRAY};
    @Nullable private RemoteContext mRemoteContext = null;

    /**
     * Get Object based on id. The system will cache things like bitmaps Paths etc. They can be
     * accessed with this command
     *
     * @param id the id of the object
     * @return the object
     */
    @Nullable
    public Object getFromId(int id) {
        return mIntDataMap.get(id);
    }

    /**
     * true if the cache contain this id
     *
     * @param id the id of the object
     * @return true if the cache contain this id
     */
    public boolean containsId(int id) {
        return mIntDataMap.get(id) != null;
    }

    /** Return the id of an item from the cache. */
    public int dataGetId(@NonNull Object data) {
        Integer res = mDataIntMap.get(data);
        if (res == null) {
            return -1;
        }
        return res;
    }

    /**
     * Add an item to the cache. Generates an id for the item and adds it to the cache based on that
     * id.
     */
    public int cacheData(@NonNull Object item) {
        int id = createNextAvailableId();
        mDataIntMap.put(item, id);
        mIntDataMap.put(id, item);
        return id;
    }

    /**
     * Add an item to the cache. Generates an id for the item and adds it to the cache based on that
     * id.
     */
    public int cacheData(@NonNull Object item, int type) {
        int id = createNextAvailableId(type);
        mDataIntMap.put(item, id);
        mIntDataMap.put(id, item);
        return id;
    }

    /** Insert an item in the cache */
    public void cacheData(int id, @NonNull Object item) {
        mDataIntMap.put(item, id);
        mIntDataMap.put(id, item);
    }

    /** Insert an item in the cache */
    public void updateData(int id, @NonNull Object item) {
        if (!mDataOverride[id]) {
            Object previous = mIntDataMap.get(id);
            if (previous != item) {
                mDataIntMap.remove(previous);
                mDataIntMap.put(item, id);
                mIntDataMap.put(id, item);
                updateListeners(id);
            }
        }
    }

    /**
     * Get the path associated with the Data
     *
     * @param id of path
     * @return path object
     */
    public @Nullable Object getPath(int id) {
        return mPathMap.get(id);
    }

    /**
     * Cache a path object. Object will be cleared if you update path data.
     *
     * @param id number associated with path
     * @param path the path object typically Android Path
     */
    public void putPath(int id, @NonNull Object path) {
        mPathMap.put(id, path);
    }

    /**
     * The path data the Array of floats that is asoicated with the path It also removes the current
     * path object.
     *
     * @param id the integer asociated with the data and path
     * @param data the array of floats that represents the path
     */
    public void putPathData(int id, float @NonNull [] data) {
        mPathData.put(id, data);
        mPathMap.remove(id);
    }

    /**
     * Get the path data associated with the id
     *
     * @param id number that represents the path
     * @return path data
     */
    public float @Nullable [] getPathData(int id) {
        return mPathData.get(id);
    }

    /**
     * Get the winding associated with the path id
     * @param id the id of the path
     * @return the winding
     */
    public int getPathWinding(int id) {
        return mPathWinding.get(id);
    }

    /**
     * Set the winding associated with the path id
     * @param id the id of the path
     * @param winding the winding
     */
    public void putPathWinding(int id, int winding) {
        mPathWinding.put(id, winding);
    }

    /**
     * Adds a data Override.
     *
     * @param id the id of the data
     * @param item the new value
     */
    public void overrideData(int id, @NonNull Object item) {
        Object previous = mIntDataMap.get(id);
        if (previous != item) {
            mDataIntMap.remove(previous);
            mDataIntMap.put(item, id);
            mIntDataMap.put(id, item);
            mDataOverride[id] = true;
            updateListeners(id);
        }
    }

    /** Insert an item in the cache */
    public int cacheFloat(float item) {
        int id = createNextAvailableId();
        mFloatMap.put(id, item);
        mIntegerMap.put(id, (int) item);
        return id;
    }

    /** Insert an item in the cache */
    public void cacheFloat(int id, float item) {
        mFloatMap.put(id, item);
    }

    /** Insert an float item in the cache */
    public void updateFloat(int id, float value) {
        if (!mFloatOverride[id]) {
            float previous = mFloatMap.get(id);
            if (previous != value) {
                mFloatMap.put(id, value);
                mIntegerMap.put(id, (int) value);
                updateListeners(id);
            }
        }
    }

    /**
     * Adds a float Override.
     *
     * @param id The id of the float
     * @param value the override value
     */
    public void overrideFloat(int id, float value) {
        float previous = mFloatMap.get(id);
        if (previous != value) {
            mFloatMap.put(id, value);
            mIntegerMap.put(id, (int) value);
            mFloatOverride[id] = true;
            updateListeners(id);
        }
    }

    /**
     * Insert an item in the cache
     *
     * @param item integer item to cache
     * @return the id of the integer
     */
    public int cacheInteger(int item) {
        int id = createNextAvailableId();
        mIntegerMap.put(id, item);
        mFloatMap.put(id, item);
        return id;
    }

    /**
     * Insert an integer item in the cache
     *
     * @param id the id of the integer
     * @param value the value of the integer
     */
    public void updateInteger(int id, int value) {
        if (!mIntegerOverride[id]) {
            int previous = mIntegerMap.get(id);
            if (previous != value) {
                mFloatMap.put(id, value);
                mIntegerMap.put(id, value);
                updateListeners(id);
            }
        }
    }

    /**
     * Adds a integer Override.
     *
     * @param id value id
     * @param value the new value
     */
    public void overrideInteger(int id, int value) {
        int previous = mIntegerMap.get(id);
        if (previous != value) {
            mIntegerMap.put(id, value);
            mFloatMap.put(id, value);
            mIntegerOverride[id] = true;
            updateListeners(id);
        }
    }

    /**
     * get a float from the float cache
     *
     * @param id of the float value
     * @return the float value
     */
    public float getFloat(int id) {
        return mFloatMap.get(id);
    }

    /**
     * get an integer from the cache
     *
     * @param id of the integer value
     * @return the integer
     */
    public int getInteger(int id) {
        return mIntegerMap.get(id);
    }

    /**
     * Get the color from the cache
     *
     * @param id The id of the color
     * @return The color
     */
    public int getColor(int id) {
        return mColorMap.get(id);
    }

    /**
     * Modify the color at id.
     *
     * @param id value id
     * @param color color (as an int)
     */
    public void updateColor(int id, int color) {
        if (id < sMaxColors && mColorOverride[id]) {
            return;
        }
        mColorMap.put(id, color);
        updateListeners(id);
    }

    private void updateListeners(int id) {
        ArrayList<VariableSupport> v = mVarListeners.get(id);
        if (v != null && mRemoteContext != null) {
            for (int i = 0; i < v.size(); i++) {
                VariableSupport c = v.get(i);
                c.markDirty();
            }
        }
    }

    /**
     * Adds a colorOverride. This is a list of ids and their colors optimized for playback;
     *
     * @param id value id
     * @param color color (as an int)
     */
    public void overrideColor(int id, int color) {
        if (id >= sMaxColors) {
            sMaxColors *= 2;
            mColorOverride = Arrays.copyOf(mColorOverride, sMaxColors);
        }
        mColorOverride[id] = true;
        mColorMap.put(id, color);
        updateListeners(id);
    }

    /** Clear the color Overrides */
    public void clearColorOverride() {
        Arrays.fill(mColorOverride, false);
    }

    /**
     * Clear the data override
     *
     * @param id the data id to clear
     */
    public void clearDataOverride(int id) {
        mDataOverride[id] = false;
        updateListeners(id);
    }

    /**
     * Clear the integer override
     *
     * @param id the integer id to clear
     */
    public void clearIntegerOverride(int id) {
        mIntegerOverride[id] = false;
        updateListeners(id);
    }

    /**
     * Clear the float override
     *
     * @param id the float id to clear
     */
    public void clearFloatOverride(int id) {
        mFloatOverride[id] = false;
        updateListeners(id);
    }

    /**
     * Method to determine if a cached value has been written to the documents WireBuffer based on
     * its id.
     *
     * @param id id to check
     * @return true if the value has not been written to the WireBuffer
     */
    public boolean wasNotWritten(int id) {
        return !mIntWrittenMap.get(id);
    }

    /** Method to mark that a value, represented by its id, has been written to the WireBuffer */
    public void markWritten(int id) {
        mIntWrittenMap.put(id, true);
    }

    /** Clear the record of the values that have been written to the WireBuffer. */
    public void reset() {
        mIntWrittenMap.clear();
        mDataIntMap.clear();
    }

    /**
     * Get the next available id
     *
     * @return next available id
     */
    public int createNextAvailableId() {
        return mNextId++;
    }

    /**
     * Get the next available id 0 is normal (float,int,String,color) 1 is VARIABLES 2 is
     * collections
     *
     * @return return a unique id in the set
     */
    public int createNextAvailableId(int type) {
        if (0 == type) {
            return mNextId++;
        }
        return mIdMaps[type]++;
    }

    /**
     * Set the next id
     *
     * @param id set the id to increment off of
     */
    public void setNextId(int id) {
        mNextId = id;
    }

    @NonNull IntMap<ArrayList<VariableSupport>> mVarListeners = new IntMap<>();
    @NonNull ArrayList<VariableSupport> mAllVarListeners = new ArrayList<>();

    private void add(int id, @NonNull VariableSupport variableSupport) {
        ArrayList<VariableSupport> v = mVarListeners.get(id);
        if (v == null) {
            v = new ArrayList<VariableSupport>();
            mVarListeners.put(id, v);
        }
        v.add(variableSupport);
        mAllVarListeners.add(variableSupport);
    }

    /**
     * Commands that listen to variables add themselves.
     *
     * @param id id of variable to listen to
     * @param variableSupport command that listens to variable
     */
    public void listenToVar(int id, @NonNull VariableSupport variableSupport) {
        add(id, variableSupport);
    }

    /**
     * get Commands that listen to variables.
     *
     * @param id id of variable to listen to
     * @return Commands that listen to variable
     */
    public @Nullable ArrayList<VariableSupport> getListeners(int id) {
        return mVarListeners.get(id);
    }

    /**
     * Is any command listening to this variable
     *
     * @param id The Variable id
     * @return true if any command is listening to this variable
     */
    public boolean hasListener(int id) {
        return mVarListeners.get(id) != null;
    }

    float mLastRepaint = Float.NaN;

    /**
     * List of Commands that need to be updated
     *
     * @param context The context
     * @param currentTime The current time
     * @return The number of ops to update
     */
    public int getOpsToUpdate(@NonNull RemoteContext context, long currentTime) {
        if (mVarListeners.get(RemoteContext.ID_CONTINUOUS_SEC) != null) {
            return 1;
        }
        int repaintMs = Integer.MAX_VALUE;
        if (!Float.isNaN(mRepaintSeconds)) {
            repaintMs = (int) (mRepaintSeconds * 1000);
            mLastRepaint = mRepaintSeconds;
        }
        if (mVarListeners.get(RemoteContext.ID_TIME_IN_SEC) != null) {
            int sub = (int) (currentTime % 1000);
            return Math.min(repaintMs, 2 + 1000 - sub);
        }
        if (mVarListeners.get(RemoteContext.ID_TIME_IN_MIN) != null) {
            int sub = (int) (currentTime % 60000);
            return Math.min(repaintMs, 2 + 1000 * 60 - sub);
        }

        return -1;
    }

    float mRepaintSeconds = Float.NaN;

    /**
     * Set the amount of time to repaint the document
     *
     * @param seconds the delay in seconds to the next render loop pass
     */
    public void wakeIn(float seconds) {
        if (Float.isNaN(seconds) || Float.isNaN(mLastRepaint) || mRepaintSeconds > seconds) {
            mRepaintSeconds = seconds;
        }
    }

    /**
     * Set the width of the overall document on screen.
     *
     * @param width the width of the document in pixels
     */
    public void setWindowWidth(float width) {
        updateFloat(RemoteContext.ID_WINDOW_WIDTH, width);
    }

    /**
     * Set the width of the overall document on screen.
     *
     * @param height the height of the document in pixels
     */
    public void setWindowHeight(float height) {
        updateFloat(RemoteContext.ID_WINDOW_HEIGHT, height);
    }

    /**
     * Add an array access
     *
     * @param id The id of the array Access
     * @param collection The array access
     */
    public void addCollection(int id, @NonNull ArrayAccess collection) {
        mCollectionMap.put(id & 0xFFFFF, collection);
    }

    @Override
    public float getFloatValue(int id, int index) {
        ArrayAccess array = mCollectionMap.get(id & 0xFFFFF);
        if (array != null) {
            return array.getFloatValue(index);
        }
        return 0f;
    }

    @Override
    public float @Nullable [] getFloats(int id) {
        ArrayAccess array = mCollectionMap.get(id & 0xFFFFF);
        if (array != null) {
            return array.getFloats();
        }
        return null;
    }

    @Override
    public int getId(int listId, int index) {
        ArrayAccess array = mCollectionMap.get(listId & 0xFFFFF);
        if (array != null) {
            return array.getId(index);
        }
        return -1;
    }

    /**
     * adds a DataMap to the cache
     *
     * @param id The id of the data map
     * @param map The data map
     */
    public void putDataMap(int id, @NonNull DataMap map) {
        mDataMapMap.put(id, map);
    }

    /**
     * Get the DataMap asociated with the id
     *
     * @param id the id of the DataMap
     * @return the DataMap
     */
    public @Nullable DataMap getDataMap(int id) {
        return mDataMapMap.get(id);
    }

    @Override
    public int getListLength(int id) {
        ArrayAccess array = mCollectionMap.get(id & 0xFFFFF);
        if (array != null) {
            return array.getLength();
        }
        return 0;
    }

    /**
     * sets the RemoteContext
     *
     * @param context the context
     */
    public void setContext(@NonNull RemoteContext context) {
        mRemoteContext = context;
        mRemoteContext.clearLastOpCount();
    }

    /**
     * Add an object to the cache. Uses the id for the item and adds it to the cache based
     *
     * @param id the id of the object
     * @param value the object
     */
    public void updateObject(int id, @NonNull Object value) {
        mObjectMap.put(id, value);
    }

    /**
     * Get an object from the cache
     *
     * @param id The id of the object
     * @return The object
     */
    public @Nullable Object getObject(int id) {
        return mObjectMap.get(id);
    }
}
