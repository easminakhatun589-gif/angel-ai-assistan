# 🔍 Git Diagnostic Checklist & Command Sequence

Use this step-by-step diagnostic checklist and terminal command sequence to inspect and resolve git remote, branch tracking, and credential helper issues when experiencing `git push` failures.

---

## Part 1: Diagnostic Command Sequence

Run these commands in your local terminal or command prompt inside your project directory:

### 1. Verify Git Remote URL
Check if your repository is pointing to the correct GitHub remote URL:
```bash
git remote -v
```
* **Expected Output**: You should see `origin  https://github.com/<your-username>/<your-repo>.git (fetch)` and `(push)`.
* **Fix (if incorrect)**:
  ```bash
  git remote set-url origin https://github.com/<your-username>/<your-repo>.git
  ```

### 2. Verify Local Branch Tracking
Check which remote branch your local branch is tracking:
```bash
git branch -vv
```
* **Expected Output**: Shows your current branch (e.g. `main`) alongside `[origin/main: ahead 1]`.
* **Fix (if untracked)**:
  ```bash
  git push --set-upstream origin main
  ```

### 3. Verify Credential Helper Configuration
Check how your Git credentials (Personal Access Token) are stored and cached:
```bash
git config --get credential.helper
```
* **Expected Output**: `store` or `cache` (or `manager` on Windows/Mac).
* **Fix (to cache credentials for 1 hour)**:
  ```bash
  git config --global credential.helper cache
  ```
* **Fix (to store credentials permanently)**:
  ```bash
  git config --global credential.helper store
  ```

### 4. Check Git Status & Staged Changes
Verify if there are any conflicts, untracked large files, or detached states:
```bash
git status
```

---

## Part 2: Troubleshooting Checklist

- [ ] **Remote URL is correct**: Pointing to your repository via HTTPS or SSH.
- [ ] **Branch tracking is active**: Local branch is linked to `origin/main` (or `origin/master`).
- [ ] **Authentication uses PAT**: You are entering your **Personal Access Token (PAT)** with `repo` scope, NOT your GitHub account password.
- [ ] **No large binary files**: Files larger than 50MB (like unignored build caches, keystores, or raw video assets) are excluded in `.gitignore`.
- [ ] **Remote is up to date**: You pulled recent changes before pushing (`git pull origin main --rebase`).
