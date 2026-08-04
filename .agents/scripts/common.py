#!/usr/bin/env python3
from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
import getpass
import json
import logging
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
from typing import Mapping

# ==============================================================================
# 1. Constants & Logging Configuration
# ==============================================================================
DEFAULT_TARGET_BRANCH = "refs/heads/androidx-main"
DEFAULT_SUPERPROJECT_REPO = "android/platform/superproject"
DEFAULT_SUPPORT_REPO = "android/frameworks/support"
SHARED_SIBLINGS = ("prebuilts", "tools", "golden", "external", "build")

logger = logging.getLogger("common")
if not logger.handlers:
    handler = logging.StreamHandler(sys.stderr)
    handler.setFormatter(logging.Formatter("[%(name)s] %(message)s"))
    logger.addHandler(handler)
    logger.setLevel(logging.INFO)


def _run(cmd: list[str], cwd: Path | None = None, timeout: int = 60, input_data: str | None = None) -> subprocess.CompletedProcess[str] | None:
    """Helper executing a subprocess with standard exception handling and logging."""
    try:
        return subprocess.run(cmd, cwd=cwd, input=input_data, capture_output=True, text=True, timeout=timeout)
    except (subprocess.SubprocessError, FileNotFoundError, OSError) as e:
        logger.error(f"Command '{shlex.join(cmd)}' failed: {e}")
        return None


def _run_bytes(cmd: list[str], cwd: Path | None = None, timeout: int = 60, input_data: bytes | None = None) -> subprocess.CompletedProcess[bytes] | None:
    try:
        return subprocess.run(cmd, cwd=cwd, input=input_data, capture_output=True, text=False, timeout=timeout)
    except (subprocess.SubprocessError, FileNotFoundError, OSError) as e:
        logger.error(f"Command '{shlex.join(cmd)}' failed: {e}")
        return None


# ==============================================================================
# 2. Workspace & Environment Detection
# ==============================================================================
def is_cog_workspace(cwd: Path) -> bool:
    """Checks if directory is inside a CoG virtual filesystem workspace."""
    path = cwd.resolve()
    try:
        return path.is_relative_to(Path("/google/cog/cloud"))
    except (ValueError, AttributeError):
        return "/google/cog/cloud/" in str(path)


def is_already_isolated_workspace(cwd: Path) -> bool:
    """Checks if directory is already in an isolated workspace (CoG or Git worktree)."""
    if is_cog_workspace(cwd):
        return True
    parts = cwd.resolve().parts
    return any(marker in parts for marker in (".worktrees", "worktrees", ".system_generated"))


def get_worktree_root(support_root: Path) -> Path:
    """Parses structural worktree root by stripping frameworks/support or worktree markers."""
    path = support_root.resolve()
    parts = path.parts

    markers = [m for m in (".worktrees", "worktrees", ".system_generated", ".repo") if m in parts]
    if markers:
        idx = min(parts.index(m) for m in markers)
        return Path(*parts[:idx])

    for idx in range(len(parts) - 1, 0, -1):
        if parts[idx - 1] == "frameworks" and parts[idx] == "support":
            return Path(*parts[:idx - 1])

    return path


def should_use_cog_workspace(workspace_root: Path, env: HostEnvironment) -> bool:
    """Determines if a workspace path requires a Cog workspace based on isolation state and host capabilities."""
    if is_already_isolated_workspace(workspace_root):
        return is_cog_workspace(workspace_root)
    return is_cog_workspace(workspace_root) or env.is_cog_supported


def find_merge_base_dynamically(src: Path) -> str:
    """Dynamically resolves merge-base against repo manifest symbolic refs."""
    res = _run(["git", "-C", str(src), "for-each-ref", "--format=%(refname:short)", "refs/remotes/m/*"], timeout=15)
    if res and res.returncode == 0 and res.stdout.strip():
        for ref in res.stdout.splitlines():
            ref = ref.strip()
            if ref and not ref.endswith("/HEAD"):
                mb = _run(["git", "-C", str(src), "merge-base", "HEAD", ref], timeout=15)
                if mb and mb.returncode == 0 and mb.stdout.strip():
                    return mb.stdout.strip()

    rev = _run(["git", "-C", str(src), "rev-parse", "HEAD"], timeout=15)
    if rev and rev.returncode == 0 and rev.stdout.strip():
        return rev.stdout.strip()

    raise RuntimeError(f"Failed to dynamically resolve git merge-base for {src}")


