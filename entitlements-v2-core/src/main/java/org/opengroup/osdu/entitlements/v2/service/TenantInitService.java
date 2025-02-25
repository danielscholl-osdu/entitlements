package org.opengroup.osdu.entitlements.v2.service;


import jakarta.annotation.Nullable;
import org.opengroup.osdu.entitlements.v2.model.init.InitServiceDto;

public interface TenantInitService {

    void createDefaultGroups();

    void bootstrapInitialAccounts(@Nullable InitServiceDto initServiceDto);
}
