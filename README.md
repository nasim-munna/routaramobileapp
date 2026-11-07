# Routara mobile

The field app for sales reps, built with Kotlin and Jetpack Compose. Reps can
work offline all day: visits, orders and new outlets are saved on the phone and
sync when there's a connection. The server ignores repeats, so a retry can't
create duplicates.

## Build

Open this folder in Android Studio, or run:

    gradlew assembleDebug

The debug build talks to `http://127.0.0.1:8000`. To reach a backend on your
PC, connect the phone by USB and run `adb reverse tcp:8000 tcp:8000`, which
also works for an emulator. For a release build, set `routara.apiUrl` and
`routara.wsUrl` in `gradle.properties` to your server, then add a signing key.
Keep that key safe: losing it means you can't update the app on Google Play.

## Tests

`gradlew recordRoborazziDebug` renders every screen to `app/build/screens`, so
the design can be reviewed with no phone.

`ApiContractTest` runs a full field day through the app's real network code
against a running backend. It is skipped unless these are set:

    ROUTARA_TEST_API=http://127.0.0.1:8000/api/v1/
    ROUTARA_TEST_EMAIL=<a field account with a beat planned today>
    ROUTARA_TEST_PASSWORD=<its password>

It writes real records and closes that account's day, so never point it at
production.

## Location

Live location is shared only while a rep is checked in, with an ongoing
notification showing that it's on. It stops at check-out or day end.