# ==============================================================================
# 3. Host Environment & Workspace Context Models
# ==============================================================================
@dataclass(frozen=True)
class HostEnvironment:
    """Explicit system environment abstraction for dependency injection."""
    is_cog_supported: bool
    user: str
    checkout_root: Path

    @classmethod
    def detect(cls, support_root: Path, environ: Mapping[str, str] = os.environ) -> HostEnvironment:
        user = environ.get("USER") or environ.get("LOGNAME") or getpass.getuser()
        use_worktree = environ.get("ANTIGRAVITY_USE_WORKTREE", "").lower() in ("1", "true", "yes")
        cog_supported = (
            not use_worktree
            and sys.platform != "darwin"
            and Path("/google/cog/cloud").exists()
            and shutil.which("git-citc") is not None
        )
        root = get_worktree_root(support_root) if is_cog_workspace(support_root) else find_main_checkout_root(support_root)
        return cls(is_cog_supported=cog_supported, user=user, checkout_root=root)


@dataclass(frozen=True)
class WorkspaceContext:
    """Encapsulates active conversation workspace metadata."""
    support_root: Path
    checkout_root: Path
    is_cog_workspace: bool
    conversation_id: str
    env: HostEnvironment

    @classmethod
    def from_root(cls, workspace_root: Path, conversation_id: str, env: HostEnvironment) -> WorkspaceContext:
        root = workspace_root.resolve()
        return cls(
            support_root=root,
            checkout_root=env.checkout_root,
            is_cog_workspace=should_use_cog_workspace(workspace_root, env),
            conversation_id=conversation_id,
            env=env,
        )

    @property
    def cl_name(self) -> str:
        return f"cl_{self.conversation_id}"


# ==============================================================================
# 4. Toolchain Symlink & Checkout Root Management
# ==============================================================================
def find_main_checkout_root(support_root: Path) -> Path:
    """Finds top-level directory of primary Git checkout containing prebuilts."""
    root = get_worktree_root(support_root)
    if (root / "prebuilts").exists():
        return root

    res = _run(["git", "-C", str(support_root), "rev-parse", "--git-common-dir"], timeout=5)
    if res and res.returncode == 0 and res.stdout.strip():
        raw_git_dir = Path(res.stdout.strip())
        git_dir = (support_root / raw_git_dir).resolve() if not raw_git_dir.is_absolute() else raw_git_dir.resolve()
        for p in [git_dir] + list(git_dir.parents):
            if p.name in (".git", ".repo"):
                return get_worktree_root(p.parent)

    for start_path in (support_root, Path(__file__).resolve()):
        curr = start_path if start_path.is_dir() else start_path.parent
        for p in [curr] + list(curr.parents):
            if (p / "prebuilts").exists():
                return get_worktree_root(p)

    return root


def ensure_sibling_symlinks(support_root: Path, main_checkout_root: Path) -> None:
    """Ensures toolchain sibling symlinks (prebuilts, tools, etc.) exist for worktrees."""
    main_root = main_checkout_root.resolve()
    target_dirs = {support_root.resolve().parent.parent, get_worktree_root(support_root)}

    for target_dir in target_dirs:
        if not target_dir.exists():
            continue

        for sibling_name in SHARED_SIBLINGS:
            source_dir = main_root / sibling_name
            target_symlink = target_dir / sibling_name

            if not source_dir.exists():
                continue

            if target_symlink.is_symlink():
                try:
                    if target_symlink.resolve() == source_dir.resolve():
                        continue
                    target_symlink.unlink()
                except OSError:
                    pass
            elif target_symlink.exists():
                continue

            try:
                target_symlink.symlink_to(source_dir)
                logger.info(f"Symlinked sibling: {target_symlink} -> {source_dir}")
            except OSError as e:
                logger.error(f"Failed to symlink {sibling_name}: {e}")


def cleanup_out_dir(cl_name: str) -> None:
    """Removes per-session build output directory ($HOME/androidxout/<cl_name>) if present."""
    shutil.rmtree(Path.home() / "androidxout" / cl_name, ignore_errors=True)


