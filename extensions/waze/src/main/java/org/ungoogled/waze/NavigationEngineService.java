package org.ungoogled.waze;

import nl.johninnl.navigation.startup.ProviderEngineService;

public final class NavigationEngineService extends ProviderEngineService {
    @Override protected boolean enabled() { return NavigationBroadcasts.isEnabled(); }
    @Override protected boolean engineRunning() { return NavigationStartup.running(); }
    @Override protected void startEngine() throws ReflectiveOperationException { NavigationStartup.initialize(getApplication()); }
}
