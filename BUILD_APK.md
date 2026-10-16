# 🚀 ANGEL AI — Automatic APK Build via GitHub Actions

This guide explains how your ANGEL AI Android APK is automatically built on every GitHub push and how to download and configure it.

---

## 1. How the Automatic Build Works

Whenever you push any code commit to the `main` or `master` branch (or create a pull request / trigger manual workflow dispatch), GitHub Actions automatically launches a fresh Ubuntu runner with:
- **Java 17 (Temurin)**
- **Android SDK (Build-Tools 34.0.0, CompileSdk 34)**
- **Gradle 8.10.2**
- **Automated Gradle Caching**

The workflow compiles the code, packages the APK, verifies the outputs, and uploads the final build artifact (`ANGEL-AI-APK`) with a **14-day retention period**.

---

## 2. How to Push Code to GitHub

If you are using git locally on your computer:

```bash
# 1. Check changes
git status

# 2. Add all changes
git add .

# 3. Commit changes
git commit -m "Update ANGEL AI features and APK configuration"

# 4. Push to GitHub
git push origin main
# (or 'git push origin master' if your primary branch is master)
```

Once pushed, GitHub Actions immediately detects the commit and begins the build process.

---

## 3. How to Find the Build Results

1. Open your repository on GitHub in your browser (`https://github.com/<your-username>/<your-repo>`).
2. Click on the **Actions** tab at the top of the repository.
3. You will see the latest workflow run named **"Build ANGEL AI APK"** with your commit message.
4. Click on the workflow run to view real-time build logs, progress, and step outputs.

---

## 4. How to Download "ANGEL-AI-APK"

1. In the **Actions** tab, click on the completed workflow run (with a green checkmark ✅).
2. Scroll down to the bottom of the page to the **Artifacts** section.
3. Click on **ANGEL-AI-APK** to download the ZIP file.
4. Unzip the downloaded file on your computer or phone to access `app-debug.apk` (and `app-release.apk` if release signing is configured).
5. Install the APK directly on your Android phone!

> 💡 **Tip for Phone Installation**: If Android Play Protect shows *"Blocked by Play Protect"*, tap **"More details"** and select **"Install anyway"**. If an older version of ANGEL AI was previously installed, uninstall it first to avoid signature conflicts.

---

## 5. How to Trigger a Manual Build (Without Pushing Code)

You can trigger a build at any time directly from the GitHub website:
1. Go to the **Actions** tab in your repository.
2. In the left sidebar, click on **"Build ANGEL AI APK"**.
3. Click the **"Run workflow"** button on the right.
4. Select the branch (e.g. `main`) and click the green **"Run workflow"** button.

---

## 6. How to Configure Optional Signed Release APK (GitHub Secrets)

The workflow **always** builds a ready-to-test Debug APK automatically. If you want a signed Release APK for Google Play or production distribution, configure the following secrets in GitHub:

1. In your GitHub repository, go to **Settings** -> **Secrets and variables** -> **Actions**.
2. Click **"New repository secret"** for each of the following:

| Secret Name | Description | Example / Format |
|---|---|---|
| `JARVIS_RELEASE_KEYSTORE_BASE64` | Base64-encoded release `.jks` or `.keystore` file | Run `base64 -w 0 your-release.keystore` |
| `JARVIS_RELEASE_STORE_PASSWORD` | Password for your release keystore | `YourStorePass123` |
| `JARVIS_RELEASE_KEY_ALIAS` | Key alias in the keystore | `angel_ai_release` |
| `JARVIS_RELEASE_KEY_PASSWORD` | Password for the key alias | `YourKeyPass123` |
| `JARVIS_RELEASE_CERT_SHA256` | 64-character SHA-256 fingerprint (uppercase, no colons) | `471D70465CD18B379BA3E516...` |
| `JARVIS_DEVELOPER_NAME` | Developer / Publisher name | `MD ASLAM` |
| `JARVIS_COPYRIGHT_OWNER` | Copyright owner name | `MD ASLAM` |

> 🔒 **Security Notice**: Never commit keystores, private signing passwords, or API keys directly to public GitHub repositories. GitHub Actions Secrets encrypt these values and keep them completely hidden from build logs.

### Optional API Keys for Pre-configured Builds:
You can also set these repository secrets to inject API keys at build time:
- `GEMINI_API_KEY`
- `GROK_API_KEY`
- `DEEPSEEK_API_KEY`
- `HIVE_API_KEY`
- `META_API_KEY`
- `GOOGLE_SERVICES_JSON` (Full content of `google-services.json` if building on private forks)

If release secrets are not configured, the workflow gracefully skips the release task and still produces the debug APK without failing.

---

## 7. How to Diagnose Failed Builds

If a build fails (red cross ❌ in Actions):
1. Click the failed workflow run in the **Actions** tab.
2. Click on the **Build Android APK** job.
3. Expand the red-highlighted step (e.g. `Build Debug APK` or `Build Release APK`).
4. Read the error message at the bottom of the log:
   - **Syntax or Compilation Error**: The log indicates the exact Kotlin/Java file and line number.
   - **Missing Dependencies**: Check network connectivity or repository declarations in `settings.gradle.kts`.
   - **Missing SDK Component**: Handled automatically by `android-actions/setup-android@v3`.
