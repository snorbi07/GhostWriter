package io.ghostwriter.rt.snaperr.core;

import io.ghostwriter.rt.snaperr.api.TriggerHandler;

final public class SystemOutWriter implements TriggerHandler {

    @Override
    public void onError(String serializedError) {
        System.out.println(serializedError);
    }

    @Override
    public void onTimeout(String serializedTimeout) {
        System.out.println(serializedTimeout);
    }

}
