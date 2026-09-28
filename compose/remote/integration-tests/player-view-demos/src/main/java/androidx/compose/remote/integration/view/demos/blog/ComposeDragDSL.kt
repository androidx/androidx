/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.blog

import android.graphics.Color
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcConditionOp
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.computePosition
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.height
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.times
import androidx.compose.remote.creation.dsl.touchPosX
import androidx.compose.remote.creation.dsl.touchPosY
import androidx.compose.remote.creation.dsl.width
import androidx.compose.remote.creation.profile.RcPlatformProfiles

/** N circles laid out from a float array, drawn in a player-side loop, dragged by index. */
@Suppress("RestrictedApiAndroidX")
fun dslDragCirclesDsl(): ByteArray {
    val objX = floatArrayOf(0.2f, 0.5f, 0.8f, 0.2f, 0.5f, 0.8f)
    val objY = floatArrayOf(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f)
    val rad = 100f

    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        val selected = (-100).rf.flush()

        Box(
            modifier =
                Modifier.fillMaxSize().background(Color.LTGRAY).onClick {
                    hostAction(
                        32,
                        with(this@createRcBuffer) {
                            remoteText(" move (") +
                                selected.genTextId(2, 0) +
                                " ) " +
                                touchPosX().genTextId(8, 0) +
                                " , " +
                                touchPosY().genTextId(8, 0)
                        },
                    )
                }
        ) {
            for (objIndex in objX.indices) {
                val px = objX[objIndex]
                val py = objY[objIndex]
                println("$px , $py")
                Canvas(
                    modifier =
                        Modifier.computePosition {
                                x = px * parentWidth - rad
                                y = py * parentHeight - rad
                            }
                            .width((rad * 2).rf)
                            .height((rad * 2).rf)
                            .onClick {
                                setValue(selected, objIndex.toFloat())
                            }
                ) {
                    paint {
                        color(0xFF00_00_AA)
                        textSize(64f)
                    }
                    conditionalOperations(RcConditionOp.Eq, objIndex.rf, selected) {
                        paint {
                            color(0xFF00_FF_FF)
                        }
                    }
                    val px = componentWidth() / 2f
                    val py = componentHeight() / 2f
                    drawCircle(px, py, rad.rf)
                    paint {
                        color(0xFF_FF_FF_AA)
                    }
                    drawTextAnchored(remoteText("$objIndex"), px, py, 0.rf, 0.rf, 0)
                }
            }
        }
    }
}

@Suppress("RestrictedApiAndroidX")
fun dslDragCirclesDsl_old2(): ByteArray {
    val objX = floatArrayOf(0.2f, 0.5f, 0.8f, 0.2f, 0.5f, 0.8f)
    val objY = floatArrayOf(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f)
    val rad = 100f

    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
        beginGlobal()
        val selected = (-100).rf.flush()
        endGlobal()

        Box(
            modifier =
                Modifier.fillMaxSize().background(Color.LTGRAY).onClick {
                    hostAction(32, this@createRcBuffer.remoteText(" move"))
                }
        ) {
            val cw = componentWidth()
            val ch = componentHeight()
            for (objIndex in objX.indices) {
                val px = objX[objIndex]
                val py = objY[objIndex]
                println("$px , $py")
                //                Box (   modifier = Modifier
                //                    .offset((px*cw-rad).toFloat(),(py*ch-rad).toFloat())
                ////                                            .computePosition {
                ////                            x = px * parentWidth-rad
                ////                            y = py * parentHeight-rad
                ////                        }
                //                    .onClick { setValue(selected, objIndex.toFloat())  }
                //                    .width((rad*2).rf)
                //                    .height((rad*2).rf)
                //                    .background(Color.DKGRAY)){
                Canvas(
                    modifier =
                        Modifier
                            //
                            // .offset((px*cw-rad).toFloat(),(py*ch-rad).toFloat())
                            .computePosition {
                                x = px * parentWidth - rad
                                y = py * parentHeight - rad
                            }
                            .width((rad * 2).rf)
                            .height((rad * 2).rf)
                            .onClick {
                                setValue(selected, objIndex.toFloat())
                            }
                ) {
                    //                    if (objIndex == 0) {
                    //                        debug(" selected ", selected)
                    //                    }
                    paint {
                        color(0xFF00_00_AA)
                        textSize(64f)
                    }
                    conditionalOperations(RcConditionOp.Eq, objIndex.rf, selected) {
                        paint {
                            color(0xFF00_FF_FF)
                        }
                    }
                    val px = componentWidth() / 2f
                    val py = componentHeight() / 2f
                    drawCircle(px, py, rad.rf)
                    paint {
                        color(0xFF_FF_FF_AA)
                    }
                    drawTextAnchored(remoteText("$objIndex"), px, py, 0.rf, 0.rf, 0)
                }
            }
            //        }
        }
    }
}

