<div align="center">

# 🌿 Nivora For Focus

### Focus with intention. Build habits that last.

A calm, plant-inspired Android productivity app to help you plan your day, focus on what matters, and track your progress.

<p>
  <a href="https://github.com/aalokkumarg8808/Plant-Focus-Study">
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  </a>
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Storage-Room%20%2B%20Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" alt="Room and Supabase">
</p>

[Features](#-features) · [Tech Stack](#-tech-stack) · [Getting Started](#-getting-started) · [Configuration](#-configuration) · [Project Structure](#-project-structure) · [Roadmap](#-roadmap)

</div>

---

## ✨ About Nivora

Nivora For Focus is an Android productivity app built around a simple idea: make it easier to show up consistently. Organize tasks, build habits, plan goals, and run focus sessions in one place—wrapped in a clean, botanical-inspired interface.

> [!NOTE]
> This repository contains the Android app source code. Features that depend on external configuration, such as Supabase cloud sync, require your own project credentials before they can work.

## 🌱 Features

<details open>
<summary><strong>Focus & productivity</strong></summary>

- ⏱️ Focus sessions to support distraction-free work.
- 🔔 Focus reminders and scheduled focus sessions.
- ✅ Task creation, editing, categories, due dates, and optional reminders.
- 🌿 Habit tracking and habit logs.
- 🎯 Goals and milestones to break bigger targets into manageable steps.
- 🏆 Achievements and progress insights.
</details>

<details>
<summary><strong>Personalization & account</strong></summary>

- 👤 Profile and account screens.
- 🎨 Light/dark theme preference support.
- 🖼️ Profile avatar optimization.
- 🔊 Soundscape support for focus sessions.
</details>

<details>
<summary><strong>Data & sync</strong></summary>

- 💾 Local persistence using Room/SQLite.
- ☁️ Supabase integration points for authentication and cloud synchronization.
- 🔐 Row Level Security (RLS) policies in the supplied Supabase schema.
- 🔄 A sync manager intended to coordinate local and cloud data.

Cloud sync behavior depends on correct Supabase configuration and policy setup. Review the code and test with your own project before relying on it for important data.
</details>

## 🧰 Tech Stack

| Area | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Android architecture | ViewModel, Kotlin Coroutines and Flow |
| Local database | Room / SQLite |
| Networking | OkHttp, Retrofit, Moshi |
| Image loading | Coil |
| Cloud backend | Supabase (optional configuration) |
| Build system | Gradle Kotlin DSL, Version Catalog |
| Tests | JUnit, Robolectric, Compose UI testing dependencies |

## 🚀 Getting Started

### Requirements

- Android Studio with a compatible Android SDK.
- JDK version supported by the project's Android Gradle Plugin.
- Git.
- An Android emulator or physical Android device.
- A Supabase project only if you want to test cloud-backed features.

### 1. Clone the repository

```bash
git clone https://github.com/aalokkumarg8808/Plant-Focus-Study.git
cd Plant-Focus-Study
```

### 2. Open in Android Studio

1. Open the cloned project folder.
2. Allow Gradle sync to complete.
3. Review any SDK or JDK compatibility messages.
4. Select an emulator or connect an Android device with USB debugging enabled.

### 3. Configure secrets

The repository includes `.env.example` as a template. Copy it to a local `.env` file and replace placeholders with your own values if the app requires them.

**Never commit real secrets.** Do not put Supabase service-role keys, database passwords, signing-key passwords, or other private credentials in source code or a public repository. A mobile app's publishable/anonymous key is not a substitute for database security—use correct RLS policies.

See [Configuration](#-configuration) before enabling cloud features.

### 4. Build and run

From Android Studio, select **Run** for the app configuration.

Or, from a terminal where the Android SDK and Java environment are configured:

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

The debug APK is typically generated under:

```text
app/build/outputs/apk/debug/
```

> [!IMPORTANT]
> These are the expected build steps, not a claim that a fresh build has already been verified on every machine. Resolve any Gradle, JDK, SDK, or dependency errors reported by your local environment.

## 🔧 Configuration

<details>
<summary><strong>Supabase setup</strong></summary>

1. Create a Supabase project.
2. Review `supabase/schema.sql` and the migration files before applying them.
3. Apply the schema/migrations to your own project using the Supabase SQL Editor or your chosen migration workflow.
4. Put the project URL and **publishable key** in your local `.env` file using the variable names expected by `.env.example`.
5. Check the app's Supabase configuration code and confirm the build picks up those values.
6. Test authentication, row ownership, uploads, and sync using test accounts.

**Security checklist**
- [ ] RLS is enabled for every table exposed to clients.
- [ ] Policies restrict users to their own records.
- [ ] Storage policies are reviewed for the intended avatar visibility.
- [ ] No service-role key or database password is shipped in the Android app.
- [ ] Real credentials are not committed in any branch or Git history.

</details>

<details>
<summary><strong>Signing a release build</strong></summary>

Use a private upload keystore and supply its path and passwords through local environment variables or a secure CI secret store. Do not commit keystore files or signing credentials. Review `app/build.gradle.kts` and your local build configuration before creating a release.

</details>

## 🗂️ Project Structure

```text
Plant-Focus-Study/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/
│       │   │   ├── data/          # Local data, repositories, Supabase
│       │   │   ├── notification/  # Focus notifications and reminders
│       │   │   ├── ui/            # Compose screens, components, theme
│       │   │   ├── util/          # Helpers and utilities
│       │   │   └── MainActivity.kt
│       │   └── res/               # Android resources and app label
│       ├── test/                  # Unit and Robolectric tests
│       └── androidTest/           # Instrumented tests
├── gradle/                        # Gradle wrapper and version catalog
├── supabase/
│   ├── schema.sql
│   └── migrations/
├── .env.example
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## 🧭 Roadmap

Progress below reflects project intentions; unchecked items are not claims that the work is complete.

- [x] Rename user-facing app branding to **Nivora For Focus**.
- [x] Provide a Supabase schema and migration files.
- [x] Include local Room/SQLite persistence.
- [ ] Verify a clean debug build from a fresh clone.
- [ ] Add screenshots or a short demo GIF.
- [ ] Test cloud sync and conflict handling across multiple devices.
- [ ] Complete a security review of authentication, RLS, and storage policies.
- [ ] Prepare and test a signed release build.

## 🧪 Testing

Run tests from Android Studio, or use Gradle:

```bash
# Windows
gradlew.bat test

# macOS / Linux
./gradlew test
```

Instrumented tests may require an emulator or connected Android device.

## 🤝 Contributing

Contributions, bug reports, and thoughtful suggestions are welcome.

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/your-feature`.
3. Make focused changes and test them.
4. Commit with a clear message.
5. Open a pull request describing the change and any testing performed.

For larger changes, open an issue first so the approach can be discussed.

## 🔒 Security

Please do not report private credentials in public issues. If you discover a security issue, avoid sharing exploit details publicly until the issue can be addressed. Rotate any credential that may have been exposed.

## 📄 License

No license has been specified in this repository yet. Unless a license is added, assume that standard copyright applies and reuse or redistribution is not automatically permitted.

---

<div align="center">

**Made to help you focus, one session at a time. 🌿**

[⬆ Back to top](#-nivora-for-focus)

</div>
