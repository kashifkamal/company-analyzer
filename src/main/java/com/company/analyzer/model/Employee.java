package com.company.analyzer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
  private String id;
  private String firstName;
  private String lastName;
  private Integer salary;
  private String managerId;
}

