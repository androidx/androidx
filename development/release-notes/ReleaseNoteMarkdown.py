#!/usr/bin/python3
#
# Copyright (C) 2020 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from enum import Enum
import datetime

## Classes for generating markdown

class HeaderType(Enum):
    H1 = 1
    H2 = 2
    H3 = 3
    H4 = 4
    H5 = 5
    H6 = 6

def getHeaderTag(headerType):
    if headerType == HeaderType.H1: return "#"
    if headerType == HeaderType.H2: return "##"
    if headerType == HeaderType.H3: return "###"
    if headerType == HeaderType.H4: return "####"
    if headerType == HeaderType.H5: return "#####"
    if headerType == HeaderType.H6: return "######"

class MarkdownHeader:
    def __init__(self, markdownHeaderType=HeaderType.H1, text=""):
        self.markdownType = markdownHeaderType
        self.text = text
    def __str__(self):
        return getHeaderTag(self.markdownType) + ' ' + self.text

class MarkdownLink:
    def __init__(self, linkText="", linkUrl=""):
        self.linkText = linkText
        self.linkUrl = linkUrl
    def __str__(self):
        return "([%s](%s))" % (self.linkText, self.linkUrl)

class MarkdownBoldText:
    def __init__(self, inputText=""):
        self.inputText = inputText
    def __str__(self):
        return "**%s**" % (self.inputText)

class MarkdownComment:
    def __init__(self, inputText=""):
        self.inputText = inputText
    def __str__(self):
        return "{# %s #}" % (self.inputText)

class MarkdownDate:
    def __init__(self, inputDate="01-01-1970"):
        self.date = datetime.datetime.strptime(inputDate, "%m-%d-%Y")
    def __str__(self):
        return self.date.strftime("%B %-d, %Y")


