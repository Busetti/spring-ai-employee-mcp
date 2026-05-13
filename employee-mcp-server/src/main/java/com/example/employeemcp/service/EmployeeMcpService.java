package com.example.employeemcp.service;

import com.example.employeemcp.client.EmployeeServiceClient;
import com.example.employeemcp.dto.Employee;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeMcpService {

    private final EmployeeServiceClient client;

    public EmployeeMcpService(EmployeeServiceClient client) {
        this.client = client;
    }

    @McpTool(name = "get-all-employees", description = "Retrieve a list of all employees")
    public List<Employee> getAllEmployees() {
        return client.getAllEmployees();
    }

    @McpTool(name = "get-employee-by-id", description = "Find a single employee by their unique numeric ID")
    public Employee getEmployeeById(
            @McpToolParam(description = "The unique numeric ID of the employee") Long id) {
        return client.getEmployeeById(id);
    }

    @McpTool(name = "get-employees-by-department",
             description = "Find all employees in a specific department (e.g. Engineering, HR, Finance, Marketing)")
    public List<Employee> getEmployeesByDepartment(
            @McpToolParam(description = "Department name to filter by") String department) {
        return client.getEmployeesByDepartment(department);
    }

    @McpTool(name = "get-employees-by-role",
             description = "Find all employees with a specific job role (e.g. Software Engineer, HR Manager)")
    public List<Employee> getEmployeesByRole(
            @McpToolParam(description = "Job role or title to filter by") String role) {
        return client.getEmployeesByRole(role);
    }

    @McpTool(name = "create-employee", description = "Create a new employee record")
    public Employee createEmployee(
            @McpToolParam(description = "First name of the employee") String firstName,
            @McpToolParam(description = "Last name of the employee") String lastName,
            @McpToolParam(description = "Email address of the employee") String email,
            @McpToolParam(description = "Department the employee belongs to") String department,
            @McpToolParam(description = "Job role or title of the employee") String role,
            @McpToolParam(description = "Annual salary of the employee") double salary) {
        Employee emp = new Employee();
        emp.setFirstName(firstName);
        emp.setLastName(lastName);
        emp.setEmail(email);
        emp.setDepartment(department);
        emp.setRole(role);
        emp.setSalary(salary);
        return client.createEmployee(emp);
    }

    @McpTool(name = "update-employee", description = "Update an existing employee record by ID")
    public Employee updateEmployee(
            @McpToolParam(description = "The unique numeric ID of the employee to update") Long id,
            @McpToolParam(description = "Updated first name") String firstName,
            @McpToolParam(description = "Updated last name") String lastName,
            @McpToolParam(description = "Updated email address") String email,
            @McpToolParam(description = "Updated department") String department,
            @McpToolParam(description = "Updated job role or title") String role,
            @McpToolParam(description = "Updated annual salary") double salary) {
        Employee emp = new Employee();
        emp.setFirstName(firstName);
        emp.setLastName(lastName);
        emp.setEmail(email);
        emp.setDepartment(department);
        emp.setRole(role);
        emp.setSalary(salary);
        return client.updateEmployee(id, emp);
    }

    @McpTool(name = "delete-employee", description = "Permanently delete an employee record by ID")
    public String deleteEmployee(
            @McpToolParam(description = "The unique numeric ID of the employee to delete") Long id) {
        client.deleteEmployee(id);
        return "Employee with ID " + id + " deleted successfully.";
    }
}