# ==============================================================================
# 5. Workspace Providers
# ==============================================================================
def find_upstream_base_commit(src: Path) -> str:
    """Finds the most recent upstream base commit in src before local user commits."""
    res = _run(["git", "-C", str(src), "log", "-n", "30", "--grep=Merge ", "--format=%H"], timeout=15)
    if res and res.returncode == 0 and res.stdout.strip():
        return res.stdout.splitlines()[0].strip()
    try:
        return find_merge_base_dynamically(src)
    except RuntimeError:
        pass
    rev = _run(["git", "-C", str(src), "rev-parse", "HEAD"], timeout=15)
    if rev and rev.returncode == 0 and rev.stdout.strip():
        return rev.stdout.strip()
    return "HEAD"

def get_commits_between(src: Path, base_ref: str, target_ref: str = "HEAD") -> list[str]:
    """Returns list of commit hashes between base_ref and target_ref in chronological order."""
    res = _run(["git", "-C", str(src), "log", "--reverse", "--format=%H", f"{base_ref}..{target_ref}"], timeout=15)
    if res and res.returncode == 0 and res.stdout.strip():
        return [line.strip() for line in res.stdout.splitlines() if line.strip()]
    return []

CHANGE_ID_PATTERN = re.compile(r"^Change-Id:\s*(I[0-9a-fA-F]{40})$", re.MULTILINE)


def extract_change_id(commit_msg: str) -> str | None:
    """Extracts Gerrit Change-Id from commit description if present."""
    if not commit_msg:
        return None
    match = CHANGE_ID_PATTERN.search(commit_msg)
    return match.group(1) if match else None


def query_gerrit_for_change_id(change_id: str, timeout: int = 10) -> dict | None:
    """Queries Gerrit REST API for change details by Change-Id."""
    if not change_id:
        return None
    url = f"https://android-review.googlesource.com/changes/?q={change_id}"
    res = _run(["curl", "-s", url], timeout=timeout)
    if not res or res.returncode != 0 or not res.stdout.strip():
        return None

    body = res.stdout.strip()
    if body.startswith(")]}'"):
        body = body[4:].strip()

    try:
        data = json.loads(body)
        if isinstance(data, list) and len(data) > 0:
            return data[0]
    except (json.JSONDecodeError, ValueError):
        pass
    return None


