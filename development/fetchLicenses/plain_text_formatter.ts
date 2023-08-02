/*
 * Copyright 2019 The Android Open Source Project
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

import { ContentNode } from './types';

export class PlainTextFormatter {
  static plainTextFor(nodes: ContentNode[]): string {
    const output: string[] = [];
    for (let i = 0; i < nodes.length; i += 1) {
      let node = nodes[i];
      if (!node.localName || node.localName.startsWith('#comment')) {
        // Ignore comments
        continue;
      }
      if (!node.textContent) {
        continue;
      }
      // Split the textContent into lines.
      // Analyze every line and check for
      // empty spaces (\s*) and new lines (\r?\n).
      let lines = node.textContent.split(/\r?\n/);
      let content: string[] = [];
      for (let i = 0; i < lines.length; i += 1) {
        if (!PlainTextFormatter.isEmpty(lines[i])) {
          content.push(lines[i]);
        }
      }
      let payload = content.join('\r\n');
      if (!PlainTextFormatter.isEmpty(payload)) {
        output.push(payload);
      }
    }
    return output.join('\r\n');
  }

  private static isEmpty(content: string): boolean {
    if (/^\s*$/.exec(content)) {
      return true;
    }
    if (/^r?\n$/.exec(content)) {
      return true;
    }
    return false;
  }
}
