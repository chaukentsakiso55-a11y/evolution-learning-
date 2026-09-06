# Evolution Learning v5 — Firebase setup

## Android identity
- Package/application ID: `com.cyberpulse.evolutionlearning`
- Firebase project: `evolution-learning-d42bd`
- Current client configuration: `app/google-services.json`

## Enabled and used
1. Firebase Authentication — Email/Password only.
2. Cloud Firestore — primary app database.
3. Password reset emails — Firebase Authentication.

## Firestore data model
- `users/{uid}` — profile (`name`, `email`, `grade`, timestamps)
- `users/{uid}/progress/current` — real progress counters
- `users/{uid}/goals/{goalId}` — study goals
- `users/{uid}/homework/{itemId}` — homework tracker

All progress starts at zero. The app increments progress only when real activity finishes.

## Firestore rules already configured
Use user-isolated rules so a signed-in user can only access their own `users/{uid}` tree.

## Realtime Database
The current Firebase config includes a Realtime Database URL, but Evolution Learning v5 does not use Realtime Database yet. Firestore is the source of truth.

## App Check
Do not enforce App Check yet. After a signed APK/project exists:
1. Run `./gradlew signingReport`.
2. Copy the SHA-256 fingerprint for the desired signing certificate.
3. Firebase Console → App Check → Evolution Learning → Play Integrity.
4. Register the SHA-256.
5. Test App Check before enabling enforcement.

## AI
AI is intentionally not simulated in this build. Add a protected backend/model integration later; do not put private AI provider keys in the Android client.
