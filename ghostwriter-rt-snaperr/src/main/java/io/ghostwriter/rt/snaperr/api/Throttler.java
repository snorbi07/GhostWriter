package io.ghostwriter.rt.snaperr.api;

/**
 * Controls whether snaperr should handle the error or timeout trigger.
 * This should be used for throttle controlling, if there are too many
 * error happening in a short time.
 *
 * <p>Concurrency: SnaperrTracer will synchronize access to this object
 * to make sure the throttle control strategy works on all threads
 *
 */
public interface Throttler {

    /**
     * @param timeoutTrigger
     * @return <li> true - SnaperrTracer will handle the timeout
     * <li> false - SnaperrTracer will NOT handle the timeout
     */
    boolean doHandleTimeout(TimeoutTrigger timeoutTrigger);

    /**
     * @param errorTrigger
     * @return <li> true - SnaperrTracer will handle the error
     * <li> false - SnaperrTracer will NOT handle the error
     */
    boolean doHandleError(ErrorTrigger errorTrigger);

}
