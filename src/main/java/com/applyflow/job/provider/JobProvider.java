package com.applyflow.job.provider;

import java.util.List;

public interface JobProvider {

    /** Stable identifier of the source, stored in jobs.source. */
    String source();

    List<RawJob> fetchJobs();
}