package io.ghostwriter.rt.snaperr;

import io.ghostwriter.Tracer;
import io.ghostwriter.rt.snaperr.api.*;
import io.ghostwriter.rt.snaperr.api.Throttler;
import io.ghostwriter.rt.snaperr.tracker.ReferenceTracker;

import java.util.Objects;

public class SnaperrTracer implements Tracer {

    private final ReferenceTracker referenceTracker;

    @SuppressWarnings("rawtypes")
    final private TriggerHandler triggerHandler;

    @SuppressWarnings("rawtypes")
    final private TriggerSerializer triggerSerializer;


    private final ThreadLocal<ErrorState> errorTrackerThreadLocal = new ThreadLocal<ErrorState>() {

        @Override
        protected ErrorState initialValue() {
            return new ErrorState();
        }
    };

    private final Throttler throttleControl;

    public SnaperrTracer(ReferenceTracker referenceTracker, TriggerSerializer triggerSerializer,
                         TriggerHandler triggerHandler, Throttler throttleControl) {
        this.referenceTracker = Objects.requireNonNull(referenceTracker);
        this.triggerHandler = Objects.requireNonNull(triggerHandler);
        this.throttleControl = Objects.requireNonNull(throttleControl);
        this.triggerSerializer = Objects.requireNonNull(triggerSerializer);
    }

    @Override
    public void entering(Object source, String method, Object... params) {
        if (hasPendingProcessing()) {
            return;
        }

        referenceTracker.pushScope(source, method);

        for (int i = 0; i < params.length - 1; i++) {
            final Object paramName = params[i++];
            final Object paramValue = params[i];
            final String name = (String) paramName;
            referenceTracker.track(name, paramValue);
        }
    }

    @Override
    public void exiting(Object source, String method) {
        if (hasPendingProcessing()) {
            return;
        }

        referenceTracker.popScope();
    }

    @Override
    public void valueChange(Object source, String method, String variable, Object value) {
        if (hasPendingProcessing()) {
            return;
        }

        referenceTracker.track(variable, value);
    }

    @Override
    public <T> void returning(Object source, String method, T returnValue) {
        // No need to do anything here, 'exiting' is called after returning and
        // it takes care of cleanup.
        // We don't capture a result of a method here.
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onError(Object source, String method, Throwable error) {
        if (hasPendingProcessing() || isPropagatingException(error)) {
            return;
        }

        // The trigger handler executes in the same thread as the one that just
        // "crashed".
        // This way we can guarantee that the watched references are not
        // changed. At least not by the current thread.
        // It is possible that the snapshot contains a reference to a global
        // state that could be modified by other threads.
        // However that can _only_ (I'm 80% sure!) happen when the original code
        // already has race conditions.

        /*
         * The missing 20%: It's valid to use and therefore GW tracks concurrent
         * collections, atomic references, volatile variables. They don't imply
         * race conditions but they can be changed by other threads in the
         * meantime. The error can also happen outside of the 'critical section'
         * when the variables are not meant to be used by the application yet,
         * but we will read them.
         */

        ErrorTrigger trigger = new ErrorTrigger(referenceTracker, error);

        // FIXME(snorbi07): change implementation to work without the "manual" synchronized keyword. The throttler should be thread safe.
        synchronized (throttleControl) {
            if (!throttleControl.doHandleError(trigger)) {
                return;
            }
        }

        startTriggerProcessing(trigger);
        String serializedError = triggerSerializer.serializeTrigger(trigger);
        triggerHandler.onError(serializedError);
        stopTriggerProcessing();
    }

    private boolean isPropagatingException(Throwable error) {
        final ErrorTrigger processedErrorTrigger = getProcessedErrorTrigger();
        if (processedErrorTrigger == null) {
            return false;
        }

        final Throwable processedThrowable = processedErrorTrigger.getThrowable();
        return processedThrowable.equals(error);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void timeout(Object source, String method, long timeoutThreshold, long timeout) {
        if (hasPendingProcessing()) {
            return;
        }
        // Unlike in the case of on error trigger, we don't need to guard for
        // timeouts propagating.
        // This is an opt-in feature, so if the user annotates the call-chain,
        // then we might end up raising timeouts at all steps...

        TimeoutTrigger trigger = new TimeoutTrigger(referenceTracker, timeoutThreshold, timeout);

        // FIXME(snorbi07): change implementation to work without the "manual" synchronized keyword. The throttler should be thread safe.
        synchronized (throttleControl) {
            if (!throttleControl.doHandleTimeout(trigger)) {
                return;
            }
        }

        startTriggerProcessing(null);
        String serializedTimeout = triggerSerializer.serializeTrigger(trigger);
        triggerHandler.onTimeout(serializedTimeout);
        stopTriggerProcessing();
    }

    private ErrorTrigger getProcessedErrorTrigger() {
        return errorTrackerThreadLocal.get().getProcessedErrorTrigger();
    }

    private void startTriggerProcessing(ErrorTrigger errorTrigger) {
        ErrorState errorState = errorTrackerThreadLocal.get();
        errorState.setProcessingInProgress(true);
        errorState.setProcessedErrorTrigger(errorTrigger);
    }

    private void stopTriggerProcessing() {
        errorTrackerThreadLocal.get().setProcessingInProgress(false);
    }

    private boolean hasPendingProcessing() {
        return errorTrackerThreadLocal.get().isProcessingInProgress();
    }

    /**
     * To hold all invariants together
     */
    private static class ErrorState {

        /**
         * We store the triggered error, since we use it to avoid triggering
         * additional error events on the same thread.
         */
        private ErrorTrigger processedErrorTrigger;

        /**
         * When we are currently in error handling state, then we should not
         * accidentally handle another error
         */
        private boolean processingInProgress = false;

        ErrorTrigger getProcessedErrorTrigger() {
            return processedErrorTrigger;
        }

        void setProcessedErrorTrigger(ErrorTrigger processedErrorTrigger) {
            this.processedErrorTrigger = processedErrorTrigger;
        }

        boolean isProcessingInProgress() {
            return processingInProgress;
        }

        void setProcessingInProgress(boolean processingInProgress) {
            this.processingInProgress = processingInProgress;
        }

    }
}