def _sync_source_diff_and_untracked(source_path: Path, cl_dir: Path, apply_3way: bool = False) -> bool:
    """Diffs committed and working-tree changes from source_path against base_ref and applies them to cl_dir, copying untracked files."""
    cl_dir.mkdir(parents=True, exist_ok=True)
    src = source_path.resolve()
    base_ref = find_upstream_base_commit(src)

    is_git_repo = (cl_dir / ".git").exists()

    if is_git_repo:
        _run(["git", "-C", str(cl_dir), "reset", "--hard", base_ref], timeout=30)
        commits = get_commits_between(src, base_ref, "HEAD")
        for commit_sha in commits:
            cp_res = _run(["git", "-C", str(cl_dir), "cherry-pick", "--allow-empty", commit_sha], timeout=30)
            if not cp_res or cp_res.returncode != 0:
                logger.error(f"Failed to cherry-pick commit {commit_sha} to workspace. Aborting cherry-pick.")
                _run(["git", "-C", str(cl_dir), "cherry-pick", "--abort"], timeout=15)
                _run(["git", "-C", str(cl_dir), "reset", "--hard", base_ref], timeout=30)
                commits = []
                break
        diff_base = "HEAD" if commits or not get_commits_between(src, base_ref, "HEAD") else base_ref
    else:
        commits = get_commits_between(src, base_ref, "HEAD")
        if commits:
            for commit_sha in commits:
                commit_msg_proc = _run(["git", "-C", str(src), "log", "-1", "--format=%B", commit_sha], timeout=15)
                commit_msg = commit_msg_proc.stdout.strip() if commit_msg_proc and commit_msg_proc.stdout else ""
                change_id = extract_change_id(commit_msg)

                gerrit_info = query_gerrit_for_change_id(change_id) if change_id else None
                gerrit_cl_number = gerrit_info.get("_number") if gerrit_info else None

                if gerrit_cl_number:
                    logger.info(f"Importing published parent Gerrit CL #{gerrit_cl_number} into CoG workspace...")
                    _run(["git", "citc", "patch", str(gerrit_cl_number)], cwd=cl_dir, timeout=60)

                    # Apply any local parent delta if workstation commit had un-uploaded edits
                    diff_proc = _run_bytes(["git", "-C", str(src), "diff-tree", "-p", "--binary", commit_sha], timeout=60)
                    if diff_proc and diff_proc.stdout:
                        _run_bytes(["git", "apply", "--whitespace=nowarn"], cwd=cl_dir, input_data=diff_proc.stdout, timeout=60)

                    if commit_msg:
                        _run(["git", "citc", "cli.describe", "-m", commit_msg + "\n"], cwd=cl_dir, timeout=30)
                else:
                    diff_proc = _run_bytes(["git", "-C", str(src), "diff-tree", "-p", "--binary", commit_sha], timeout=60)
                    if diff_proc and diff_proc.stdout:
                        _run_bytes(["git", "apply", "--whitespace=nowarn"], cwd=cl_dir, input_data=diff_proc.stdout, timeout=60)

                    if commit_msg:
                        _run(["git", "citc", "cli.describe", "-m", commit_msg + "\n"], cwd=cl_dir, timeout=30)

                    _run(["git", "citc", "cli.new"], cwd=cl_dir, timeout=30)
            diff_base = "HEAD"
        else:
            diff_base = base_ref

    res = _run(["git", "-C", str(src), "diff", "--name-status", diff_base], timeout=30)
    modified_files = []
    if res and res.returncode == 0 and res.stdout.strip():
        for line in res.stdout.splitlines():
            parts = line.strip().split("\t")
            for p in parts[1:]:
                p = p.strip()
                if p:
                    modified_files.append(p)

    if modified_files:
        for rel_file in modified_files:
            (cl_dir / rel_file).parent.mkdir(parents=True, exist_ok=True)

        diff_proc = _run_bytes(["git", "-C", str(src), "diff", "--binary", diff_base, "--"] + modified_files, timeout=60)
        if diff_proc and diff_proc.stdout:
            apply_cmd = ["git", "apply", "--whitespace=nowarn"]
            if apply_3way:
                apply_cmd.insert(2, "--3way")
            apply_proc = _run_bytes(apply_cmd, cwd=cl_dir, input_data=diff_proc.stdout, timeout=60)
            if not apply_proc or apply_proc.returncode != 0:
                err = apply_proc.stderr.decode("utf-8", errors="replace") if apply_proc else "Unknown error"
                logger.error(f"Failed to apply git diff to workspace: {err}")
                return False

    untracked_proc = _run(["git", "-C", str(src), "ls-files", "--others", "--exclude-standard"], timeout=30)
    if untracked_proc and untracked_proc.stdout:
        for rel_file in untracked_proc.stdout.splitlines():
            rel_file = rel_file.strip()
            if rel_file:
                src_file = src / rel_file
                dest_file = cl_dir / rel_file
                if src_file.is_file():
                    dest_file.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copy2(src_file, dest_file)
    return True

class WorkspaceProvider(ABC):
    """Abstract interface for workspace provisioning and synchronization."""
    @abstractmethod
    def get_workspace_parent(self, ctx: WorkspaceContext) -> Path:
        pass

    @abstractmethod
    def get_cl_dir(self, ctx: WorkspaceContext) -> Path:
        pass

    @abstractmethod
    def workspace_exists(self, ctx: WorkspaceContext) -> bool:
        pass

    @abstractmethod
    def create_workspace(self, ctx: WorkspaceContext, base_ref: str = DEFAULT_TARGET_BRANCH) -> bool:
        pass

    @abstractmethod
    def sync_workspace_to_ref(self, ctx: WorkspaceContext, source_path: Path, target_ref: str, clean: bool = False) -> bool:
        pass

    @abstractmethod
    def sync_workspace_to_source(self, ctx: WorkspaceContext, source_path: Path, strip_change_id: bool = False) -> bool:
        pass

    @abstractmethod
    def cleanup_workspace(self, ctx: WorkspaceContext) -> None:
        pass

    @abstractmethod
    def ensure_workspace_ready(self, ctx: WorkspaceContext) -> None:
        """Ensures an existing workspace has required environment setup (e.g. symlinks)."""
        pass


