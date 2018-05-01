package io.ghostwriter.rt.snaperr;

import io.ghostwriter.rt.snaperr.api.*;
import io.ghostwriter.rt.snaperr.tracker.ReferenceTracker;
import io.ghostwriter.rt.snaperr.tracker.StackBasedReferenceTracker;
import io.ghostwriter.rt.snaperr.api.TrackedValue;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.*;

public class SnaperrTest {

    /**
     * Test that {@link TriggerSerializer#serializeTrigger(ErrorTrigger)} gets called
     * with the correct values
     */
    @Test
    public void testTriggerCreation() {
        final String METHOD_NAME = "testTriggerCreation";
        final Object expectedContext = this;

        TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger trigger) {
                final TrackedScope topLevelScope = trigger.currentScope();
                assertTrackedScope(topLevelScope);

                Throwable throwable = trigger.getThrowable();
                String errorClassName = throwable.getClass().getName();
                Class<IllegalArgumentException> expectedErrorType = IllegalArgumentException.class;
                assertTrue("Expected error type: '" + expectedErrorType.getName() + "', got: " + errorClassName,
                        throwable instanceof IllegalArgumentException);

                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                final TrackedScope topLevelScope = timeoutTrigger.getReferenceTracker().currentScope();
                assertTrackedScope(topLevelScope);

                final long timeoutThreshold = timeoutTrigger.getTimeoutThreshold();
                assertTrue("Expected 'timeoutThreshold' value of 1000L, got: " + timeoutThreshold, timeoutThreshold == 1000L);

                final long timeout = timeoutTrigger.getTimeout();
                assertTrue("Expected 'timeout' value of 2000L, got: " + timeout, timeout == 2000L);

                return "";
            }

