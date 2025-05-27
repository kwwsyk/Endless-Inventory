package com.kwwsyk.endinv.common.options;

import com.kwwsyk.endinv.common.util.Accessibility;

public interface IServerConfig {

    int getMaxAllowedStackSize();

    boolean allowInfinityMode();

    boolean enableAutoPick();

    ContentTransferMode transferMode();

    Accessibility defaultAccessibility();

    MissingEndInvPolicy policyHandlingMissing();
}
