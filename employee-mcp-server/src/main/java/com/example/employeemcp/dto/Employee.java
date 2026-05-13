package com.example.employeemcp.dto;

public class Employee {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private String role;
    private double salary;

    public Employee() {}

    public Employee(Long id, String firstName, String lastName, String email,
                    String department, String role, double salary) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.role = role;
        this.salary = salary;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    @Override
    public String toString() {
        return "Employee{id=" + id + ", firstName='" + firstName + "', lastName='" + lastName
                + "', email='" + email + "', department='" + department
                + "', role='" + role + "', salary=" + salary + "}";
    }
}
