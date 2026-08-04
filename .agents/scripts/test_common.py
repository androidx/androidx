#!/usr/bin/env python3
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch, MagicMock

import common
from common import HostEnvironment, WorkspaceContext


class TestCommon(unittest.TestCase):

    def test_is_cog_workspace(self):
        self.assertTrue(common.is_cog_workspace(Path("/google/cog/cloud/user/cl_123/android/frameworks/support")))
        self.assertFalse(common.is_cog_workspace(Path("/usr/local/google/home/user/Desktop/androidx-main/frameworks/support")))

    def test_is_already_isolated_workspace(self):
        self.assertTrue(common.is_already_isolated_workspace(Path("/google/cog/cloud/user/cl_123/android/frameworks/support")))
        self.assertTrue(common.is_already_isolated_workspace(Path("/fake/checkout/.worktrees/cl_123/frameworks/support")))
        self.assertTrue(common.is_already_isolated_workspace(Path("/home/user/.gemini/jetski/brain/123/.system_generated/worktrees/subagent-A")))
        self.assertFalse(common.is_already_isolated_workspace(Path("/usr/local/google/home/user/Desktop/androidx-main/frameworks/support")))

    def test_get_worktree_root(self):
        root = common.get_worktree_root(Path("/fake/checkout/frameworks/support"))
        self.assertEqual(root, Path("/fake/checkout"))

        root_cog = common.get_worktree_root(Path("/google/cog/cloud/user/cl_123/android/frameworks/support"))
        self.assertEqual(root_cog, Path("/google/cog/cloud/user/cl_123/android"))

        root_wt = common.get_worktree_root(Path("/fake/checkout/.worktrees/cl_123/frameworks/support"))
        self.assertEqual(root_wt, Path("/fake/checkout"))

        root_sg = common.get_worktree_root(Path("/home/user/.gemini/jetski/brain/123/.system_generated/worktrees/subagent-A"))
        self.assertEqual(root_sg, Path("/home/user/.gemini/jetski/brain/123"))

    def test_find_main_checkout_root_direct_prebuilts(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            main_dir = Path(tmpdir) / "main"
            support_dir = main_dir / "frameworks" / "support"
            prebuilts_dir = main_dir / "prebuilts"
            support_dir.mkdir(parents=True)
            prebuilts_dir.mkdir(parents=True)

            res = common.find_main_checkout_root(support_dir)
            self.assertEqual(res, main_dir)

    def test_find_main_checkout_root_script_location_fallback(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            subagent_dir = Path(tmpdir) / ".system_generated" / "worktrees" / "subagent-A"
            subagent_dir.mkdir(parents=True)

            res = common.find_main_checkout_root(subagent_dir)
            self.assertTrue((res / "prebuilts").exists())

    def test_find_main_checkout_root_git_common_dir(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            main_dir = Path(tmpdir) / "main_checkout"
            prebuilts_dir = main_dir / "prebuilts"
            git_common_dir = main_dir / ".repo" / "projects" / "frameworks" / "support.git"
            subagent_dir = Path(tmpdir) / "brain" / "conv1" / ".system_generated" / "worktrees" / "subagent-A"

            prebuilts_dir.mkdir(parents=True)
            git_common_dir.mkdir(parents=True)
            subagent_dir.mkdir(parents=True)

            with patch("subprocess.run") as mock_run:
                mock_run.return_value.returncode = 0
                mock_run.return_value.stdout = str(git_common_dir)
                res = common.find_main_checkout_root(subagent_dir)
                self.assertEqual(res.resolve(), main_dir.resolve())

    def test_find_main_checkout_root_mac_git_clone(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            main_dir = Path(tmpdir) / "androidx-main"
            support_dir = main_dir / "frameworks" / "support"
            prebuilts_dir = main_dir / "prebuilts"
            git_dir = support_dir / ".git"

            support_dir.mkdir(parents=True)
            prebuilts_dir.mkdir(parents=True)
            git_dir.mkdir(parents=True)

            with patch("subprocess.run") as mock_run:
                mock_run.return_value.returncode = 0
                mock_run.return_value.stdout = ".git"
                res = common.find_main_checkout_root(support_dir)
                self.assertEqual(res.resolve(), main_dir.resolve())

    def test_ensure_sibling_symlinks(self):
        with tempfile.TemporaryDirectory() as tmpdir:
            main_root = Path(tmpdir) / "main"
            main_root.mkdir()
            (main_root / "prebuilts").mkdir()
            (main_root / "tools").mkdir()

            worktree_dir = Path(tmpdir) / ".worktrees" / "cl_123" / "frameworks" / "support"
            worktree_dir.mkdir(parents=True)

            common.ensure_sibling_symlinks(worktree_dir, main_root)

            target_parent = worktree_dir.parent.parent
            self.assertTrue((target_parent / "prebuilts").is_symlink())
            self.assertTrue((target_parent / "tools").is_symlink())
            self.assertEqual((target_parent / "prebuilts").resolve(), (main_root / "prebuilts").resolve())
            self.assertEqual((target_parent / "tools").resolve(), (main_root / "tools").resolve())

    def test_find_merge_base_dynamically_repo_manifest_ref(self):
        """Verifies find_merge_base_dynamically inspects refs/remotes/m/* refs dynamically."""
        with tempfile.TemporaryDirectory() as tmpdir:
            repo_dir = Path(tmpdir)

            def mock_run(cmd, **kwargs):
                cmd_str = " ".join(cmd)
                mock_res = MagicMock()
                if "for-each-ref" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "m/androidx-main\n"
                    return mock_res
                elif "merge-base" in cmd_str and "m/androidx-main" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "abc123sha\n"
                    return mock_res
                mock_res.returncode = 1
                mock_res.stdout = ""
                return mock_res

            with patch("common._run", side_effect=mock_run):
                mb = common.find_merge_base_dynamically(repo_dir)
                self.assertEqual(mb, "abc123sha")

    def test_find_merge_base_dynamically_raises_when_no_ref(self):
        """Verifies find_merge_base_dynamically strictly raises RuntimeError when no repo manifest ref is found."""
        with tempfile.TemporaryDirectory() as tmpdir:
            repo_dir = Path(tmpdir)

            def mock_run(cmd, **kwargs):
                mock_res = MagicMock()
                mock_res.returncode = 1
                mock_res.stdout = ""
                return mock_res

            with patch("common._run", side_effect=mock_run):
                with self.assertRaises(RuntimeError):
                    common.find_merge_base_dynamically(repo_dir)

    def test_sync_workspace_to_source_multi_commit_binary_diff(self):
        """Verifies sync_workspace_to_source uses dynamic merge-base and binary diffing."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "src"
            cl_dir = Path(tmpdir) / "cl_dir"
            src_dir.mkdir()
            cl_dir.mkdir()

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=src_dir)
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=src_dir, is_cog_workspace=True, conversation_id="123", env=env)
            provider = common.CogWorkspaceProvider()

            def mock_run(cmd, **kwargs):
                cmd_str = " ".join(cmd)
                mock_res = MagicMock()
                if "for-each-ref" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "m/androidx-main\n"
                    return mock_res
                elif "merge-base" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "base_commit_sha\n"
                    return mock_res
                elif "diff" in cmd_str and "--name-status" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "M\tfile.txt\n"
                    return mock_res
                elif "diff" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "diff_content\n"
                    return mock_res
                elif "apply" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = ""
                    return mock_res
                elif "log" in cmd_str and "--grep=Merge " in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "base_commit_sha\n"
                    return mock_res
                elif "log" in cmd_str and "--format=%H" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = ""
                    return mock_res
                elif "log" in cmd_str:
                    mock_res.returncode = 0
                    mock_res.stdout = "commit msg\n"
                    return mock_res
                mock_res.returncode = 0
                mock_res.stdout = ""
                return mock_res

            with patch("common._run", side_effect=mock_run), patch("common._run_bytes", side_effect=mock_run) as mock_rb, patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)

                diff_calls = [call for call in mock_rb.call_args_list if "diff" in call[0][0]]
                self.assertTrue(len(diff_calls) > 0)
                diff_cmd = diff_calls[0][0][0]
                self.assertIn("--binary", diff_cmd)
                self.assertIn("base_commit_sha", diff_cmd)

    def test_sync_workspace_to_source_unmocked_real_git_execution(self):
        """Verifies sync_workspace_to_source with real unmocked git execution and string piping."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "cl_dir"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            subprocess.run(["git", "-C", str(src_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.name", "Test"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.email", "test@test.com"], check=True, capture_output=True)

            (src_dir / "file.txt").write_text("base content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "base"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "update-ref", "refs/remotes/m/androidx-main", "HEAD"], check=True, capture_output=True)

            (cl_dir / "file.txt").write_text("base content\n")

            (src_dir / "file.txt").write_text("updated workstation content\n")
            (src_dir / "new_file.txt").write_text("new workstation file\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "workstation edit"], check=True, capture_output=True)

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=src_dir.parent.parent)
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=src_dir.parent.parent, is_cog_workspace=True, conversation_id="unmocked-test", env=env)
            provider = common.CogWorkspaceProvider()

            with patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)

            self.assertEqual((cl_dir / "file.txt").read_text(), "updated workstation content\n")
            self.assertEqual((cl_dir / "new_file.txt").read_text(), "new workstation file\n")

    def test_sync_workspace_includes_unstaged_working_tree_edits(self):
        """Verifies sync_workspace_to_source syncs committed HEAD changes AND unstaged working tree edits."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "cl_dir"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            subprocess.run(["git", "-C", str(src_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.name", "Test"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.email", "test@test.com"], check=True, capture_output=True)

            (src_dir / "file.txt").write_text("base content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "base"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "update-ref", "refs/remotes/m/androidx-main", "HEAD"], check=True, capture_output=True)

            (cl_dir / "file.txt").write_text("base content\n")

            # Local committed change
            (src_dir / "file.txt").write_text("committed content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "committed edit"], check=True, capture_output=True)

            # Unstaged dirty edit
            (src_dir / "dirty.txt").write_text("dirty unstaged content\n")

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=src_dir.parent.parent)
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=src_dir.parent.parent, is_cog_workspace=True, conversation_id="include-unstaged-test", env=env)
            provider = common.CogWorkspaceProvider()

            with patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)

            self.assertEqual((cl_dir / "file.txt").read_text(), "committed content\n")
            self.assertTrue((cl_dir / "dirty.txt").exists())
            self.assertEqual((cl_dir / "dirty.txt").read_text(), "dirty unstaged content\n")

    def test_sync_workspace_binary_and_rename_support(self):
        """Verifies sync_workspace_to_source handles binary diffs and file renames losslessly."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "cl_dir"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            subprocess.run(["git", "-C", str(src_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.name", "Test"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.email", "test@test.com"], check=True, capture_output=True)

            (src_dir / "old_name.txt").write_text("file to rename\n")
            (src_dir / "data.bin").write_bytes(bytes([0x00, 0x11, 0x22]))
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "base"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "update-ref", "refs/remotes/m/androidx-main", "HEAD"], check=True, capture_output=True)

            (cl_dir / "old_name.txt").write_text("file to rename\n")
            (cl_dir / "data.bin").write_bytes(bytes([0x00, 0x11, 0x22]))

            # Rename and edit binary
            subprocess.run(["git", "-C", str(src_dir), "mv", "old_name.txt", "new_name.txt"], check=True, capture_output=True)
            (src_dir / "data.bin").write_bytes(bytes([0x00, 0x11, 0x22, 0xFF]))
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "rename & binary edit"], check=True, capture_output=True)

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=src_dir.parent.parent)
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=src_dir.parent.parent, is_cog_workspace=True, conversation_id="binary-rename-test", env=env)
            provider = common.CogWorkspaceProvider()

            with patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)

            self.assertTrue((cl_dir / "new_name.txt").exists())
            self.assertFalse((cl_dir / "old_name.txt").exists())
            self.assertEqual((cl_dir / "data.bin").read_bytes(), bytes([0x00, 0x11, 0x22, 0xFF]))



    def test_sync_workspace_stale_manifest_ref_with_upstream_merge_commit(self):
        """Verifies sync_workspace_to_source correctly resolves upstream base commit even when manifest ref m/* is stale."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "cl_dir"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            subprocess.run(["git", "-C", str(src_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.name", "Test"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.email", "test@test.com"], check=True, capture_output=True)

            # Commit A: Old baseline
            (src_dir / "file.txt").write_text("commit A content\n")
            (src_dir / "other.txt").write_text("other A content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "commit A"], check=True, capture_output=True)

            # Set stale manifest ref refs/remotes/m/androidx-main to Commit A
            subprocess.run(["git", "-C", str(src_dir), "update-ref", "refs/remotes/m/androidx-main", "HEAD"], check=True, capture_output=True)

            # Commit B: Upstream Merge commit (simulating upstream main advancing)
            (src_dir / "file.txt").write_text("commit B content\n")
            (src_dir / "other.txt").write_text("other B content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "Merge \"Upstream feature\" into androidx-main"], check=True, capture_output=True)

            # Prepare cl_dir simulating a fresh CoG workspace initialized at Commit B
            (cl_dir / "file.txt").write_text("commit B content\n")
            (cl_dir / "other.txt").write_text("other B content\n")

            # Local edits on top of Commit B in src_dir
            (src_dir / "file.txt").write_text("local updated workstation content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "file.txt"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "local commit edit"], check=True, capture_output=True)

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=src_dir.parent.parent)
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=src_dir.parent.parent, is_cog_workspace=True, conversation_id="stale-ref-test", env=env)
            provider = common.CogWorkspaceProvider()

            with patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)

            self.assertEqual((cl_dir / "file.txt").read_text(), "local updated workstation content\n")

    def test_sync_workspace_individual_commits_transfer(self):
        """Verifies sync_workspace_to_source transfers multiple commits individually as commits to a git workspace."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "worktrees" / "cl_test" / "frameworks" / "support"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            # Init src repo
            subprocess.run(["git", "-C", str(src_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.name", "Test"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.email", "test@test.com"], check=True, capture_output=True)

            # Base commit
            (src_dir / "base.txt").write_text("base\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "Merge \"Base commit\" into androidx-main"], check=True, capture_output=True)
            base_sha = subprocess.run(["git", "-C", str(src_dir), "rev-parse", "HEAD"], check=True, capture_output=True, text=True).stdout.strip()
            subprocess.run(["git", "-C", str(src_dir), "update-ref", "refs/remotes/m/androidx-main", base_sha], check=True, capture_output=True)

            # Local Commit 1
            (src_dir / "file1.txt").write_text("commit 1 content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "Commit 1 Message"], check=True, capture_output=True)

            # Local Commit 2
            (src_dir / "file2.txt").write_text("commit 2 content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "Commit 2 Message"], check=True, capture_output=True)

            # Init cl_dir as worktree linked to src_dir at base_sha
            subprocess.run(["git", "-C", str(src_dir), "worktree", "add", "-b", "cl_test", str(cl_dir), base_sha], check=True, capture_output=True)

            env = HostEnvironment(is_cog_supported=False, user="testuser", checkout_root=Path(tmpdir))
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=Path(tmpdir), is_cog_workspace=False, conversation_id="commits-test", env=env)
            provider = common.LocalWorktreeProvider()

            with patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)

            # Check commits in cl_dir between base_sha and HEAD
            log_proc = subprocess.run(
                ["git", "-C", str(cl_dir), "log", "--oneline", f"{base_sha}..HEAD"],
                check=True, capture_output=True, text=True
            )
            log_lines = log_proc.stdout.strip().splitlines()
            self.assertEqual(len(log_lines), 2)
            self.assertIn("Commit 2 Message", log_lines[0])
            self.assertIn("Commit 1 Message", log_lines[1])

            self.assertEqual((cl_dir / "file1.txt").read_text(), "commit 1 content\n")
            self.assertEqual((cl_dir / "file2.txt").read_text(), "commit 2 content\n")

    def test_sync_cog_workspace_individual_commits_transfer(self):
        """Verifies sync_workspace_to_source creates individual commit nodes via git citc for CoG workspaces."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "cog_workspace" / "cl_test"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            subprocess.run(["git", "-C", str(src_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.name", "Test"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.email", "test@test.com"], check=True, capture_output=True)

            (src_dir / "base.txt").write_text("base\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "Merge \"Base commit\" into androidx-main"], check=True, capture_output=True)
            base_sha = subprocess.run(["git", "-C", str(src_dir), "rev-parse", "HEAD"], check=True, capture_output=True, text=True).stdout.strip()
            subprocess.run(["git", "-C", str(src_dir), "update-ref", "refs/remotes/m/androidx-main", base_sha], check=True, capture_output=True)

            (src_dir / "file1.txt").write_text("commit 1 content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "CoG Commit 1 Message"], check=True, capture_output=True)

            (src_dir / "file2.txt").write_text("commit 2 content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "CoG Commit 2 Message"], check=True, capture_output=True)

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=Path(tmpdir))
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=Path(tmpdir), is_cog_workspace=True, conversation_id="cog-commits-test", env=env)
            provider = common.CogWorkspaceProvider()

            citc_commands = []
            orig_run = common._run
            def track_run(cmd, **kwargs):
                cmd_str = " ".join(cmd)
                if "git citc" in cmd_str:
                    citc_commands.append((cmd, kwargs.get("cwd")))
                    res = MagicMock()
                    res.returncode = 0
                    res.stdout = ""
                    return res
                return orig_run(cmd, **kwargs)

            with patch("common._run", side_effect=track_run), patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)

            describe_calls = [cmd for cmd, cwd in citc_commands if "cli.describe" in " ".join(cmd)]
            new_calls = [cmd for cmd, cwd in citc_commands if "cli.new" in " ".join(cmd)]

            self.assertEqual(len(describe_calls), 2)
            self.assertEqual(len(new_calls), 2)
            self.assertIn("CoG Commit 1 Message", " ".join(describe_calls[0]))
            self.assertIn("CoG Commit 2 Message", " ".join(describe_calls[1]))

    def test_extract_change_id(self):
        msg = "Fix bug in LazyColumn\n\nTest: unit test\nChange-Id: If4864d9a5a87bdd8712b9fe33165288ae7cf7354"
        self.assertEqual(common.extract_change_id(msg), "If4864d9a5a87bdd8712b9fe33165288ae7cf7354")
        self.assertIsNone(common.extract_change_id("No change id here"))
        self.assertIsNone(common.extract_change_id(""))

    def test_query_gerrit_for_change_id(self):
        fake_response = ")]}'\n[{\"_number\": 4187771, \"change_id\": \"If4864d9a5a87bdd8712b9fe33165288ae7cf7354\"}]"
        with patch("common._run") as mock_run:
            mock_res = MagicMock()
            mock_res.returncode = 0
            mock_res.stdout = fake_response
            mock_run.return_value = mock_res

            res = common.query_gerrit_for_change_id("If4864d9a5a87bdd8712b9fe33165288ae7cf7354")
            self.assertIsNotNone(res)
            self.assertEqual(res["_number"], 4187771)

    def test_sync_source_auto_patches_published_gerrit_parent(self):
        """Verifies sync_workspace_to_source automatically calls git citc patch when parent commit has published Gerrit CL."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "cog" / "cl_test"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            subprocess.run(["git", "-C", str(src_dir), "init"], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.name", "Test User"], check=True)
            subprocess.run(["git", "-C", str(src_dir), "config", "user.email", "test@google.com"], check=True)

            (src_dir / "base.txt").write_text("base content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", "Merge \"base\" into androidx-main"], check=True, capture_output=True)

            msg_with_change_id = "Parent commit\n\nChange-Id: If4864d9a5a87bdd8712b9fe33165288ae7cf7354\n"
            (src_dir / "file1.txt").write_text("parent commit content\n")
            subprocess.run(["git", "-C", str(src_dir), "add", "."], check=True, capture_output=True)
            subprocess.run(["git", "-C", str(src_dir), "commit", "-m", msg_with_change_id], check=True, capture_output=True)

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=Path(tmpdir))
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=Path(tmpdir), is_cog_workspace=True, conversation_id="auto-patch-test", env=env)
            provider = common.CogWorkspaceProvider()

            patch_calls = []
            orig_run = common._run

            def mock_gerrit(change_id, timeout=10):
                if change_id == "If4864d9a5a87bdd8712b9fe33165288ae7cf7354":
                    return {"_number": 4187771}
                return None

            def track_run(cmd, **kwargs):
                cmd_str = " ".join(cmd)
                if "git citc patch" in cmd_str:
                    patch_calls.append(cmd)
                    res = MagicMock()
                    res.returncode = 0
                    res.stdout = ""
                    return res
                elif "git citc" in cmd_str:
                    res = MagicMock()
                    res.returncode = 0
                    res.stdout = ""
                    return res
                return orig_run(cmd, **kwargs)

            with patch("common.query_gerrit_for_change_id", side_effect=mock_gerrit), patch("common._run", side_effect=track_run), patch.object(provider, "get_cl_dir", return_value=cl_dir):
                success = provider.sync_workspace_to_source(ctx, src_dir)
                self.assertTrue(success)
                self.assertEqual(len(patch_calls), 1)
                self.assertIn("4187771", patch_calls[0])

    def test_sync_workspace_to_ref_formatting_and_defaults(self):
        """Verifies sync_workspace_to_ref routes HEAD directly to source sync and extracts numeric change numbers from refs."""
        with tempfile.TemporaryDirectory() as tmpdir:
            src_dir = Path(tmpdir) / "main" / "frameworks" / "support"
            cl_dir = Path(tmpdir) / "cog" / "cl_test"
            src_dir.mkdir(parents=True)
            cl_dir.mkdir(parents=True)

            env = HostEnvironment(is_cog_supported=True, user="testuser", checkout_root=Path(tmpdir))
            ctx = WorkspaceContext(support_root=src_dir, checkout_root=Path(tmpdir), is_cog_workspace=True, conversation_id="sync-ref-test", env=env)
            provider = common.CogWorkspaceProvider()

            patch_calls = []

            def mock_run(cmd, **kwargs):
                cmd_str = " ".join(cmd)
                if "git citc patch" in cmd_str:
                    patch_calls.append(cmd)
                    res = MagicMock()
                    res.returncode = 0
                    res.stdout = ""
                    return res
                res = MagicMock()
                res.returncode = 0
                res.stdout = ""
                return res

            with patch.object(provider, "workspace_exists", return_value=True), \
                 patch.object(provider, "sync_workspace_to_source", return_value=True) as mock_sync_src, \
                 patch("common._run", side_effect=mock_run):

                # 1. Target ref "HEAD" -> directly calls sync_workspace_to_source, no git citc patch
                self.assertTrue(provider.sync_workspace_to_ref(ctx, src_dir, "HEAD"))
                mock_sync_src.assert_called_once_with(ctx, src_dir)
                self.assertEqual(len(patch_calls), 0)

                mock_sync_src.reset_mock()

                # 2. Target ref "aosp/4209909" -> extracts change number 4209909 for git citc patch
                self.assertTrue(provider.sync_workspace_to_ref(ctx, src_dir, "aosp/4209909"))
                self.assertEqual(len(patch_calls), 1)
                self.assertEqual(patch_calls[0], ["git", "citc", "patch", "4209909"])
                mock_sync_src.assert_called_once_with(ctx, src_dir)

                # 3. Target ref "refs/changes/09/4209909" -> extracts change number 4209909
                patch_calls.clear()
                mock_sync_src.reset_mock()
                self.assertTrue(provider.sync_workspace_to_ref(ctx, src_dir, "refs/changes/09/4209909"))
                self.assertEqual(len(patch_calls), 1)
                self.assertEqual(patch_calls[0], ["git", "citc", "patch", "4209909"])

                # 4. Target ref with clean=True -> does not call sync_workspace_to_source
                patch_calls.clear()
                mock_sync_src.reset_mock()
                self.assertTrue(provider.sync_workspace_to_ref(ctx, src_dir, "aosp/4209909", clean=True))
                self.assertEqual(len(patch_calls), 1)
                mock_sync_src.assert_not_called()


if __name__ == '__main__':
    unittest.main()
