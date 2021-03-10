/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.emoji2.benchmark.reflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class ReflectionImplementation extends ReflectionParent {
    @Override
    public ReflectionParent actualCall() {
        return super.actualCall();
    }

    ReflectionParent staticActualCall() throws Throwable {
        return ParentMethodHandles.staticCall(this);
    }

    MethodHandle doMethodLookup() throws NoSuchMethodException, IllegalAccessException {
        return ParentMethodHandles.doMethodLookup();
    }

    private static class ParentMethodHandles {
        private static final MethodHandle sMethodHandle;

        static {
            MethodHandle sMethodHandle1 = null;
            try {
                sMethodHandle1 = doMethodLookup();
            } catch (Throwable ignored) { }
            sMethodHandle = sMethodHandle1;
        }

        private static MethodHandle doMethodLookup() throws IllegalAccessException,
                NoSuchMethodException {
            MethodHandles.Lookup lookup = MethodHandles.lookup().in(ReflectionParent.class);
            Method method = ReflectionParent.class.getDeclaredMethod("actualCall");
            method.setAccessible(true);
            return lookup.unreflectSpecial(method, ReflectionParent.class);
        }

        public static ReflectionParent staticCall(ReflectionImplementation reflectionImplementation)
                throws Throwable {
            return (ReflectionParent) sMethodHandle.bindTo(reflectionImplementation)
                    .invokeWithArguments();
        }
    }

}
