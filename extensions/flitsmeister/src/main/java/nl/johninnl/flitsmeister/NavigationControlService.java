package nl.johninnl.flitsmeister;

import nl.johninnl.navigation.startup.ProviderControlService;
import nl.johninnl.navigation.startup.ProviderEngineService;

public final class NavigationControlService extends ProviderControlService {
    @Override protected boolean enabled() { return true; }
    @Override protected boolean engineRunning() { return NavigationBroadcasts.engineRunning(); }
    @Override protected String receiverPackage() { return NavigationBroadcasts.receiverPackage(); }
    @Override protected Class<? extends ProviderEngineService> engineService() { return NavigationEngineService.class; }
}
