package com.promanage.scheduler.service;

import com.promanage.scheduler.model.Project;
import com.promanage.scheduler.model.ProjectStatus;
import com.promanage.scheduler.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    @Value("${predictive.revenue.threshold:15000}")
    private double revenueThreshold;

    @Autowired
    private ProjectRepository projectRepository;

    // 1. Add a Project
    public Project addProject(Project project) {
        project.setStatus(ProjectStatus.PENDING);
        return projectRepository.save(project);
    }

    // 2. Get All Projects
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Maximum-profit schedule using greedy job sequencing with deadlines.
    public List<Project> getMaxProfitSchedule() {
        List<Project> allProjects = projectRepository.findAll();

        List<Project> validProjects = allProjects.stream()
                .filter(project -> project.getDeadline() != null && project.getDeadline() > 0)
                .filter(project -> project.getRevenue() != null)
                .sorted(Comparator.comparing(Project::getRevenue).reversed())
                .toList();

        int maxDeadline = validProjects.stream()
                .map(Project::getDeadline)
                .max(Integer::compareTo)
                .orElse(0);

        if (maxDeadline == 0) {
            return Collections.emptyList();
        }

        Project[] slots = new Project[maxDeadline + 1];

        for (Project project : validProjects) {
            int lastPossibleSlot = Math.min(project.getDeadline(), maxDeadline);

            for (int slot = lastPossibleSlot; slot >= 1; slot--) {
                if (slots[slot] == null) {
                    slots[slot] = project;
                    break;
                }
            }
        }

        List<Project> selectedProjects = new ArrayList<>();
        for (int slot = 1; slot <= maxDeadline; slot++) {
            if (slots[slot] != null) {
                selectedProjects.add(slots[slot]);
            }
        }

        return selectedProjects;
    }

    public BigDecimal getTotalProfit(List<Project> selectedProjects) {
        if (selectedProjects == null || selectedProjects.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return selectedProjects.stream()
                .map(Project::getRevenue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // 3. Predictive Trend Detection
    public boolean isHighRevenueTrend(List<Project> projects) {

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        double averageRevenue = projects.stream()
                .filter(project -> project.getCreatedDate() != null)
                .filter(project -> !project.getCreatedDate().isBefore(thirtyDaysAgo))
                .filter(project -> project.getRevenue() != null)
                .mapToDouble(project -> project.getRevenue().doubleValue())
                .average()
                .orElse(0.0);

        log.info("Average revenue for projects created in the last 30 days: {}", averageRevenue);
        log.info("Revenue threshold configured: {}", revenueThreshold);

        return averageRevenue > revenueThreshold;
    }

    // 4. Greedy + Predictive Hybrid Scheduling
    @Transactional
    public List<Project> generateOptimalSchedule() {

        List<Project> allProjects = projectRepository.findAll();
        List<Project> validProjects = new ArrayList<>();

        // A. Data validation + Reset statuses
        for (Project p : allProjects) {

            p.setStatus(ProjectStatus.PENDING);

            if (p.getRevenue() == null || p.getDeadline() == null || p.getDeadline() <= 0) {
                p.setStatus(ProjectStatus.REJECTED);
            } else {
                validProjects.add(p);
            }
        }

        // B. Sort by revenue descending (Greedy step)
        validProjects.sort((p1, p2) -> p2.getRevenue().compareTo(p1.getRevenue()));

        // C. Predictive logic
        boolean highRevenueTrend = isHighRevenueTrend(allProjects);
        int maxSlots = highRevenueTrend ? 4 : 5;

        if (highRevenueTrend) {
            log.info("High revenue trend detected. Reserving 1 slot for predicted future project.");
        } else {
            log.info("Normal revenue trend. Scheduling with full 5 slots.");
        }

        Project[] weekSchedule = new Project[maxSlots];
        List<Project> scheduledList = new ArrayList<>();

        // D. Greedy assignment (Latest possible slot before deadline)
        for (Project p : validProjects) {

            int maxDayIndex = Math.min(p.getDeadline(), maxSlots) - 1;

            for (int day = maxDayIndex; day >= 0; day--) {

                if (weekSchedule[day] == null) {
                    weekSchedule[day] = p;
                    p.setStatus(ProjectStatus.SCHEDULED);
                    scheduledList.add(p);
                    break;
                }
            }
        }

        // E. Mark remaining as REJECTED
        for (Project p : validProjects) {
            if (p.getStatus() == ProjectStatus.PENDING) {
                p.setStatus(ProjectStatus.REJECTED);
            }
        }

        projectRepository.saveAll(allProjects);

        log.info("Total scheduled projects this week: {}", scheduledList.size());

        return scheduledList;
    }
}
