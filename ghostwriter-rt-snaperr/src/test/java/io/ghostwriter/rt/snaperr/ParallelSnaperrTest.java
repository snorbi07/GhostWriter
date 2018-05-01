package io.ghostwriter.rt.snaperr;

import io.ghostwriter.rt.snaperr.api.*;
import io.ghostwriter.rt.snaperr.tracker.ReferenceTracker;
import io.ghostwriter.rt.snaperr.tracker.StackBasedReferenceTracker;
import io.ghostwriter.rt.snaperr.api.TrackedValue;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Testing concurrency of Snaperr
 */
public class ParallelSnaperrTest {

    @Test
    public void testStateConflict() {
        final ParallelTriggerSerializer parallelTriggerSerializer = new ParallelTriggerSerializer();
        ReferenceTracker referenceTracker = new StackBasedReferenceTracker();

        final TriggerHandler noopTriggerHandler = new NoopTriggerHandler();
        final SnaperrTracer gwErrMon = new SnaperrTracer(referenceTracker, parallelTriggerSerializer, noopTriggerHandler, new UnrestrictedThrottler());

        // We need to make sure that  2 threads are executed at the "same time".
        // This way we can assure the timing from the main thread. Hence the 3 threads instead of 2.
        final int NUMBER_OF_PARALLEL_THREADS = 3;  // 2 created test threads + the main thread
        final CyclicBarrier gate = new CyclicBarrier(NUMBER_OF_PARALLEL_THREADS);

        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    gate.await();
                    final String METHOD_NAME = "thread1";
                    gwErrMon.entering(this, METHOD_NAME);
                    int someValue = 11;
                    gwErrMon.valueChange(this, METHOD_NAME, "someValue", someValue);
                    gwErrMon.onError(this, METHOD_NAME, new IllegalArgumentException());
                } catch (Exception e) {
                    assertTrue("Unexpected exception: " + e, e != null);
                }
            }
        });

        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    gate.await();
                    final String METHOD_NAME = "thread2";
                    gwErrMon.entering(this, METHOD_NAME);
                    int differentValue = 22;
                    gwErrMon.valueChange(this, METHOD_NAME, "differentValue", differentValue);
                    gwErrMon.onError(this, METHOD_NAME, new IllegalArgumentException());
                } catch (Exception e) {
                    assertTrue("Unexpected exception: " + e, e != null);
                }
            }
        });

        thread1.start();
        thread2.start();

        try {
            // unleash the already started threads!
            gate.await();
            // wait for them to finish, then do some verification
            thread1.join();
            thread2.join();
            final boolean allErrorsHandled = parallelTriggerSerializer.bothThreadErrorsHandled();
            assertTrue("One or more errors were not handled in parallel!", allErrorsHandled);
        } catch (Exception e) {
            assertTrue("Unexpected exception: " + e, e != null);
        }


    }

    /**
     * This class captures calls to the serializeTrigger method for two different threads and two different values.
     * It asserts that the StackBasedReferenceTracker isolates Scopes per threads.
     *
     */
    static private class ParallelTriggerSerializer implements TriggerSerializer {

        private AtomicBoolean thread1ErrorProcessed = new AtomicBoolean(false);

        private AtomicBoolean thread2ErrorProcessed = new AtomicBoolean(false);

        @Override
        public String serializeTrigger(ErrorTrigger errorTrigger) {
            final TrackedScope topLevelScope = errorTrigger.currentScope();
            Map<String, TrackedValue> watched = topLevelScope.getReferences();

            assertTrue("Expected number of watched variables: 1, got: " + watched.size(), watched.size() == 1);

            if ("thread1".equals(topLevelScope.getMethodName())) {

                TrackedValueAsserter.assertTrackedValue(watched, "someValue", 11);
                thread1ErrorProcessed.compareAndSet(false, true);
                TrackedValueAsserter.assertTrackedValueNotCaptured(watched, "differentValue", 22);
            }

            if ("thread2".equals(topLevelScope.getMethodName())) {

                TrackedValueAsserter.assertTrackedValue(watched, "differentValue", 22);
                thread2ErrorProcessed.compareAndSet(false, true);
                TrackedValueAsserter.assertTrackedValueNotCaptured(watched, "someValue", 11);
            }

            return "";
        }

        @Override
        public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
            assertFalse("this method should not be called it this test case", true);
            return "";
        }

        public boolean bothThreadErrorsHandled() {
            final boolean thread1Processed = thread1ErrorProcessed.get();
            final boolean thread2Processed = thread2ErrorProcessed.get();

            return thread1Processed && thread2Processed;
        }

    }

}
