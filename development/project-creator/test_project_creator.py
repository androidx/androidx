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
from create_project import *

class TestNewDirectory(unittest.TestCase):

    def test_package_name(self):
        package = generate_package_name("androidx.foo", "foo")
        self.assertEqual("androidx.foo", package)

        package = generate_package_name("androidx.foo", "foo-bar")
        self.assertEqual("androidx.foo.bar", package)

        package = generate_package_name("androidx.foo.bar", "bar")
        self.assertEqual("androidx.foo.bar", package)

        package = generate_package_name("androidx.foo.bar", "bar-qux")
        self.assertEqual("androidx.foo.bar.qux", package)

    def test_name_correctness(self):
        self.assertFalse(validate_name("foo", "bar"))
        self.assertFalse(validate_name("foo", "foo"))
        self.assertFalse(validate_name("androidx.foo", "bar"))
        self.assertFalse(validate_name("androidx.foo", "bar-qux"))
        self.assertFalse(validate_name("androidx.foo.bar", "foo"))
        self.assertTrue(validate_name("androidx.foo", "foo"))
        self.assertTrue(validate_name("androidx.foo", "foo-bar"))
        self.assertTrue(validate_name("androidx.foo.bar", "bar"))
        self.assertTrue(validate_name("androidx.foo.bar", "bar-qux"))

    def test_full_directory_name(self):
        full_fp = get_full_artifact_path("androidx.foo", "foo")
        self.assertTrue(full_fp.endswith("frameworks/support/foo/foo"))

        full_fp = get_full_artifact_path("androidx.foo", "foo-bar")
        self.assertTrue(full_fp.endswith("frameworks/support/foo/foo-bar"))

        full_fp = get_full_artifact_path("androidx.foo.bar", "bar")
        self.assertTrue(full_fp.endswith("frameworks/support/foo/bar/bar"))

        full_fp = get_full_artifact_path("androidx.foo.bar", "bar-qux")
        self.assertTrue(full_fp.endswith("frameworks/support/foo/bar/bar-qux"))

    def test_get_package_info_file_dir(self):
        package_info_dir_fp = get_package_info_file_dir("androidx.foo", "foo")
        frameworks_support_fp = os.path.abspath(os.path.join(os.getcwd(), '..', '..'))
        self.assertEqual(frameworks_support_fp + "/foo/foo/src/main/androidx/foo", package_info_dir_fp)

        package_info_dir_fp = get_package_info_file_dir("androidx.foo", "foo-bar")
        self.assertEqual(frameworks_support_fp + "/foo/foo-bar/src/main/androidx/foo", package_info_dir_fp)

        package_info_dir_fp = get_package_info_file_dir("androidx.foo.bar", "bar")
        self.assertEqual(frameworks_support_fp + "/foo/bar/bar/src/main/androidx/foo/bar", package_info_dir_fp)

        package_info_dir_fp = get_package_info_file_dir("androidx.foo.bar", "bar-qux")
        self.assertEqual(frameworks_support_fp + "/foo/bar/bar-qux/src/main/androidx/foo/bar", package_info_dir_fp)

    def test_group_id_directory_name(self):
        full_fp = get_group_id_path("androidx.foo")
        self.assertTrue(full_fp.endswith("frameworks/support/foo"))

        full_fp = get_group_id_path("androidx.foo")
        self.assertTrue(full_fp.endswith("frameworks/support/foo"))

        full_fp = get_group_id_path("androidx.foo.bar")
        self.assertTrue(full_fp.endswith("frameworks/support/foo/bar"))

        full_fp = get_group_id_path("androidx.foo.bar")
        self.assertTrue(full_fp.endswith("frameworks/support/foo/bar"))


