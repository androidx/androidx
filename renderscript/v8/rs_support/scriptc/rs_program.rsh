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

/** @file rs_program.rsh
 *  \brief Program object routines
 *
 *
 */

#ifndef __RS_PROGRAM_RSH__
#define __RS_PROGRAM_RSH__

#if (defined(RS_VERSION) && (RS_VERSION >= 16))

/**
 * Get program store depth function
 *
 * @param ps program store to query
 */
extern rs_depth_func __attribute__((overloadable))
    rsgProgramStoreGetDepthFunc(rs_program_store ps);

/**
 * Get program store depth mask
 *
 * @param ps program store to query
 */
extern bool __attribute__((overloadable))
    rsgProgramStoreIsDepthMaskEnabled(rs_program_store ps);
/**
 * Get program store red component color mask
 *
 * @param ps program store to query
 */
extern bool __attribute__((overloadable))
    rsgProgramStoreIsColorMaskRedEnabled(rs_program_store ps);

/**
 * Get program store green component color mask
 *
 * @param ps program store to query
 */
extern bool __attribute__((overloadable))
    rsgProgramStoreIsColorMaskGreenEnabled(rs_program_store ps);

/**
 * Get program store blur component color mask
 *
 * @param ps program store to query
 */
extern bool __attribute__((overloadable))
    rsgProgramStoreIsColorMaskBlueEnabled(rs_program_store ps);

/**
 * Get program store alpha component color mask
 *
 * @param ps program store to query
 */
extern bool __attribute__((overloadable))
    rsgProgramStoreIsColorMaskAlphaEnabled(rs_program_store ps);

/**
 * Get program store blend source function
 *
 * @param ps program store to query
 */
extern rs_blend_src_func __attribute__((overloadable))
        rsgProgramStoreGetBlendSrcFunc(rs_program_store ps);

/**
 * Get program store blend destination function
 *
 * @param ps program store to query
 */
extern rs_blend_dst_func __attribute__((overloadable))
    rsgProgramStoreGetBlendDstFunc(rs_program_store ps);

/**
 * Get program store dither state
 *
 * @param ps program store to query
 */
extern bool __attribute__((overloadable))
    rsgProgramStoreIsDitherEnabled(rs_program_store ps);

/**
 * Get program raster point sprite state
 *
 * @param pr program raster to query
 */
extern bool __attribute__((overloadable))
    rsgProgramRasterIsPointSpriteEnabled(rs_program_raster pr);

/**
 * Get program raster cull mode
 *
 * @param pr program raster to query
 */
extern rs_cull_mode __attribute__((overloadable))
    rsgProgramRasterGetCullMode(rs_program_raster pr);

#endif // (defined(RS_VERSION) && (RS_VERSION >= 16))

#endif // __RS_PROGRAM_RSH__

