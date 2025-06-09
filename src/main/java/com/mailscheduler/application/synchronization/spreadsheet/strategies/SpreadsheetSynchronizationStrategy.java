package com.mailscheduler.application.synchronization.spreadsheet.strategies;

import com.mailscheduler.domain.model.common.vo.spreadsheet.SpreadsheetConfiguration;
import com.mailscheduler.domain.model.common.vo.spreadsheet.SheetConfiguration;

/**
 * Interface defining the contract for synchronization strategies between
 * a spreadsheet data source and the application's domain model.
 */
public interface SpreadsheetSynchronizationStrategy {
    /**
     * Synchronizes data from the spreadsheet to the application domain.
     *
     * @param configuration The spreadsheet configuration containing necessary metadata
     * @throws IllegalArgumentException if the configuration is invalid
     */
    void synchronize(SpreadsheetConfiguration configuration);

    /**
     * Returns the name of the strategy for logging and identification purposes.
     *
     * @return The name of this synchronization strategy
     */
    String getStrategyName();

    /**
     * Checks if this strategy should process the given sheet.
     *
     * @param sheetConfiguration The sheet configuration to check
     * @return true if this strategy should process the sheet, false otherwise
     */
    boolean shouldProcessSheet(SheetConfiguration sheetConfiguration);
}