class TestSettingsGradle(unittest.TestCase):

    def test_settings_gradle_line(self):
        line = get_new_settings_gradle_line("androidx.foo", "foo")
        self.assertEqual("includeProject(\":foo:foo\", \"foo/foo\", " + \
                         "[BuildType.MAIN])\n", line)

        line = get_new_settings_gradle_line("androidx.foo", "foo-bar")
        self.assertEqual("includeProject(\":foo:foo-bar\", \"foo/foo-bar\", " + \
                         "[BuildType.MAIN])\n", line)

        line = get_new_settings_gradle_line("androidx.foo.bar", "bar")
        self.assertEqual("includeProject(\":foo:bar:bar\", \"foo/bar/bar\", " + \
                         "[BuildType.MAIN])\n", line)

        line = get_new_settings_gradle_line("androidx.foo.bar", "bar-qux")
        self.assertEqual("includeProject(\":foo:bar:bar-qux\", \"foo/bar/bar-qux\", " + \
                         "[BuildType.MAIN])\n", line)

        line = get_new_settings_gradle_line("androidx.compose", "compose-foo")
        self.assertEqual("includeProject(\":compose:compose-foo\", \"compose/compose-foo\", " + \
                         "[BuildType.COMPOSE])\n", line)

        line = get_new_settings_gradle_line("androidx.compose.foo", "foo-bar")
        self.assertEqual("includeProject(\":compose:foo:foo-bar\", \"compose/foo/foo-bar\", " + \
                         "[BuildType.COMPOSE])\n", line)

        line = get_new_settings_gradle_line("androidx.foo.bar", "bar-compose")
        self.assertEqual("includeProject(\":foo:bar:bar-compose\", \"foo/bar/bar-compose\", " + \
                         "[BuildType.COMPOSE])\n", line)

    def test_gradle_project_coordinates(self):
        coordinates = get_gradle_project_coordinates("androidx.foo", "foo")
        self.assertEqual(":foo:foo", coordinates)

        coordinates = get_gradle_project_coordinates("androidx.foo", "foo-bar")
        self.assertEqual(":foo:foo-bar", coordinates)

        coordinates = get_gradle_project_coordinates("androidx.foo.bar", "bar")
        self.assertEqual(":foo:bar:bar", coordinates)

        coordinates = get_gradle_project_coordinates("androidx.foo.bar", "bar-qux")
        self.assertEqual(":foo:bar:bar-qux", coordinates)

class TestBuildGradle(unittest.TestCase):
    def test_correct_library_type_is_returned(self):
        library_type = get_library_type("foo-samples")
        self.assertEqual("SAMPLES", library_type)

        library_type = get_library_type("foo-compiler")
        self.assertEqual("ANNOTATION_PROCESSOR", library_type)

        library_type = get_library_type("foo-lint")
        self.assertEqual("LINT", library_type)

        library_type = get_library_type("foo-inspection")
        self.assertEqual("IDE_PLUGIN", library_type)

        library_type = get_library_type("foo")
        self.assertEqual("PUBLISHED_LIBRARY", library_type)

        library_type = get_library_type("foo-inspect")
        self.assertEqual("PUBLISHED_LIBRARY", library_type)

        library_type = get_library_type("foocomp")
        self.assertEqual("PUBLISHED_LIBRARY", library_type)

        library_type = get_library_type("foo-bar")
        self.assertEqual("PUBLISHED_LIBRARY", library_type)


class TestDocsTipOfTree(unittest.TestCase):

    def test_docs_tip_of_tree_build_grade_line(self):
        line = get_new_docs_tip_of_tree_build_grade_line("androidx.foo", "foo")
        self.assertEqual("    docs(project(\":foo:foo\"))\n", line)

        line = get_new_docs_tip_of_tree_build_grade_line("androidx.foo", "foo-bar")
        self.assertEqual("    docs(project(\":foo:foo-bar\"))\n", line)

        line = get_new_docs_tip_of_tree_build_grade_line("androidx.foo.bar", "bar")
        self.assertEqual("    docs(project(\":foo:bar:bar\"))\n", line)

        line = get_new_docs_tip_of_tree_build_grade_line("androidx.foo.bar", "bar-qux")
        self.assertEqual("    docs(project(\":foo:bar:bar-qux\"))\n", line)

        line = get_new_docs_tip_of_tree_build_grade_line("androidx.foo", "foo-samples")
        self.assertEqual("    samples(project(\":foo:foo-samples\"))\n", line)

        line = get_new_docs_tip_of_tree_build_grade_line("androidx.foo.bar", "bar-qux-samples")
        self.assertEqual("    samples(project(\":foo:bar:bar-qux-samples\"))\n", line)

