# 🛠️ Git Push Troubleshooting & Resolution Guide

If you encounter errors when pushing your ANGEL AI project to GitHub (such as `Failed to push commit to GitHub: Request contains an invalid argument` or authentication errors), this guide will help you diagnose and resolve the issue quickly.

---

## 1. Large File Tracking & Payload Limits (Invalid Argument Error)

**Error Example:** 
`Failed to push commit to GitHub: Request contains an invalid argument.`

**Cause:**
GitHub's commit API and remote storage reject commits containing oversized files, compiled binaries, keystores, or heavy build caches that exceed payload limits.

**How to Fix:**
1. Ensure your `.gitignore` file correctly ignores build directories, keystores, and local environment files:
   ```gitignore
   .gradle/
   build/
   **/build/
   local.properties
   .env
   debug.keystore
   debug.keystore.base64
   *.apk
   *.aab
   ```
2. If you accidentally committed a large file in your staging history, remove it from git tracking without deleting it locally:
   ```bash
   git rm --cached path/to/large-file
   git commit --amend -m "Remove large binary file from commit"
   ```

---

## 2. Authentication Credentials: Personal Access Token (PAT) vs. Password

**Error Example:**
`Support for password authentication was removed on August 13, 2021` or `Authentication failed`.

**Cause:**
GitHub **no longer accepts your GitHub account password** when pushing code via HTTPS terminal or external tools. You must use a **Personal Access Token (PAT)**.

**How to Fix:**
1. Go to your GitHub account **Settings** -> **Developer settings** -> **Personal access tokens** -> **Tokens (classic)** (or Fine-grained tokens).
2. Generate a new token with the **`repo`** scope (Full control of private repositories).
3. Copy your generated Personal Access Token (starts with `ghp_...`).
4. When prompted for your password during `git push`, paste your **Personal Access Token** instead of your GitHub password.

---

## 3. Mismatched Remotes & Branch Divergence

**Error Example:**
`Updates were rejected because the remote contains work that you do not have locally.`

**Cause:**
Your local repository branch is behind the remote repository branch on GitHub.

**How to Fix:**
1. Pull the latest remote changes and rebase your local commits:
   ```bash
   git pull origin main --rebase
   ```
2. If your remote URL needs verification or correction:
   ```bash
   # Check current remote
   git remote -v
   
   # Set correct remote URL if needed
   git remote set-url origin https://github.com/<your-username>/<your-repo>.git
   ```
3. Push your changes again:
   ```bash
   git push origin main
   ```

---

## 4. Quick Troubleshooting Checklist

| Symptom | Probable Cause | Corrective Action |
|---|---|---|
| `Request contains an invalid argument` | Large binary file or keystore included in commit payload | Add file to `.gitignore` and unstage via `git rm --cached` |
| `Authentication failed` / 403 / 401 | Using GitHub account password instead of PAT | Generate and use a Personal Access Token with `repo` scope |
| `Rejected because remote contains work` | Remote branch has newer commits | Run `git pull origin main --rebase` before pushing |
| `Permission to user/repo denied` | Logged into wrong GitHub account or lacking write access | Check `git remote -v` and authenticate with correct account |
