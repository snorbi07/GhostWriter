package io.ghostwriter.rt.snaperr.api;

/**
 * Called when an unexpected error is triggered by a running thread.
 * <p>{@link TriggerSerializer} serializes the message and the implementation of this interfaces
 * sends the serialized message to the target system e.g. Log file, remote system, persists in DB etc..
 *
 * <p>The implementation of this interface will execute in the {@link Thread} that triggered
 * the error, thus blocking the execution (exception propagation) of application until the
 * {@link TriggerHandler} implementation finishes the handling.
 * <p>
 * The interface implementations must be thread-safe.
 */
public interface TriggerHandler {

    void onError(String serializedError);

    void onTimeout(String serializedTimeout);

}
