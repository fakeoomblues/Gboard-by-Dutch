package dev.jason.gboardpatches.patches.gboard.features.calculator

import dev.jason.gboardpatches.patches.gboard.shared.GboardFieldTarget
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget

internal object GboardCalculatorTargets {
    val inputView = GboardFieldTarget(
        classType = "Lpsx;",
        name = "i",
        type = "Lcom/google/android/libraries/inputmethod/inputview/InputView;",
    )
    val onStartInputView = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onStartInputView",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "V",
    )
    val onUpdateSelection = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onUpdateSelection",
        parameterTypes = listOf("I", "I", "I", "I", "I", "I"),
        returnType = "V",
    )
    val onWindowHidden = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onWindowHidden",
        parameterTypes = emptyList(),
        returnType = "V",
    )
}
