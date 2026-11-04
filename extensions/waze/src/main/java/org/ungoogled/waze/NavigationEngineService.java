package org.ungoogled.waze;

import nl.johninnl.navigation.startup.ProviderEngineService;

public final class NavigationEngineService extends ProviderEngineService {
    @Override protected boolean enabled() { return NavigationBroadcasts.isEnabled(); }
    @Override protected boolean engineRunning() { return NavigationStartup.running(); }
    @Override protected void startEngine() throws ReflectiveOperationException { NavigationStartup.initialize(getApplication());NativeWarnings.prepareControllers(); }
    @Override protected String integrationStatus(){
        nl.johninnl.navigation.alerts.WarningEndpoint endpoint=NativeWarnings.endpoint();
        boolean dutch=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("nl");
        return (engineRunning()?(dutch?"Motor actief":"Engine running"):(dutch?"Motor wordt gestart":"Engine starting"))+" · "+nl.johninnl.navigation.alerts.WarningStatus.label(endpoint==null?"UNOWNED":endpoint.audioStatus(),dutch);
    }
}