            private void assertTrackedScope(TrackedScope trackedScope) {
                String methodName = trackedScope.getMethodName();
                assertTrue("Expected method name: '" + METHOD_NAME + "', got: " +
                        methodName, METHOD_NAME.equals(methodName));

                Object source = trackedScope.getSource();
                assertTrue("Invalid source reference!", source.equals(expectedContext));

                Map<String, TrackedValue> watched = trackedScope.getReferences();
                assertTrue("Expected number of watched variables: 2, got: " + watched.size(),
                        watched.size() == 2);

                TrackedValueAsserter.assertTrackedValue(watched, "x", 42);
                TrackedValueAsserter.assertTrackedValue(watched, "q", 7);
            }

        };

        SnaperrTracer gwErrMon = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        gwErrMon.entering(this, METHOD_NAME);
        int x = 42;
        gwErrMon.valueChange(this, METHOD_NAME, "x", x);

        int q = 7;
        gwErrMon.valueChange(this, METHOD_NAME, "q", q);
        gwErrMon.onError(this, METHOD_NAME, new IllegalArgumentException());
        gwErrMon.timeout(this, METHOD_NAME, 1000L, 2000L);
    }

    /**
     * Test tracking of primitive variables i.e. int (not Integer)
     */
    @Test
    public void testTrackedPrimitiveValueUpdate() {
        final String METHOD_NAME = "testTrackedPrimitiveValueUpdate";
        TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger trigger) {
                final TrackedScope topLevelScope = trigger.currentScope();
                Map<String, TrackedValue> watched = topLevelScope.getReferences();

                assertTrue("Expected number of watched variables: 1, got: " + watched.size(),
                        watched.size() == 1);

                TrackedValueAsserter.assertTrackedValue(watched, "y", 2);
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }
        };

        SnaperrTracer gwErrMon = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        gwErrMon.entering("this", METHOD_NAME);
        gwErrMon.valueChange(this, METHOD_NAME, "y", 1);
        gwErrMon.valueChange(this, METHOD_NAME, "y", 2);
        gwErrMon.onError(this, METHOD_NAME, new IllegalArgumentException());
    }

    /**
     * Test that once a mutable object is captured by {@link SnaperrTracer#valueChange(Object, String, String, Object)},
     * a change in the new state of the mutable object will be present at the
     * {@link TriggerSerializer#serializeTrigger(ErrorTrigger)}
     */
    @Test
    public void testTrackedMutableObjectStateChange() {
        final String METHOD_NAME = "testTrackedMutableObjectStateChange";

        final MutableValueClass mvc = new MutableValueClass("this is a String", 981);
        TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger trigger) {
                final TrackedScope topLevelScope = trigger.currentScope();
                Map<String, TrackedValue> watched = topLevelScope.getReferences();

                assertTrue("Expected number of watched variables: 1, got: " + watched.size(),
                        watched.size() == 1);

                TrackedValueAsserter.assertTrackedValue(watched, "mvc", new MutableValueClass("this is a String", 314));

                TrackedValue capturedMvc = watched.get("mvc");
                assertTrue("Got different instance!", mvc == capturedMvc.getValue());
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }
        };

        SnaperrTracer gwErrMon = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        gwErrMon.entering(this, METHOD_NAME);
        gwErrMon.valueChange(this, METHOD_NAME, "mvc", mvc);
        mvc.setPrimitiveValue(314);
        gwErrMon.onError(this, METHOD_NAME, new IllegalArgumentException());
    }

    /**
     * Test that multiple {@link SnaperrTracer#valueChange(Object, String, String, Object)}
     * on the same object will result in only one tracker variable on the scope
     */
    @Test
    public void testTrackedReferenceChange() {
        final String METHOD_NAME = "testTrackedReferenceChange";
        TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger trigger) {
                final TrackedScope topLevelScope = trigger.currentScope();
                Map<String, TrackedValue> watched = topLevelScope.getReferences();

                assertTrue("Expected number of watched variables: 1, got: " + watched.size(),
                        watched.size() == 1);

                TrackedValueAsserter.assertTrackedValue(watched, "mvc", new MutableValueClass("second", 2));
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }
        };


        SnaperrTracer gwErrMon = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        MutableValueClass mvc = new MutableValueClass("first", 1);
        gwErrMon.entering(this, METHOD_NAME);
        gwErrMon.valueChange(this, METHOD_NAME, "mvc", mvc);
        mvc = new MutableValueClass("second", 2);
        gwErrMon.valueChange(this, METHOD_NAME, "mvc", mvc);
        gwErrMon.onError(this, METHOD_NAME, new IllegalArgumentException());
    }

    /**
     * Test that a new scope is created on every {@link SnaperrTracer#entering(Object, String, Object...)}
     * call, and {@link SnaperrTracer#onError(Object, String, Throwable)} only the current scope is
     * the last scope
     */
    @Test
    public void testTrackedScope() {
        TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger trigger) {
                final TrackedScope topLevelScope = trigger.currentScope();
                Map<String, TrackedValue> watched = topLevelScope.getReferences();

                assertTrue("Expected number of watched variables: 1, got: " + watched.size(),
                        watched.size() == 1);

                TrackedValueAsserter.assertTrackedValue(watched, "b", 2);
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }
        };

        SnaperrTracer gwSnaperr = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        gwSnaperr.entering(this, "methodX");
        gwSnaperr.valueChange(this, "methodX", "a", 1);
        gwSnaperr.entering(this, "methodY");
        gwSnaperr.valueChange(this, "methodY", "b", 2);
        gwSnaperr.onError(this, "methodY", new IllegalArgumentException());
    }

    /**
     * Simulation of a scenario where an unexpected error is only handled at the top of the call stack:
     * <pre>
     *
     *      method1() {
     *          try {
     *              x = 1
     *              method2() {
     *                  y = 2
     *                  method3() {
     *                      z = 3
     *                      throw new NullPointerException()
     *                  }
     *              }
     *          }
     *          catch (Exception e) {
     *              ...
     *          }
     *      }
     * </pre>
     */
    @Test
    public void testExceptionPropagationDetection() {

        TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger trigger) {
                final TrackedScope topLevelScope = trigger.currentScope();
                final String methodName = topLevelScope.getMethodName();
                boolean isCallerMethod = "method1".equals(methodName) || "method2".equals(methodName);
                assertFalse("Caller method triggered on error!", isCallerMethod);
                assertTrue("Expected method name 'method3', got: " + methodName, "method3".equals(methodName));

                Map<String, TrackedValue> watched = topLevelScope.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watched, 1);
                TrackedValueAsserter.assertTrackedValue(watched, "z", 3);
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }
        };

        SnaperrTracer gwSnaperr = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        gwSnaperr.entering(this, "method1");
        gwSnaperr.valueChange(this, "method1", "x", 1);
        gwSnaperr.entering(this, "method2");
        gwSnaperr.valueChange(this, "method2", "y", 2);
        gwSnaperr.entering(this, "method3");
        gwSnaperr.valueChange(this, "method3", "z", 3);
        Exception propagatingException = new IllegalArgumentException();
        gwSnaperr.onError(this, "method3", propagatingException);
        gwSnaperr.exiting(this, "method3");
        gwSnaperr.onError(this, "method2", propagatingException);
        gwSnaperr.exiting(this, "method2");
        gwSnaperr.exiting(this, "method1");
    }

    /**
     * During propagation a different type of exception is (re)thrown, so Snaperr should detect it
     * <pre>
     * method1() {
     *     x = 1
     *     method2() {
     *         try {
     *             y = 2
     *             method3() {
     *                 z = 3
     *                 throw new NullPointerException()
     *             }
     *         }
     *         catch (Exception e) {
     *             throw new IllegalArgumentException(e)
     *         }
     *     }
     * }
     * </pre>
     */
    @Test
    public void testExceptionRethrow() {
        final ExceptionRethrowSerializer exceptionRethrowSerializer = new ExceptionRethrowSerializer();
        SnaperrTracer gwSnaperr = new SnaperrTracer(new StackBasedReferenceTracker(), exceptionRethrowSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        gwSnaperr.entering(this, "method1");
        gwSnaperr.valueChange(this, "method1", "x", 1);
        gwSnaperr.entering(this, "method2");
        gwSnaperr.valueChange(this, "method2", "y", 2);
        gwSnaperr.entering(this, "method3");
        gwSnaperr.valueChange(this, "method3", "z", 3);
        Exception rootCauseException = new NullPointerException();
        gwSnaperr.onError(this, "method3", rootCauseException);
        gwSnaperr.exiting(this, "method3");
        Exception rethrownException = new IllegalArgumentException("rethrow for the win!", rootCauseException);
        gwSnaperr.onError(this, "method2", rethrownException);
        gwSnaperr.exiting(this, "method2");
        gwSnaperr.onError(this, "method1", rethrownException);
        gwSnaperr.exiting(this, "method1");

        assertTrue("expected '2' error events, got: '" + exceptionRethrowSerializer.getNumberOfErrorEvents() + "'",
                exceptionRethrowSerializer.getNumberOfErrorEvents() == 2);
    }

    /**
     * Simulation of a scenario where we enter and successfully exit from a method and before encountering an error
     * in the caller method
     * <pre>
     * method1() {
     *     x = 12
     *     y = 114
     *     method2(x,y) {
     *     }
     *     z = 212
     *     throw IllegalStateException
     * }
     *
     * </pre>
     */
    @Test
    public void testScopePersistence() {

        TriggerSerializer triggerSerializer = new TriggerSerializer() {

            @Override
            public String serializeTrigger(ErrorTrigger errorTrigger) {
                final TrackedScope trackedScope = errorTrigger.currentScope();
                final String methodName = trackedScope.getMethodName();
                assertTrue("Expected method name 'method1', got: " + methodName, "method1".equals(methodName));
                final Map<String, TrackedValue> watched = trackedScope.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watched, 3);
                TrackedValueAsserter.assertTrackedValue(watched, "x", 12);
                TrackedValueAsserter.assertTrackedValue(watched, "y", 114);
                TrackedValueAsserter.assertTrackedValue(watched, "z", 212);
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case!", false);
                return "";
            }
        };

        SnaperrTracer gwSnapper = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());

        gwSnapper.entering(this, "method1");
        gwSnapper.valueChange(this, "method1", "x", 12);
        gwSnapper.valueChange(this, "method1", "y", 114);
        gwSnapper.entering(this, "method2", new Object[]{"xX", 13, "yY", 115});
        gwSnapper.exiting(this, "method2");
        gwSnapper.valueChange(this, "method1", "z", 212);
        gwSnapper.onError(this, "method1", new IllegalStateException("the state is bad!"));
    }

    /**
     * It can happen that we call a GW instrumented function during Trigger processing, for example toString method of a POJO.
     * The implementation should be robust and handle this as well.
     */
    @Test
    public void testProcessingTracedValues() {

        final DelegatingTracer delegatingTracer = new DelegatingTracer();
        final TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger errorTrigger) {

                final TrackedScope trackedScopeBefore = errorTrigger.currentScope();
                final Map<String, TrackedValue> watchedBefore = trackedScopeBefore.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watchedBefore, 2);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "ten", 10);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "zero", 0);

                // simulation of a use case where we would serialize a watched POJO that is instrumented
                // the runtime needs to be able to handle such use cases
                delegatingTracer.entering(this, "breadcrumbs");
                delegatingTracer.valueChange(this, "breadcrumbs", "aww", "yiss");
                // ensure that if a processing is triggered, the state is not affected until the current processing is done
                final TrackedScope scopeDuringProcessing = errorTrigger.currentScope();
                final Map<String, TrackedValue> watchedDuringProcessing = scopeDuringProcessing.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watchedDuringProcessing, 2);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "ten", 10);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "zero", 0);
                delegatingTracer.exiting(this, "breadcrumbs");

                final TrackedScope trackedScopeAfter = errorTrigger.currentScope();
                final Map<String, TrackedValue> watchedAfter = trackedScopeAfter.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watchedAfter, 2);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "ten", 10);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "zero", 0);
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case!", false);
                return "";
            }
        };
        // the 2 step trigger setting is required because otherwise we could not reference the gwSnapper in the handler itself
        SnaperrTracer gwSnapper = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());
        delegatingTracer.setOtherTracer(gwSnapper);

        gwSnapper.entering(this, "tenPerZero");
        gwSnapper.valueChange(this, "tenPerZero", "ten", 10);
        gwSnapper.valueChange(this, "tenPerZero", "zero", 0);
        gwSnapper.onError(this, "tenPerZero", new ArithmeticException("legendary zero division"));
    }

    /**
     * It can happen that we call a GW instrumented function during Trigger
     * processing that also triggers an error, for example circular serialization
     */
    @Test
    public void testErrorEventDuringTriggerProcessing() {

        final DelegatingTracer delegatingGwSnapper = new DelegatingTracer();
        final TriggerSerializer triggerSerializer = new TriggerSerializer() {
            @Override
            public String serializeTrigger(ErrorTrigger errorTrigger) {
                final TrackedScope trackedScopeBefore = errorTrigger.currentScope();
                final Map<String, TrackedValue> watchedBefore = trackedScopeBefore.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watchedBefore, 2);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "a", 5);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "b", 6);

                // simulation of a use case where we have a potential error during trigger processing
                delegatingGwSnapper.entering(this, "bird");
                delegatingGwSnapper.valueChange(this, "bird", "pig", "grass");
                delegatingGwSnapper.onError(this, "bird", new NullPointerException());
                // ensure that if a processing is triggered, the state is not affected until the current processing is done
                final TrackedScope scopeDuringProcessing = errorTrigger.currentScope();
                final Map<String, TrackedValue> watchedDuringProcessing = scopeDuringProcessing.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watchedDuringProcessing, 2);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "a", 5);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "b", 6);
                delegatingGwSnapper.exiting(this, "bird");

                final TrackedScope trackedScopeAfter = errorTrigger.currentScope();
                final Map<String, TrackedValue> watchedAfter = trackedScopeAfter.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(watchedAfter, 2);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "a", 5);
                TrackedValueAsserter.assertTrackedValue(watchedBefore, "b", 6);
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case!", false);
                return "";
            }
        };

        SnaperrTracer gwSnapper = new SnaperrTracer(new StackBasedReferenceTracker(), triggerSerializer, new NoopTriggerHandler(), new UnrestrictedThrottler());
        delegatingGwSnapper.setOtherTracer(gwSnapper);

        // the 2 step trigger setting is required because otherwise we could not reference the gwSnapper in the handler itself

        gwSnapper.entering(this, "method1");
        gwSnapper.valueChange(this, "method1", "a", 5);
        gwSnapper.valueChange(this, "method1", "b", 6);
        gwSnapper.onError(this, "method1", new IllegalArgumentException("It's dead Jim."));
    }

    /**
     * In case of nested method calls, when an error occurs we should be able to access watched values from callers
     * <pre>
     * method1() {
     *     a = 1
     *     b = 2
     *     method2() {
     *         c = 3
     *         d = 4
     *         method3() {
     *             e = 5
     *             f = 6
     *             throw new NullPointerException()
     *         }
     *     }
     * }
     * </pre>
     */
    @Test
    public void testWatchedStackUnwinding() {


        TriggerSerializer triggerSerializer = new TriggerSerializer() {

            @Override
            public String serializeTrigger(ErrorTrigger trigger) {
                Iterator<TrackedScope> trackedScopeIterator = trigger.scopes();

                // method3 scope
                TrackedScope currentScope = trackedScopeIterator.next();
                String methodName = currentScope.getMethodName();
                assertTrue("expected 'method3', got: '" + methodName + "'", "method3".equals(methodName));
                Map<String, TrackedValue> references = currentScope.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(references, 3);
                TrackedValueAsserter.assertTrackedValue(references, "g", 7);
                TrackedValueAsserter.assertTrackedValue(references, "f", 6);
                TrackedValueAsserter.assertTrackedValue(references, "e", 5);

                // method2 scope
                currentScope = trackedScopeIterator.next();
                methodName = currentScope.getMethodName();
                assertTrue("expected 'method2', got: '" + methodName + "'", "method2".equals(methodName));
                references = currentScope.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(references, 2);
                TrackedValueAsserter.assertTrackedValue(references, "d", 4);
                TrackedValueAsserter.assertTrackedValue(references, "c", 3);

                // method1 scope
                currentScope = trackedScopeIterator.next();
                methodName = currentScope.getMethodName();
                assertTrue("expected 'method1', got: '" + methodName + "'", "method1".equals(methodName));
                references = currentScope.getReferences();
                TrackedValueAsserter.assertNumberOfTrackedVariables(references, 2);
                TrackedValueAsserter.assertTrackedValue(references, "b", 2);
                TrackedValueAsserter.assertTrackedValue(references, "a", 1);


                assertFalse("additional stack entries present", trackedScopeIterator.hasNext());

                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }
        };


        ReferenceTracker referenceTracker = new StackBasedReferenceTracker();
        TriggerHandler noopTriggerHandler = new NoopTriggerHandler();
        SnaperrTracer gwSnaperr = new SnaperrTracer(referenceTracker, triggerSerializer, noopTriggerHandler, new UnrestrictedThrottler());

        gwSnaperr.entering(this, "method1");
        gwSnaperr.valueChange(this, "method1", "a", 1);
        gwSnaperr.valueChange(this, "method1", "b", 2);
        gwSnaperr.entering(this, "method2");
        gwSnaperr.valueChange(this, "method2", "c", 3);
        gwSnaperr.valueChange(this, "method2", "d", 4);
        gwSnaperr.entering(this, "method3");
        gwSnaperr.valueChange(this, "method3", "e", 5);
        gwSnaperr.valueChange(this, "method3", "f", 6);
        gwSnaperr.valueChange(this, "method3", "g", 7);
        gwSnaperr.onError(this, "method3", new NullPointerException());
    }

    /**
     * Test that both {@link TriggerHandler#onError(String)} and {@link TriggerSerializer#serializeTrigger(ErrorTrigger)}
     * is called on error
     */
    @Test
    public void testTriggerHandlerCalled() {

        final int[] methodCallCount = {0};
        final String SERIALIZED_ERROR = "serialized error string";

        ReferenceTracker referenceTracker = new StackBasedReferenceTracker();
        TriggerHandler triggerHandler = new TriggerHandler() {

            @Override
            public void onError(String serializedError) {
                assertEquals("Serialized error message on trigger handler is not the same as returned from triggerserializer", SERIALIZED_ERROR, serializedError);
                methodCallCount[0]++;
            }

            @Override
            public void onTimeout(String serializedTimeout) {
                assertFalse("this should not be called in this test case", true);
            }

        };

        TriggerSerializer triggerSerializer = new TriggerSerializer() {

            @Override
            public String serializeTrigger(ErrorTrigger errorTrigger) {
                methodCallCount[0]++;
                return SERIALIZED_ERROR;
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }

        };

        SnaperrTracer gwSnaperr = new SnaperrTracer(referenceTracker, triggerSerializer, triggerHandler, new UnrestrictedThrottler());
        gwSnaperr.entering(this, "testTriggerHandlerCalled");
        gwSnaperr.onError(this, "testTriggerHandlerCalled", new NullPointerException());
        assertTrue("One of TriggerSerializer#serializeTrigger(ErrorTrigger) or TriggerHandler#onError() was not called", methodCallCount[0] == 2);
    }

    /**
     * Test that both {@link TriggerHandler#onTimeout(String)} and {@link TriggerSerializer#serializeTrigger(TimeoutTrigger)}
     * is called on timeout
     */
    @Test
    public void testTriggerHandlerCalled2() {

        final int[] methodCallCount = {0};
        final String SERIALIZED_TIMEOUT = "serialized error string";

        ReferenceTracker referenceTracker = new StackBasedReferenceTracker();
        TriggerHandler triggerHandler = new TriggerHandler() {

            @Override
            public void onError(String serializedError) {
                assertFalse("this should not be called in this test case", true);
            }

            @Override
            public void onTimeout(String serializedTimeout) {
                assertEquals("Serialized error message on trigger handler is not the same as returned from triggerserializer", SERIALIZED_TIMEOUT, serializedTimeout);
                methodCallCount[0]++;
            }

        };

        TriggerSerializer triggerSerializer = new TriggerSerializer() {

            @Override
            public String serializeTrigger(ErrorTrigger errorTrigger) {
                assertFalse("this should not be called in this test case", true);
                return "";
            }

            @Override
            public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
                methodCallCount[0]++;
                return SERIALIZED_TIMEOUT;
            }

        };

        SnaperrTracer gwSnaperr = new SnaperrTracer(referenceTracker, triggerSerializer, triggerHandler, new UnrestrictedThrottler());
        gwSnaperr.entering(this, "testTriggerHandlerCalled2");
        gwSnaperr.timeout(this, "testTriggerHandlerCalled2", 10, 11);
        assertTrue("One of TriggerSerializer#serializeTrigger(TimeoutTrigger) or TriggerHandler#onTimeout() was not called", methodCallCount[0] == 2);
    }

    private static class ExceptionRethrowSerializer implements TriggerSerializer {

        private int numberOfErrorEvents = 0;

        public int getNumberOfErrorEvents() {
            return numberOfErrorEvents;
        }

        @Override
        public String serializeTrigger(ErrorTrigger trigger) {
            final TrackedScope topLevelScope = trigger.currentScope();
            final String methodName = topLevelScope.getMethodName();
            if ("method3".equals(methodName)) {
                final Throwable throwable = trigger.getThrowable();
                assertTrue("expected exception type of '" + NullPointerException.class.getSimpleName() +
                                "', got: " + throwable.getClass(),
                        throwable instanceof NullPointerException);
                ++numberOfErrorEvents;
            } else if ("method2".equals(methodName)) {
                final Throwable throwable = trigger.getThrowable();
                assertTrue("expected exception type of '" + IllegalArgumentException.class.getSimpleName() +
                                "', got: " + throwable.getClass(),
                        throwable instanceof IllegalArgumentException);
                assertTrue("root cause does not match the expected '" + NullPointerException.class.getSimpleName() + "'",
                        throwable.getCause() instanceof NullPointerException);
                ++numberOfErrorEvents;
            } else {
                assertFalse("error propagating from method: " + methodName, false);
            }

            return "";
        }

        @Override
        public String serializeTrigger(TimeoutTrigger timeoutTrigger) {
            assertFalse("this should not be called in this test case", true);
            return "";
        }

    }

}