class TestReplacements(unittest.TestCase):

    def test_version_macro(self):
        macro = get_group_id_version_macro("androidx.foo")
        self.assertEqual("FOO", macro)

        macro = get_group_id_version_macro("androidx.foo.bar")
        self.assertEqual("FOO_BAR", macro)

        macro = get_group_id_version_macro("androidx.compose.bar")
        self.assertEqual("BAR", macro)

        macro = get_group_id_version_macro("androidx.compose.foo.bar")
        self.assertEqual("FOO_BAR", macro)

        macro = get_group_id_version_macro("androidx.compose")
        self.assertEqual("COMPOSE", macro)

    def test_sed(self):
        out_dir = "./out"
        test_file = out_dir + "/temp.txt"
        test_file_contents = "a\nb\nc"
        if not os.path.exists(out_dir):
            os.makedirs(out_dir)
        with open(test_file,"w") as f:
           f.write("a\nb\nc")
        sed("a", "d", test_file)

        # write back the file
        with open(test_file) as f:
           file_contents = f.read()
        self.assertEqual("d\nb\nc", file_contents)
        rm(out_dir)

    def test_mv_dir_within_same_dir(self):
        src_out_dir = "./src_out"
        test_src_file = src_out_dir + "/temp.txt"
        test_file_contents = "a\nb\nc"
        if not os.path.exists(src_out_dir):
            os.makedirs(src_out_dir)
        with open(test_src_file,"w") as f:
           f.write("a\nb\nc")

        dst_out_dir = "./dst_out"
        mv_dir(src_out_dir, dst_out_dir)
        # write back the file
        with open(dst_out_dir + "/temp.txt") as f:
           file_contents = f.read()
        self.assertEqual("a\nb\nc", file_contents)
        rm(src_out_dir)
        rm(dst_out_dir)

    def test_mv_dir_to_different_dir(self):
        src_out_dir = "./src_out_2"
        test_src_file = src_out_dir + "/temp.txt"
        test_file_contents = "a\nb\nc"
        if not os.path.exists(src_out_dir):
            os.makedirs(src_out_dir)
        with open(test_src_file,"w") as f:
           f.write("a\nb\nc")

        dst_out_dir_parent = "./dst_out_2"
        dst_out_dir = dst_out_dir_parent + "/hello/world"
        mv_dir(src_out_dir, dst_out_dir)
        # write back the file
        with open(dst_out_dir + "/temp.txt") as f:
           file_contents = f.read()
        self.assertEqual("a\nb\nc", file_contents)
        rm(src_out_dir)
        rm(dst_out_dir_parent)

    def test_remove_line(self):
        out_dir = "./out"
        test_file = out_dir + "/temp.txt"
        test_file_contents = "a\nb\nc"
        if not os.path.exists(out_dir):
            os.makedirs(out_dir)

        with open(test_file,"w") as f:
           f.write("a\nb\nc")
        remove_line("b", test_file)
        # read back the file and check
        with open(test_file) as f:
           file_contents = f.read()
        self.assertEqual("a\nc", file_contents)

        with open(test_file,"w") as f:
           f.write("abc\ndef\nghi")
        remove_line("c", test_file)
        # read back the file and check
        with open(test_file) as f:
           file_contents = f.read()
        self.assertEqual("def\nghi", file_contents)

        # Clean up
        rm(out_dir)


class TestLibraryGroupKt(unittest.TestCase):

    def test_library_group_atomicity_is_correctly_determined(self):
        self.assertFalse(is_group_id_atomic("androidx.core"))
        self.assertFalse(is_group_id_atomic("androidx.foo"))
        self.assertFalse(is_group_id_atomic(""))
        self.assertFalse(is_group_id_atomic("androidx.compose.foo"))
        self.assertTrue(is_group_id_atomic("androidx.cardview"))
        self.assertTrue(is_group_id_atomic("androidx.tracing"))
        self.assertTrue(is_group_id_atomic("androidx.compose.foundation"))


if __name__ == '__main__':
    unittest.main()