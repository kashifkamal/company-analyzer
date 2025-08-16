package com.company.analyzer.service;

import com.company.analyzer.model.Employee;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EmployeeLoaderService {

  public List<Employee> loadEmployeesFromResource() throws IOException {
    ClassPathResource resource = new ClassPathResource("employees.csv");
    try (Stream<String> stream = Files.lines(resource.getFile().toPath())) {
      return stream
          .skip(1) // Skip header
          .map(this::parseEmployee)
          .collect(Collectors.toList());
    }
  }

  private Employee parseEmployee(String line) {
    String[] parts = line.split(",");
    String managerId = parts.length > 4 ? parts[4].trim() : null;
    managerId = (managerId != null && !managerId.isEmpty()) ? managerId : null;

    return new Employee(
        parts[0].trim(),
        parts[1].trim(),
        parts[2].trim(),
        Integer.parseInt(parts[3].trim()),
        managerId
    );
  }

}
