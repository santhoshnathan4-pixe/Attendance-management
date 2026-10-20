package com.example.attendance;

import java.util.ArrayList;
import java.util.List;

public class ExcelImportResponse {

    private final int importedCount;
    private final int failedCount;
    private final int incompleteCount;
    private final List<String> errors;
    private final List<String> warnings;

    public ExcelImportResponse(
            int importedCount,
            int failedCount,
            List<String> errors) {

        this(importedCount, failedCount, 0, errors, new ArrayList<>());
    }

    public ExcelImportResponse(
            int importedCount,
            int failedCount,
            int incompleteCount,
            List<String> errors,
            List<String> warnings) {

        this.importedCount = importedCount;
        this.failedCount = failedCount;
        this.incompleteCount = incompleteCount;
        this.errors = errors;
        this.warnings = warnings;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    /** Same number as failedCount (the page reads this name). */
    public int getSkippedCount() {
        return failedCount;
    }

    /** Employees that were added but still need details filled by Edit. */
    public int getIncompleteCount() {
        return incompleteCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}