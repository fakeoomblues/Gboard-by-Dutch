package patches.deezertv

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.FormFactor
import app.morphe.patcher.patch.SupportedAbi

internal val deezerTv = Compatibility(
    name = "Deezer for Android TV",
    packageName = "deezer.android.tv",
    apkFileType = ApkFileType.APKM,
    description = "Leanback Deezer build. Shares the licence, advertising and tracking " +
        "modules with the phone app but has its own 10-foot UI, so the patches are " +
        "re-expressed against this build instead of copied from the phone bundle.",
    formFactor = FormFactor.TV,
    signatures = setOf("51a891a6c5d17b4e36a6f46bab675c1cae786a5af06f51d0105dab7ec6a94516"),
    targets = listOf(
        AppTarget(
            version = "4.4.2", minSdk = 23,
            versionCodes = mapOf(
                SupportedAbi.ARM64_V8A to 6251102,
                SupportedAbi.ARMEABI_V7A to 6251102,
            ),
        ),
    ),
)

internal val originalInput = patches.shared.originalInput(deezerTv)
