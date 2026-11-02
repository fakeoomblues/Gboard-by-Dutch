package nl.johninnl.flitsmeister;

import android.content.Intent;
import nl.johninnl.navigation.startup.ProviderEngineService;

public final class NavigationEngineService extends ProviderEngineService {
    @Override protected boolean enabled() { return true; }
    @Override protected boolean engineRunning() { return NavigationBroadcasts.engineRunning(); }
    @Override protected void startEngine() {
        startForegroundService(new Intent().setClassName(this, "nl.flitsmeister.services.base.FlitsmeisterBaseService")
            .putExtra("checkBackgroundPermission", false));
    }
}
