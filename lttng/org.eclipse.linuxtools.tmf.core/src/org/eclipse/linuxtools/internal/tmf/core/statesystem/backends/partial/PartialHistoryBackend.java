/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.internal.tmf.core.statesystem.backends.partial;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.backend.IStateHistoryBackend;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.statesystem.core.interval.TmfStateInterval;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.event.ITmfEvent;
import org.eclipse.linuxtools.tmf.core.request.ITmfEventRequest;
import org.eclipse.linuxtools.tmf.core.request.TmfEventRequest;
import org.eclipse.linuxtools.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.linuxtools.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.linuxtools.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;

/**
 * Partial state history back-end.
 *
 * This is a shim inserted between the real state system and a "real" history
 * back-end. It will keep checkpoints, every n trace events (where n is called
 * the granularity) and will only forward to the real state history the state
 * intervals that crosses at least one checkpoint. Every other interval will
 * be discarded.
 *
 * This would mean that it can only answer queries exactly at the checkpoints.
 * For any other timestamps (ie, most of the time), it will load the closest
 * earlier checkpoint, and will re-feed the state-change-input with events from
 * the trace, to restore the real state at the time that was requested.
 *
 * @author Alexandre Montplaisir
 */
public class PartialHistoryBackend implements IStateHistoryBackend {

    private boolean[] fQuarksAdded;

    /**
     * A partial history needs the state input plugin to re-generate state
     * between checkpoints.
     */
    private final ITmfStateProvider partialInput;

    /**
     * Fake state system that is used for partially rebuilding the states (when
     * going from a checkpoint to a target query timestamp).
     */
    private final PartialStateSystem partialSS;

    /** Reference to the "real" state history that is used for storage */
    private final IStateHistoryBackend innerHistory;

    /** Checkpoints map, <Timestamp, Rank in the trace> */
    private final TreeSet<Long> checkpoints = new TreeSet<>();

    /** Latch tracking if the initial checkpoint registration is done */
//    private final CountDownLatch checkpointsReady = new CountDownLatch(1);

//    private final long granularity;
    private long fCheckpointQuark;
    private boolean initialized = false;

    private long latestTime;

    /**
     * Constructor
     *
     * @param partialInput
     *            The state change input object that was used to build the
     *            upstream state system. This partial history will make its own
     *            copy (since they have different targets).
     * @param pss
     *            The partial history's inner state system. It should already be
     *            assigned to partialInput.
     * @param realBackend
     *            The real state history back-end to use. It's supposed to be
     *            modular, so it should be able to be of any type.
     * @param granularity
     *            Configuration parameter indicating how many trace events there
     *            should be between each checkpoint
     */
    public PartialHistoryBackend(ITmfStateProvider partialInput, PartialStateSystem pss,
            IStateHistoryBackend realBackend, long granularity) {
        if (granularity <= 0 || partialInput == null || pss == null ||
                partialInput.getAssignedStateSystem() != pss) {
            throw new IllegalArgumentException();
        }

        final long startTime = realBackend.getStartTime();

        this.partialInput = partialInput;
        this.partialSS = pss;

        this.innerHistory = realBackend;
//        this.granularity = granularity;

        latestTime = startTime;

        //registerCheckpoints();

    }

//    private void registerCheckpoints() {
//        ITmfEventRequest request = new CheckpointsRequest(partialInput, checkpoints);
//        partialInput.getTrace().sendRequest(request);
//        /* The request will countDown the checkpoints latch once it's finished */
//    }

    @Override
    public long getStartTime() {
        return innerHistory.getStartTime();
    }

    @Override
    public long getEndTime() {
//        return latestTime;
        return innerHistory.getEndTime();
    }

    @Override
    public void insertPastState(long stateStartTime, long stateEndTime,
            int quark, ITmfStateValue value) throws TimeRangeException {
        if (!initialized) {
            fCheckpointQuark = partialSS.getQuarkAbsoluteAndAdd(AbstractTmfStateProvider.CHECKPOINT_NAME);
            fQuarksAdded = new boolean[partialSS.getNbAttributes()];
            Arrays.fill(fQuarksAdded, Boolean.TRUE);
            checkpoints.add(partialInput.getStartTime());
            initialized = true;
        }
        if( quark == fCheckpointQuark){
            fQuarksAdded = new boolean[partialSS.getNbAttributes()];
            Arrays.fill(fQuarksAdded, Boolean.TRUE);
            checkpoints.add(stateEndTime);
            return;
        }

        /* Update the latest time */
        if (stateEndTime > latestTime) {
            latestTime = stateEndTime;
        }

        /*
         * Check if the interval intersects the previous checkpoint. If so,
         * insert it in the real history back-end.
         */
        if ( quark >= fQuarksAdded.length || (quark < fQuarksAdded.length && fQuarksAdded[quark])) {
            if (quark >= fQuarksAdded.length) {
                fQuarksAdded = Arrays.copyOf(fQuarksAdded, partialSS.getNbAttributes());
            }
            fQuarksAdded[quark] = false;
            innerHistory.insertPastState(stateStartTime, stateEndTime, quark, value);
        }
    }

