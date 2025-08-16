package com.company.analyzer;

import com.company.analyzer.service.EmployeeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class CompanyAnalyzerApplication implements CommandLineRunner {

	private final EmployeeService employeeService;

	public CompanyAnalyzerApplication(EmployeeService employeeService) {
		this.employeeService = employeeService;
	}

	public static void main(String[] args) {
		SpringApplication.run(CompanyAnalyzerApplication.class, args);
	}

	@Override
	public void run(String... args) {
		try {
			System.out.println("Starting analysis...\n");
			Map<String, Object> results = employeeService.analyzeEmployeeStructure();
			printResults(results);
		} catch (IOException e) {
			System.err.println("Error reading employee data: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Error during analysis: " + e.getMessage());
		}
	}

	private void printResults(Map<String, Object> results) {
		printSalaryIssues(results);
		printReportingLineIssues(results);
	}

	private void printSalaryIssues(Map<String, Object> results) {
		List<Map<String, Object>> salaryIssues = (List<Map<String, Object>>) results.get("salaryIssues");
		System.out.println("1. Manager Salary Analysis");

		if (salaryIssues.isEmpty()) {
			System.out.println("All managers comply with salary requirements (20-50% above average subordinates)");
		} else {
			System.out.println("The following managers have salary issues:");
			salaryIssues.forEach(issue -> {
				System.out.printf("- %s (ID: %s) is %s by %,.2f%n",
						issue.get("employeeFirstName"),
						issue.get("employeeId"),
						issue.get("issueType"),
						issue.get("difference"));
			});
		}
	}

	private void printReportingLineIssues(Map<String, Object> results) {
		List<Map<String, Object>> longReportingLines = (List<Map<String, Object>>) results.get("longReportingLines");

		System.out.println("2. Reporting Line Analysis");

		if (longReportingLines.isEmpty()) {
			System.out.println("All employees have acceptable reporting line lengths (≤4 managers to CEO)");
		} else {
			System.out.println("The following employees have too long reporting lines:");
			longReportingLines.forEach(issue -> {
				System.out.printf("- Employee ID: %s has %d more managers than allowed (total managers: %d)%n",
						issue.get("employeeId"),
						issue.get("excessManagers"),
						(Integer) issue.get("excessManagers") + 4);
			});
		}
		System.out.println("\nAnalysis complete.");
	}
}