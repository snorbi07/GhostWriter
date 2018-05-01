package io.ghostwriter.rt.snaperr.tracker;


import io.ghostwriter.rt.snaperr.TrackedValueAsserter;
import io.ghostwriter.rt.snaperr.api.TrackedScope;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StackBasedReferenceTrackerTest {

    @Test
    public void testIsEmptyByDefault() {
        final ReferenceTracker referenceTracker = new StackBasedReferenceTracker();
        assertTrue("new instance should be empty by default", referenceTracker.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testEnsureNonNullVariableName() {
        final ReferenceTracker referenceTracker = new StackBasedReferenceTracker();
        referenceTracker.pushScope(this, "testEnsureNonNullVariableName");
        referenceTracker.track(null, null);
    }

    @Test
    public void testTraversal() {
        final ReferenceTracker referenceTracker = new StackBasedReferenceTracker();
        referenceTracker.pushScope(this, "method1");
        referenceTracker.track("a", 1);
        referenceTracker.pushScope(this, "method2");
        referenceTracker.track("b", 2);
        referenceTracker.pushScope(this, "method3");
        referenceTracker.track("c", 3);

        ReferenceTrackerTraverser traverser = new ReferenceTrackerTraverser(referenceTracker);

        assertTrue(traverser.hasNext());

        TrackedScope next = traverser.next();

        assertTrue("method3".equals(next.getMethodName()));
        TrackedValueAsserter.assertNumberOfTrackedVariables(next.getReferences(), 1);
        TrackedValueAsserter.assertTrackedValue(next.getReferences(), "c", 3);

        next = traverser.next();
        assertTrue("method2".equals(next.getMethodName()));
        TrackedValueAsserter.assertNumberOfTrackedVariables(next.getReferences(), 1);
        TrackedValueAsserter.assertTrackedValue(next.getReferences(), "b", 2);

        next = traverser.next();
        assertTrue("method1".equals(next.getMethodName()));
        TrackedValueAsserter.assertNumberOfTrackedVariables(next.getReferences(), 1);
        TrackedValueAsserter.assertTrackedValue(next.getReferences(), "a", 1);

        assertFalse(traverser.hasNext());

        ReferenceTrackerTraverser newTraverser = new ReferenceTrackerTraverser(referenceTracker);
        assertFalse("traverser does not modify the state of the reference tracker", newTraverser.hasNext());
    }

}
