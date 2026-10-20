#!/usr/bin/env python3
"""Unit tests for PreToolUse auto-formatting hook (.agents/scripts/cog_format_modified.py)."""

import io
import json
from pathlib import Path
import subprocess
import sys
import unittest
from unittest.mock import patch

_SCRIPTS_DIR = str(Path(__file__).resolve().parent)
if _SCRIPTS_DIR not in sys.path:
    sys.path.insert(0, _SCRIPTS_DIR)

import cog_format_modified


class TestCogFormatModified(unittest.TestCase):
    FAKE_COG_ROOT = Path("/google/cog/cloud/user/ws/android/frameworks/support")
    FAKE_SETTINGS = 'includeProject(":wear:compose:compose-material3-samples", "wear/compose/compose-material3/samples")\n'

    def _run_hook(self, payload: dict) -> dict:
        stdin = io.StringIO(json.dumps(payload))
        stdout = io.StringIO()
        with patch("sys.stdin", stdin), patch("sys.stdout", stdout):
            rc = cog_format_modified.main()
        self.assertEqual(rc, 0)
        return json.loads(stdout.getvalue())

    def test_repo_root_points_to_support_root_with_gradlew(self):
        self.assertTrue((cog_format_modified.REPO_ROOT / "gradlew").is_file())

    def test_skips_unrelated_commands_without_running_subprocesses(self):
        for cmd in (
            "ls -la",
            "cat commit.py",
            "python3 test_commit.py",
            "python3 scripts/test_commit.py",
            "python3 -m unittest test_commit.py",
            "cog_api.py publish",
        ):
            with patch("cog_format_modified.REPO_ROOT", self.FAKE_COG_ROOT), patch("subprocess.run") as mock_run:
                res = self._run_hook({"toolCall": {"name": "run_command", "args": {"CommandLine": cmd}}})
            self.assertEqual(res, {"decision": "allow"})
            mock_run.assert_not_called()

    def test_skips_non_cog_workspace(self):
        with patch("cog_format_modified.REPO_ROOT", Path("/home/user/androidx/frameworks/support")), patch("subprocess.run") as mock_run:
            res = self._run_hook(
                {"toolCall": {"name": "run_command", "args": {"CommandLine": "python3 commit.py -m msg"}}}
            )
        self.assertEqual(res, {"decision": "allow"})
        mock_run.assert_not_called()

    def test_direct_cli_with_empty_non_tty_stdin_formats_and_returns_exit_code(self):
        diff = "Diffing abc..@\n[M] wear/compose/compose-material3/samples/src/main/java/Sample.java\n"
        ok = subprocess.CompletedProcess(args=[], returncode=0, stdout="", stderr="")
        with (
            patch("cog_format_modified.REPO_ROOT", self.FAKE_COG_ROOT),
            patch.object(Path, "read_text", return_value=self.FAKE_SETTINGS),
            patch("sys.stdin", io.StringIO("")),
            patch("sys.stdout", io.StringIO()) as stdout,
            patch("subprocess.run", side_effect=[subprocess.CompletedProcess([], 0, diff, ""), ok]) as mock_run,
        ):
            rc = cog_format_modified.main()

        self.assertEqual(rc, 0)
        self.assertEqual(stdout.getvalue(), "")
        java_call = mock_run.call_args_list[1]
        self.assertEqual(java_call.args[0], ["./gradlew", "-q", "--continue", ":wear:compose:compose-material3-samples:javaFormat"])
        self.assertEqual(java_call.kwargs["env"]["PROJECT_PREFIX"], ":wear:compose:compose-material3-samples")

    def test_formats_kotlin_and_java_files_on_commit(self):
        diff = (
            "Diffing abc..@\n"
            "[M] wear/compose/compose-material3/src/main/java/Slider.kt\n"
            "[A*] wear/wear-input/src/main/java/Foo.java\n"
            "[M] buildSrc/private/src/main/kotlin/Build.kt\n"
            "[D] wear/compose/Deleted.kt\n"
        )
        ok = subprocess.CompletedProcess(args=[], returncode=0, stdout="", stderr="")
        with (
            patch("cog_format_modified.REPO_ROOT", self.FAKE_COG_ROOT),
            patch.object(Path, "read_text", return_value=self.FAKE_SETTINGS),
            patch("subprocess.run", side_effect=[subprocess.CompletedProcess([], 0, diff, ""), ok, ok]) as mock_run,
        ):
            res = self._run_hook(
                {"toolCall": {"name": "run_command", "args": {"CommandLine": "/path/to/commit.py -m 'Commit msg'"}}}
            )

        self.assertEqual(res, {"decision": "allow"})
        kt_call, java_call = mock_run.call_args_list[1], mock_run.call_args_list[2]
        self.assertEqual(
            kt_call.args[0],
            [
                "./gradlew",
                "-q",
                "--continue",
                ":ktCheckFile",
                "--format",
                "--file=wear/compose/compose-material3/src/main/java/Slider.kt",
                "--file=buildSrc/private/src/main/kotlin/Build.kt",
            ],
        )
        self.assertEqual(
            kt_call.kwargs["env"]["PROJECT_PREFIX"],
            ":wear:wear-input",
        )
        self.assertEqual(
            java_call.args[0],
            ["./gradlew", "-q", "--continue", ":wear:wear-input:javaFormat"],
        )

    def test_denies_commit_when_formatting_fails(self):
        diff = "Diffing abc..@\n[M] wear/compose/compose-material3/src/main/java/Slider.kt\n"
        fail = subprocess.CompletedProcess(
            args=[], returncode=1, stdout="Error: syntax error", stderr="FAILURE: Build failed"
        )
        with (
            patch("cog_format_modified.REPO_ROOT", self.FAKE_COG_ROOT),
            patch("subprocess.run", side_effect=[subprocess.CompletedProcess([], 0, diff, ""), fail]) as mock_run,
        ):
            res = self._run_hook(
                {"toolCall": {"name": "run_command", "args": {"CommandLine": "python3 /path/to/cog_api.py commit -m msg"}}}
            )

        self.assertEqual(res["decision"], "deny")
        self.assertIn("Error: syntax error", res["reason"])
        self.assertIn("FAILURE: Build failed", res["reason"])
        self.assertEqual(mock_run.call_args_list[1].kwargs["env"]["PROJECT_PREFIX"], ":lint-checks")


if __name__ == "__main__":
    unittest.main()
