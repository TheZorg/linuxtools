package org.eclipse.linuxtools.internal.gcov.test;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

public class JobsRunning implements ICondition {

    private final Object family;
    private Job[] allJobs;

    public JobsRunning(Object family) {
        this.family = family;
    }

    @Override
    public boolean test() {
        this.allJobs = Job.getJobManager().find(family);
        return allJobs.length == 0;
    }

    @Override
    public void init(SWTBot bot) {

    }

    @Override
    public String getFailureMessage() {
        String message = "---JOB RUN ERROR---";
        if (allJobs != null) {
            message += "\nJobs in family \"" + family + "\" still running at time of last test:";
            for (Job job : allJobs) {
                message += "\n\"" + job.getName() + "\" with state " + job.getState();
            }
        } else {
            message += "\nTimeout happened before first test.";
        }
        return message;
    }
}
