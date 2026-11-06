package dev.jason.gboardpatches.patches.gboard.features.splitkeyboard

import dev.jason.gboardpatches.patches.gboard.shared.GboardFieldTarget
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.generated.GboardVersionBindings

internal object GboardSplitKeyboardTargets {
    const val ORDER_RETURN_TYPE = "Lwyw;"

    val splitEligibility get() = GboardVersionBindings.splitEligibility
    val splitDeviceEligibility get() = GboardVersionBindings.splitDeviceEligibility
    val splitWidthEligibility get() = GboardVersionBindings.splitWidthEligibility

    val accessPointName = GboardFieldTarget(
        classType = "Ltqr;",
        name = "d",
        type = "Ljava/lang/Object;",
    )
    val accessPointState = GboardMethodTarget(
        classType = "Ltqr;",
        name = "e",
        parameterTypes = listOf("I"),
        returnType = "V",
    )
    val accessPointOrder = GboardMethodTarget(
        classType = "Lnhm;",
        name = "c",
        parameterTypes = emptyList(),
        returnType = ORDER_RETURN_TYPE,
    )
    val modeAttach = GboardMethodTarget(
        classType = "Lqcc;",
        name = "E",
        parameterTypes = listOf("Lqba;", "Lpqb;", "Lpvf;", "Z"),
        returnType = "V",
    )
    val modeEnvironmentReload = GboardMethodTarget(
        classType = "Lqcc;",
        name = "Q",
        parameterTypes = emptyList(),
        returnType = "V",
    )
    val splitAction = GboardMethodTarget(
        classType = "Lqcc;",
        name = "W",
        parameterTypes = emptyList(),
        returnType = "V",
    )
}
