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

import unittest
import os
from update_versions_for_release import *

class TestVersionUpdates(unittest.TestCase):

    def test_increment_version(self):
        new_version = increment_version("1.0.0-alpha01")
        self.assertEqual("1.0.0-alpha02", new_version)

        new_version = increment_version("1.1.0-alpha01")
        self.assertEqual("1.1.0-alpha02", new_version)

        new_version = increment_version("1.0.0-alpha19")
        self.assertEqual("1.0.0-alpha20", new_version)

        new_version = increment_version("1.0.0-rc01")
        self.assertEqual("1.1.0-alpha01", new_version)

        new_version = increment_version("1.3.0-beta02")
        self.assertEqual("1.3.0-beta03", new_version)

        new_version = increment_version("1.0.1")
        self.assertEqual("1.1.0-alpha01", new_version)

    def test_get_higher_version(self):
        higher_version = get_higher_version("1.0.0-alpha01", "1.0.0-alpha02")
        self.assertEqual("1.0.0-alpha02", higher_version)

        higher_version = get_higher_version("1.0.0-alpha02", "1.0.0-alpha01")
        self.assertEqual("1.0.0-alpha02", higher_version)

        higher_version = get_higher_version("1.0.0-alpha02", "1.0.0-alpha02")
        self.assertEqual("1.0.0-alpha02", higher_version)

        higher_version = get_higher_version("1.1.0-alpha01", "1.0.0-alpha02")
        self.assertEqual("1.1.0-alpha01", higher_version)

        higher_version = get_higher_version("1.0.0-rc05", "1.2.0-beta02")
        self.assertEqual("1.2.0-beta02", higher_version)

        higher_version = get_higher_version("1.3.0-beta01", "1.5.0-beta01")
        self.assertEqual("1.5.0-beta01", higher_version)

        higher_version = get_higher_version("3.0.0-alpha01", "1.0.0-alpha02")
        self.assertEqual("3.0.0-alpha01", higher_version)

        higher_version = get_higher_version("1.0.0-beta01", "1.0.0-rc01")
        self.assertEqual("1.0.0-rc01", higher_version)

        higher_version = get_higher_version("1.4.0-beta01", "1.0.2")
        self.assertEqual("1.4.0-beta01", higher_version)

        higher_version = get_higher_version("1.4.0-beta01", "1.4.2")
        self.assertEqual("1.4.2", higher_version)

        higher_version = get_higher_version("1.4.0", "1.4.2")
        self.assertEqual("1.4.2", higher_version)

    def test_should_update_version_in_library_versions_kt(self):
        generic_line = "    val CONTENTPAGER = Version(\"1.1.0-alpha01\")"
        compose_line = "    val COMPOSE = Version(System.getenv(\"COMPOSE_CUSTOM_VERSION\") ?: \"1.0.0-beta04\")"
        self.assertTrue(should_update_version_in_library_versions_kt(generic_line, "1.1.0-alpha02"))
        self.assertTrue(should_update_version_in_library_versions_kt(generic_line, "1.3.0-alpha01"))
        self.assertFalse(should_update_version_in_library_versions_kt(generic_line, "1.0.0-alpha01"))

        self.assertTrue(should_update_version_in_library_versions_kt(compose_line, "1.1.0-alpha02"))
        self.assertTrue(should_update_version_in_library_versions_kt(compose_line, "1.3.0-alpha01"))
        self.assertFalse(should_update_version_in_library_versions_kt(compose_line, "1.0.0-alpha01"))



if __name__ == '__main__':
    unittest.main()