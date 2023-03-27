/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.core.widgets;

import androidx.constraintlayout.core.ArrayRow;
import androidx.constraintlayout.core.Cache;
import androidx.constraintlayout.core.SolverVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class BasicSolverVariableValues implements ArrayRow.ArrayRowVariables {

    private static float sEpsilon = 0.001f;

    class Item {
        SolverVariable mVariable;
        float mValue;
    }

    private final ArrayRow mRow; // our owner
    ArrayList<Item> mList = new ArrayList<>();
    //LinkedList<Item> list = new LinkedList<>();

    Comparator<Item> mComparator = new Comparator<Item>() {
        @Override
        public int compare(Item s1, Item s2) {
            return s1.mVariable.id - s2.mVariable.id;
        }
    };

    BasicSolverVariableValues(ArrayRow row, Cache cache) {
        mRow = row;
    }

    @Override
    public int getCurrentSize() {
        return mList.size();
    }

    @Override
    public SolverVariable getVariable(int i) {
        return mList.get(i).mVariable;
    }

    @Override
    public float getVariableValue(int i) {
        return mList.get(i).mValue;
    }

    @Override
    public boolean contains(SolverVariable variable) {
        for (Item item : mList) {
            if (item.mVariable.id == variable.id) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int indexOf(SolverVariable variable) {
        for (int i = 0; i < getCurrentSize(); i++) {
            Item item = mList.get(i);
            if (item.mVariable.id == variable.id) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public float get(SolverVariable variable) {
        if (contains(variable)) {
            return mList.get(indexOf(variable)).mValue;
        }
        return 0;
    }

    @Override
    public void display() {
        int count = getCurrentSize();
        System.out.print("{ ");
        for (int i = 0; i < count; i++) {
            SolverVariable v = getVariable(i);
            if (v == null) {
                continue;
            }
            System.out.print(v + " = " + getVariableValue(i) + " ");
        }
        System.out.println(" }");
    }

    @Override
    public void clear() {
        int count = getCurrentSize();
        for (int i = 0; i < count; i++) {
            SolverVariable v = getVariable(i);
            v.removeFromRow(mRow);
        }
        mList.clear();
    }

    @Override
    public void put(SolverVariable variable, float value) {
        if (value > -sEpsilon && value < sEpsilon) {
            remove(variable, true);
            return;
        }
//        System.out.println("Put " + variable + " [" + value + "] in " + mRow);
        //list.add(item);

        if (mList.size() == 0) {
            Item item = new Item();
            item.mVariable = variable;
            item.mValue = value;
            mList.add(item);
            variable.addToRow(mRow);
            variable.usageInRowCount++;
        } else {
            if (contains(variable)) {
                Item currentItem = mList.get(indexOf(variable));
                currentItem.mValue = value;
                return;
            } else {
                Item item = new Item();
                item.mVariable = variable;
                item.mValue = value;
                mList.add(item);
                variable.usageInRowCount++;
                variable.addToRow(mRow);
                Collections.sort(mList, mComparator);
            }
//            if (false) {
//                int previousItem = -1;
//                int n = 0;
//                for (Item currentItem : list) {
//                    if (currentItem.variable.id == variable.id) {
//                        currentItem.value = value;
//                        return;
//                    }
//                    if (currentItem.variable.id < variable.id) {
//                        previousItem = n;
//                    }
//                    n++;
//                }
//                Item item = new Item();
//                item.variable = variable;
//                item.value = value;
//                list.add(previousItem + 1, item);
//                variable.usageInRowCount++;
//                variable.addToRow(mRow);
//            }
        }
    }

    @Override
    public int sizeInBytes() {
        return 0;
    }

    @Override
    public float remove(SolverVariable v, boolean removeFromDefinition) {
        if (!contains(v)) {
            return 0;
        }
        int index = indexOf(v);
        float value = mList.get(indexOf(v)).mValue;
        mList.remove(index);
        v.usageInRowCount--;
        if (removeFromDefinition) {
            v.removeFromRow(mRow);
        }
        return value;
    }

    @Override
    public void add(SolverVariable v, float value, boolean removeFromDefinition) {
        if (value > -sEpsilon && value < sEpsilon) {
            return;
        }
        if (!contains(v)) {
            put(v, value);
        } else {
            Item item = mList.get(indexOf(v));
            item.mValue += value;
            if (item.mValue > -sEpsilon && item.mValue < sEpsilon) {
                item.mValue = 0;
                mList.remove(item);
                v.usageInRowCount--;
                if (removeFromDefinition) {
                    v.removeFromRow(mRow);
                }
            }
        }
    }

    @Override
    public float use(ArrayRow definition, boolean removeFromDefinition) {
        return 0;
    }

    @Override
    public void invert() {
        for (Item item : mList) {
            item.mValue *= -1;
        }
    }

    @Override
    public void divideByAmount(float amount) {
        for (Item item : mList) {
            item.mValue /= amount;
        }
    }

}
