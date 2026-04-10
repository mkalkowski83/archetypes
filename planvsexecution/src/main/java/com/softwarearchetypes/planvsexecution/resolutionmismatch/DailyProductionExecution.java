package com.softwarearchetypes.planvsexecution.resolutionmismatch;

import java.time.LocalDate;
import java.util.List;

// Wykonanie: szczegółowe, z dołu — "270 + 12 braków + 18 poprawek w dniach 2,4,7,12"
record DailyProductionExecution(LocalDate date, int produced, int defects, int rework) {}

record DailyProductionExecutionHistory(List<DailyProductionExecution> days) {}
