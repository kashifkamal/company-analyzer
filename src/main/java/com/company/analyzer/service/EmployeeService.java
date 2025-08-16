package com.company.analyzer.service;

import com.company.analyzer.model.Employee;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class EmployeeService {

  private final EmployeeLoaderService loaderService;

  public EmployeeService(EmployeeLoaderService loaderService ) {
    this.loaderService = loaderService;
  }

  public Map<String, Object> analyzeEmployeeStructure() throws IOException {
    List<Employee> employees = loaderService.loadEmployeesFromResource();

    // Build manager-subordinate relationships
    Map<String, List<Employee>> subordinatesMap = employees.stream()
        .filter(e -> e.getManagerId() != null)
        .collect(Collectors.groupingBy(Employee::getManagerId));

    // Calculate reporting line lengths
    Map<String, Integer> reportingLineLengths = calculateReportingLineLengths(employees);

    // Analyze manager salaries
    List<Map<String, Object>> salaryIssues = analyzeManagerSalaries(employees, subordinatesMap);

    // Analyze reporting lines
    List<Map<String, Object>> longReportingLines = analyzeReportingLines(reportingLineLengths);

    Map<String, Object> result = new HashMap<>();
    result.put("salaryIssues", salaryIssues);
    result.put("longReportingLines", longReportingLines);

    return result;
  }

  private Map<String, Integer> calculateReportingLineLengths(List<Employee> employees) {
    // Build manager mapping, skipping null managerIds
    Map<String, String> managerMap = employees.stream()
        .filter(e -> e.getManagerId() != null)
        .collect(Collectors.toMap(
            Employee::getId,
            Employee::getManagerId,
            (existing, replacement) -> existing));

    Map<String, Integer> reportingLineLengths = new HashMap<>();

    for (Employee employee : employees) {
      int length = 0;
      String currentManagerId = employee.getManagerId();

      // Follow the management chain up to the CEO
      while (currentManagerId != null && managerMap.containsKey(currentManagerId)) {
        length++;
        currentManagerId = managerMap.get(currentManagerId);

        // Prevent infinite loops in case of circular references
        if (length > employees.size()) {
          throw new IllegalStateException("Circular reference detected in management chain");
        }
      }

      reportingLineLengths.put(employee.getId(), length);
    }

    return reportingLineLengths;
  }

  private List<Map<String, Object>> analyzeManagerSalaries(List<Employee> employees,
                                                           Map<String, List<Employee>> subordinatesMap) {
    List<Map<String, Object>> issues = new ArrayList<>();

    for (Employee manager : employees) {
      List<Employee> subordinates = subordinatesMap.get(manager.getId());

      // Skip non-managers (employees with no subordinates)
      if (subordinates == null || subordinates.isEmpty()) {
        continue;
      }

      double avgSubordinateSalary = subordinates.stream()
          .mapToInt(Employee::getSalary)
          .average()
          .orElse(0);

      double minExpectedSalary = avgSubordinateSalary * 1.2; //(20% )
      double maxExpectedSalary = avgSubordinateSalary * 1.5; //(50% )

      if (manager.getSalary() < minExpectedSalary) {
        // If the manager's salary is below the minimum expected, create an issue
        System.out.println("Manager " + manager.getFirstName() + " is underpaid: ");
        double difference = minExpectedSalary - manager.getSalary();
        issues.add(createSalaryIssueMap(manager, "UNDERPAID", difference));
      } else if (manager.getSalary() > maxExpectedSalary) {
        // If the manager's salary is above the maximum expected, create an issue
        System.out.println("Manager " + manager.getFirstName() + " is overpaid: ");
        double difference = manager.getSalary() - maxExpectedSalary;
        issues.add(createSalaryIssueMap(manager, "OVERPAID", difference));
      }
    }

    return issues;
  }

  private Map<String, Object> createSalaryIssueMap(Employee manager, String issueType, double difference) {
    Map<String, Object> issue = new HashMap<>();
    issue.put("employeeId", manager.getId());
    issue.put("employeeFirstName", manager.getFirstName());
    issue.put("employeeLastName", manager.getLastName());
    issue.put("issueType", issueType);
    issue.put("difference", Math.round(difference * 100) / 100.0);
    return issue;
  }

  private List<Map<String, Object>> analyzeReportingLines(Map<String, Integer> reportingLineLengths) {
    return reportingLineLengths.entrySet().stream()
        .filter(entry -> entry.getValue() > 4)
        .map(entry -> {
          Map<String, Object> issue = new HashMap<>();
          issue.put("employeeId", entry.getKey());
          issue.put("excessManagers", entry.getValue() - 4);
          return issue;
        })
        .collect(Collectors.toList());
  }
}
