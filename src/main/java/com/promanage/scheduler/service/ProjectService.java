package com.promanage.scheduler.service;

import com.promanage.scheduler.model.Project;
import com.promanage.scheduler.model.ProjectStatus;
import com.promanage.scheduler.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    // 1. Add a Project
    public Project addProject(Project project) {
        // Set default status
        project.setStatus(ProjectStatus.PENDING);
        return projectRepository.save(project);
    }

    // 2. Get All Projects
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // 3. THE ALGORITHM (Updated with Safety Checks)
    @Transactional
    public List<Project> generateOptimalSchedule() {
        List<Project> allProjects = projectRepository.findAll();

        // A. Filter out BAD data (Null revenue or deadlines)
        List<Project> validProjects = new ArrayList<>();

        for (Project p : allProjects) {
            // Reset status first
            p.setStatus(ProjectStatus.PENDING);

            // Safety Check: If data is missing, mark REJECTED and skip it
            if (p.getRevenue() == null || p.getDeadline() == null || p.getDeadline() <= 0) {
                p.setStatus(ProjectStatus.REJECTED);
            } else {
                validProjects.add(p);
            }
        }

        // B. Sort only the VALID projects (Highest Revenue first)
        validProjects.sort((p1, p2) -> p2.getRevenue().compareTo(p1.getRevenue()));

        // C. Schedule them
        Project[] weekSchedule = new Project[5];
        List<Project> scheduledList = new ArrayList<>();

        for (Project p : validProjects) {
            // Find slot (Math.min ensures we don't go past index 4)
            int maxDayIndex = Math.min(p.getDeadline(), 5) - 1;

            for (int day = maxDayIndex; day >= 0; day--) {
                if (weekSchedule[day] == null) {
                    weekSchedule[day] = p;
                    p.setStatus(ProjectStatus.SCHEDULED);
                    scheduledList.add(p);
                    break;
                }
            }
        }

        // D. Mark unscheduled valid projects as REJECTED
        for (Project p : validProjects) {
            if (p.getStatus() == ProjectStatus.PENDING) {
                p.setStatus(ProjectStatus.REJECTED);
            }
        }

        // Save everything back to DB
        projectRepository.saveAll(allProjects);

        return scheduledList;
    }
}