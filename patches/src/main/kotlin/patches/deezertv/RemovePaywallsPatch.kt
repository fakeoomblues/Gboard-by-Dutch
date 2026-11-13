package patches.deezertv

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val NATIVE_PREMIUM = "Lcom/deezer/ui/premium/NativePremiumActivity;"
private const val TV_HOME = "Lcom/deezer/android/tv/ui/feature/home/TabBarActivity;"

/**
 * Subscription surfaces on TV are shared activities from the phone bundle plus the TV
 * subscribe page. Close the entry screens after their normal lifecycle start so the
 * still-running TV task stays usable, and redirect the native premium activity when it
 * is restored as the task root.
 */
@Suppress("unused")
val deezerTvRemovePaywalls = bytecodePatch(
    name = "Remove subscription paywalls (TV)",
    description = "Stops the native premium activity from blocking startup by redirecting it to the " +
        "leanback home, and closes the premium tab, premium web tab, artist paywall, onboarding " +
        "paywall, trial start, offers dialog and family management screens after their normal " +
        "lifecycle starts. Account, sign-in, billing and catalogue services are unchanged.",
    default = true,
) {
    compatibleWith(deezerTv)
    dependsOn(originalInput)

    execute {
        // NativePremiumActivity can be the restored task root at startup. Finishing it
        // would empty the task and expose the launcher; start the normal leanback home
        // surface instead, then finish the subscription activity.
        redirectAfterSuperOnCreate(NATIVE_PREMIUM, TV_HOME)

        listOf(
            "Lcom/deezer/ui/premium/PremiumTabActivity;",
            "Lcom/deezer/ui/premium/NativePremiumTabActivity;",
            "Lcom/deezer/feature/artist/ArtistPaywallActivity;",
            "Lcom/deezer/feature/onboarding/paywall/OnboardingPaywallActivity;",
            "Lcom/deezer/android/ui/activity/UserOffersDialogActivity;",
            "Lcom/deezer/feature/trialstart/TrialStartActivity;",
        ).forEach { closeAfterSuperOnCreate(it) }

        // The premiumtab deep link still opens the shared WebView dialog. Close it after
        // the normal activity start instead of loading the offer page.
        val webView = "Lcom/deezer/android/ui/activity/WebViewDialogActivity;"
        exact(webView, "onCreate", "V", "Landroid/os/Bundle;").method.addInstructions(
            superOnCreateIndex(webView) + 1,
            """
                invoke-virtual {p0}, Landroid/app/Activity;->getIntent()Landroid/content/Intent;
                move-result-object v0
                const-string v1, "url"
                invoke-virtual {v0, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :deezer_tv_web_continue
                const-string v1, "premiumtab"
                invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v0
                if-eqz v0, :deezer_tv_web_continue
                invoke-virtual {p0}, Lcom/deezer/android/ui/activity/WebViewDialogActivity;->finish()V
                return-void
                :deezer_tv_web_continue
            """,
        )
    }
}
