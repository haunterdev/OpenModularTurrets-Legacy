package com.ommods.omlib.api.network;

import javax.annotation.Nonnull;

/** A controller that names its network. */
public interface INameController extends IController {

    @Nonnull
    String getNetworkName();
}
