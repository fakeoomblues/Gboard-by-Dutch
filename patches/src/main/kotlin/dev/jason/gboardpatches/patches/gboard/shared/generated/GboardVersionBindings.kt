package dev.jason.gboardpatches.patches.gboard.shared.generated

import dev.jason.gboardpatches.patches.gboard.shared.GboardFieldTarget
import dev.jason.gboardpatches.patches.gboard.shared.GboardMethodTarget
import dev.jason.gboardpatches.patches.gboard.shared.GboardTypeTarget

internal object GboardVersionBindings {
    const val targetVersion = "18.5.11"
    const val profileVersion = "1.7.0"
    const val sourceSha256 = "ported-from-18.5.4"

    val `apCatalogCopy` = GboardMethodTarget(
        classType = "Lxae;",
        name = "n",
        parameterTypes = listOf("Ljava/util/Collection;"),
        returnType = "Lxae;",
    )

    val `apControllerOrderField` = GboardFieldTarget(
        classType = "Lnib;",
        name = "g",
        type = "Lnhm;",
    )

    val `apDescriptorAction` = GboardMethodTarget(
        classType = "Lnes;",
        name = "q",
        parameterTypes = listOf("Ljava/lang/Runnable;"),
        returnType = "V",
    )

    val `apDescriptorActions` = GboardFieldTarget(
        classType = "Lnes;",
        name = "e",
        type = "Lwzd;",
    )

    val `apDescriptorBuild` = GboardMethodTarget(
        classType = "Lnes;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "Lnex;",
    )

    val `apDescriptorFactory` = GboardMethodTarget(
        classType = "Lnex;",
        name = "c",
        parameterTypes = emptyList(),
        returnType = "Lnes;",
    )

    val `apDescriptorIcon` = GboardMethodTarget(
        classType = "Lnes;",
        name = "i",
        parameterTypes = listOf("I"),
        returnType = "V",
    )

    val `apDescriptorLabel` = GboardFieldTarget(
        classType = "Lnes;",
        name = "d",
        type = "Ljava/lang/String;",
    )

    val `apDescriptorRegister` = GboardMethodTarget(
        classType = "Lnib;",
        name = "g",
        parameterTypes = listOf("Lnex;", "Z"),
        returnType = "V",
    )

    val `apDescriptorToken` = GboardMethodTarget(
        classType = "Lnes;",
        name = "l",
        parameterTypes = listOf("Ljava/lang/String;"),
        returnType = "V",
    )

    val `apMasterCatalogSet` = GboardFieldTarget(
        classType = "Lnhm;",
        name = "c",
        type = "Lxae;",
    )

