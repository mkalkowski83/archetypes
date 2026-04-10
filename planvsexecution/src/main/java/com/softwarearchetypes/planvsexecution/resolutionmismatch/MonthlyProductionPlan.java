package com.softwarearchetypes.planvsexecution.resolutionmismatch;

import java.time.YearMonth;

// Plan: uproszczony, z góry — "300 sztuk w miesiącu"
record MonthlyProductionPlan(YearMonth month, int targetQuantity) {}
