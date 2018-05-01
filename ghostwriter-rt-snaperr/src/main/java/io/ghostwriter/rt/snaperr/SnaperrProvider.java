package io.ghostwriter.rt.snaperr;

import io.ghostwriter.TracerProvider;
import io.ghostwriter.rt.snaperr.api.Throttler;
import io.ghostwriter.rt.snaperr.api.TriggerHandler;
import io.ghostwriter.rt.snaperr.api.TriggerSerializer;
import io.ghostwriter.rt.snaperr.core.ThrottleController;
import io.ghostwriter.rt.snaperr.core.SystemOutWriter;
import io.ghostwriter.rt.snaperr.core.StringSerializer;
import io.ghostwriter.rt.snaperr.tracker.ReferenceTracker;
import io.ghostwriter.rt.snaperr.tracker.StackBasedReferenceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnaperrProvider implements TracerProvider<SnaperrTracer> {

    private static final Logger LOG = LoggerFactory.getLogger(SnaperrProvider.class);

    @Override
    public SnaperrTracer getTracer() {
        LOG.info("Loading GhostWriter Snaperr...");
        final ReferenceTracker referenceTracker = new StackBasedReferenceTracker();
        final TriggerSerializer serializer = new StringSerializer();
        final TriggerHandler handler = new SystemOutWriter();
        final Throttler throttler = new ThrottleController();

        return new SnaperrTracer(referenceTracker, serializer, handler, throttler);
    }

}
