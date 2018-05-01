package io.ghostwriter.rt.snaperr.core;

import io.ghostwriter.rt.snaperr.api.ErrorTrigger;
import io.ghostwriter.rt.snaperr.tracker.StackBasedReferenceTracker;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class StringSerializerTest {

    @Test
    public void testErrorTriggerSerialization() {
        final StringSerializer stringSerializer = new StringSerializer();

        final StackBasedReferenceTracker stackBasedReferenceTracker = new StackBasedReferenceTracker();

        final DummmyClass dummmyClass = new DummmyClass();
        stackBasedReferenceTracker.pushScope(dummmyClass, "testErrorTriggerSerialization");
        stackBasedReferenceTracker.track("drugs", "bad");
        stackBasedReferenceTracker.track("coffee", "good");
        stackBasedReferenceTracker.track("meaningOfLife", 42);

        stackBasedReferenceTracker.pushScope(dummmyClass, "thisMethodIsALie");
        stackBasedReferenceTracker.track("a", 1);
        stackBasedReferenceTracker.track("b", 2);

        stackBasedReferenceTracker.pushScope(dummmyClass, "anotherLie");
        stackBasedReferenceTracker.track("c", 3);
        stackBasedReferenceTracker.track("d", 4);
        stackBasedReferenceTracker.track("e", 5);
        stackBasedReferenceTracker.track("f", 6);

        final ErrorTrigger errorTrigger = new ErrorTrigger(stackBasedReferenceTracker, new NullPointerException("A wild exception has appeared!"));
        final String serializeTrigger = stringSerializer.serializeTrigger(errorTrigger);

        final String FORMATTED_RESULT = "Snaperr - state snapshot of error 'java.lang.NullPointerException: A wild exception has appeared!': [\n" +
                "\tDummmyClass{}.anotherLie {\n" +
                "\t\tc=3\n" +
                "\t\td=4\n" +
                "\t\te=5\n" +
                "\t\tf=6\n" +
                "\t}\n" +
                "\tDummmyClass{}.thisMethodIsALie {\n" +
                "\t\ta=1\n" +
                "\t\tb=2\n" +
                "\t}\n" +
                "\tDummmyClass{}.testErrorTriggerSerialization {\n" +
                "\t\tdrugs=bad\n" +
                "\t\tcoffee=good\n" +
                "\t\tmeaningOfLife=42\n" +
                "\t}\n" +
                "]";

        assertTrue(FORMATTED_RESULT.equals(serializeTrigger));
    }

    private static class DummmyClass {
        @Override
        public String toString() {
            return "DummmyClass{}";
        }
    }

}
