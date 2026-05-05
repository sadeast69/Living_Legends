package com.worldremembers.livinglegends.fabric;

import com.worldremembers.livinglegends.WorldRemembersLivingLegends;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;

@JourneyMapPlugin(apiVersion = IClientAPI.API_VERSION)
public final class JourneyMapFabricPlugin implements IClientPlugin {
    @Override
    public String getModId() {
        return WorldRemembersLivingLegends.MOD_ID;
    }

    @Override
    public void initialize(IClientAPI jmClientApi) {
        FabricMapIntegrationClient.installIntegration(new JourneyMapFabricIntegration(jmClientApi));
    }
}
