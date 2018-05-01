package io.ghostwriter.rt.snaperr;

import io.ghostwriter.rt.snaperr.api.ErrorTrigger;
import io.ghostwriter.rt.snaperr.api.TrackedScope;
import io.ghostwriter.rt.snaperr.core.ThrottleController;
import io.ghostwriter.rt.snaperr.tracker.ReferenceTracker;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ThrottleControllerTest {

    @Test
    public void testDefaultThrottleControlStrategy() {
        long errorWindowSize = 1000;
        int maxErrorsInWindow = 3;
        final long[] currentTimeMillis = {0L};
        ThrottleController throttleControl = new ThrottleController(errorWindowSize, maxErrorsInWindow) {

            @Override
            protected long currentTimeMillis() {
                return currentTimeMillis[0];
            }

        };

        final ErrorTrigger dummyErrorTrigger = new ErrorTrigger(new MockReferenceTracker(), new NullPointerException());

        currentTimeMillis[0] = 10;
        boolean handleError = throttleControl.doHandleError(dummyErrorTrigger);
        assertTrue("Throttle control should allow handling error at time " + currentTimeMillis[0], handleError);

        currentTimeMillis[0] = 20;
        handleError = throttleControl.doHandleError(dummyErrorTrigger);
        assertTrue("Throttle control should allow handling error at time " + currentTimeMillis[0], handleError);

        currentTimeMillis[0] = 30;
        handleError = throttleControl.doHandleError(dummyErrorTrigger);
        assertTrue("Throttle control should allow handling error at time " + currentTimeMillis[0], handleError);

        currentTimeMillis[0] = 40;
        // This is where we reached maxErrorsInWindow = 3 within errorWindowSize = 1000
        handleError = throttleControl.doHandleError(dummyErrorTrigger);
        assertFalse("Throttle control should prohibit handling error at time " + currentTimeMillis[0], handleError);

        // now let's move forward in time and see if we can handle errors again
        currentTimeMillis[0] = 1000;
        handleError = throttleControl.doHandleError(dummyErrorTrigger);
        assertTrue("Throttle control should allow handling error at time " + currentTimeMillis[0], handleError);
    }

    private static class MockReferenceTracker implements ReferenceTracker {

        @Override
        public <T> void track(String variableName, T variableReference) {
            return;
        }

        @Override
        public void pushScope(Object source, String methodName) {
            return;
        }

        @Override
        public void popScope() {
            return;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public TrackedScope currentScope() {
            return null;
        }

        @Override
        public Iterator<TrackedScope> scopes() {
            return Collections.emptyIterator();
        }
    }

}
