/*
 * Copyright (C) 2011-2013 The Android Open Source Project
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

#ifndef __RS_CPU_RUNTIME_MATH_FUNCS_H__
#define __RS_CPU_RUNTIME_MATH_FUNCS_H__

#define F32_FN_F32(func)                            \
float __attribute__((overloadable)) func(float x) { \
    return func##f(x);                                \
}

F32_FN_F32(exp2)
F32_FN_F32(log)
F32_FN_F32(ceil)
F32_FN_F32(floor)
F32_FN_F32(fabs)
F32_FN_F32(atan)
F32_FN_F32(exp)

#define F32_FN_F32_F32(func)                                            \
float __attribute__((overloadable)) func(float x, float y)  {       \
    return func##f(x, y);                                             \
}

F32_FN_F32_F32(pow)

#undef F32_FN_F32
#undef F32_FN_F32_F32

#endif
