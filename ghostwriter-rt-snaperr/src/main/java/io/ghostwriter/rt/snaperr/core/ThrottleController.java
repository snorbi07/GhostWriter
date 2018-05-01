package io.ghostwriter.rt.snaperr.core;

import io.ghostwriter.rt.snaperr.api.ErrorTrigger;
import io.ghostwriter.rt.snaperr.api.Throttler;
import io.ghostwriter.rt.snaperr.api.TimeoutTrigger;

public class ThrottleController implements Throttler {

    private static long DEFAULT_ERROR_WINDOW_LENGTH = 1000L;

    private static int DEFAULT_ERROR_LIMIT_PER_WINDOW = 5;

    /**
     * For throttling, amount of time where maximum {@link #maxErrorsInWindow}
     * errors are allowed. < 0 means no throttling
     */
    private final long errorWindowSizeMs;
    /**
     * For throttling, amount of errors allowed in each throttling window. < 0
     * means no throttling
     */
    private final int maxErrorsInWindow;

    private int errorsInBucket = 0;

    private long bucketPurgedLastTimeMs = currentTimeMillis();

    /**
     * @param errorWindowLengthMs Window size in millisec
     * @param maxErrorsInWindow maximum number of errors in the window to be handled
     */
    public ThrottleController(long errorWindowLengthMs, int maxErrorsInWindow) {
        this.errorWindowSizeMs = errorWindowLengthMs;
        this.maxErrorsInWindow = maxErrorsInWindow;
    }

    public ThrottleController() {
        this(DEFAULT_ERROR_WINDOW_LENGTH, DEFAULT_ERROR_LIMIT_PER_WINDOW);
    }

    @Override
    public boolean doHandleTimeout(TimeoutTrigger timeoutTrigger) {
        return !throttleControl();
    }

    @Override
    public boolean doHandleError(ErrorTrigger errorTrigger) {
        return !throttleControl();
    }

    /**
     * Only the specified number of errors are handled within the given window.
     * <p>
     * <p>
     * if the maxErrorCountInBucket number of errors have been reached, then
     * there are no new errors handled until the bucket is emptied.
     *
     * @return <li>true - throttling, must not handle error
     * <li>false - no throttling, error the handle now
     */
    private boolean throttleControl() {
        if (isThrottlingDisabled()) {
            return false;
        }

        final long errorWindowSizeMs = getErrorWindowSizeMs();
        final int maxErrorCountInWindow = getMaxErrorCountInWindow();

        final long nowMs = currentTimeMillis();
        final long elapsedSinceLastPurgeMs = nowMs - bucketPurgedLastTimeMs;

        if (elapsedSinceLastPurgeMs >= errorWindowSizeMs) {
            // Empty the bucket
            errorsInBucket = 0;
            bucketPurgedLastTimeMs = nowMs;
        }

        errorsInBucket++;

        return errorsInBucket > maxErrorCountInWindow;
    }

    private boolean isThrottlingDisabled() {
        final long errorWindowSizeMs = getErrorWindowSizeMs();
        final int maxErrorCountInWindow = getMaxErrorCountInWindow();

        return errorWindowSizeMs < 1L || maxErrorCountInWindow < 1;
    }

    private long getErrorWindowSizeMs() {
        return errorWindowSizeMs;
    }

    private int getMaxErrorCountInWindow() {
        return maxErrorsInWindow;
    }

    /**
     * @return current time in milliseconds
     */
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

}
