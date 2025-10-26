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
import { transformUrl } from './url-transforms';

const CHROME_LAUNCH_ARGS = ['--enable-dom-distiller'];

// Only allow requests to these hostnames (add more as required)
const ALLOWED_HOSTNAMES = [
  "github.com",
  "raw.githubusercontent.com",
  // add more allowed domains as needed
];
// Import punycode for normalizing potential Unicode hostnames
import * as punycode from 'punycode/';

/**
 * Returns true if the hostname is a public (non-private, non-loopback) domain and explicitly in our allowlist.
 */
function isAllowedHost(requestUrl: string): boolean {
  try {
    const { hostname } = new URL(requestUrl);
    // Normalize hostname for Unicode/domain tricks
    const normalizedHostname = punycode.toASCII(hostname).toLowerCase();

    // Block IP addresses (especially local, private, loopback)
    if (isIpAddress(normalizedHostname)) {
      log(`IP address not allowed in host check: ${normalizedHostname}`);
      return false;
    }

    // Strict comparison: only allow exactly the listed domains
    return ALLOWED_HOSTNAMES.some(allowed => normalizedHostname === allowed);
  } catch (e) {
    log(`Failed to parse URL in host check: ${requestUrl}`);
    return false;
  }
}

/**
 * Returns true if the input string is an IPv4 or IPv6 address.
 */
function isIpAddress(host: string): boolean {
  // Matches IPv4 addresses
  if (/^(?:\d{1,3}\.){3}\d{1,3}$/.test(host)) {
    // Check for private ranges
    const parts = host.split('.').map(Number);
    if (
      parts[0] === 10 || // 10.0.0.0/8
      (parts[0] === 172 && parts[1] >= 16 && parts[1] <= 31) || // 172.16.0.0 - 172.31.255.255
      (parts[0] === 192 && parts[1] === 168) || // 192.168.0.0/16
      parts[0] === 127 // 127.0.0.1/8 loopback
    ) {
      return true;
    }
    return false;
  }
  // Matches IPv6 addresses (incl. ::1 loopback or fc00::/7 unique local)
  if (/^[a-f0-9:]+$/i.test(host)) {
    if (
      host === '::1' || // loopback 
      host.startsWith('fc') || host.startsWith('fd') // unique local
    ) {
      return true;
    }
    return false;
  }
  return false;
}

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
      if (!isAllowedHost(url)) {
        response.status(400).send('Host not allowed.');
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

async function handleLicenseRequest(url: string, enableLocalDebugging: boolean = false): Promise<ContentNode[]> {
  const transformed = transformUrl(url);
  if (url !== transformed) {
    log(`Transformed request url to ${transformed}`);
  }
  // Validate the transformed URL to prevent SSRF
  if (!isValidProtocol(transformed)) {
    throw new Error('Invalid protocol in transformed URL');
  }
  if (!isAllowedHost(transformed)) {
    throw new Error('Transformed URL host not allowed.');
  }
  // Defensive: Extract hostname again after transformation and validate strictly
  const finalHostname = new URL(transformed).hostname.toLowerCase();
  const allowed = ALLOWED_HOSTNAMES.some(h => h.toLowerCase() === finalHostname);
  if (!allowed) {
    throw new Error('Transformed URL host not in allowlist (final check).');
  }
  const browser = await puppeteer.launch({
    args: CHROME_LAUNCH_ARGS,
    devtools: enableLocalDebugging,
    // https://developer.chrome.com/articles/new-headless/
    headless: true
  });
  const page = await browser.newPage();
  if (enableLocalDebugging) {
    page.on('console', (message) => {
      log(`Puppeteer: ${message.text()}`);
    });
  }
  await page.goto(transformed, { waitUntil: 'domcontentloaded' });
  const content = await page.evaluate(() => {
    // A map of banned nodes
    const BANNED_LOCAL_NAMES: BannedNames = {
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
      // Handle elements of different types
      if (cloned instanceof HTMLAnchorElement) {
        // anchor element
        // Ensure that it has reasonable href content
        const href = cloned.href;
        if (href.length <= 0 || href === '#') {
          return null;
        }
      }
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
