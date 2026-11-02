package nl.johninnl.flitsmeister;

import nl.johninnl.navigation.startup.ProviderControlService;
import nl.johninnl.navigation.startup.ProviderEngineService;

public final class NavigationControlService extends ProviderControlService {
    @Override protected void observeControlClient(int uid,boolean warningAware){NativeWarnings.init(this).controlClient(uid,warningAware);}
    @Override protected void appendWarningCapabilities(android.os.Bundle response,int uid) {
        response.putBinder("warning_api",NativeWarnings.init(this).binder());
        response.putIntArray("warning_protocols",new int[]{1});
    }
    @Override protected boolean enabled() { return true; }
    @Override protected boolean engineRunning() { return NavigationBroadcasts.engineRunning(); }
    @Override protected String receiverPackage() { return NavigationBroadcasts.receiverPackage(); }
    @Override protected Class<? extends ProviderEngineService> engineService() { return NavigationEngineService.class; }
}
