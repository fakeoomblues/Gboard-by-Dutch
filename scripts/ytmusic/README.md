# Gearslip caller validation

The compatibility patch authenticates only configured host packages using the `clientUid` supplied to `MusicBrowserService.onGetRoot`, forwarded unchanged into the app's allowlist method. It retains the actual caller package and never substitutes Google's Android Auto package. Its separate eligibility hook enables browsing for that configured client after authentication. Other clients retain the original methods.

`packageNames` defaults to `app.seb3thehacker.gearslip`. A separate Gearslip test copy must be explicitly configured, for example `app.seb3thehacker.gearslip.morphetest`. That package is not a default trusted host. Explicit manifest queries make those configured package lookups visible on Android 11 and newer.

Run the JVM extension policy checks:

```sh
python3 -m unittest tests/test_gearslip_ytm_policy.py
```

Inspect actual emitted APK DEX after applying the bundle:

```sh
python3 scripts/ytmusic/verify_gearslip_dex.py patched.apk --version 9.30.53 --apkanalyzer /path/to/android-sdk/cmdline-tools/latest/bin/apkanalyzer
python3 scripts/ytmusic/verify_gearslip_dex.py patched.apk --version 9.44.52 --apkanalyzer /path/to/android-sdk/cmdline-tools/latest/bin/apkanalyzer
```

The same checks fail on both supplied inputs before patching with `Missing actual-callback-UID trust hook`. Structural inspection is not execution.

`CallerPolicyProbe.java` is an Android instrumentation runner for the real patched DEX. Build a small probe APK with this runner and its manifest `instrumentation` target set to a separately installed patched YT Music test copy. Sign the probe with the same test key as the target. Invoke it with `am instrument -w -e host app.seb3thehacker.gearslip.morphetest <probe-package>/nl.johninnl.ytmusic.probe.CallerPolicyProbe` after explicitly configuring that host in the patch. The probe uses the target's actual classloader, actual obfuscated methods, and real PackageManager UID ownership. It verifies:

- A configured installed host is admitted before dereferencing the server flag or certificate dependencies.
- A configured host string under a different UID is rejected, including an invalid UID.
- Browser eligibility succeeds for the host and is unchanged for an unrelated client.
- An unrelated trust request reaches the original policy. The deliberately unset flag provider raises an expected NPE; this proves fallback preservation, not a runtime rejection result.

To validate the real Binder path separately, connect the configured host's `MediaBrowserCompat` to the patched target's `com.google.android.apps.youtube.music.mediabrowser.MusicBrowserService`. Record the callback package and UID from the service's callback or the compat transport, and verify `PackageManager.getPackagesForUid(clientUid)` contains exactly that claimed package. Do not read `Binder.getCallingUid()` later from a handler callback: it can be the service's own UID. A test app claiming the host's package while using another UID must fail; an unrelated correctly identified client must retain the stock result. Capture root ID, children result, metadata and play/pause separately. A reflective policy check does not establish successful browsing, authenticated content, playback or vehicle projection.

The matching signing key for the existing installed Morphe YT Music copy is unavailable locally. Do not replace or uninstall that account-bearing app. Parent-controlled validation uses separately installed copies. No emulator is required or recreated by these tools.
