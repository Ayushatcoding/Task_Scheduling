package com.promanage.scheduler.controller;

import com.promanage.scheduler.model.Project;
import com.promanage.scheduler.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Tells Spring: "I speak JSON"
@RequestMapping("/api/projects") // Base URL: http://localhost:8080/api/projects
@CrossOrigin(origins = "*") // Allows your future React frontend to talk to this
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    // POST: Add a new project
    @PostMapping("/add")
    public Project addProject(@RequestBody Project project) {
        return projectService.addProject(project);
    }

    // GET: See everyone
    @GetMapping("/all")
    public List<Project> getAllProjects() {
        return projectService.getAllProjects();
    }

    // POST: Trigger the Algorithm
    @PostMapping("/schedule")
    public List<Project> generateSchedule() {
        return projectService.generateOptimalSchedule();
    }
}