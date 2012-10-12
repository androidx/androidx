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



#include <machine/cpu-features.h>
#include <machine/asm.h>

/*
        r0 = dst
        r1 = y0 base pointer
        r2 = y1 base pointer
        r3 = y2 base pointer
        sp = coeffs
        sp = length / 2
*/

ENTRY(rsdIntrinsicConvolve3x3_K)
        push            {r4-r8, r10, r11, lr}
        vpush           {q4-q7}

        /* Get the coeffs pointer from the stack and load the
           coefficients in the q0, q1 NEON registers */
        ldr r4, [sp, #32+64]
        vld1.16 {q0, q1}, [r4]

        /* Get count from the stack */
        ldr r4, [sp, #36+64]

        /* Load the frequently used immediate in a register */
        mov r5, #8

1:
        /* Load and post-increase the address by r5=#8 */
        vld1.8 {q13}, [r1], r5
        vld1.8 {q14}, [r2], r5
        vld1.8 {q15}, [r3], r5

        /* Signal memory for data that will be used in the loop after the next */
        PLD         (r1, r5)
        PLD         (r2, r5)
        PLD         (r3, r5)

        vmovl.u8 q2, d26
        vmovl.u8 q3, d27
        vmovl.u8 q4, d28
        vmovl.u8 q5, d29
        vmovl.u8 q6, d30
        vmovl.u8 q7, d31

/*
        The two pixel source array is
        d4,  d5,  d6,  d7
        d8,  d9,  d10, d11
        d12, d13, d14, d15
*/

        vmull.s16 q8, d4, d0[0]
        vmlal.s16 q8, d5, d0[1]
        vmlal.s16 q8, d6, d0[2]
        vmlal.s16 q8, d8, d0[3]
        vmlal.s16 q8, d9, d1[0]
        vmlal.s16 q8, d10, d1[1]
        vmlal.s16 q8, d12, d1[2]
        vmlal.s16 q8, d13, d1[3]
        vmlal.s16 q8, d14, d2[0]

        vmull.s16 q9, d5, d0[0]
        vmlal.s16 q9, d6, d0[1]
        vmlal.s16 q9, d7, d0[2]
        vmlal.s16 q9, d9, d0[3]
        vmlal.s16 q9, d10, d1[0]
        vmlal.s16 q9, d11, d1[1]
        vmlal.s16 q9, d13, d1[2]
        vmlal.s16 q9, d14, d1[3]
        vmlal.s16 q9, d15, d2[0]

        vshrn.i32 d16, q8, #8
        vshrn.i32 d17, q9, #8

        vqmovun.s16 d16, q8
        vst1.8 d16, [r0]!

        /* Are we done yet? */
        subs r4, r4, #1
        bne 1b

        /* We're done, bye! */
        vpop            {q4-q7}
        pop             {r4-r8, r10, r11, lr}
        bx              lr
END(TestConvolveK)

/*
        r0 = dst
        r1 = src
        r2 = matrix
        r3 = length
*/
ENTRY(rsdIntrinsicColorMatrix4x4_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        vld1.16 {q2}, [r2]!
        vld1.16 {q3}, [r2]!

1:
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!

        vmovl.u8 q12, d0  /* R */
        vmovl.u8 q13, d1  /* G */
        vmovl.u8 q14, d2  /* B */
        vmovl.u8 q15, d3  /* A */

        vmull.s16 q8,  d24, d4[0]
        vmull.s16 q9,  d24, d4[1]
        vmull.s16 q10, d24, d4[2]
        vmull.s16 q11, d24, d4[3]

        vmlal.s16 q8,  d26, d5[0]
        vmlal.s16 q9,  d26, d5[1]
        vmlal.s16 q10, d26, d5[2]
        vmlal.s16 q11, d26, d5[3]

        vmlal.s16 q8,  d28, d6[0]
        vmlal.s16 q9,  d28, d6[1]
        vmlal.s16 q10, d28, d6[2]
        vmlal.s16 q11, d28, d6[3]

        vmlal.s16 q8,  d30, d7[0]
        vmlal.s16 q9,  d30, d7[1]
        vmlal.s16 q10, d30, d7[2]
        vmlal.s16 q11, d30, d7[3]

        vshrn.i32 d24, q8, #8
        vshrn.i32 d26, q9, #8
        vshrn.i32 d28, q10, #8
        vshrn.i32 d30, q11, #8

        vqmovun.s16 d0, q12
        vqmovun.s16 d1, q13
        vqmovun.s16 d2, q14
        vqmovun.s16 d3, q15

        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!

        subs r3, r3, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicColorMatrix4x4_K)

/*
        r0 = dst
        r1 = src
        r2 = matrix
        r3 = length
*/
ENTRY(rsdIntrinsicColorMatrix3x3_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        vld1.16 {q2}, [r2]!
        vld1.16 {q3}, [r2]!

1:
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!

        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2

        vmull.s16 q8,  d24, d4[0]
        vmull.s16 q9,  d24, d4[1]
        vmull.s16 q10, d24, d4[2]

        vmlal.s16 q8,  d26, d5[0]
        vmlal.s16 q9,  d26, d5[1]
        vmlal.s16 q10, d26, d5[2]

        vmlal.s16 q8,  d28, d6[0]
        vmlal.s16 q9,  d28, d6[1]
        vmlal.s16 q10, d28, d6[2]

        vshrn.i32 d24, q8, #8
        vshrn.i32 d26, q9, #8
        vshrn.i32 d28, q10, #8

        vqmovun.s16 d0, q12
        vqmovun.s16 d1, q13
        vqmovun.s16 d2, q14

        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!

        subs r3, r3, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicColorMatrix3x3_K)

/*
        r0 = dst
        r1 = src
        r2 = matrix
        r3 = length
*/
ENTRY(rsdIntrinsicColorMatrixDot_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        vld1.16 {q2}, [r2]!
        vld1.16 {q3}, [r2]!

1:
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!

        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2

        vmull.s16 q8,  d24, d4[0]
        vmlal.s16 q8,  d26, d5[0]
        vmlal.s16 q8,  d28, d6[0]
        vshrn.i32 d24, q8, #8
        vqmovun.s16 d0, q12
        vmov.u8 d1, d0
        vmov.u8 d2, d0

        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!

        subs r3, r3, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicColorMatrixDot_K)


/*
static void OneVF(float4 *out, const uchar *ptrIn, int iStride,
                  const float* gPtr, int iradius, int x1, int x2)

    r0 = out
    r1 = pin
    r2 = stride
    r3 = gptr
    r4 = sp, ct
    r5 = sp+4, x1
    r6 = sp+8, x2
*/
ENTRY(rsdIntrinsicBlurVF_K)
        push            {r4-r8, r10, r11, lr}
        vpush           {q4-q7}

        ldr r4, [sp, #32+64]
        ldr r5, [sp, #32+64 + 4]
        ldr r6, [sp, #32+64 + 8]

1:
        veor q10, q10, q10         /* float4 blurredPixel = 0; */
        veor q11, q11, q11         /* float4 blurredPixel = 0; */
        add r7, r1, r5, lsl #2  /* const uchar *pi = ptrIn + x1 * 4; */
        mov r10, r3

        mov r11, r4

2:
        vld1.32 {d2}, [r7]
        vmovl.u8 q1, d2
        vmovl.u16 q3, d2
        vmovl.u16 q4, d3
        vcvt.f32.s32 q3, q3
        vcvt.f32.s32 q4, q4
        vld1.32 {d0[0]}, [r10]!
        add r7, r7, r2
        vmla.f32 q10, q3, d0[0]
        vmla.f32 q11, q4, d0[0]
        subs r11, r11, #1
        bne 2b

        vst1.32 {q10}, [r0]!
        vst1.32 {q11}, [r0]!
        add r5, r5, #2
        cmp r5, r6
        bne 1b


        vpop            {q4-q7}
        pop             {r4-r8, r10, r11, lr}
        bx              lr
END(rsdIntrinsicBlurVF_K)

/*
static void OneVF(float4 *out, const uchar *ptrIn, int iStride,
                  const float* gPtr, int iradius, int x1, int x2)

    r0 = out
    r1 = pin
    r2 = gptr
    r3 = ct
    r4 = sp, x1
    r5 = sp+4, x2
*/
ENTRY(rsdIntrinsicBlurHF_K)
        push            {r4-r8, r10, r11, lr}
        vpush           {q4-q7}

        ldr r4, [sp, #32+64]
        ldr r5, [sp, #32+64 + 4]

1:
        add r7, r1, r4, lsl #4  /* const uchar *pi = ptrIn + x1 * 4; */
        mov r10, r2
        mov r11, r3

        vld1.32 {q1}, [r7]!
        vld1.32 {d6[0]}, [r10]!
        vmul.f32 q0, q1, d6[0]
        sub r11, r11, #1

2:
        vld1.32 {q1}, [r7]!
        vld1.32 {q2}, [r7]!
        vld1.32 {d6[0]}, [r10]!
        vld1.32 {d6[1]}, [r10]!
        vmla.f32 q0, q1, d6[0]
        vmla.f32 q0, q2, d6[1]
        subs r11, r11, #2
        bne 2b

        vcvt.s32.f32 q0, q0
        vmovn.u32 d0, q0
        vmovn.u16 d0, q0

        vst1.32 {d0[0]}, [r0]!
        add r4, r4, #1
        cmp r4, r5
        bne 1b

        vpop            {q4-q7}
        pop             {r4-r8, r10, r11, lr}
        bx              lr
END(rsdIntrinsicBlurHF_K)

/*
        r0 = dst
        r1 = Y
        r2 = VU
        r3 = length (pixels / 8)
        r4 = sp, params

        This function converts 8 pixels per iteration
*/
ENTRY(rsdIntrinsicYuv_K)
        push            {r4-r8, r10, r11, lr}
        vpush           {q4-q7}

        ldr r4, [sp, #32+64]
        vld1.16 {q2}, [r4]!  // mults
        vld1.16 {q3}, [r4]!  // y offset
        vld1.16 {q4}, [r4]!  // 128
        vdup.8 d3, d5[1]

1:
        vld1.8 {d10}, [r1]!
        vld1.8 {d12}, [r2]!
        vmovl.u8 q5, d10 // Y at .16
        vmovl.u8 q6, d12 // vu at .16

        vsub.i16 q5, q5, q3
        vsub.i16 q6, q6, q4
        vtrn.16 d12, d13  // d12 = u, d13 = v
        vmov q7, q6
        vtrn.16 d12, d14
        vtrn.32 d12, d14
        vtrn.16 d13, d15
        vtrn.32 d13, d15

        vmull.s16 q8, d10, d4[0]
        vmull.s16 q11, d11, d4[0]
        vmov q9, q8
        vmov q10, q8
        vmov q12, q11
        vmov q13, q11

        vmlal.s16 q8,  d12, d4[1]
        vmlal.s16 q9,  d12, d5[0]
        vmlal.s16 q10, d13, d4[3]
        vmlal.s16 q9,  d13, d4[2]

        vmlal.s16 q11, d14, d4[1]
        vmlal.s16 q12, d14, d5[0]
        vmlal.s16 q13, d15, d4[3]
        vmlal.s16 q12, d15, d4[2]


        vshrn.i32 d16, q8, #8
        vshrn.i32 d18, q9, #8
        vshrn.i32 d20, q10, #8
        vqmovun.s16 d0, q8
        vqmovun.s16 d1, q9
        vqmovun.s16 d2, q10
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!

        vshrn.i32 d16, q11, #8
        vshrn.i32 d18, q12, #8
        vshrn.i32 d20, q13, #8
        vqmovun.s16 d0, q8
        vqmovun.s16 d1, q9
        vqmovun.s16 d2, q10
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!

        subs r3, r3, #1
        bne 1b

        vpop            {q4-q7}
        pop             {r4-r8, r10, r11, lr}
        bx              lr
END(rsdIntrinsicYuv_K)

/* Convolve 5x5 */

/*
        r0 = dst
        r1 = y0 base pointer
        r2 = y1 base pointer
        r3 = y2 base pointer
        r4 = y3 base pointer
        r5 = y4 base pointer
        r6 = coeffs
        r7 = length
*/
ENTRY(rsdIntrinsicConvolve5x5_K)
        push        {r4-r7, lr}
        vpush       {q4-q7}

        /* load y3 in r4 */
        ldr     r4, [sp, #20 + 64]

        /* load y4 in r5 */
        ldr     r5, [sp, #24 + 64]

        /* Load the coefficients pointer */
        ldr     r6, [sp, #28 + 64]

        /* Create the coefficients vector */
        vld1.16     {d0, d1, d2, d3}, [r6]!
        vld1.16     {d4, d5, d6}, [r6]

        /* load the count */
        ldr     r6, [sp, #32 + 64]

        /* Load the frequently used immediate in a register */
        mov     r7, #8

1:
        /* Load the y base pointers in Qregs and post-increase the address by r7=#8 */
        vld1.8  {d24, d25, d26}, [r1], r7      @  y0 ( y - 2 )
        vld1.8  {d27, d28, d29}, [r2], r7      @  y0 ( y - 1 )

        /* Signal memory for data that will be used in the loop after the next */
        PLD         (r1, r7)
        PLD         (r2, r7)

        /* Promoting the 8bit channels to 16bit */
        vmovl.u8 q9,  d24
        vmovl.u8 q10, d25
        vmovl.u8 q11, d26
        vmovl.u8 q12, d27
        vmovl.u8 q13, d28
        vmovl.u8 q14, d29

/*
        d18,  d19,  d20, d21, d22, d23,
        d24,  d25
*/
        vmull.s16 q4, d18, d0[0]
        vmlal.s16 q4, d19, d0[1]
        vmlal.s16 q4, d20, d0[2]
        vmlal.s16 q4, d21, d0[3]
        vmlal.s16 q4, d22, d1[0]

        vmlal.s16 q4, d24, d1[1]
        vmlal.s16 q4, d25, d1[2]
        vmlal.s16 q4, d26, d1[3]
        vmlal.s16 q4, d27, d2[0]
        vmlal.s16 q4, d28, d2[1]

        vmull.s16 q5, d19, d0[0]
        vmlal.s16 q5, d20, d0[1]
        vmlal.s16 q5, d21, d0[2]
        vmlal.s16 q5, d22, d0[3]
        vmlal.s16 q5, d23, d1[0]

        vmlal.s16 q5, d25, d1[1]
        vmlal.s16 q5, d26, d1[2]
        vmlal.s16 q5, d27, d1[3]
        vmlal.s16 q5, d28, d2[0]
        vmlal.s16 q5, d29, d2[1]


        /* Next 2 rows */
        /* Load the y base pointers in Qregs and post-increase the address by r7=#8 */
        vld1.8  {d24, d25, d26}, [r3], r7      @  y0 ( y )
        vld1.8  {d27, d28, d29}, [r4], r7      @  y0 ( y + 1 )

        /* Signal memory for data that will be used in the loop after the next */
        PLD         (r3, r7)
        PLD         (r4, r7)

        /* Promoting the 8bit channels to 16bit */
        vmovl.u8 q9,  d24
        vmovl.u8 q10, d25
        vmovl.u8 q11, d26
        vmovl.u8 q12, d27
        vmovl.u8 q13, d28
        vmovl.u8 q14, d29

/*
        d18,  d19,  d20, d21, d22, d23,
        d24,  d25
*/
        vmlal.s16 q4, d18, d2[2]
        vmlal.s16 q4, d19, d2[3]
        vmlal.s16 q4, d20, d3[0]
        vmlal.s16 q4, d21, d3[1]
        vmlal.s16 q4, d22, d3[2]

        vmlal.s16 q4, d24, d3[3]
        vmlal.s16 q4, d25, d4[0]
        vmlal.s16 q4, d26, d4[1]
        vmlal.s16 q4, d27, d4[2]
        vmlal.s16 q4, d28, d4[3]

        vmlal.s16 q5, d19, d2[2]
        vmlal.s16 q5, d20, d2[3]
        vmlal.s16 q5, d21, d3[0]
        vmlal.s16 q5, d22, d3[1]
        vmlal.s16 q5, d23, d3[2]

        vmlal.s16 q5, d25, d3[3]
        vmlal.s16 q5, d26, d4[0]
        vmlal.s16 q5, d27, d4[1]
        vmlal.s16 q5, d28, d4[2]
        vmlal.s16 q5, d29, d4[3]

        /* Last row */
        /* Load the y base pointers in Qregs and post-increase the address by r7=#8 */
        vld1.8  {d24, d25, d26}, [r5], r7      @  y0 ( y + 2 )

        /* Signal memory for data that will be used in the loop after the next */
        PLD         (r5, r7)

        /* Promoting the 8bit channels to 16bit */
        vmovl.u8 q9,  d24
        vmovl.u8 q10, d25
        vmovl.u8 q11, d26

/*
        d18,  d19,  d20, d21, d22, d23,
        d24,  d25
*/

        vmlal.s16 q4, d18, d5[0]
        vmlal.s16 q4, d19, d5[1]
        vmlal.s16 q4, d20, d5[2]
        vmlal.s16 q4, d21, d5[3]
        vmlal.s16 q4, d22, d6[0]

        vmlal.s16 q5, d19, d5[0]
        vmlal.s16 q5, d20, d5[1]
        vmlal.s16 q5, d21, d5[2]
        vmlal.s16 q5, d22, d5[3]
        vmlal.s16 q5, d23, d6[0]




/*      Narrow it to a d-reg 32 -> 16 bit */
        vshrn.i32 d8, q4, #8
        vshrn.i32 d9, q5, #8

/*      Pack 16 -> 8 bit, saturate, put two pixels into D reg */
        vqmovun.s16 d8, q4

        vst1.8 d8, [r0]!           @ return the output and increase the address of r0

        /* Are we done? */
        subs r6, r6, #1
        bne 1b

        /* Yup, bye */
        vpop        {q4-q7}
        pop         {r4-r7, lr}
        bx          lr

END(rsdIntrinsicConvolve5x5_K)




/*
        dst = src + dst * (1.0 - src.a)

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendSrcOver_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vshll.u8 q12, d0, #8
        vshll.u8 q13, d1, #8
        vshll.u8 q14, d2, #8
        vmovl.u8 q6, d3
        vsub.i16 q6, q7, q6        // q6 = 1 - src.a
        vshll.u8 q15, d3, #8

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3

        vmla.i16 q12, q8, q6
        vmla.i16 q13, q9, q6
        vmla.i16 q14, q10, q6
        vmla.i16 q15, q11, q6

        vshrn.i16 d0, q12, #8
        vshrn.i16 d1, q13, #8
        vshrn.i16 d2, q14, #8
        vshrn.i16 d3, q15, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendSrcOver_K)

/*
        dst = dst + src * (1.0 - dst.a)

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendDstOver_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vshll.u8 q8, d0, #8
        vshll.u8 q9, d1, #8
        vshll.u8 q10, d2, #8
        vmovl.u8 q6, d3
        vsub.i16 q6, q7, q6        // q6 = 1 - dst.a
        vshll.u8 q11, d3, #8


        vmla.i16 q8, q12, q6
        vmla.i16 q9, q13, q6
        vmla.i16 q10, q14, q6
        vmla.i16 q11, q15, q6

        vshrn.i16 d0, q8, #8
        vshrn.i16 d1, q9, #8
        vshrn.i16 d2, q10, #8
        vshrn.i16 d3, q11, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendDstOver_K)

/*
        dst = src * dst.a

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendSrcIn_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        //vmovl.u8 q8, d0
        //vmovl.u8 q9, d1
        //vmovl.u8 q10, d2
        vmovl.u8 q11, d3

        vmul.i16 q12, q12, q11
        vmul.i16 q13, q13, q11
        vmul.i16 q14, q14, q11
        vmul.i16 q15, q15, q11

        vshrn.i16 d0, q12, #8
        vshrn.i16 d1, q13, #8
        vshrn.i16 d2, q14, #8
        vshrn.i16 d3, q15, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendSrcIn_K)

/*
        dst = dst * src.a

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendDstIn_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        //vmovl.u8 q12, d0
        //vmovl.u8 q13, d1
        //vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3

        vmul.i16 q8, q8, q15
        vmul.i16 q9, q9, q15
        vmul.i16 q10, q10, q15
        vmul.i16 q11, q11, q15

        vshrn.i16 d0, q8, #8
        vshrn.i16 d1, q9, #8
        vshrn.i16 d2, q10, #8
        vshrn.i16 d3, q11, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendDstIn_K)



/*
        dst = src * (1.0 - dst.a)

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendSrcOut_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        //vmovl.u8 q8, d0
        //vmovl.u8 q9, d1
        //vmovl.u8 q10, d2
        vmovl.u8 q11, d3


        vsub.i16 q6, q7, q11        // q6 = 1 - dst.a
        vmul.i16 q12, q12, q6
        vmul.i16 q13, q13, q6
        vmul.i16 q14, q14, q6
        vmul.i16 q15, q15, q6

        vshrn.i16 d0, q12, #8
        vshrn.i16 d1, q13, #8
        vshrn.i16 d2, q14, #8
        vshrn.i16 d3, q15, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendSrcOut_K)


/*
        dst = dst * (1.0 - src.a)

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendDstOut_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        //vmovl.u8 q12, d0
        //vmovl.u8 q13, d1
        //vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3


        vsub.i16 q6, q7, q15        // q6 = 1 - src.a
        vmul.i16 q12, q8, q6
        vmul.i16 q13, q9, q6
        vmul.i16 q14, q10, q6
        vmul.i16 q15, q11, q6

        vshrn.i16 d0, q12, #8
        vshrn.i16 d1, q13, #8
        vshrn.i16 d2, q14, #8
        vshrn.i16 d3, q15, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendDstOut_K)


/*
        dst.rgb = src.rgb * dst.a + (1.0 - src.a) * dst.rgb
        dst.a = dst.a

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendSrcAtop_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3


        vsub.i16 q6, q7, q15        // q6 = 1 - src.a
        vmul.i16 q8, q8, q6
        vmul.i16 q9, q9, q6
        vmul.i16 q10, q10, q6

        vmla.i16 q8, q12, q11
        vmla.i16 q9, q13, q11
        vmla.i16 q10, q14, q11


        vshrn.i16 d0, q8, #8
        vshrn.i16 d1, q9, #8
        vshrn.i16 d2, q10, #8
        //vshrn.i16 d3, q15, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendSrcAtop_K)

/*
        dst = dst.rgb * src.a + (1.0 - dst.a) * src.rgb
        dst.a = src.a

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendDstAtop_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3


        vsub.i16 q6, q7, q11        // q6 = 1 - dst.a
        vmul.i16 q12, q12, q6
        vmul.i16 q13, q13, q6
        vmul.i16 q14, q14, q6

        vmla.i16 q12, q8, q15
        vmla.i16 q13, q9, q15
        vmla.i16 q14, q10, q15


        vshrn.i16 d0, q12, #8
        vshrn.i16 d1, q13, #8
        vshrn.i16 d2, q14, #8
        //vshrn.i16 d3, q15, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendDstAtop_K)

/*
        dst = dst ^ src

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendXor_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmov.u8 d4, d0
        vmov.u8 d5, d1
        vmov.u8 d6, d2
        vmov.u8 d7, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!

        veor d0, d0, d4
        veor d1, d1, d5
        veor d2, d2, d6
        veor d3, d3, d7

        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendXor_K)

/*
        dst = dst * src

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendMultiply_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3


        vmul.i16 q8, q8, q12
        vmul.i16 q9, q9, q13
        vmul.i16 q10, q10, q14
        vmul.i16 q11, q11, q15

        vshrn.i16 d0, q8, #8
        vshrn.i16 d1, q9, #8
        vshrn.i16 d2, q10, #8
        vshrn.i16 d3, q11, #8
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendMultiply_K)

/*
        dst = min(src + dst, 1.0)

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendAdd_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3


        vadd.i16 q8, q8, q12
        vadd.i16 q9, q9, q13
        vadd.i16 q10, q10, q14
        vadd.i16 q11, q11, q15

        vqmovun.s16 d0, q8
        vqmovun.s16 d1, q9
        vqmovun.s16 d2, q10
        vqmovun.s16 d3, q11
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendAdd_K)


/*
        dst = max(dst - src, 0.0)

        r0 = dst
        r1 = src
        r2 = length
*/
ENTRY(rsdIntrinsicBlendSub_K)
        .save           {r4, lr}
        stmfd           sp!, {r4, lr}
        vpush           {q4-q7}

        mov r4, #255
        vdup.16 q7, r4

        mov r4, r0
1:

        /* src */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r1]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r1]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r1]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r1]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r1]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r1]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r1]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r1]!
        vmovl.u8 q12, d0
        vmovl.u8 q13, d1
        vmovl.u8 q14, d2
        vmovl.u8 q15, d3

        /* dst */
        vld4.8 {d0[0],d1[0],d2[0],d3[0]}, [r0]!
        vld4.8 {d0[1],d1[1],d2[1],d3[1]}, [r0]!
        vld4.8 {d0[2],d1[2],d2[2],d3[2]}, [r0]!
        vld4.8 {d0[3],d1[3],d2[3],d3[3]}, [r0]!
        vld4.8 {d0[4],d1[4],d2[4],d3[4]}, [r0]!
        vld4.8 {d0[5],d1[5],d2[5],d3[5]}, [r0]!
        vld4.8 {d0[6],d1[6],d2[6],d3[6]}, [r0]!
        vld4.8 {d0[7],d1[7],d2[7],d3[7]}, [r0]!
        vmovl.u8 q8, d0
        vmovl.u8 q9, d1
        vmovl.u8 q10, d2
        vmovl.u8 q11, d3


        vsub.i16 q8, q8, q12
        vsub.i16 q9, q9, q13
        vsub.i16 q10, q10, q14
        vsub.i16 q11, q11, q15

        vqmovun.s16 d0, q8
        vqmovun.s16 d1, q9
        vqmovun.s16 d2, q10
        vqmovun.s16 d3, q11
        vst4.8 {d0[0],d1[0],d2[0],d3[0]}, [r4]!
        vst4.8 {d0[1],d1[1],d2[1],d3[1]}, [r4]!
        vst4.8 {d0[2],d1[2],d2[2],d3[2]}, [r4]!
        vst4.8 {d0[3],d1[3],d2[3],d3[3]}, [r4]!
        vst4.8 {d0[4],d1[4],d2[4],d3[4]}, [r4]!
        vst4.8 {d0[5],d1[5],d2[5],d3[5]}, [r4]!
        vst4.8 {d0[6],d1[6],d2[6],d3[6]}, [r4]!
        vst4.8 {d0[7],d1[7],d2[7],d3[7]}, [r4]!

        subs r2, r2, #1
        bne 1b

        vpop            {q4-q7}
        ldmfd           sp!, {r4, lr}
        bx              lr
END(rsdIntrinsicBlendSub_K)