class CogWorkspaceProvider(WorkspaceProvider):
    """Manages virtual Citc workspaces on Google Cloud via git-citc."""
    def get_workspace_parent(self, ctx: WorkspaceContext) -> Path:
        return Path(f"/google/cog/cloud/{ctx.env.user}/{ctx.cl_name}")

    def get_cl_dir(self, ctx: WorkspaceContext) -> Path:
        return self.get_workspace_parent(ctx) / "android" / "frameworks" / "support"

    def workspace_exists(self, ctx: WorkspaceContext) -> bool:
        p = self.get_cl_dir(ctx)
        return p.exists() and any(p.iterdir())

    def ensure_workspace_ready(self, ctx: WorkspaceContext) -> None:
        pass

    def create_workspace(self, ctx: WorkspaceContext, base_ref: str = DEFAULT_TARGET_BRANCH) -> bool:
        cl_dir = self.get_cl_dir(ctx)
        workspace_parent = self.get_workspace_parent(ctx)
        if self.workspace_exists(ctx):
            cl_dir.mkdir(parents=True, exist_ok=True)
            return True
        target_ref = DEFAULT_TARGET_BRANCH if base_ref in ("HEAD", "LOCAL_HEAD", "@", "parent") or not base_ref else base_ref
        cmd = ["git", "citc", "create", ctx.cl_name, DEFAULT_SUPERPROJECT_REPO, f"--ref={target_ref}", f"--target_branch={DEFAULT_TARGET_BRANCH}"]
        res = _run(cmd, timeout=60)
        if res and res.returncode == 0:
            cl_dir.mkdir(parents=True, exist_ok=True)
            return True
        if workspace_parent.exists():
            cl_dir.mkdir(parents=True, exist_ok=True)
            return True
        return False

    def sync_workspace_to_ref(self, ctx: WorkspaceContext, source_path: Path, target_ref: str, clean: bool = False) -> bool:
        cl_dir = self.get_cl_dir(ctx)
        if not self.workspace_exists(ctx) and not self.create_workspace(ctx):
            return False

        if target_ref in ("HEAD", "LOCAL_HEAD", "parent", "@", DEFAULT_TARGET_BRANCH) or target_ref.startswith("refs/heads/"):
            if clean:
                return True
            return self.sync_workspace_to_source(ctx, source_path)

        cl_num = target_ref.split("/")[-1]
        res = _run(["git", "citc", "patch", cl_num], cwd=cl_dir, timeout=60)
        if res and res.returncode == 0:
            logger.info(f"Synced CoG workspace at {cl_dir} to change {target_ref}")
            if clean:
                drafts_res = _run(["git", "citc", "api.call", "GetDrafts", ""], cwd=cl_dir, timeout=30)
                if drafts_res and drafts_res.stdout:
                    target_node = None
                    for block in drafts_res.stdout.split("drafts {"):
                        if f"change_number: {cl_num}" in block or f'change_number: "{cl_num}"' in block:
                            m = re.search(r'node_id:\s*"([^"]+)"', block)
                            if m:
                                target_node = m.group(1)
                                break
                    if target_node:
                        _run(["git", "citc", "api.call", "Rebase", f'repo_root: "android/frameworks/support" source_node_id: "@" new_base: "{target_node}"'], cwd=cl_dir, timeout=30)
                return True
            return self.sync_workspace_to_source(ctx, source_path)

        if not clean:
            return self.sync_workspace_to_source(ctx, source_path)
        return False

    def sync_workspace_to_source(self, ctx: WorkspaceContext, source_path: Path, strip_change_id: bool = False) -> bool:
        cl_dir = self.get_cl_dir(ctx)
        if not _sync_source_diff_and_untracked(source_path, cl_dir, apply_3way=False):
            return False

        src = source_path.resolve()
        base_ref = find_upstream_base_commit(src)
        if not get_commits_between(src, base_ref, "HEAD"):
            commit_msg_proc = _run(["git", "-C", str(src), "log", "-1", "--format=%B"], timeout=15)
            if commit_msg_proc and commit_msg_proc.stdout:
                lines = commit_msg_proc.stdout.splitlines()
                cleaned_lines = [line for line in lines if not line.strip().startswith("Change-Id:")] if strip_change_id else lines
                final_msg = "\n".join(cleaned_lines).strip() + "\n"
                _run(["git", "citc", "cli.describe", "-m", final_msg], cwd=cl_dir, timeout=30)

        logger.info(f"Successfully synced local workspace to {cl_dir}")
        return True

    def cleanup_workspace(self, ctx: WorkspaceContext) -> None:
        logger.info(f"Cleaning up CoG workspace for CL '{ctx.cl_name}'...")
        cleanup_out_dir(ctx.cl_name)
        _run(["git", "citc", "delete", ctx.cl_name], timeout=60)


