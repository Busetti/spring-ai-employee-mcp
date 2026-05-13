package com.example.employeemcp.extensions;

import com.example.employeemcp.client.EmployeeServiceClient;
import com.example.employeemcp.dto.Employee;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EmployeeMcpExtensions {

    private final EmployeeServiceClient client;

    public EmployeeMcpExtensions(EmployeeServiceClient client) {
        this.client = client;
    }

    @McpResource(uri = "employees://all", name = "All Employees",
                 description = "Read-only list of all employees as plain text")
    public String getAllEmployeesResource() {
        List<Employee> employees = client.getAllEmployees();
        return employees.stream()
                .map(e -> String.format("[%d] %s %s | %s | %s | $%.0f",
                        e.getId(), e.getFirstName(), e.getLastName(),
                        e.getDepartment(), e.getRole(), e.getSalary()))
                .collect(Collectors.joining("\n"));
    }

    @McpResource(uri = "employees://{id}", name = "Employee Profile",
                 description = "Read-only profile of a single employee by ID")
    public String getEmployeeResource(String id) {
        Employee e = client.getEmployeeById(Long.parseLong(id));
        return String.format("ID: %d\nName: %s %s\nEmail: %s\nDepartment: %s\nRole: %s\nSalary: $%.0f",
                e.getId(), e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getDepartment(), e.getRole(), e.getSalary());
    }

    @McpPrompt(name = "employee-summary",
               description = "Generate a prompt to summarise and analyse employee data for a department")
    public String employeeSummaryPrompt(
            @McpArg(name = "department", description = "Department name to analyse") String department) {
        return """
                Analyse the employees in the '%s' department.
                Use the get-employees-by-department tool to retrieve the list.
                Summarise: headcount, average salary, unique roles present,
                and any observations about team composition.
                """.formatted(department);
    }

    @McpPrompt(name = "employee-search-prompt",
               description = "Generate a prompt to find and describe an employee by ID")
    public String employeeSearchPrompt(
            @McpArg(name = "employeeId", description = "Numeric employee ID to look up") Long employeeId) {
        return """
                Look up employee with ID %d using the get-employee-by-id tool.
                Provide a brief professional summary including their name,
                department, role, and salary range context.
                """.formatted(employeeId);
    }
}
