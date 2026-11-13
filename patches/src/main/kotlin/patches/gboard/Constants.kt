package patches.gboard

import patches.gboard.gboard as gboardCompatibility

internal object PatchBuildInfo {
    const val VERSION = "18.5.11-port"
}

internal object Constants {
    const val GBOARD_PACKAGE_NAME = "com.google.android.inputmethod.latin"
    const val GBOARD_PATCHED_PACKAGE_NAME = "dev.jason.com.google.android.inputmethod.latin"
    const val GBOARD_PATCH_AUTHOR = "jasonwu1994"
    const val GBOARD_PATCH_AUTHOR_URL = "https://github.com/jasonwu1994"
    const val GBOARD_PATCH_REPOSITORY_URL = "https://github.com/jasonwu1994/Gboard-patches"
    val GBOARD_PATCH_VERSION = PatchBuildInfo.VERSION

    val COMPATIBILITY_GBOARD = gboardCompatibility
}