class LocalWorktreeProvider(WorkspaceProvider):
    """Manages isolated local Git worktrees via git worktree."""
    def get_workspace_parent(self, ctx: WorkspaceContext) -> Path:
        return ctx.checkout_root / ".worktrees" / ctx.cl_name

    def get_cl_dir(self, ctx: WorkspaceContext) -> Path:
        return self.get_workspace_parent(ctx) / "frameworks" / "support"

    def workspace_exists(self, ctx: WorkspaceContext) -> bool:
        p = self.get_cl_dir(ctx)
        return p.exists() and (p / ".git").exists()

    def ensure_workspace_ready(self, ctx: WorkspaceContext) -> None:
        cl_dir = self.get_cl_dir(ctx)
        target = ctx.support_root if is_already_isolated_workspace(ctx.support_root) else cl_dir
        ensure_sibling_symlinks(target, ctx.checkout_root)

    def create_workspace(self, ctx: WorkspaceContext, base_ref: str = DEFAULT_TARGET_BRANCH) -> bool:
        cl_dir = self.get_cl_dir(ctx)
        workspace_parent = self.get_workspace_parent(ctx)
        if self.workspace_exists(ctx):
            logger.info(f"Worktree already exists at {cl_dir}")
            return True

        if workspace_parent.exists() or cl_dir.exists():
            shutil.rmtree(workspace_parent, ignore_errors=True)

        upstream_base = find_upstream_base_commit(ctx.support_root)
        target_base = upstream_base if base_ref in ("HEAD", "LOCAL_HEAD", "@", "parent", DEFAULT_TARGET_BRANCH) or not base_ref else base_ref
        cmd = ["git", "-C", str(ctx.support_root), "worktree", "add", "-b", ctx.cl_name, str(cl_dir), target_base]
        logger.info(f"Creating git worktree: {shlex.join(cmd)}")
        res = _run(cmd, timeout=60)

        if res and res.returncode != 0 and "already exists" in res.stderr:
            res = _run(["git", "-C", str(ctx.support_root), "worktree", "add", str(cl_dir), ctx.cl_name], timeout=60)

        if res and res.returncode == 0:
            self.ensure_workspace_ready(ctx)
            logger.info(f"Successfully created local git worktree at {cl_dir}")
            return True

        if res and res.stderr:
            logger.error(f"Failed to create local git worktree: {res.stderr.strip()}")
        return False

    def sync_workspace_to_ref(self, ctx: WorkspaceContext, source_path: Path, target_ref: str, clean: bool = False) -> bool:
        cl_dir = self.get_cl_dir(ctx)
        if not self.workspace_exists(ctx) and not self.create_workspace(ctx):
            return False

        if target_ref in ("HEAD", "LOCAL_HEAD", "parent", "@"):
            if clean:
                return True
            return self.sync_workspace_to_source(ctx, source_path)

        res = _run(["git", "-C", str(cl_dir), "checkout", target_ref], timeout=60)
        if res and res.returncode == 0:
            if not clean:
                return self.sync_workspace_to_source(ctx, source_path)
            return True

        if not clean:
            return self.sync_workspace_to_source(ctx, source_path)
        return False

    def sync_workspace_to_source(self, ctx: WorkspaceContext, source_path: Path, strip_change_id: bool = False) -> bool:
        cl_dir = self.get_cl_dir(ctx)
        return _sync_source_diff_and_untracked(source_path, cl_dir, apply_3way=True)

    def cleanup_workspace(self, ctx: WorkspaceContext) -> None:
        logger.info(f"Cleaning up local Git worktree for '{ctx.cl_name}'...")
        cleanup_out_dir(ctx.cl_name)
        cl_dir = self.get_cl_dir(ctx)
        workspace_parent = self.get_workspace_parent(ctx)
        _run(["git", "-C", str(ctx.support_root), "worktree", "remove", "--force", str(cl_dir)], timeout=60)
        _run(["git", "-C", str(ctx.support_root), "branch", "-D", ctx.cl_name], timeout=30)
        if workspace_parent.exists():
            shutil.rmtree(workspace_parent, ignore_errors=True)

def get_workspace_provider(root: Path, env: HostEnvironment) -> WorkspaceProvider:
    """Factory returning the active WorkspaceProvider for a given root directory and host environment."""
    return CogWorkspaceProvider() if should_use_cog_workspace(root, env) else LocalWorktreeProvider()
