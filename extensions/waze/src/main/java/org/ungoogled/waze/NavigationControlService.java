package org.ungoogled.waze;

import nl.johninnl.navigation.startup.ProviderControlService;
import nl.johninnl.navigation.startup.ProviderEngineService;

public final class NavigationControlService extends ProviderControlService {
    @Override protected boolean enabled() { return NavigationBroadcasts.isEnabled(); }
    @Override protected boolean engineRunning() { return NavigationStartup.running(); }
    @Override protected String receiverPackage() { return NavigationBroadcasts.receiverPackage(); }
    @Override protected Class<? extends ProviderEngineService> engineService() { return NavigationEngineService.class; }
}