    @Override
    public void finishedBuilding(long endTime) throws TimeRangeException {
        innerHistory.finishedBuilding(endTime);
    }

    @Override
    public FileInputStream supplyAttributeTreeReader() {
        return innerHistory.supplyAttributeTreeReader();
    }

    @Override
    public File supplyAttributeTreeWriterFile() {
        return innerHistory.supplyAttributeTreeWriterFile();
    }

    @Override
    public long supplyAttributeTreeWriterFilePosition() {
        return innerHistory.supplyAttributeTreeWriterFilePosition();
    }

    @Override
    public void removeFiles() {
        innerHistory.removeFiles();
    }

    @Override
    public void dispose() {
        innerHistory.dispose();
    }

    @Override
    public void doQuery(List<ITmfStateInterval> currentStateInfo, long t)
            throws TimeRangeException, StateSystemDisposedException {
        /* Wait for required steps to be done */
//        waitForCheckpoints();
        partialSS.getUpstreamSS().waitUntilBuilt();

        if (!checkValidTime(t)) {
            throw new TimeRangeException();
        }

        /* Reload the previous checkpoint */
        long checkpointTime = checkpoints.floor(t);
        innerHistory.doQuery(currentStateInfo, checkpointTime);

        /*
         * Set the initial contents of the partial state system (which is the
         * contents of the query at the checkpoint).
         */
        partialSS.takeQueryLock();
        partialSS.replaceOngoingState(currentStateInfo);

        /* Send an event request to update the state system to the target time. */
        TmfTimeRange range = new TmfTimeRange(
                /*
                 * The state at the checkpoint already includes any state change
                 * caused by the event(s) happening exactly at 'checkpointTime',
                 * if any. We must not include those events in the query.
                 */
                new TmfTimestamp(checkpointTime + 1, ITmfTimestamp.NANOSECOND_SCALE),
                new TmfTimestamp(t, ITmfTimestamp.NANOSECOND_SCALE));
        ITmfEventRequest request = new PartialStateSystemRequest(partialInput, range);
        partialInput.getTrace().sendRequest(request);

        try {
            request.waitForCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        /*
         * Now the partial state system should have the ongoing time we are
         * looking for. However, the method expects a List of *state intervals*,
         * not state values, so we'll create intervals with a dummy end time.
         */
        try {
            for (int i = 0; i < currentStateInfo.size(); i++) {
                long start = 0;
                ITmfStateValue val = null;
                start = ((ITmfStateSystem) partialSS).getOngoingStartTime(i);
                val = ((ITmfStateSystem) partialSS).queryOngoingState(i);

                ITmfStateInterval interval = new TmfStateInterval(start, t, i, val);
                currentStateInfo.set(i, interval);
            }
        } catch (AttributeNotFoundException e) {
            /* Should not happen, we iterate over existing values. */
            e.printStackTrace();
        }

        partialSS.releaseQueryLock();
    }

    /**
     * Single queries are not supported in partial histories. To get the same
     * result you can do a full query, then call fullState.get(attribute).
     * @throws StateSystemDisposedException
     * @throws TimeRangeException
     */
    @Override
    public ITmfStateInterval doSingularQuery(long t, int attributeQuark)
            throws TimeRangeException, StateSystemDisposedException {
        partialSS.getUpstreamSS().waitUntilBuilt();
        /* In previous checkpoint? */
        long previousCheckpointTime = checkpoints.floor(t);
        ITmfStateInterval interval = null;
        try {
            interval = innerHistory.doSingularQuery(previousCheckpointTime, attributeQuark);
            if (interval != null && interval.getEndTime() >= t) {
                return interval;
            }
        } catch (AttributeNotFoundException e) {
            // Not in this checkpoint, keep going
        }

        /* In next checkpoint? */
        long nextCheckpointTime = checkpoints.ceiling(t+1);
        try {
            interval = innerHistory.doSingularQuery(nextCheckpointTime, attributeQuark);
            if (interval != null && interval.getStartTime() <= t) {
                return interval;
            }
        } catch (AttributeNotFoundException e) {
         // Not in this checkpoint, keep going
        }

        /* In neither checkpoint, check partial state system backend */
        try {
            interval = partialSS.querySingleState(t, attributeQuark);
            if (interval != null) {
                return interval;
            }
        } catch (AttributeNotFoundException | TimeRangeException e) {
         // Not in backend cache, keep going
        }

        /* Not in partial state backend, read all events between checkpoints */
        int nbAttributes = partialSS.getNbAttributes();
        List<ITmfStateInterval> currentStateInfo = new ArrayList<>(nbAttributes);
        /* Bring the size of the array to the current number of attributes */
        for (int i = 0; i < nbAttributes; i++) {
            currentStateInfo.add(null);
        }

        /* Reload the previous checkpoint */
        innerHistory.doQuery(currentStateInfo, previousCheckpointTime);

        /*
         * Set the initial contents of the partial state system (which is the
         * contents of the query at the checkpoint).
         */
        partialSS.takeQueryLock();
        partialSS.reset();
        partialSS.replaceOngoingState(currentStateInfo);

        /* Send an event request to update the state system to the target time. */
        TmfTimeRange range = new TmfTimeRange(
                /*
                 * The state at the checkpoint already includes any state change
                 * caused by the event(s) happening exactly at 'checkpointTime',
                 * if any. We must not include those events in the query.
                 */
                new TmfTimestamp(previousCheckpointTime + 1, ITmfTimestamp.NANOSECOND_SCALE),
                new TmfTimestamp(nextCheckpointTime, ITmfTimestamp.NANOSECOND_SCALE));
        ITmfEventRequest request = new PartialStateSystemRequest(partialInput, range);
        partialInput.getTrace().sendRequest(request);

        try {
            long start = System.nanoTime();
            request.waitForCompletion();
            long end = System.nanoTime();
            System.out.println("Request took: " + (end - start) + "ns.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            interval = partialSS.querySingleState(t, attributeQuark);
            // Don't forget to release the lock (should go in a finally)
            partialSS.releaseQueryLock();
            return interval;
        } catch (AttributeNotFoundException e) {
            // Shouldn't happen...
        }
        partialSS.releaseQueryLock();
        return null;
    }

    @Override
    public boolean checkValidTime(long t) {
        return (t >= getStartTime() && t <= getEndTime());
    }

    @Override
    public void debugPrint(PrintWriter writer) {
        // TODO Auto-generated method stub
    }

//    private void waitForCheckpoints() {
//        try {
//            checkpointsReady.await();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    // ------------------------------------------------------------------------
    // Event requests types
    // ------------------------------------------------------------------------

//    private class CheckpointsRequest extends TmfEventRequest {
//        private final ITmfTrace trace;
//        private final Map<Long, Long> checkpts;
//        private long eventCount;
//        private long lastCheckpointAt;
//
//        public CheckpointsRequest(ITmfStateProvider input, Map<Long, Long> checkpoints) {
//            super(input.getExpectedEventType(),
//                    TmfTimeRange.ETERNITY,
//                    0,
//                    ITmfEventRequest.ALL_DATA,
//                    ITmfEventRequest.ExecutionType.FOREGROUND);
//            checkpoints.clear();
//            this.trace = input.getTrace();
//            this.checkpts = checkpoints;
//            eventCount = 0;
//            lastCheckpointAt = 0;
//
//            /* Insert a checkpoint at the start of the trace */
//            checkpoints.put(input.getStartTime(), 0L);
//        }
//
//        @Override
//        public void handleData(final ITmfEvent event) {
//            super.handleData(event);
//            if (event.getTrace() == trace) {
//                eventCount++;
//
//                /* Check if we need to register a new checkpoint */
//                if (eventCount >= lastCheckpointAt + granularity) {
//                    checkpts.put(event.getTimestamp().getValue(), eventCount);
//                    lastCheckpointAt = eventCount;
//                }
//            }
//        }
//
//        @Override
//        public void handleCompleted() {
//            super.handleCompleted();
//            checkpointsReady.countDown();
//        }
//    }

    private class PartialStateSystemRequest extends TmfEventRequest {
        private final ITmfStateProvider sci;
        private final ITmfTrace trace;
        private final long endTime;

        PartialStateSystemRequest(ITmfStateProvider sci, TmfTimeRange range) {
            super(sci.getExpectedEventType(),
                    range,
                    0,
                    ITmfEventRequest.ALL_DATA,
                    ITmfEventRequest.ExecutionType.FOREGROUND);
            this.sci = sci;
            this.trace = sci.getTrace();
            this.endTime = range.getEndTime().getValue();
        }

        @Override
        public void handleData(final ITmfEvent event) {
            super.handleData(event);
            if (event.getTrace() == trace) {
                sci.processEvent(event);
            }
        }

        @Override
        public void handleCompleted() {
            /*
             * If we're using a threaded state provider, we need to make sure
             * all events have been handled by the state system before doing
             * queries on it.
             */
            if (partialInput instanceof AbstractTmfStateProvider) {
                ((AbstractTmfStateProvider) partialInput).waitForEmptyQueue();
            }
            super.handleCompleted();
            partialSS.closeHistory(endTime);
        }
    }
}
