package com.common.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.common.dto.PermissionQueryResult;
import com.common.entity.EmployeeView;
import com.common.entity.EmployeeViewId;


public interface EmployeeViewRepository extends JpaRepository<EmployeeView, EmployeeViewId> {

    @Query("SELECT e FROM EmployeeView e WHERE e.id.empId = :empId AND e.password = :password")
    List<EmployeeView> findByCredentials(@Param("empId") int empId, @Param("password") String password);

    @Query(value = "SELECT new com.common.dto.PermissionQueryResult(" + // <-- THIS LINE IS FIXED
            "e.roleName, e.screenName, e.permissionName) " +
            "FROM EmployeeView e " +
            "WHERE e.firstName = :firstName " +
            "AND e.designationName = :designationName " +
            "AND e.roleName IN :roleNames")
    List<PermissionQueryResult> findPermissionsByUserAndRoles(@Param("firstName") String firstName,
            @Param("designationName") String designationName, @Param("roleNames") List<String> roleNames);
    
    @Query("SELECT e FROM EmployeeView e WHERE e.id.empId = :empId")
    List<EmployeeView> findAllByEmpId(@Param("empId") int empId);
}
