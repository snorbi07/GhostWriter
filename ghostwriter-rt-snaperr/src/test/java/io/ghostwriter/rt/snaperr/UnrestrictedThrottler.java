package io.ghostwriter.rt.snaperr;

import io.ghostwriter.rt.snaperr.api.ErrorTrigger;
import io.ghostwriter.rt.snaperr.api.Throttler;
import io.ghostwriter.rt.snaperr.api.TimeoutTrigger;

/**
 * Default to handle triggers
 *
 */
public class UnrestrictedThrottler implements Throttler {

    @Override
    public boolean doHandleTimeout(TimeoutTrigger timeoutTrigger) {
        return true;
    }

    @Override
    public boolean doHandleError(ErrorTrigger errorTrigger) {
        return true;
    }

}
