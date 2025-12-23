package com.ecommerece.project.repositories;

import com.ecommerece.project.model.AppRole;
import com.ecommerece.project.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Integer> {
    Optional<Role> findByRoleName(AppRole appRole);
}
