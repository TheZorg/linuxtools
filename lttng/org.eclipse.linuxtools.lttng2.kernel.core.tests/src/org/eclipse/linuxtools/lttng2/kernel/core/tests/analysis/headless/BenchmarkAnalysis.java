/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.core.tests.analysis.headless;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import org.eclipse.linuxtools.lttng2.kernel.core.analysis.LttngKernelAnalysisModule;
import org.eclipse.linuxtools.lttng2.kernel.core.trace.LttngKernelTrace;
import org.eclipse.linuxtools.tmf.core.analysis.IAnalysisModule;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.linuxtools.tmf.core.exceptions.TmfTraceException;
import org.eclipse.linuxtools.tmf.core.tests.shared.TmfTestHelper;
import org.eclipse.linuxtools.tmf.core.trace.TmfTraceManager;
import org.eclipse.linuxtools.tmf.ctf.core.CtfTmfEvent;
import org.eclipse.linuxtools.tmf.ctf.core.tests.shared.CtfTmfTestTrace;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This is a test of the time to build a kernel state system
 *
 * @author Genevieve Bastien
 */
public class BenchmarkAnalysis {

    private static final int LOOP_COUNT = 10;
    private static final CtfTmfTestTrace testTrace = CtfTmfTestTrace.TRACE2;

    /**
     * Set-up.
     */
    @BeforeClass
    public static void initialize() {
        assumeTrue(testTrace.exists());
    }

    /**
     * Performance test
     */
    @Test
    public void testTrace() {
        long[] time = new long[LOOP_COUNT];
        for (int i = 0; i < LOOP_COUNT; i++) {

            try (IAnalysisModule module = new LttngKernelAnalysisModule();
                    LttngKernelTrace trace = new LttngKernelTrace()) {
                module.setId("test");
                trace.initTrace(null, testTrace.getPath(), CtfTmfEvent.class);
                module.setTrace(trace);
                long start = System.nanoTime();
                TmfTestHelper.executeAnalysis(module);
                long end = System.nanoTime();
                File suppDir = new File(TmfTraceManager.getSupplementaryFileDir(trace));
                for (File file : suppDir.listFiles()) {
                    file.delete();
                }
                time[i] = end - start;
            } catch (TmfAnalysisException | TmfTraceException e) {
                fail(e.getMessage());
            }
        }
        double avg = 0, avgHot = 0;
        for (int i = 0; i < LOOP_COUNT; i++) {
            avg += time[i];
            if (i > 2) {
                avgHot += time[i];
            }
        }
        System.out.println("Cache cold : " + String.format("%.3f", 0.000001 * time[0]) + "ms\t average time : " + String.format("%.3f", 0.000001 * avg / LOOP_COUNT) + " ms\t average (cache hot) time : "
                + String.format("%.3f", 0.000001 * (avgHot / LOOP_COUNT - 3)));
    }
}
