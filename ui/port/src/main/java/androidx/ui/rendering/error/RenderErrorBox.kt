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

package androidx.ui.rendering.error

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.box.RenderBox
import androidx.ui.rendering.obj.PaintingContext

const val _kMaxWidth: Double = 100000.0
const val _kMaxHeight: Double = 100000.0

// Line length to fit small phones without dynamically checking size.
const val _kLine: String = "\n\n────────────────────\n\n"

// A render object used as a placeholder when an error occurs.
/**
 *
 * The box will be painted in the color given by the
 * [RenderErrorBox.backgroundColor] static property.
 *
 * A message can be provided. To simplify the class and thus help reduce the
 * likelihood of this class itself being the source of errors, the message
 * cannot be changed once the object has been created. If provided, the text
 * will be painted on top of the background, using the styles given by the
 * [RenderErrorBox.textStyle] and [RenderErrorBox.paragraphStyle] static
 * properties.
 *
 * Again to help simplify the class, this box tries to be 100000.0 pixels wide
 * and high, to approximate being infinitely high but without using infinities.
 */
class RenderErrorBox(
    /** The message to attempt to display at paint time. */
    val message: String = ""
) : RenderBox() {

    /**
     * Creates a RenderErrorBox render object.
     *
     * A message can optionally be provided. If a message is provided, an attempt
     * will be made to render the message when the box paints.
     */
    init {
//        if (message != "") {
//            // This class is intentionally doing things using the low-level
//            // primitives to avoid depending on any subsystems that may have ended
//            // up in an unstable state -- after all, this class is mainly used when
//            // things have gone wrong.
//            //
//            // Generally, the much better way to draw text in a RenderObject is to
//            // use the TextPainter class. If you're looking for code to crib from,
//            // see the paragraph.dart file and the RenderParagraph class.
//            val builder = ui.ParagraphBuilder (paragraphStyle);
//            builder.pushStyle(textStyle);
//            builder.addText(
//                    "$message$_kLine$message$_kLine$message$_kLine$message$_kLine$message$_kLine$message$_kLine" +
//                    "$message$_kLine$message$_kLine$message$_kLine$message$_kLine$message$_kLine$message"
//            );
//            _paragraph = builder.build();
//        }
    }

    companion object {
//        /// The color to use when painting the background of [RenderErrorBox] objects.
//        val backgroundColor: Color = Color.valueOf(0xF0900000)
//
//        /// The text style to use when painting [RenderErrorBox] objects.
//        val textStyle = ui.TextStyle(
//            color = Color(0xFFFFFF66),
//            fontFamily = "monospace",
//            fontSize = 14.0,
//            fontWeight = FontWeight.bold
//        );
//
//        /// The paragraph style to use when painting [RenderErrorBox] objects.
//        val paragraphStyle = ui.ParagraphStyle(lineHeight = 1.0);
    }

//    var _paragraph: ui.Paragraph? = null;

    override fun computeMaxIntrinsicWidth(height: Double): Double {
        return _kMaxWidth
    }

    override fun computeMaxIntrinsicHeight(width: Double): Double {
        return _kMaxHeight
    }

    override var sizedByParent = true

    override fun hitTestSelf(position: Offset): Boolean = true

    override fun performResize() {
        size = constraints!!.constrain(Size(_kMaxWidth, _kMaxHeight))
    }

    override fun paint(context: PaintingContext, offset: Offset) {
        TODO("Migration/Filip: implement")
//        try {
//            context.canvas.drawRect(offset & size, new Paint() .. color = backgroundColor);
//            double width;
//            if (_paragraph != null) {
//                // See the comment in the RenderErrorBox constructor. This is not the
//                // code you want to be copying and pasting. :-)
//                if (parent is RenderBox) {
//                    final RenderBox parentBox = parent;
//                    width = parentBox.size.width;
//                } else {
//                    width = size.width;
//                }
//                _paragraph.layout(new ui.ParagraphConstraints(width: width));
//
//                context.canvas.drawParagraph(_paragraph, offset);
//            }
//        } catch (e) { } // ignore: empty_catches
    }
}
