package com.example.employeemcp.client;

import com.example.employeemcp.dto.Employee;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class EmployeeServiceClient {

    private final RestClient restClient;

    public EmployeeServiceClient(
            @Value("${employee.service.base-url:http://localhost:8081}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<Employee> getAllEmployees() {
        return restClient.get()
                .uri("/api/employees")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Employee getEmployeeById(Long id) {
        return restClient.get()
                .uri("/api/employees/{id}", id)
                .retrieve()
                .body(Employee.class);
    }

    public List<Employee> getEmployeesByDepartment(String department) {
        return restClient.get()
                .uri("/api/employees/department/{department}", department)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public List<Employee> getEmployeesByRole(String role) {
        return restClient.get()
                .uri("/api/employees/role/{role}", role)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Employee createEmployee(Employee employee) {
        return restClient.post()
                .uri("/api/employees")
                .body(employee)
                .retrieve()
                .body(Employee.class);
    }

    public Employee updateEmployee(Long id, Employee employee) {
        return restClient.put()
                .uri("/api/employees/{id}", id)
                .body(employee)
                .retrieve()
                .body(Employee.class);
    }

    public void deleteEmployee(Long id) {
        restClient.delete()
                .uri("/api/employees/{id}", id)
                .retrieve()
                .toBodilessEntity();
    }
}
