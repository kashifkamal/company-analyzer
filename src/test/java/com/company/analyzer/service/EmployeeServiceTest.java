package com.company.analyzer.service;

import com.company.analyzer.model.Employee;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class EmployeeServiceTest {

  @Test
  void testUnderpaidManagerDetection() throws IOException {
    // Setup
    List<Employee> employees = Arrays.asList(
        new Employee("1", "CEO", "Company", 250000, null),
        new Employee("2", "Manager", "Team", 120000, "1"), // Should be >= 144000 (120k avg * 1.2)
        new Employee("3", "Employee", "A", 100000, "2"),
        new Employee("4", "Employee", "B", 140000, "2") // Avg = 120k
    );

    EmployeeService service = new EmployeeService(new TestEmployeeLoader(employees));

    // Execute
    Map<String, Object> results = service.analyzeEmployeeStructure();
    List<Map<String, Object>> issues = (List<Map<String, Object>>) results.get("salaryIssues");

    // Verify
    assertEquals(2, issues.size());
    assertEquals("OVERPAID", issues.get(0).get("issueType"));
    assertEquals(70000.0, (Double) issues.get(0).get("difference"), 0.01);
  }

  @Test
  void testOverpaidManagerDetection() throws IOException {
    // Setup
    List<Employee> employees = Arrays.asList(
        new Employee("1", "CEO", "Company", 250000, null),
        new Employee("2", "Manager", "Team", 200000, "1"), // Should be <= 150k (100k avg * 1.5)
        new Employee("3", "Employee", "A", 90000, "2"),
        new Employee("4", "Employee", "B", 110000, "2") // Avg = 100k
    );

    EmployeeService service = new EmployeeService(new TestEmployeeLoader(employees));

    // Execute
    Map<String, Object> results = service.analyzeEmployeeStructure();
    List<Map<String, Object>> issues = (List<Map<String, Object>>) results.get("salaryIssues");

    // Verify
    assertEquals(1, issues.size());
    assertEquals("OVERPAID", issues.get(0).get("issueType"));
    assertEquals(50000.0, (Double) issues.get(0).get("difference"), 0.01);
  }

  @Test
  void testReportingLineValidation() throws IOException {
    // Setup - 6 level hierarchy (max allowed is 5)
    List<Employee> employees = Arrays.asList(
        new Employee("1", "CEO", "Company", 250000, null),
        new Employee("2", "VP", "Eng", 200000, "1"),
        new Employee("3", "Director", "Dev", 180000, "2"),
        new Employee("4", "Manager", "Team", 150000, "3"),
        new Employee("5", "Lead", "Dev", 120000, "4"),
        new Employee("6", "Senior", "Dev", 100000, "5"), // 5 levels (OK)
        new Employee("7", "Junior", "Dev", 80000, "6")  // 6 levels (violation)
    );

    EmployeeService service = new EmployeeService(new TestEmployeeLoader(employees));

    // Execute
    Map<String, Object> results = service.analyzeEmployeeStructure();
    List<Map<String, Object>> longLines = (List<Map<String, Object>>) results.get("longReportingLines");

    // Verify
    assertEquals(1, longLines.size());
    assertEquals("7", longLines.get(0).get("employeeId"));
    assertEquals(1, longLines.get(0).get("excessManagers"));
  }

  @Test
  void testNonManagersAreSkipped() throws IOException {
    // Setup - Employee with no subordinates
    List<Employee> employees = Arrays.asList(
        new Employee("1", "CEO", "Company", 250000, null),
        new Employee("2", "Individual", "Contributor", 100000, "1")
    );

    EmployeeService service = new EmployeeService(new TestEmployeeLoader(employees));

    // Execute
    Map<String, Object> results = service.analyzeEmployeeStructure();
    List<Map<String, Object>> issues = (List<Map<String, Object>>) results.get("salaryIssues");

    assertFalse(issues.isEmpty());
  }

  // Test double for EmployeeRepository
  private static class TestEmployeeLoader extends EmployeeLoaderService {
    private final List<Employee> employees;

    public TestEmployeeLoader(List<Employee> employees) {
      this.employees = employees;
    }

    @Override
    public List<Employee> loadEmployeesFromResource() {
      return employees;
    }
  }
}