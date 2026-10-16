# 🚀 GitHub APK Auto-Build — Full Setup Guide (বাংলা ও English)

এই প্রজেক্টে GitHub Actions-এর মাধ্যমে সম্পূর্ণ অটোমেটিক Android APK বিল্ড কনফিগারেশন সেটআপ করা হয়েছে। আপনি গিটহাবে কোড পুশ করলেই স্বয়ংক্রিয়ভাবে ক্লাউডে Android APK তৈরি হবে এবং ডাউনলোড করতে পারবেন।

---

## 📋 ধাপ ১: GitHub-এ একটি নতুন Repository তৈরি করুন

1. [github.com](https://github.com)-এ লগইন করুন।
2. উপরে ডানদিকের **`+`** আইকনে ক্লিক করে **"New repository"** নির্বাচন করুন।
3. **Repository name** দিন (উদাহরণ: `angel-ai` বা `jarvis-assistant`)।
4. Repository অবশ্যই **Public** অথবা **Private** রাখতে পারেন।
5. ⚠️ **Initialize with README/gitignore টিক চিহ্ন দেবেন না** (কারণ আমাদের প্রজেক্টে এগুলো ইতিমধ্যে প্রস্তুত রয়েছে)।
6. **"Create repository"** বাটনে ক্লিক করুন।
7. আপনার রিপোসিটরির URL কপি করে নিন (যেমন: `https://github.com/your-username/angel-ai.git`)।

---

## 💻 ধাপ ২: কোড GitHub-এ পুশ করুন (Terminal Commands)

আপনার কম্পিউটারের টার্মিনাল বা AI Studio-র টার্মিনালে নিচের কমান্ডগুলো চালান:

```bash
# ১. গিটহাব রিমোট লিংক যোগ করুন (your-username ও repo-name আপনারটা দিয়ে পরিবর্তন করুন)
git remote add origin https://github.com/<YOUR-USERNAME>/<YOUR-REPO-NAME>.git

# ২. বর্তমান ব্রাঞ্চ 'main' নিশ্চিত করুন
git branch -M main

# ৩. গিটহাবে সব কোড পুশ করুন
git push -u origin main
```

> 💡 **নোট (GitHub Authentication)**: গিটহাব পাসওয়ার্ড গ্রহণ করে না। পাসওয়ার্ড চাইলে আপনার **GitHub Personal Access Token (PAT)** ব্যবহার করুন (GitHub Settings ➔ Developer Settings ➔ Personal Access Tokens ➔ Generate new token with `repo` permission)।

---

## ⚙️ ধাপ ৩: GitHub Actions-এ স্বয়ংক্রিয় APK বিল্ড দেখা

1. আপনার GitHub রিপোজিটরির **"Actions"** ট্যাবে যান।
2. আপনি দেখতে পাবেন **"Build ANGEL AI APK"** নামের ওয়ার্কফ্লো স্বয়ংক্রিয়ভাবে রান হচ্ছে।
3. এর উপর ক্লিক করলে রিয়েল-টাইম লগ ও বিল্ড প্রগ্রেস দেখতে পারবেন (সাধারণত ২-৩ মিনিট সময় নেয়)।
4. বিল্ড সফল হলে সবুজ টিক চিহ্ন (✅) দেখতে পাবেন।

---

## 📦 ধাপ ৪: APK ফাইল ডাউনলোড করা

1. **Actions** ট্যাবে গিয়ে সফল বিল্ডটিতে (✅) ক্লিক করুন।
2. পেজের একেবারে নিচে স্ক্রোল করে **"Artifacts"** সেকশনে যান।
3. **`ANGEL-AI-APK`** নামের ফাইলে ক্লিক করুন — একটি ZIP ফাইল ডাউনলোড হবে।
4. ZIP ফাইল আনজিপ করলেই ভেতরে আপনার ইনস্টলযোগ্য **`app-debug.apk`** পেয়ে যাবেন!
5. এটি আপনার অ্যান্ড্রয়েড মোবাইলে নিয়ে সরাসরি ইনস্টল করুন।

> 📱 **মোবাইল ইনস্টলেশন টিপস**: ফোনে ইনস্টল করার সময় যদি *Google Play Protect* সতর্কবার্তা দেয়, তবে **"More details"**-এ ক্লিক করে **"Install anyway"** চাপুন।

---

## 🎯 ধাপ ৫: কোড পরিবর্তন ছাড়াই সরাসরি GitHub থেকে APK তৈরি করা (Manual Build)

আপনি কোনো নতুন কোড পুশ না করেও যেকোনো সময় GitHub ওয়েবসাইট থেকে সরাসরি APK বিল্ড করতে পারবেন:

1. GitHub-এ আপনার রিপোজিটরির **Actions** ট্যাবে যান।
2. বামদিকের মেনু থেকে **"Build ANGEL AI APK"** সিলেক্ট করুন।
3. ডানদিকের **"Run workflow"** ড্রপডাউন বাটনে ক্লিক করুন।
4. **Build Type** সিলেক্ট করুন (`all`, `debug`, অথবা `release`)।
5. সবুজ রঙের **"Run workflow"** বাটনে চাপ দিন — সাথে সাথে নতুন APK তৈরি শুরু হয়ে যাবে!

---

## 🏷️ ধাপ ৬: রিলিজ ট্যাগ দিয়ে সরাসরি APK রিলিজ করা (One-Click Direct Download)

আপনি যদি ট্যাগ পুশ করেন:
```bash
git tag v1.8.8
git push origin v1.8.8
```
GitHub Actions স্বয়ংক্রিয়ভাবে আপনার GitHub রিপোসিটরির **Releases** পেজে সরাসরি ডাউনলোডযোগ্য APK ফাইল আপলোড করে দেবে। তখন কাউকে ZIP ডাউনলোড করতে হবে না, সরাসরি `.apk` লিংকে ক্লিক করে নামিয়ে নেওয়া যাবে।

---

## 🔐 ধাপ ৭ (ঐচ্ছিক): Release Keystore ও API Keys সেটআপ (GitHub Secrets)

অটোমেটিক বিল্ডটি কোনো সিক্রেট ছাড়াও সম্পূর্ণ কার্যকর `app-debug.apk` সফলভাবে তৈরি করে। কিন্তু আপনি যদি প্রোডাকশন সাইনড Release APK অথবা ক্লাউড API কি যুক্ত করতে চান:

1. GitHub রিপোজিটরির **Settings** ➔ **Secrets and variables** ➔ **Actions**-এ যান।
2. **"New repository secret"**-এ ক্লিক করে নিচের সিক্রেটগুলো যোগ করতে পারেন:

| Secret Name | বিবরণ |
|---|---|
| `GEMINI_API_KEY` | আপনার Gemini API Key |
| `GROK_API_KEY` | xAI Grok API Key (ঐচ্ছিক) |
| `DEEPSEEK_API_KEY` | DeepSeek API Key (ঐচ্ছিক) |
| `JARVIS_RELEASE_KEYSTORE_BASE64` | Base64 এনকোডেড Release Keystore (`base64 -w 0 my.keystore`) |
| `JARVIS_RELEASE_STORE_PASSWORD` | Keystore পাসওয়ার্ড |
| `JARVIS_RELEASE_KEY_ALIAS` | Key Alias |
| `JARVIS_RELEASE_KEY_PASSWORD` | Key পাসওয়ার্ড |
| `JARVIS_RELEASE_CERT_SHA256` | SHA256 Fingerprint (Uppercase, no colons) |

---

## 🛠️ ফাইল ও কনফিগারেশন চেকলিস্ট
- ✅ **GitHub Actions Workflow File**: `/.github/workflows/android-apk.yml` (সম্পূর্ণ প্রস্তুত)
- ✅ **Gradle Wrapper ও Permissions**: `./gradlew` (Executable mode 100755)
- ✅ **Git Commit Status**: প্রাথমিক কমিট সম্পূর্ণ প্রস্তুত (`main` branch)
- ✅ **Build Compatibility**: JDK 17, Android SDK 34, Gradle 8.10.2
