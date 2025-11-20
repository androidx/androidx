/*
 * Copyright 2025 The Android Open Source Project
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

package androidx

import sample.annotation.provider.ExperimentalSampleAnnotation

annotation class MyAnnotation

class KtClassWithAnnotatedCompanionA {

    @ExperimentalSampleAnnotation
    companion object {

        @MyAnnotation const val A: Int = 1

        @ExperimentalSampleAnnotation const val B: Int = 2

        const val C: Int = 3

        @MyAnnotation fun myFun() {}

        @ExperimentalSampleAnnotation fun myFun2() {}

        fun myFun3() {}
    }
}

@ExperimentalSampleAnnotation
class KtClassWithAnnotatedCompanionB {

    companion object {
        @MyAnnotation const val A: Int = 1

        @ExperimentalSampleAnnotation const val B: Int = 2

        @MyAnnotation fun myFun() {}

        @ExperimentalSampleAnnotation fun myFun2() {}
    }
}

@ExperimentalSampleAnnotation
class KtClassWithAnnotatedCompanionC {

    @ExperimentalSampleAnnotation
    companion object {
        @MyAnnotation const val A: Int = 1

        @ExperimentalSampleAnnotation const val B: Int = 2

        @MyAnnotation fun myFun() {}

        @ExperimentalSampleAnnotation fun myFun2() {}
    }
}

@ExperimentalSampleAnnotation
class KtClassWithAnnotatedCompanionD {

    class MyInnerClass {
        @ExperimentalSampleAnnotation
        companion object {
            @MyAnnotation const val A: Int = 1

            @ExperimentalSampleAnnotation const val B: Int = 2

            @MyAnnotation fun myFun() {}

            @ExperimentalSampleAnnotation fun myFun2() {}
        }
    }
}

class KtClassWithAnnotatedCompanionE {

    class MyInnerClass {
        @ExperimentalSampleAnnotation
        companion object {
            val A: Int = 1

            @ExperimentalSampleAnnotation val B: Int = 2

            var C: Int = 1

            @ExperimentalSampleAnnotation var D: Int = 2
        }
    }
}

@ExperimentalSampleAnnotation
class KtClassWithAnnotatedCompanionF {

    class MyInnerClass {
        @ExperimentalSampleAnnotation
        companion object {
            val A: Int = 1

            @ExperimentalSampleAnnotation val B: Int = 2

            var C: Int = 1

            @ExperimentalSampleAnnotation var D: Int = 2
        }
    }
}
