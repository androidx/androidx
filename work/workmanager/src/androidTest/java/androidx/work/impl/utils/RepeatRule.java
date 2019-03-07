/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work.impl.utils;

import androidx.annotation.NonNull;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A {@link TestRule} which can be used to run the same test multiple times. Useful when trying
 * to debug flaky tests.
 *
 * To use this {@link TestRule} do the following. <br/><br/>
 *
 * Add the Rule to your JUnit test. <br/><br>
 * {@code
 *  @Rule
 *  RepeatRule mRepeatRule = new RepeatRule();
 * }
 * <br/><br>
 *
 * Add the {@link Repeat} annotation to your test case. <br/><br>
 * {@code
 *  @Test
 *  @Repeat(times=10)
 *  public void yourTestCase() {
 *
 *  }
 * }
 * <br/><br>
 */
public class RepeatRule implements TestRule {
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Repeat {
        int times();
    }

    public static class RepeatStatement extends Statement {
        private final int mTimes;
        private final Statement mStatement;

        public RepeatStatement(int times, @NonNull Statement statement) {
            mTimes = times;
            mStatement = statement;
        }

        @Override
        public void evaluate() throws Throwable {
            for (int i = 0; i < mTimes; i++) {
                mStatement.evaluate();
            }
        }
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Repeat repeat = description.getAnnotation(Repeat.class);
        if (repeat != null) {
            return new RepeatStatement(repeat.times(), base);
        } else {
            return base;
        }
    }
}
