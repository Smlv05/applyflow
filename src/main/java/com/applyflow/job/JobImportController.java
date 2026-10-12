package com.applyflow.job;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobImportController {

    private final JobImportService importService;

    public JobImportController(JobImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ImportResult> importJobs() {
        return importService.importAll();
    }
}