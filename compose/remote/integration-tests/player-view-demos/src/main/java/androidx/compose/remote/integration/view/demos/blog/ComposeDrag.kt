/// *
// * Copyright 2026 The Android Open Source Project
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
// package androidx.compose.remote.integration.view.demos.blog
// import androidx.compose.remote.creation.RemoteComposeWriterAndroid
// import androidx.compose.remote.creation.compose.action.Action.Companion.Empty
// import androidx.compose.remote.creation.compose.action.hostAction
// import androidx.compose.remote.creation.compose.action.valueChange
// import androidx.compose.remote.creation.compose.layout.RemoteAlignment
// import androidx.compose.remote.creation.compose.layout.RemoteBox
// import androidx.compose.remote.creation.compose.layout.RemoteCanvas
// import androidx.compose.remote.creation.compose.layout.RemoteComposable
// import androidx.compose.remote.creation.compose.modifier.RemoteModifier
// import androidx.compose.remote.creation.compose.modifier.background
// import androidx.compose.remote.creation.compose.modifier.fillMaxSize
// import androidx.compose.remote.creation.compose.modifier.height
// import androidx.compose.remote.creation.compose.modifier.offset
// import androidx.compose.remote.creation.compose.modifier.onTouchDown
// import androidx.compose.remote.creation.compose.modifier.width
// import androidx.compose.remote.creation.compose.state.RemoteColor
// import androidx.compose.remote.creation.compose.state.RemoteString
// import androidx.compose.remote.creation.compose.state.rememberMutableRemoteFloat
// import androidx.compose.remote.creation.compose.state.rememberMutableRemoteInt
// import androidx.compose.remote.creation.compose.state.toRemoteDp
// import androidx.compose.remote.creation.compose.widgets.onClick
// import androidx.compose.remote.creation.dsl.RcCanvasScope
// import androidx.compose.remote.creation.dsl.RcConditionOp
// import androidx.compose.remote.creation.dsl.RcFloat
// import androidx.compose.remote.creation.dsl.RcTextAnchorFlags
// import androidx.compose.remote.creation.dsl.hypot
// import androidx.compose.runtime.Composable
// import androidx.compose.ui.graphics.Color
//
/// **
// * N circles laid out from a float array, drawn in a player-side loop,
// * dragged by index.
// */
// @Suppress("RestrictedApiAndroidX")
//
// @RemoteComposable
// @Composable
// public fun dslDragCircles_old() {
//
//    val objX = floatArrayOf(0.2f, 0.5f, 0.8f, 0.2f, 0.5f, 0.8f)
//    val objY = floatArrayOf(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f)
//
//
//    RemoteBox(
//        modifier =
//            RemoteModifier.fillMaxSize().background(RemoteColor(Color.White)).onTouchDown(Empty),
//        contentAlignment = RemoteAlignment.Center,
//    ) {
//
//
//        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
//            val doc = remoteComposeCreationState.document as RemoteComposeWriterAndroid
//
//            RcCanvasScope(doc) {
//                beginGlobal()
//                val rad = 70f
//                val down = 1.rf.flush()
//                val selected = (-100).rf.flush()
//                val posX = remoteDynamicFloatArray(objX.size.toFloat())
//                val posY = remoteDynamicFloatArray(objX.size.toFloat())
//                val initPosX = remoteFloatArray(objX)
//                val initPosY = remoteFloatArray(objY)
//                val first = 0.rf.flush()
//                endGlobal()
//                val w = componentWidth()
//                val h = componentHeight()
//                val touchX = (touchPosX() + 0f).flush()
//                val touchY = (touchPosY() + 0f).flush()
////               impulse(touchTime(),0.1f.rf){
////                debug("impulse hello", touchTime())
////                   runAction {
////                       setValue(down, -1f)
////                   }
////                   impulseProcess {
////                       debug("impulseProcess ", touchTime())
////                   }
////                }
//                conditionalOperations(RcConditionOp.Eq, 0.rf, first) {
//                    this@RcCanvasScope.loop(0f.rf, 1f.rf, arrayLength(initPosX)) { i ->
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
//                    conditionalOperations(RcConditionOp.Lte, 0.rf, selected) {
//                        setArrayValue(posX, selected, tpX)
//                        setArrayValue(posY, selected, tpY)
//                        runAction {
//                            setValue(selected, -100f)
//                        }
//                    }
//                    conditionalOperations(RcConditionOp.Gt, 0.rf, selected) {
//
//
//                        this@RcCanvasScope.loop(0f.rf, 1f.rf, arrayLength(posY)) { idx ->
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
//
//            }
//        }
//    }
// }
//
//
// @RemoteComposable
// @Composable
// public fun dslDragCircles() {
//
//    val objX = floatArrayOf(0.2f, 0.5f, 0.8f, 0.2f, 0.5f, 0.8f)
//    val objY = floatArrayOf(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f)
//    val rad = 100;
////    val selected = rememberMutableRemoteInt (-1 )
//    val selected = rememberMutableRemoteFloat (-1f )
//
//    //  val  h = hostAction(RemoteString(" move"), RemoteString("foo"))
//    RemoteBox(
//        modifier =
//            RemoteModifier.fillMaxSize().background(RemoteColor(Color.White))
////                .onClick{h},
//
//    ) {
//
//        val cw = rememberMutableRemoteFloat { componentWidth() }
//        val ch = rememberMutableRemoteFloat { componentHeight() }
//
//        for (objIndex in objX.indices) {
//            val x = rememberMutableRemoteFloat { cw * objX[objIndex] }
//            val y = rememberMutableRemoteFloat { ch * objY[objIndex] }
//            val idx = rememberMutableRemoteInt(objIndex)
////            RemoteBox(
////                modifier = RemoteModifier
////                    .offset((x - rad.toFloat()).toRemoteDp(), (y - rad.toFloat()).toRemoteDp())
////                    .width(rad * 2)
////                    .height(rad * 2)
////                    .background(RemoteColor(Color.Yellow))
////                    .onClick {
////                        valueChange(selected, idx)
////                        hostAction(RemoteString(" select "), selected)
////                    }
////            ) {
//                RemoteCanvas(
//                    modifier = RemoteModifier.fillMaxSize()
//
//                        .onClick {
//                            valueChange(selected, idx.toRemoteFloat())
//                            hostAction(RemoteString(" select $objIndex"), RemoteString(" select
// $objIndex"))
//                        }
//
//                ) {
//                    val doc = remoteComposeCreationState.document as RemoteComposeWriterAndroid
//
//                    RcCanvasScope(doc) {
//                        val x = (componentWidth() * objX[objIndex]).flush()
//                        val y = (componentHeight() * objY[objIndex]).flush()
//                        val touchX = touchPosX().flush()
//                        val touchY = touchPosY().flush();
//
//                        impulse(1.rf, touchTime()) {
//                            val dist = hypot(x - touchX, y - touchY)
//                        conditionalOperations(RcConditionOp.Lt, dist, rad.rf) {
//                            runAction {
//                                setValue(RcFloat(selected.floatId), objIndex.toFloat())
//                            }
//                        }
//                    }
//
////                        if (objIndex == 0) {
////                            debug(" $objIndex  === ", selected.floatId.rf)
////                        }
//                        paint {
//                            color(0xFF00_00_FF)
//                            textSize(64f)
//                        }
////                    if (objIndex == 0) {
////                        debug(" $objIndex  ===  selected =  ", selected.floatId.rf)
////                    }
//                        conditionalOperations(RcConditionOp.Eq, objIndex.rf, selected.floatId.rf)
// {
//                            paint {
//                                color(0xFF00_FF_FF)
//                            }
//                        }
//
//
//                        drawCircle(x, y, rad.rf)
//                        paint {
//                            color(0xFF_FF_FF_AA)
//                        }
//
//                        drawTextAnchored(
//                            remoteText("$objIndex"),
//                            x,
//                             y,
//                            0.rf,
//                            0.rf,
//                            RcTextAnchorFlags.None
//                        );
//
//                    }
//                }
//            }
//        }
////    }
// }
//
//
//
// @RemoteComposable
// @Composable
// public fun dslDragCircles_old3() {
//
//    val objX = floatArrayOf(0.2f, 0.5f, 0.8f, 0.2f, 0.5f, 0.8f)
//    val objY = floatArrayOf(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f)
//    val rad = 100;
//    RemoteBox(
//        modifier =
//            RemoteModifier.fillMaxSize().background(RemoteColor(Color.White))
//    ) {
//        RemoteCanvas(
//            modifier = RemoteModifier.fillMaxSize().onTouchDown(Empty)
//        ) {
//            val doc = remoteComposeCreationState.document as RemoteComposeWriterAndroid
//
//            RcCanvasScope(doc) {
//
//                val cw = componentWidth()
//                val ch = componentHeight()
//                val touchX = touchPosX();
//                val touchY = touchPosY();
//                val selected = (-1).rf.flush()
//
//                for (objIndex in objX.indices) {
//                    val x = cw * objX[objIndex]
//                    val y = ch * objY[objIndex]
//                    val dist = hypot(touchX - x, touchY - y)
//                    conditionalOperations(RcConditionOp.Lt, dist, rad.rf) {
//                        runAction {
//                            setValue(selected, objIndex.toFloat())
//                        }
//                    }
//
//                    paint {
//                        color(0xFF_00_00_FF)
//                        textSize(64f)
//                    }
//                    conditionalOperations(RcConditionOp.Eq, objIndex.rf, selected) {
//                        paint {
//                            color(0xFF_00_FF_FF)
//                        }
//                    }
//
//                    drawCircle(x, y, rad.rf)
//                    paint {
//                        color(0xFF_FF_FF_AA)
//                    }
//                    val text = remoteText("$objIndex")
//                    drawTextAnchored(text, x, y, 0.rf, 0.rf, RcTextAnchorFlags.None);
//                }
//                impulse(1.rf, touchTime()) {
//                    debug("selected = ", selected)
//                    val res = remoteText("move(") +
//                        selected.genTextId(2, 0) + ") to " +
//                        touchX.genTextId(7, 0) + "," + touchY.genTextId(8, 0)
//                    runAction {
//                        //      hostAction(33, res)
//                    }
//                }
//
//            }
//        }
//    }
// }
//
//
// @RemoteComposable
// @Composable
// public fun dslDragCircles_old2() {
//
//    val objX = floatArrayOf(0.2f, 0.5f, 0.8f, 0.2f, 0.5f, 0.8f)
//    val objY = floatArrayOf(0.3f, 0.3f, 0.3f, 0.6f, 0.6f, 0.6f)
//    val rad = 100;
//    val selected = rememberMutableRemoteInt (-1 )
//    //  val  h = hostAction(RemoteString(" move"), RemoteString("foo"))
//    RemoteBox(
//        modifier =
//            RemoteModifier.fillMaxSize().background(RemoteColor(Color.White))
////                .onClick{h},
//
//    ) {
//
//        val cw = rememberMutableRemoteFloat { componentWidth() }
//        val ch = rememberMutableRemoteFloat { componentHeight() }
//
//        for (objIndex in objX.indices) {
//            val x = rememberMutableRemoteFloat { cw * objX[objIndex] }
//            val y = rememberMutableRemoteFloat { ch * objY[objIndex] }
//            val idx = rememberMutableRemoteInt(objIndex)
//            RemoteBox(
//                modifier = RemoteModifier
//                    .offset((x - rad.toFloat()).toRemoteDp(), (y - rad.toFloat()).toRemoteDp())
//                    .width(rad * 2)
//                    .height(rad * 2)
//                    .background(RemoteColor(Color.Yellow))
//                    .onClick {
//                        valueChange(selected, idx)
//                        hostAction(RemoteString(" select "), selected)
//                    }
//            ) {
//                RemoteCanvas(
//                    modifier = RemoteModifier.fillMaxSize()
//                        .background(RemoteColor(Color.Red))
//                        .onClick {
//                            valueChange(selected, idx)
//                            hostAction(RemoteString(" select $objIndex"), RemoteString(" select
// $objIndex"))
//                        }
//
//                ) {
//                    val doc = remoteComposeCreationState.document as RemoteComposeWriterAndroid
//
//                    RcCanvasScope(doc) {
//                        if (objIndex == 0) {
//                            debug(" $objIndex  === ", selected.floatId.rf)
//                        }
//                        paint {
//                            color(0xFF00_00_FF)
//                            textSize(64f)
//                        }
////                    if (objIndex == 0) {
////                        debug(" $objIndex  ===  selected =  ", selected.floatId.rf)
////                    }
//                        conditionalOperations(RcConditionOp.Eq, objIndex.rf, selected.floatId.rf)
// {
//                            paint {
//                                color(0xFF00_FF_FF)
//                            }
//                        }
//
//                        val px = componentWidth() / 2f
//                        val py = componentHeight() / 2f
//                        drawCircle(px, py, min(width, height) / 2f)
//                        paint {
//                            color(0xFF_FF_FF_AA)
//                        }
//
//                        drawTextAnchored(
//                            remoteText("$objIndex"),
//                            px,
//                            py,
//                            0.rf,
//                            0.rf,
//                            RcTextAnchorFlags.None
//                        );
//
//                    }
//                }
//            }
//        }
//    }
// }
//
