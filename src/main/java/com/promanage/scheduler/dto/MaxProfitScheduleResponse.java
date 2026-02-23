package com.promanage.scheduler.dto;

import com.promanage.scheduler.model.Project;

import java.math.BigDecimal;
import java.util.List;

public record MaxProfitScheduleResponse(List<Project> selectedProjects, BigDecimal totalProfit) {
}
