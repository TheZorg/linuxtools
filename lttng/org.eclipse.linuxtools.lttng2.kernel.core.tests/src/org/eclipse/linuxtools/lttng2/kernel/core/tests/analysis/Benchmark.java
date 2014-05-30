package org.eclipse.linuxtools.lttng2.kernel.core.tests.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.linuxtools.internal.lttng2.kernel.core.Attributes;
import org.eclipse.linuxtools.lttng2.kernel.core.analysis.LttngKernelAnalysisModule;
import org.eclipse.linuxtools.statesystem.core.ITmfStateSystem;
import org.eclipse.linuxtools.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.linuxtools.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.linuxtools.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.linuxtools.statesystem.core.statevalue.ITmfStateValue;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.trace.ITmfTrace;
import org.eclipse.linuxtools.tmf.ctf.core.tests.shared.CtfTmfTestTrace;

public class Benchmark {

    public static void main(String[] args) {
        // TODO Auto-generated method stub
        long startTime = System.nanoTime();
        try (LttngKernelAnalysisModule module = new LttngKernelAnalysisModule()) {
            ITmfTrace trace = CtfTmfTestTrace.KERNEL.getTrace();
            module.setId("test");
            try {
                module.setTrace(trace);
            } catch (TmfAnalysisException e) {
                e.printStackTrace();
                return;
            }
            module.schedule();
            module.waitForInitialization();
            ITmfStateSystem ssq = module.getStateSystem();
            if (ssq == null) {
                return;
            }

            long start = ssq.getStartTime();
            boolean complete = false;
            while (!complete) {
                ssq.waitUntilBuilt();
                complete = ssq.waitUntilBuilt(500);
                if (ssq.isCancelled()) {
                    return;
                }
                long end = ssq.getCurrentEndTime();
                if (start == end && !complete) {
                    continue;
                }
                List<Integer> quarks = ssq.getQuarks(Attributes.THREADS, "*");
                Set<Integer> entries = new HashSet<>();
                for (int threadQuark : quarks) {
                    doThread(ssq, start, end, entries, threadQuark);
                }
            }
        }
        long endTime = System.nanoTime();
    System.out.println("done in " + (endTime-startTime)/1000000000);
    }

    private static void doThread(ITmfStateSystem ssq, long start, long end, Set<Integer> entries, int threadQuark) {
        String threadName = ssq.getAttributeName(threadQuark);
        int threadId = -1;
        try {
            threadId = Integer.parseInt(threadName);
        } catch (NumberFormatException e1) {
            return;
        }
        if (threadId <= 0) { // ignore the 'unknown' (-1) and swapper (0) threads
            return;
        }
        int execNameQuark;
        List<ITmfStateInterval> execNameIntervals;
        try {
            execNameQuark = ssq.getQuarkRelative(threadQuark, Attributes.EXEC_NAME);
            execNameIntervals = ssq.queryHistoryRange(execNameQuark, start, end);
        } catch (AttributeNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        } catch (StateSystemDisposedException e) {
            e.printStackTrace();
            return;
        }
        for (ITmfStateInterval execNameInterval : execNameIntervals) {
            doExecName(ssq, entries, threadQuark, threadId, execNameInterval);
        }
    }

    private static void doExecName(ITmfStateSystem ssq, Set<Integer> entries, int threadQuark, int threadId, ITmfStateInterval execNameInterval) {
        if (!execNameInterval.getStateValue().isNull() &&
                execNameInterval.getStateValue().getType() == ITmfStateValue.Type.STRING) {
            String execName = execNameInterval.getStateValue().unboxStr();
            long startTime = execNameInterval.getStartTime();
            long endTime = execNameInterval.getEndTime() + 1;
            if (!entries.contains(threadId)) {
                ITmfStateInterval ppidInterval = null;
                try {
                    int ppidQuark = ssq.getQuarkRelative(threadQuark, Attributes.PPID);
                    ppidInterval = ssq.querySingleState(startTime, ppidQuark);
                } catch (AttributeNotFoundException e) {
                    /* No info, keep PPID at -1 */
                } catch (StateSystemDisposedException e) {
                    /* SS is closing down, time to bail */
                    return;
                }
                entries.add(threadId);
                int ppid = -1;
                if (!(ppidInterval == null) && !ppidInterval.getStateValue().isNull()) {
                    ppid = ppidInterval.getStateValue().unboxInt();
                }
                System.out.println(String.format("%s: %d-%d %d", execName.trim(), startTime, endTime, ppid));
                execName.trim();
                ppid = (int)endTime;
                endTime = ppid;
            }
        }
    }
}



