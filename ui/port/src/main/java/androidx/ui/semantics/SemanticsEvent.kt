package androidx.ui.semantics

// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// import 'package:flutter/painting.dart';

// / An event sent by the application to notify interested listeners that
// / something happened to the user interface (e.g. a view scrolled).
// /
// / These events are usually interpreted by assistive technologies to give the
// / user additional clues about the current state of the UI.
abstract class SemanticsEvent(
    // / The type of this event.
    // /
    // / The type is used by the engine to translate this event into the
    // / appropriate native event (`UIAccessibility*Notification` on iOS and
    // / `AccessibilityEvent` on Android).
    val type: String
) {

    // / Converts this event to a Map that can be encoded with
    // / [StandardMessageCodec].
    // /
    // / [nodeId] is the unique identifier of the semantics node associated with
    // / the event, or null if the event is not associated with a semantics node.
    fun toMap(nodeId: Int? = null): Map<String, Any?> {
        val event = hashMapOf(
            "type" to type,
            "data" to dataMap
        )
        if (nodeId != null) {
            event["nodeId"] = nodeId
        }

        return event
    }

    // / Returns the event's data object.
    abstract val dataMap: Map<String, Any?>

    override fun toString(): String {
        val pairs = mutableListOf<String>()
        val sortedKeys = dataMap.keys.toMutableList()
        sortedKeys.sort()
        for (key in sortedKeys) {
            pairs.add("$key: ${dataMap[key]}")
        }
        return "$javaClass(${pairs.joinToString(", ")})"
    }
}

// / An event for a semantic announcement.
// /
// / This should be used for announcement that are not seamlessly announced by
// / the system as a result of a UI state change.
// /
// / For example a camera application can use this method to make accessibility
// / announcements regarding objects in the viewfinder.
// /
// / When possible, prefer using mechanisms like [Semantics] to implicitly
// / trigger announcements over using this event.
class AnnounceSemanticsEvent(
    // / The message to announce.
    val message: String,
    // / Text direction for [message].
    val textDirection: TextDirection
) : SemanticsEvent("announce") {

    override val dataMap: Map<String, Any?>
        get() = mapOf(
            "message" to message,
            "textDirection" to textDirection.index
        )
}

// / An event for a semantic announcement of a tooltip.
// /
// / This is only used by Android to announce tooltip values.
class TooltipSemanticsEvent(
    // / The text content of the tooltip.
    val message: String
) : SemanticsEvent("tooltip") {

    override val dataMap: Map<String, Any?>
        get() = mapOf(
            "message" to message
        )
}

// / An event which triggers long press semantic feedback.
// /
// / Currently only honored on Android. Triggers a long-press specific sound
// / when TalkBack is enabled.
class LongPressSemanticsEvent : SemanticsEvent("longPress") {
    override val dataMap: Map<String, Any?>
        get() = mapOf()
}

// / An event which triggers tap semantic feedback.
// /
// / Currently only honored on Android. Triggers a tap specific sound when
// / TalkBack is enabled.
class TapSemanticEvent : SemanticsEvent("tap") {
    override val dataMap: Map<String, Any?>
        get() = mapOf()
}