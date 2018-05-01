package io.ghostwriter.rt.snaperr;

import io.ghostwriter.rt.snaperr.api.TriggerHandler;

/**
 * Do nothing
 *
 */
public class NoopTriggerHandler implements TriggerHandler {

    @Override
    public void onError(String serializedError) {

    }

    @Override
    public void onTimeout(String serializedTimeout) {

    }

}