    val `calculatorHidden` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onWindowHidden",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `calculatorInputStart` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onStartInputView",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "V",
    )

    val `calculatorInputView` = GboardFieldTarget(
        classType = "Lpsx;",
        name = "i",
        type = "Lcom/google/android/libraries/inputmethod/inputview/InputView;",
    )

    val `calculatorSelection` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onUpdateSelection",
        parameterTypes = listOf("I", "I", "I", "I", "I", "I"),
        returnType = "V",
    )

    val `clipboardAdapterContext` = GboardFieldTarget(
        classType = "Lgbs;",
        name = "e",
        type = "Landroid/content/Context;",
    )

    val `clipboardAdapterItems` = GboardFieldTarget(
        classType = "Lgbs;",
        name = "n",
        type = "Ljava/util/List;",
    )

    val `clipboardAdapterRecycler` = GboardFieldTarget(
        classType = "Lgbs;",
        name = "j",
        type = "Landroid/support/v7/widget/RecyclerView;",
    )

    val `clipboardCleanupCursor` = GboardMethodTarget(
        classType = "Lgcc;",
        name = "t",
        parameterTypes = listOf("Landroid/database/Cursor;"),
        returnType = "V",
    )

    val `clipboardClipId` = GboardFieldTarget(
        classType = "Lgap;",
        name = "d",
        type = "J",
    )

    val `clipboardClipModel` = GboardFieldTarget(
        classType = "Lgap;",
        name = "g",
        type = "Lgar;",
    )

    val `clipboardClipTimestamp` = GboardFieldTarget(
        classType = "Lgap;",
        name = "e",
        type = "Lj${'$'}/time/Instant;",
    )

    val `clipboardColumns` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/clipboard/ClipboardKeyboard;",
        name = "l",
        parameterTypes = emptyList(),
        returnType = "I",
    )

    val `clipboardDelete` = GboardMethodTarget(
        classType = "Lgbw;",
        name = "h",
        parameterTypes = listOf("Landroid/content/Context;", "Ljava/util/List;"),
        returnType = "V",
    )

    val `clipboardGridManagerConstructor` = GboardMethodTarget(
        classType = "Lgck;",
        name = "<init>",
        parameterTypes = listOf("Lcom/google/android/apps/inputmethod/libs/clipboard/ClipboardKeyboard;", "I", "Landroid/support/v7/widget/RecyclerView;"),
        returnType = "V",
    )

    val `clipboardHolderText` = GboardFieldTarget(
        classType = "Lgbq;",
        name = "t",
        type = "Landroid/widget/TextView;",
    )

    val `clipboardIsPinned` = GboardMethodTarget(
        classType = "Lgap;",
        name = "n",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `clipboardIsSensitive` = GboardMethodTarget(
        classType = "Lgap;",
        name = "m",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `clipboardIsSpecial` = GboardMethodTarget(
        classType = "Lgap;",
        name = "o",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `clipboardItemBind` = GboardMethodTarget(
        classType = "Lgbs;",
        name = "p",
        parameterTypes = listOf("Lkr;", "I"),
        returnType = "V",
    )

    val `clipboardKeyboardAdapter` = GboardFieldTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/clipboard/ClipboardKeyboard;",
        name = "c",
        type = "Lgbs;",
    )

    val `clipboardKeyboardContext` = GboardFieldTarget(
        classType = "Lcom/google/android/libraries/inputmethod/keyboard/impl/AbstractKeyboard;",
        name = "z",
        type = "Landroid/content/Context;",
    )

    val `clipboardLayoutBuilt` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/clipboard/ClipboardKeyboard;",
        name = "e",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Ljava/lang/Object;"),
        returnType = "V",
    )

    val `clipboardLoader` = GboardMethodTarget(
        classType = "Lgas;",
        name = "call",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/Object;",
    )

    val `clipboardLoaderContext` = GboardFieldTarget(
        classType = "Lgat;",
        name = "b",
        type = "Landroid/content/Context;",
    )

    val `clipboardLoaderOwner` = GboardFieldTarget(
        classType = "Lgas;",
        name = "a",
        type = "Lgat;",
    )

    val `clipboardModelSensitive` = GboardFieldTarget(
        classType = "Lgar;",
        name = "h",
        type = "Z",
    )

    val `clipboardModelText` = GboardFieldTarget(
        classType = "Lgar;",
        name = "a",
        type = "Ljava/lang/String;",
    )

    val `clipboardModelType` = GboardFieldTarget(
        classType = "Lgar;",
        name = "c",
        type = "I",
    )

    val `clipboardNotifyRemoved` = GboardMethodTarget(
        classType = "Ljt;",
        name = "n",
        parameterTypes = listOf("I"),
        returnType = "V",
    )

    val `clipboardPinnedCount` = GboardFieldTarget(
        classType = "Lgbs;",
        name = "z",
        type = "I",
    )

    val `clipboardPinnedHeader` = GboardFieldTarget(
        classType = "Lgap;",
        name = "b",
        type = "Lgap;",
    )

    val `clipboardPreferences` = GboardMethodTarget(
        classType = "Lrgy;",
        name = "I",
        parameterTypes = listOf("Landroid/content/Context;"),
        returnType = "Lrgy;",
    )

    val `clipboardPrimaryTimestamp` = GboardMethodTarget(
        classType = "Lgcc;",
        name = "a",
        parameterTypes = listOf("Landroid/content/Context;"),
        returnType = "J",
    )

    val `clipboardPrune` = GboardMethodTarget(
        classType = "Lgcc;",
        name = "g",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `clipboardPruneContext` = GboardFieldTarget(
        classType = "Lgcc;",
        name = "c",
        type = "Landroid/content/Context;",
    )

    val `clipboardPruneCount` = GboardFieldTarget(
        classType = "Lgcc;",
        name = "g",
        type = "Ljava/util/concurrent/atomic/AtomicInteger;",
    )

    val `clipboardPruneDisabled` = GboardFieldTarget(
        classType = "Lgcc;",
        name = "f",
        type = "Z",
    )

    val `clipboardQuery` = GboardMethodTarget(
        classType = "Lgbw;",
        name = "j",
        parameterTypes = listOf("Landroid/content/Context;", "Ljava/lang/String;", "[Ljava/lang/String;", "Ljava/lang/String;"),
        returnType = "Lwyw;",
    )

    val `clipboardQueryType` = GboardMethodTarget(
        classType = "Lgcc;",
        name = "s",
        parameterTypes = listOf("Landroid/net/Uri;", "I"),
        returnType = "Landroid/database/Cursor;",
    )

    val `clipboardReadTimestamp` = GboardMethodTarget(
        classType = "Lchu;",
        name = "m",
        parameterTypes = listOf("I", "J"),
        returnType = "J",
    )

    val `clipboardRecentCount` = GboardFieldTarget(
        classType = "Lgbs;",
        name = "o",
        type = "I",
    )

    val `clipboardRecentHeader` = GboardFieldTarget(
        classType = "Lgap;",
        name = "a",
        type = "Lgap;",
    )

    val `clipboardRecyclerManager` = GboardFieldTarget(
        classType = "Landroid/support/v7/widget/RecyclerView;",
        name = "l",
        type = "Lka;",
    )

    val `clipboardRefresh` = GboardMethodTarget(
        classType = "Lgbs;",
        name = "R",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `clipboardSetSpan` = GboardMethodTarget(
        classType = "Landroid/support/v7/widget/StaggeredGridLayoutManager;",
        name = "N",
        parameterTypes = listOf("I"),
        returnType = "V",
    )

    val `clipboardSpanCount` = GboardFieldTarget(
        classType = "Landroid/support/v7/widget/StaggeredGridLayoutManager;",
        name = "a",
        type = "I",
    )

    val `clipboardSpecialHeader` = GboardFieldTarget(
        classType = "Lgap;",
        name = "c",
        type = "Lgap;",
    )

    val `clipboardTrim` = GboardMethodTarget(
        classType = "Lgbs;",
        name = "F",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `clipboardUri` = GboardMethodTarget(
        classType = "Lgbw;",
        name = "d",
        parameterTypes = listOf("Landroid/content/Context;", "I", "J"),
        returnType = "Landroid/net/Uri;",
    )

    val `clipboardWriteTimestamp` = GboardMethodTarget(
        classType = "Lchu;",
        name = "s",
        parameterTypes = listOf("I", "J"),
        returnType = "V",
    )

    val `cursorResourcePreference` = GboardMethodTarget(
        classType = "Lchu;",
        name = "x",
        parameterTypes = listOf("I", "Z"),
        returnType = "Z",
    )

    val `cursorStringPreference` = GboardMethodTarget(
        classType = "Lrgy;",
        name = "am",
        parameterTypes = listOf("Ljava/lang/String;", "Z", "Z"),
        returnType = "Z",
    )

    val `customSymbolsAdapterBind` = GboardMethodTarget(
        classType = "Ljuc;",
        name = "p",
        parameterTypes = listOf("Lkr;", "I"),
        returnType = "V",
    )

    val `customSymbolsAdapterConstructor` = GboardMethodTarget(
        classType = "Ljuc;",
        name = "<init>",
        parameterTypes = listOf("Landroid/content/Context;", "Ljuv;", "Ljava/util/function/Consumer;", "I", "I"),
        returnType = "V",
    )

    val `customSymbolsAdapterCreate` = GboardMethodTarget(
        classType = "Ljuc;",
        name = "d",
        parameterTypes = listOf("Landroid/view/ViewGroup;", "I"),
        returnType = "Lkr;",
    )

    val `customSymbolsAdapterViewType` = GboardMethodTarget(
        classType = "Ljt;",
        name = "gW",
        parameterTypes = listOf("I"),
        returnType = "I",
    )

    val `customSymbolsBodyReady` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonKeyboardM2;",
        name = "eR",
        parameterTypes = listOf("Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;", "Lcom/google/android/libraries/inputmethod/metadata/KeyboardViewDef;"),
        returnType = "V",
    )

    val `customSymbolsCategoryBind` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonKeyboardM2;",
        name = "D",
        parameterTypes = listOf("Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonRecyclerView;", "Ljava/lang/String;"),
        returnType = "V",
    )

    val `customSymbolsCategoryChange` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonKeyboardM2;",
        name = "E",
        parameterTypes = listOf("I", "I"),
        returnType = "V",
    )

    val `customSymbolsCategoryName` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonKeyboardM2;",
        name = "l",
        parameterTypes = listOf("I"),
        returnType = "Ljava/lang/String;",
    )

    val `customSymbolsCorpus` = GboardMethodTarget(
        classType = "Lhjs;",
        name = "a",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "Lwyw;",
    )

    val `customSymbolsCorpusItemExtension` = GboardFieldTarget(
        classType = "Loss;",
        name = "g",
        type = "Ljava/lang/Class;",
    )

    val `customSymbolsCorpusItemKeyboard` = GboardFieldTarget(
        classType = "Loss;",
        name = "c",
        type = "Lqnm;",
    )

    val `customSymbolsFooterClick` = GboardMethodTarget(
        classType = "Lhjx;",
        name = "accept",
        parameterTypes = listOf("Ljava/lang/Object;"),
        returnType = "V",
    )

    val `customSymbolsHeaderCallback` = GboardMethodTarget(
        classType = "Lgza;",
        name = "accept",
        parameterTypes = listOf("Ljava/lang/Object;"),
        returnType = "V",
    )

    val `customSymbolsHeaderGuard` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/expression/header/ConstraintHeaderViewImpl;",
        name = "n",
        parameterTypes = listOf("Lham;"),
        returnType = "V",
    )

    val `customSymbolsHistoryCase` = GboardFieldTarget(
        classType = "Ljsk;",
        name = "${'$'}cid",
        type = "I",
    )

    val `customSymbolsHistoryConsumer` = GboardMethodTarget(
        classType = "Ljsk;",
        name = "accept",
        parameterTypes = listOf("Ljava/lang/Object;"),
        returnType = "V",
    )

    val `customSymbolsHistoryReceiver` = GboardFieldTarget(
        classType = "Ljsk;",
        name = "a",
        type = "Ljava/lang/Object;",
    )

    val `customSymbolsHistorySink` = GboardMethodTarget(
        classType = "Lhby;",
        name = "b",
        parameterTypes = listOf("Ljava/lang/String;"),
        returnType = "V",
    )

    val `customSymbolsIdentityFactory` = GboardMethodTarget(
        classType = "Lqaz;",
        name = "ag",
        parameterTypes = listOf("Ljava/lang/Object;"),
        returnType = "Lqch;",
    )

    val `customSymbolsKeyboardReady` = GboardMethodTarget(
        classType = "Lpnc;",
        name = "a",
        parameterTypes = listOf("Lpwe;", "Lcom/google/android/libraries/inputmethod/metadata/KeyboardDef;", "Lqnm;"),
        returnType = "V",
    )

    val `customSymbolsM2Constructor` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonKeyboardM2;",
        name = "<init>",
        parameterTypes = listOf("Landroid/content/Context;", "Lpwf;", "Lcom/google/android/libraries/inputmethod/metadata/KeyboardDef;", "Lcom/google/android/libraries/inputmethod/metadata/ImeDef;", "Lqnm;"),
        returnType = "V",
    )

    val `customSymbolsM2HistoryField` = GboardFieldTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonKeyboardM2;",
        name = "b",
        type = "Lhby;",
    )

    val `customSymbolsMetrics` = GboardMethodTarget(
        classType = "Lisn;",
        name = "b",
        parameterTypes = listOf("Lqnm;"),
        returnType = "Lxrw;",
    )

    val `customSymbolsNavigation` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/expression/navbar/ScrollableNavigationView;",
        name = "a",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Lqnm;", "Z", "Ljava/util/function/Consumer;", "Landroid/os/Parcelable;", "Ljava/util/function/Supplier;", "Ljava/util/function/Supplier;"),
        returnType = "V",
    )

    val `customSymbolsProviderBefore` = GboardMethodTarget(
        classType = "Lpwm;",
        name = "b",
        parameterTypes = listOf("Lqnm;", "Lcom/google/android/libraries/inputmethod/metadata/KeyboardDef;"),
        returnType = "Lpwe;",
    )

    val `customSymbolsProviderCreated` = GboardMethodTarget(
        classType = "Lpwm;",
        name = "a",
        parameterTypes = listOf("Lqnm;", "Lpwe;", "Lcom/google/android/libraries/inputmethod/metadata/KeyboardDef;"),
        returnType = "V",
    )

    val `customSymbolsProviderValid` = GboardMethodTarget(
        classType = "Lpwm;",
        name = "c",
        parameterTypes = listOf("Lqnm;"),
        returnType = "Z",
    )

    val `customSymbolsRecyclerAttach` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonRecyclerView;",
        name = "aL",
        parameterTypes = listOf("Ljuv;", "Ljava/util/function/Consumer;"),
        returnType = "V",
    )

    val `customSymbolsSelectedIndex` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/search/emoticon/EmoticonKeyboardM2;",
        name = "k",
        parameterTypes = listOf("Lwyw;"),
        returnType = "I",
    )

    val `customSymbolsViewTransform` = GboardMethodTarget(
        classType = "Ltxp;",
        name = "j",
        parameterTypes = listOf("Landroid/graphics/Matrix;", "Landroid/view/View;", "Landroid/view/View;"),
        returnType = "Landroid/view/View;",
    )

    val `customThemeFragmentPeerField` = GboardFieldTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/theme/listing/ThemeListingFragment;",
        name = "a",
        type = "Lkst;",
    )

    val `customThemeLauncherLaunch` = GboardMethodTarget(
        classType = "Lsgz;",
        name = "H",
        parameterTypes = listOf("Landroid/content/Intent;", "I", "Landroid/os/Bundle;"),
        returnType = "V",
    )

    val `customThemePeerContextField` = GboardFieldTarget(
        classType = "Lkst;",
        name = "b",
        type = "Landroid/content/Context;",
    )

    val `customThemePeerLauncherField` = GboardFieldTarget(
        classType = "Lkst;",
        name = "s",
        type = "Lsgz;",
    )

    val `customThemePeerResultHandler` = GboardMethodTarget(
        classType = "Lkst;",
        name = "i",
        parameterTypes = listOf("Landroid/content/Intent;"),
        returnType = "V",
    )

    val `customThemePlusHandler` = GboardMethodTarget(
        classType = "Lksz;",
        name = "f",
        parameterTypes = listOf("Lkst;", "Lktc;", "I"),
        returnType = "V",
    )

    val `customThemeResultHandler` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/theme/listing/ThemeListingFragment;",
        name = "X",
        parameterTypes = listOf("I", "I", "Landroid/content/Intent;"),
        returnType = "V",
    )

    val `customThemeValidator` = GboardMethodTarget(
        classType = "Lrzb;",
        name = "h",
        parameterTypes = listOf("Ljava/io/File;"),
        returnType = "Z",
    )

    val `editingAccessPointsApController` = GboardMethodTarget(
        classType = "Lnib;",
        name = "<init>",
        parameterTypes = listOf("Landroid/content/Context;", "Lnji;"),
        returnType = "V",
    )

    val `editingAccessPointsApOrderFactory` = GboardMethodTarget(
        classType = "Lngm;",
        name = "m",
        parameterTypes = listOf("Landroid/content/Context;", "Lnub;", "Lojy;", "Lxae;", "Lxae;"),
        returnType = "Lngm;",
    )

    val `editingApOndestroy` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onDestroy",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `editingApOnfinishinput` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onFinishInput",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `editingApOnfinishinputview` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onFinishInputView",
        parameterTypes = listOf("Z"),
        returnType = "V",
    )

    val `editingApOnstartinput` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onStartInput",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "V",
    )

    val `editingApOnstartinputview` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onStartInputView",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "V",
    )

    val `editingApOnwindowhidden` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onWindowHidden",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `flagBoolGetter` = GboardMethodTarget(
        classType = "Lovo;",
        name = "g",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/Object;",
    )

    val `flagNameField` = GboardFieldTarget(
        classType = "Lovo;",
        name = "a",
        type = "Ljava/lang/String;",
    )

    val `frostedConfigure` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onConfigureWindow",
        parameterTypes = listOf("Landroid/view/Window;", "Z", "Z"),
        returnType = "V",
    )

    val `frostedHidden` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onWindowHidden",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `frostedNavApply` = GboardMethodTarget(
        classType = "Lscf;",
        name = "f",
        parameterTypes = listOf("Landroid/view/Window;", "Lsce;"),
        returnType = "V",
    )

    val `frostedNavBody` = GboardFieldTarget(
        classType = "Lsce;",
        name = "c",
        type = "Lj${'$'}/util/OptionalInt;",
    )

    val `frostedNavColor` = GboardFieldTarget(
        classType = "Lsce;",
        name = "a",
        type = "I",
    )

    val `frostedNavDivider` = GboardFieldTarget(
        classType = "Lsce;",
        name = "b",
        type = "I",
    )

    val `frostedNavHeader` = GboardFieldTarget(
        classType = "Lsce;",
        name = "d",
        type = "Lj${'$'}/util/OptionalInt;",
    )

    val `frostedNavLight` = GboardFieldTarget(
        classType = "Lsce;",
        name = "e",
        type = "Z",
    )

    val `frostedNavParamsConstructor` = GboardMethodTarget(
        classType = "Lsce;",
        name = "<init>",
        parameterTypes = listOf("I", "I", "Lj${'$'}/util/OptionalInt;", "Lj${'$'}/util/OptionalInt;", "Z"),
        returnType = "V",
    )

    val `frostedShown` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onWindowShown",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `gestureDispatch` = GboardMethodTarget(
        classType = "Lqtq;",
        name = "f",
        parameterTypes = listOf("Lqts;", "Lqlk;", "Lqmg;", "Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;", "J", "Z", "Z", "I", "Z", "J", "I"),
        returnType = "V",
    )

    val `gestureDispatcherType` = GboardTypeTarget("Lqtq;")

    val `gestureStockDelegateField` = GboardFieldTarget(
        classType = "Lqtq;",
        name = "n",
        type = "Lqto;",
    )

    val `gestureStockDispatch` = GboardMethodTarget(
        classType = "Lqto;",
        name = "o",
        parameterTypes = listOf("Lqts;", "Lqlk;", "Lqmg;", "Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;", "J", "Z", "Z", "I", "Z", "J", "I"),
        returnType = "V",
    )

    val `incognitoDictationFlag` = GboardFieldTarget(
        classType = "Leta;",
        name = "v",
        type = "Z",
    )

    val `incognitoModeToggleApController` = GboardMethodTarget(
        classType = "Lnib;",
        name = "<init>",
        parameterTypes = listOf("Landroid/content/Context;", "Lnji;"),
        returnType = "V",
    )

    val `incognitoModeToggleApOrderFactory` = GboardMethodTarget(
        classType = "Lngm;",
        name = "m",
        parameterTypes = listOf("Landroid/content/Context;", "Lnub;", "Lojy;", "Lxae;", "Lxae;"),
        returnType = "Lngm;",
    )

    val `keyboardCompletionCallbackType` = GboardTypeTarget("Lpwg;")

    val `keyboardDefinitionType` = GboardTypeTarget("Lqnm;")

    val `keyboardExtensionManager` = GboardMethodTarget(
        classType = "Lotc;",
        name = "c",
        parameterTypes = listOf("Lqnm;"),
        returnType = "Lpwh;",
    )

    val `keyboardProviderRequest` = GboardMethodTarget(
        classType = "Ljtw;",
        name = "H",
        parameterTypes = listOf("Landroid/content/Context;", "Lpwf;", "Lcom/google/android/libraries/inputmethod/metadata/ImeDef;", "Lqnm;", "Ljava/lang/String;", "Luuz;", "Lpwg;"),
        returnType = "V",
    )

    val `languageModelsBakedGetter` = GboardMethodTarget(
        classType = "Lovo;",
        name = "d",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/Object;",
    )

    val `languageModelsDecoderAddEngine` = GboardMethodTarget(
        classType = "Lcom/google/android/keyboard/client/delight5/Decoder;",
        name = "addEngine",
        parameterTypes = listOf("Lzaq;"),
        returnType = "V",
    )

    val `languageModelsDecoderDump` = GboardMethodTarget(
        classType = "Lcom/google/android/keyboard/client/delight5/Decoder;",
        name = "dump",
        parameterTypes = listOf("Landroid/util/Printer;", "Z"),
        returnType = "V",
    )

    val `languageModelsDecoderLoad` = GboardMethodTarget(
        classType = "Lcom/google/android/keyboard/client/delight5/Decoder;",
        name = "loadLanguageModel",
        parameterTypes = listOf("Lzim;"),
        returnType = "Z",
    )

    val `languageModelsDecoderRemoveEngine` = GboardMethodTarget(
        classType = "Lcom/google/android/keyboard/client/delight5/Decoder;",
        name = "removeEngine",
        parameterTypes = listOf("Lzaq;"),
        returnType = "V",
    )

    val `languageModelsDecoderReset` = GboardMethodTarget(
        classType = "Lcom/google/android/keyboard/client/delight5/Decoder;",
        name = "createOrResetDecoder",
        parameterTypes = listOf("Lzfk;"),
        returnType = "Z",
    )

    val `languageModelsDecoderUnload` = GboardMethodTarget(
        classType = "Lcom/google/android/keyboard/client/delight5/Decoder;",
        name = "unloadLanguageModel",
        parameterTypes = listOf("Lzim;"),
        returnType = "Z",
    )

    val `languageModelsDescriptor` = GboardFieldTarget(
        classType = "Lzbe;",
        name = "c",
        type = "Lzbd;",
    )

    val `languageModelsEngineModelFile` = GboardFieldTarget(
        classType = "Lzio;",
        name = "c",
        type = "Ljava/lang/String;",
    )

    val `languageModelsEngineModelVersion` = GboardFieldTarget(
        classType = "Lzio;",
        name = "d",
        type = "J",
    )

    val `languageModelsEngineModels` = GboardFieldTarget(
        classType = "Lzaq;",
        name = "d",
        type = "Laaxh;",
    )

    val `languageModelsEngineType` = GboardFieldTarget(
        classType = "Lzaq;",
        name = "c",
        type = "I",
    )

    val `languageModelsHmmEnroll` = GboardMethodTarget(
        classType = "Lhvi;",
        name = "d",
        parameterTypes = listOf("Ljava/lang/String;", "Lcom/google/android/apps/inputmethod/libs/hmm/DataManagerImpl;"),
        returnType = "Z",
    )

    val `languageModelsHmmPack` = GboardFieldTarget(
        classType = "Lhvi;",
        name = "d",
        type = "Ljava/io/File;",
    )

    val `languageModelsHmmProvider` = GboardFieldTarget(
        classType = "Lhvi;",
        name = "a",
        type = "Lhtw;",
    )

    val `languageModelsHmmVersion` = GboardFieldTarget(
        classType = "Lhtw;",
        name = "b",
        type = "I",
    )

    val `languageModelsModelCountry` = GboardFieldTarget(
        classType = "Lzil;",
        name = "j",
        type = "Ljava/lang/String;",
    )

    val `languageModelsModelFile` = GboardFieldTarget(
        classType = "Lzil;",
        name = "f",
        type = "Ljava/lang/String;",
    )

    val `languageModelsModelLanguage` = GboardFieldTarget(
        classType = "Lzil;",
        name = "i",
        type = "Ljava/lang/String;",
    )

    val `languageModelsModelType` = GboardFieldTarget(
        classType = "Lzil;",
        name = "e",
        type = "I",
    )

    val `languageModelsModelVersion` = GboardFieldTarget(
        classType = "Lzil;",
        name = "l",
        type = "J",
    )

    val `languageModelsNwpManagerFactory` = GboardMethodTarget(
        classType = "Lghk;",
        name = "a",
        parameterTypes = listOf("Landroid/content/Context;"),
        returnType = "Lghk;",
    )

    val `languageModelsNwpOpen` = GboardMethodTarget(
        classType = "Lghh;",
        name = "j",
        parameterTypes = listOf("Ljava/util/Locale;", "Ljava/lang/String;"),
        returnType = "Lgig;",
    )

    val `languageModelsPackMetadata` = GboardMethodTarget(
        classType = "Lgig;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "Luqu;",
    )

    val `languageModelsPackParent` = GboardMethodTarget(
        classType = "Luqu;",
        name = "e",
        parameterTypes = emptyList(),
        returnType = "Luqj;",
    )

    val `languageModelsParentVersion` = GboardMethodTarget(
        classType = "Luqj;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "I",
    )

    val `latinGlobeFactory` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/libs/latin5/LatinIme;",
        name = "N",
        parameterTypes = emptyList(),
        returnType = "Lzcv;",
    )

    val `latinGlobeIntervalField` = GboardFieldTarget(
        classType = "Lzcv;",
        name = "h",
        type = "F",
    )

    val `longPressQuickActionsInputEvent` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "aw",
        parameterTypes = listOf("Losj;"),
        returnType = "Z",
    )

    val `manualIncognitoClipboard` = GboardMethodTarget(
        classType = "Lgcc;",
        name = "onPrimaryClipChanged",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `manualIncognitoDictation` = GboardMethodTarget(
        classType = "Lteg;",
        name = "b",
        parameterTypes = listOf("Leta;", "Z", "Z"),
        returnType = "Llih;",
    )

    val `manualIncognitoHide` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onWindowHidden",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `manualIncognitoPredicate` = GboardMethodTarget(
        classType = "Lewx;",
        name = "I",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `manualIncognitoStart` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onStartInput",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "V",
    )

    val `manualIncognitoStartView` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onStartInputView",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "V",
    )

    val `manualIncognitoVoice` = GboardMethodTarget(
        classType = "Lvpg;",
        name = "aQ",
        parameterTypes = listOf("Landroid/content/Context;", "Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "Z",
    )

    val `menuBodyProviderSelect` = GboardMethodTarget(
        classType = "Lpxy;",
        name = "f",
        parameterTypes = listOf("I"),
        returnType = "V",
    )

    val `menuControllerBind` = GboardMethodTarget(
        classType = "Lnib;",
        name = "k",
        parameterTypes = listOf("Lned;"),
        returnType = "V",
    )

    val `menuControllerOpen` = GboardMethodTarget(
        classType = "Lnib;",
        name = "F",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `menuDragInflate` = GboardMethodTarget(
        classType = "Lcom/google/android/libraries/inputmethod/accesspoint/impl/AccessPointDragPopupView;",
        name = "onFinishInflate",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `menuDragSetup` = GboardMethodTarget(
        classType = "Lnih;",
        name = "y",
        parameterTypes = listOf("Ljava/lang/String;", "Z"),
        returnType = "V",
    )

    val `menuNativeView` = GboardMethodTarget(
        classType = "Lcom/google/android/libraries/inputmethod/keyboard/impl/Keyboard;",
        name = "F",
        parameterTypes = listOf("Lcom/google/android/libraries/inputmethod/metadata/KeyboardViewDef;", "Lpxh;"),
        returnType = "Lpxi;",
    )

    val `ocrConfigAvailability` = GboardMethodTarget(
        classType = "Laauc;",
        name = "g",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `ocrConfigLabel` = GboardMethodTarget(
        classType = "Laauc;",
        name = "b",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/String;",
    )

    val `ocrConfigLanguageHint` = GboardMethodTarget(
        classType = "Laauc;",
        name = "c",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/String;",
    )

    val `ocrConfigLoggingLibrary` = GboardMethodTarget(
        classType = "Laauc;",
        name = "d",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/String;",
    )

    val `ocrConfigModuleId` = GboardMethodTarget(
        classType = "Laauc;",
        name = "f",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/String;",
    )

    val `ocrConfigOptionalModule` = GboardMethodTarget(
        classType = "Laauc;",
        name = "e",
        parameterTypes = emptyList(),
        returnType = "Ljava/lang/String;",
    )

    val `ocrManagerRequest` = GboardMethodTarget(
        classType = "Laatx;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "[Llrf;",
    )

    val `ocrOptionalRequest` = GboardMethodTarget(
        classType = "Lzws;",
        name = "a",
        parameterTypes = listOf("Laatd;"),
        returnType = "[Llrf;",
    )

    val `ocrThinInit` = GboardMethodTarget(
        classType = "Laatq;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `pointerCancel` = GboardMethodTarget(
        classType = "Lqts;",
        name = "s",
        parameterTypes = listOf("J"),
        returnType = "V",
    )

    val `pointerFinish` = GboardMethodTarget(
        classType = "Lqts;",
        name = "r",
        parameterTypes = listOf("J", "I"),
        returnType = "V",
    )

    val `pointerOwner` = GboardMethodTarget(
        classType = "Lqts;",
        name = "B",
        parameterTypes = listOf("Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;", "F", "F", "J", "I"),
        returnType = "V",
    )

    val `pointerOwnerType` = GboardTypeTarget("Lqts;")

    val `pointerPreReset` = GboardMethodTarget(
        classType = "Lqts;",
        name = "ac",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `pointerReset` = GboardMethodTarget(
        classType = "Lqts;",
        name = "C",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `ramblerSelectorCreate` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/latin/preference/VoiceSettingsFragment;",
        name = "f",
        parameterTypes = listOf("Landroid/os/Bundle;"),
        returnType = "V",
    )

    val `ramblerSelectorDefault` = GboardMethodTarget(
        classType = "Lfta;",
        name = "hU",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `ramblerSelectorLayout` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/latin/preference/VoiceSettingsFragment;",
        name = "aC",
        parameterTypes = emptyList(),
        returnType = "I",
    )

    val `ramblerSelectorRead` = GboardMethodTarget(
        classType = "Lnng;",
        name = "a",
        parameterTypes = listOf("Landroid/content/Context;"),
        returnType = "Z",
    )

    val `ramblerSelectorSetup` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/latin/preference/VoiceSettingsFragment;",
        name = "ac",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `ramblerSelectorWrite` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/latin/preference/VoiceSettingsFragment;",
        name = "aE",
        parameterTypes = listOf("Z", "Lrgy;", "Lcom/google/android/libraries/inputmethod/preferencewidgets/CustomSelectorWithWidgetPreference;", "Lcom/google/android/libraries/inputmethod/preferencewidgets/CustomSelectorWithWidgetPreference;"),
        returnType = "V",
    )

    val `roundedKeyboardAdmission` = GboardMethodTarget(
        classType = "Lrxr;",
        name = "b",
        parameterTypes = listOf("Landroid/content/Context;"),
        returnType = "Z",
    )

    val `roundedKeyboardRadiiConstructor` = GboardMethodTarget(
        classType = "Lsay;",
        name = "<init>",
        parameterTypes = listOf("F", "F", "F", "F"),
        returnType = "V",
    )

    val `roundedKeyboardRadiiType` = GboardTypeTarget("Lsay;")

    val `roundedKeyboardRadiiValuesField` = GboardFieldTarget(
        classType = "Lsay;",
        name = "a",
        type = "[F",
    )

    val `roundedKeyboardRadiiWrapperFirstParameterField` = GboardFieldTarget(
        classType = "Lrzx;",
        name = "b",
        type = "Ljava/lang/Float;",
    )

    val `roundedKeyboardRadiiWrapperParameterizedConstructor` = GboardMethodTarget(
        classType = "Lrzx;",
        name = "<init>",
        parameterTypes = listOf("Lsay;", "F", "F"),
        returnType = "V",
    )

    val `roundedKeyboardRadiiWrapperRadiiField` = GboardFieldTarget(
        classType = "Lrzx;",
        name = "a",
        type = "Lsay;",
    )

    val `roundedKeyboardRadiiWrapperSecondParameterField` = GboardFieldTarget(
        classType = "Lrzx;",
        name = "c",
        type = "Ljava/lang/Float;",
    )

    val `roundedKeyboardRadiiWrapperSimpleConstructor` = GboardMethodTarget(
        classType = "Lrzx;",
        name = "<init>",
        parameterTypes = listOf("Lsay;"),
        returnType = "V",
    )

    val `roundedKeyboardRadiiWrapperType` = GboardTypeTarget("Lrzx;")

    val `roundedKeyboardStyleResolver` = GboardMethodTarget(
        classType = "Lryg;",
        name = "b",
        parameterTypes = listOf("Ljava/lang/String;", "Ljava/util/List;"),
        returnType = "Ljava/util/List;",
    )

    val `settingsHomepageMode` = GboardFieldTarget(
        classType = "Ldtp;",
        name = "a",
        type = "I",
    )

    val `settingsHomepageOnCreate` = GboardMethodTarget(
        classType = "Lcom/google/android/apps/inputmethod/latin/preference/SettingsActivity;",
        name = "onCreate",
        parameterTypes = listOf("Landroid/os/Bundle;"),
        returnType = "V",
    )

    val `settingsHomepageSelector` = GboardMethodTarget(
        classType = "Ldtp;",
        name = "a",
        parameterTypes = listOf("Landroid/content/Context;"),
        returnType = "Z",
    )

    val `signatureCheck` = GboardMethodTarget(
        classType = "Lsqy;",
        name = "a",
        parameterTypes = listOf("Landroid/content/Context;", "Ljava/lang/String;"),
        returnType = "Z",
    )

    val `softKeyBind` = GboardMethodTarget(
        classType = "Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;",
        name = "s",
        parameterTypes = listOf("Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;", "J"),
        returnType = "Z",
    )

    val `softKeyMetadataType` = GboardTypeTarget("Lcom/google/android/libraries/inputmethod/metadata/SoftKeyDef;")

    val `softKeyViewType` = GboardTypeTarget("Lcom/google/android/libraries/inputmethod/widgets/SoftKeyView;")

    val `splitAccessPointName` = GboardFieldTarget(
        classType = "Ltqr;",
        name = "d",
        type = "Ljava/lang/Object;",
    )

    val `splitDeviceEligibility` = GboardMethodTarget(
        classType = "Lqdw;",
        name = "w",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `splitEligibility` = GboardMethodTarget(
        classType = "Lqcc;",
        name = "au",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `splitModeAttach` = GboardMethodTarget(
        classType = "Lqcc;",
        name = "E",
        parameterTypes = listOf("Lqba;", "Lpqb;", "Lpvf;", "Z"),
        returnType = "V",
    )

    val `splitModeCurrent` = GboardMethodTarget(
        classType = "Lqcc;",
        name = "G",
        parameterTypes = emptyList(),
        returnType = "I",
    )

    val `splitModeManual` = GboardMethodTarget(
        classType = "Lqcc;",
        name = "W",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `splitModeReload` = GboardMethodTarget(
        classType = "Lqcc;",
        name = "Q",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `splitModeRestore` = GboardMethodTarget(
        classType = "Lqcc;",
        name = "O",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `splitModeTransition` = GboardMethodTarget(
        classType = "Lqcc;",
        name = "ag",
        parameterTypes = listOf("I", "Z", "Z"),
        returnType = "Z",
    )

    val `splitOrder` = GboardMethodTarget(
        classType = "Lnhm;",
        name = "c",
        parameterTypes = emptyList(),
        returnType = "Lwyw;",
    )

    val `splitState` = GboardMethodTarget(
        classType = "Ltqr;",
        name = "e",
        parameterTypes = listOf("I"),
        returnType = "V",
    )

    val `splitWidthEligibility` = GboardMethodTarget(
        classType = "Lqdw;",
        name = "q",
        parameterTypes = listOf("Landroid/content/Context;", "I", "Z"),
        returnType = "Z",
    )

    val `swipeDismissHeaderDispatch` = GboardMethodTarget(
        classType = "Lcom/google/android/libraries/inputmethod/keyboard/impl/KeyboardViewHolder;",
        name = "dispatchTouchEvent",
        parameterTypes = listOf("Landroid/view/MotionEvent;"),
        returnType = "Z",
    )

    val `symbolFooterCorpus` = GboardMethodTarget(
        classType = "Lhjs;",
        name = "a",
        parameterTypes = listOf("Landroid/view/inputmethod/EditorInfo;", "Z"),
        returnType = "Lwyw;",
    )

    val `voiceEligibilityConstructor` = GboardMethodTarget(
        classType = "Ltec;",
        name = "<init>",
        parameterTypes = listOf("Landroid/content/Context;", "Luho;", "Lvjr;", "Lsva;", "Luho;", "Luho;", "Ltoc;", "Luho;", "Ltoj;", "Ltjp;", "Llqg;", "Ltiw;", "Ltje;", "Lvpg;", "Ltkn;", "Lton;", "Ltqm;", "Lsuf;", "Lter;", "Ltdq;", "Luho;", "Lacwx;", "Ljava/util/Set;"),
        returnType = "V",
    )

    val `voiceFormatterConstructor` = GboardMethodTarget(
        classType = "Lsxu;",
        name = "<init>",
        parameterTypes = listOf("Ljava/util/Locale;", "Letg;", "Ltkg;", "I", "Z", "Lsxx;", "Ltqd;", "Lvpg;", "Ljava/util/concurrent/Executor;"),
        returnType = "V",
    )

    val `voiceMddConstructor` = GboardMethodTarget(
        classType = "Lsva;",
        name = "<init>",
        parameterTypes = listOf("Landroid/content/Context;", "Lsvb;", "Lsuv;", "Lacwx;"),
        returnType = "V",
    )

    val `voiceNativeReadiness` = GboardMethodTarget(
        classType = "Ltdn;",
        name = "a",
        parameterTypes = emptyList(),
        returnType = "Z",
    )

    val `webClipboardCreate` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "onCreate",
        parameterTypes = emptyList(),
        returnType = "V",
    )

    val `writingOfficialPreference` = GboardMethodTarget(
        classType = "Lchu;",
        name = "x",
        parameterTypes = listOf("I", "Z"),
        returnType = "Z",
    )

    val `zhuyinTogglePopupAction` = GboardMethodTarget(
        classType = "Lcom/google/android/libraries/inputmethod/popup/BasicPopupView;",
        name = "b",
        parameterTypes = listOf("Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;", "Landroid/view/View;", "F", "F", "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;", "[I", "Z"),
        returnType = "Lqmg;",
    )

    val `zhuyinTogglePopupTarget` = GboardMethodTarget(
        classType = "Lcom/google/android/libraries/inputmethod/popup/BasicPopupView;",
        name = "b",
        parameterTypes = listOf("Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;", "Landroid/view/View;", "F", "F", "Lcom/google/android/libraries/inputmethod/metadata/ActionDef;", "[I", "Z"),
        returnType = "Lqmg;",
    )

    val `zhuyinTogglePreferenceFactory` = GboardMethodTarget(
        classType = "Lrgy;",
        name = "I",
        parameterTypes = listOf("Landroid/content/Context;"),
        returnType = "Lrgy;",
    )

    val `zhuyinTogglePreferenceWrite` = GboardMethodTarget(
        classType = "Lrgy;",
        name = "f",
        parameterTypes = listOf("Ljava/lang/String;", "Z"),
        returnType = "V",
    )

    val `zhuyinToggleReset` = GboardMethodTarget(
        classType = "Lpsx;",
        name = "at",
        parameterTypes = listOf("Z"),
        returnType = "V",
    )
}
