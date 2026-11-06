package dev.jason.gboardpatches.patches.gboard.shared.accesspoint

import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget

internal object GboardAccessPointContributionsTargets {
    val orderFactory = GboardMethodTarget(
        classType = "Lngm;",
        name = "m",
        parameterTypes = listOf(
            "Landroid/content/Context;",
            "Lnub;",
            "Lojy;",
            "Lxae;",
            "Lxae;",
        ),
        returnType = "Lngm;",
    )

    val controllerConstructor = GboardMethodTarget(
        classType = "Lnib;",
        name = "<init>",
        parameterTypes = listOf("Landroid/content/Context;", "Lnji;"),
        returnType = "V",
    )
}
