# 🚀 Comprehensive Git Push Troubleshooting Guide

If you are experiencing errors when pushing your ANGEL AI project to GitHub (such as authentication failures, remote rejection, or invalid arguments), follow this comprehensive troubleshooting guide and command sequence.

---

## 1. Check Current Git Remote (`git remote -v`)

Verify that your local repository is correctly linked to your GitHub repository remote URL.

* **Command**:
  ```bash
  git remote -v
  ```
* **Expected Output**:
  ```text
  origin  https://github.com/<your-username>/<your-repo>.git (fetch)
  origin  https://github.com/<your-username>/<your-repo>.git (push)
  ```
* **How to Fix (if remote is missing or incorrect)**:
  ```bash
  git remote remove origin
  git remote add origin https://github.com/<your-username>/<your-repo>.git
  ```

---

## 2. Re-verify Branch Tracking (`git branch -vv`)

Ensure your local branch is actively tracking the correct remote branch (`origin/main` or `origin/master`).

* **Command**:
  ```bash
  git branch -vv
  ```
* **Expected Output**:
  ```text
  * main  abc1234 [origin/main] Initial commit
  ```
* **How to Fix (if tracking is missing or unlinked)**:
  ```bash
  git push --set-upstream origin main
  ```

---

## 3. Configure Git Credential Helper to Avoid Authentication Errors

GitHub **no longer accepts your account password** for terminal or API git operations. You must use a **Personal Access Token (PAT)** with `repo` scope. To prevent repeated authentication prompts and cache your credentials securely:

* **Configure Credential Cache (stores token in memory for 1 hour)**:
  ```bash
  git config --global credential.helper cache
  ```
* **Configure Permanent Store (stores token securely on disk)**:
  ```bash
  git config --global credential.helper store
  ```
* **How to Authenticate**:
  1. Generate a Personal Access Token on GitHub (*Settings -> Developer Settings -> Personal access tokens -> Tokens (classic)* with `repo` scope).
  2. When running `git push`, use your GitHub username and paste your **Personal Access Token** as your password.

---

## 4. Common Git Push Errors & Solutions

| Error Message | Probable Cause | Corrective Action |
|---|---|---|
| `Request contains an invalid argument` | Large binary file, keystore, or build artifact in commit | Add files to `.gitignore` and remove from git index: `git rm --cached <file>` |
| `Authentication failed` / 403 | Using account password instead of Personal Access Token | Use a GitHub PAT (`ghp_...`) instead of password |
| `Updates were rejected because the remote contains work that you do not have locally` | Remote branch is ahead of local branch | Pull remote changes first: `git pull origin main --rebase` |
