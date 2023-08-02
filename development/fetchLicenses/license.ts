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

import { Request, Response } from 'express';
import puppeteer = require('puppeteer');
import { log } from './logger';
import { ContentNode } from './types';
import { PlainTextFormatter } from './plain_text_formatter';

const CHROME_LAUNCH_ARGS = ['--enable-dom-distiller'];

// A list of DOM Node types that are usually not useful in the context
// of fetching text content from the page.
type BannedNames = {
  [key: string]: true
};

/**
 * Handles the actual license request.
 */
export async function handleRequest(request: Request, response: Response) {
  const url = request.body.url;
  if (url) {
    try {
      log(`Handling license request for ${url}`);
      if (!isValidProtocol(url)) {
        response.status(400).send('Invalid request.');
        return;
      }

      const nodes = await handleLicenseRequest(url);
      const content = PlainTextFormatter.plainTextFor(nodes);
      response.status(200).send(content);
    } catch (error) {
      log('Error handling license request ', error);
      response.status(400).send('Something bad happened. Check the logs');
    }
  } else {
    response.status(400).send('URL required');
  }
}

/**
 * Validates the protocol. Only allows `https?` requests.
 * @param requestUrl The request url
 * @return `true` if the protocol is valid.
 */
function isValidProtocol(requestUrl: string): boolean {
  const url = new URL(requestUrl);
  if (url.protocol === 'https:') {
    // Allow https requests
    return true;
  } else if (url.protocol === 'http:') {
    // Allow http requests
    return true;
  } else {
    log(`Invalid protocol ${url.protocol}`);
    return false;
  }
}

async function handleLicenseRequest(url: string): Promise<ContentNode[]> {
  const browser = await puppeteer.launch({ args: CHROME_LAUNCH_ARGS });
  const page = await browser.newPage();
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  const content = await page.evaluate(() => {
    // A map of banned nodes
    const BANNED_LOCAL_NAMES: BannedNames = {
      'a': true,
      'button': true,
      'canvas': true,
      'footer': true,
      'header': true,
      'code': true,
      'img': true,
      'nav': true,
      'script': true,
      'style': true,
      'svg': true,
    };

    // node list handler
    function contentForNodeList(list: NodeList | null | undefined): ContentNode[] {
      const contentNodes: ContentNode[] = [];
      if (!list) {
        return contentNodes;
      }

      for (let i = 0; i < list.length; i += 1) {
        const node = contentForNode(list.item(i));
        if (node) {
          contentNodes.push(node);
        }
      }
      return contentNodes;
    }

    // content handler
    const contentWithPath = function (node: ContentNode, accumulator: ContentNode[]) {
      if (node.textContent && node.textContent.length > 0) {
        accumulator.push({ localName: node.localName, textContent: node.textContent });
      }
      if (node.children) {
        for (let i = 0; i < node.children.length; i += 1) {
          contentWithPath(node.children[i], accumulator);
        }
      }
    };

    // node handler
    function contentForNode(node: Node | null | undefined) {
      if (!node) {
        return null;
      }

      const name = node.nodeName.toLowerCase();
      // Check if node is banned.
      if (name && BANNED_LOCAL_NAMES[name] === true) {
        return null;
      }
      // Shallow clone node, as we are only interested in the textContent
      // of the node, and not the child nodes.
      const cloned = node.cloneNode();
      const localName = name;
      const textContent = cloned.textContent;
      const children = contentForNodeList(node.childNodes);
      return {
        localName: localName,
        textContent: textContent,
        children: children
      };
    }
    const body = document.querySelector('body');
    const nodes: ContentNode[] =
      body == null ? [] : contentForNodeList(body.childNodes);

    // Accumulate nodes with content
    const accumulator: ContentNode[] = [];
    for (let i = 0; i < nodes.length; i += 1) {
      const node = nodes[i];
      contentWithPath(node, accumulator);
    }
    return accumulator;
  });
  await browser.close();
  return content;
}
