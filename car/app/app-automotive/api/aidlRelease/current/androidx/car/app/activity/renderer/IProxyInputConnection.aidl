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
///////////////////////////////////////////////////////////////////////////////
// THIS FILE IS IMMUTABLE. DO NOT EDIT IN ANY CASE.                          //
///////////////////////////////////////////////////////////////////////////////

// This file is a snapshot of an AIDL file. Do not edit it manually. There are
// two cases:
// 1). this is a frozen version file - do not edit this in any case.
// 2). this is a 'current' file. If you make a backwards compatible change to
//     the interface (from the latest frozen version), the build system will
//     prompt you to update this file with `m <name>-update-api`.
//
// You must not make a backward incompatible change to any AIDL file built
// with the aidl_interface module type with versions property set. The module
// type is used to build AIDL files in a way that they can be used across
// independently updatable components of the system. If a device is shipped
// with such a backward incompatible change, it has a high risk of breaking
// later when a module using the interface is updated, e.g., Mainline modules.

package androidx.car.app.activity.renderer;
/* @hide */
interface IProxyInputConnection {
  CharSequence getTextBeforeCursor(int length, int flags) = 1;
  CharSequence getTextAfterCursor(int length, int flags) = 2;
  CharSequence getSelectedText(int flags) = 3;
  int getCursorCapsMode(int reqModes) = 4;
  boolean deleteSurroundingText(int beforeLength, int afterLength) = 5;
  boolean setComposingText(CharSequence text, int newCursorPosition) = 6;
  boolean setComposingRegion(int start, int end) = 7;
  boolean finishComposingText() = 8;
  boolean commitText(CharSequence text, int newCursorPosition) = 9;
  boolean setSelection(int start, int end) = 10;
  boolean performEditorAction(int editorAction) = 11;
  boolean performContextMenuAction(int id) = 12;
  boolean beginBatchEdit() = 13;
  boolean endBatchEdit() = 14;
  boolean sendKeyEvent(in android.view.KeyEvent event) = 15;
  boolean clearMetaKeyStates(int states) = 16;
  boolean reportFullscreenMode(boolean enabled) = 17;
  boolean performPrivateCommand(String action, in android.os.Bundle data) = 18;
  boolean requestCursorUpdates(int cursorUpdateMode) = 19;
  boolean commitCorrection(in android.view.inputmethod.CorrectionInfo correctionInfo) = 20;
  boolean commitCompletion(in android.view.inputmethod.CompletionInfo text) = 21;
  android.view.inputmethod.ExtractedText getExtractedText(in android.view.inputmethod.ExtractedTextRequest request, int flags) = 22;
  void closeConnection() = 23;
  android.view.inputmethod.EditorInfo getEditorInfo() = 24;
}
