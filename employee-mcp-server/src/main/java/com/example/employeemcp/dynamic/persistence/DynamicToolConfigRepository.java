package com.example.employeemcp.dynamic.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DynamicToolConfigRepository extends JpaRepository<DynamicToolConfigEntity, String> {
}