//
// @Suppress("RestrictedApiAndroidX")
//    fun dslDragCirclesDsl_orig(): ByteArray {
//    val objX = floatArrayOf(0.2f, 0.5f, 0.8f, 0.2f, 0.5f, 0.8f)
//    val objY = floatArrayOf(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f)
//    return createRcBuffer(RcProfile(RcPlatformProfiles.ANDROIDX), experimental = true) {
//
//        val rad = 70f
//        val down = 1.rf.flush()
//        val selected = (-100).rf.flush()
//        val posX = remoteDynamicFloatArray(objX.size.toFloat())
//        val posY = remoteDynamicFloatArray(objX.size.toFloat())
//        val isSelectedArray = remoteDynamicFloatArray(objX.size.toFloat())
//
//        val first = 0.rf.flush()
//        val lastDownX = 0.rf.flush()
//        val lastDownY = 0.rf.flush()
//
//        Box(
//            modifier = Modifier.fillMaxSize().background(android.graphics.Color.DKGRAY)
//        ) {
//
//            Canvas(
//                modifier = Modifier.fillMaxSize()
//                    .onTouchDown {
//                        setValue(down, -1f)
//                        setValue(lastDownX, this@Box.touchPosX())
//                        setValue(lastDownY, this@Box.touchPosY())
//                    }
//                    .onTouchUp {
//                        setValue(down, 1f)
//                    }) {
//                val w = componentWidth()
//                val h = componentHeight()
//                val initPosX = remoteFloatArray(objX)
//                val initPosY = remoteFloatArray(objY)
//
//                val touchX = (touchPosX() + 0f).flush()
//                val touchY = (touchPosY() + 0f).flush()
//
//                conditionalOperations(RcConditionOp.Eq, 0.rf, first) {
//                    this@Canvas.loop(0f.rf, 1f.rf, arrayLength(initPosX)) { i ->
//                        setArrayValue(posX, i + 0f, initPosX.get(i))
//                        setArrayValue(posY, i + 0f, initPosY.get(i))
//                    }
//                    runAction {
//                        setValue(first, 1f)
//                    }
//                }
//
//                paint {
//                    textSize(64f)
//                }
//                paint {
//                    color(0xFFFF0000.toInt())
//                }
//                val tpX = (touchX / w).flush()
//                val tpY = (touchY / h).flush()
//                conditionalOperations(RcConditionOp.Lt, 0.rf, down) {
//                    // Selected case
//                    conditionalOperations(RcConditionOp.Lte, 0.rf, selected) {
//                        setArrayValue(posX, selected, tpX)
//                        setArrayValue(posY, selected, tpY)
//                        runAction {
//                            setValue(selected, -100f)
//                        }
//                    }
//
//                    // Nothing selected find selected
//                    conditionalOperations(RcConditionOp.Gt, 0.rf, selected) {
//                        this@Canvas.loop(0f.rf, 1f.rf, arrayLength(posY)) { idx ->
//                            val dist =
//                                hypot(
//                                    (posX.get(idx) * w - touchX),
//                                    (posY.get(idx) * h - touchY)
//                                ) - rad
//                            conditionalOperations(RcConditionOp.Lt, dist, 0.rf) {
//                                val foo = idx + 0f
//                                runAction {
//                                    setValue(selected, foo)
//                                }
//                            }
//                        }
//                    }
//
//                }
//                debug("selected ", selected)
//                loop(0f.rf, 1f.rf, arrayLength(posY)) { idx ->
//
//                    paint {
//                        color(0xFF00_00_FF)
//                    }
//                    conditionalOperations(RcConditionOp.Eq, idx, selected) {
//                        paint {
//                            color(0xFF00_FF_FF)
//                        }
//                    }
//                    val px = posX.get(idx) * w
//                    val py = posY.get(idx) * h
//                    drawCircle(px, py, rad.rf)
//                    paint {
//                        color(0xFF_FF_FF_AA)
//                    }
//                    drawTextAnchored(idx.genTextId(2, 0), px, py, 0.rf, 0.rf, 0);
//
//                }
//            }
//        }
//    }
// }
//

//                // Down: latch which circle, and where the finger started.
//                .onTouchDown {
//                    setValue(selected, 0f)
//                    setValue(grabX, touchPosX())
//                    setValue(grabY, touchPosY())
//                }
//                // Up and cancel both release — a cancelled gesture must not
//                // leave a circle stuck to the finger.
//                .onTouchUp { setValue(selected, -1f) }
//                .onTouchCancel { setValue(selected, -1f) }
//                    ) {
//            val w = componentWidth() / 2f
//            val h = componentHeight() / 2f
//            val cx = componentWidth() / 2f
//            val cy = componentHeight() / 2f
//            loop(0f.rf, 1f.rf, arrayLength(xs)) { idx ->
//                val dist  = hypot((xs.get(idx) - touchPosX()), (xs.get(idx) - touchPosX()))
//                conditionalOperations(RcConditionOp.Gt, dist , max  )
//            }

//
//            val goalX =
//                addTouch(
//                    defValue = cx,
//                    min = 0.rf,
//                    max = w,
//                    stopMode = RcTouchStopMode.Gently,
//                    velocity = 0.rf,
//                    notchHaptic = RcHaptic.NoHaptics,
//                    touchSpec = null,
//                    easingSpec = null,
//                    exp = touchPosX(),
//                )
//
//            val delta =
//                addTouch(
//                    defValue = cy,
//                    min = 0.rf,
//                    max = h,
//                    stopMode = RcTouchStopMode.Gently,
//                    velocity = 0.rf,
//                    notchHaptic = RcHaptic.NoHaptics,
//                    touchSpec = null,
//                    easingSpec = null,
//                    exp = touchPosX(),
//                )
//                        val shiftX = touchPosX() - grabX
//                        val shiftY = touchPosY() - grabY
//
//                        loop(0f.rf, 1f.rf, n.toFloat().rf) { i ->
//
//                            val sel = 1f - clamp(0f.rf, 1f.rf, abs(i - 2f))
//                            val cx = xs.get(i) + sel * shiftX
//                            val cy = ys.get(i) + sel * shiftY
//
//                            paint {
//                                color(0xFF2A3550.toInt())
//                                style(RcPaintStyle.Fill)
//                            }
//                            drawCircle(cx, cy, 70f.rf)
//
//                            paint {
//                                color(0xFFE8EAF0.toInt())
//                                textSize(50f)
//                            }
//                            drawTextAnchored(i.genTextId(2, 0) ,cx, cy, 0.rf, 0.rf, 0);
//                        }
