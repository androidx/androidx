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

package androidx.car.app.sample.navigation.common.model;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.List;

/** Represents an instruction sequence and parameters for a executing a script. */
public class Script {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<Instruction> mInstructions;
    private final Processor mProcessor;
    private int mCurrentInstruction;

    /** An interface for a block of code that processes an instruction. */
    public interface Processor {
        /** A block of code that processes an instruction. */
        void process(@NonNull Instruction instruction);
    }

    /** Executes the given list of instructions. */
    @NonNull
    public static Script execute(@NonNull List<Instruction> instructions,
            @NonNull Processor processor) {
        return new Script(instructions, processor);
    }

    /** Stops executing the instructions. */
    public void stop() {
        mHandler.removeCallbacksAndMessages(null);
        mCurrentInstruction = mInstructions.size();
    }

    private Script(@NonNull List<Instruction> instructions, @NonNull Processor processor) {
        mInstructions = instructions;
        mProcessor = processor;
        mCurrentInstruction = 0;
        // Execute the first instruction right away to start navigation and avoid flicker.
        nextInstruction();
    }

    private void nextInstruction() {
        if (mCurrentInstruction >= mInstructions.size()) {
            // Script is finished.
            return;
        }
        Instruction instruction = mInstructions.get(mCurrentInstruction);
        mProcessor.process(instruction);
        mCurrentInstruction++;
        mHandler.postDelayed(this::nextInstruction, instruction.getDurationMillis());
    }
}
