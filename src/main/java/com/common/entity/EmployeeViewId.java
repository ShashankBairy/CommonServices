package com.common.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
public class EmployeeViewId implements Serializable {

    
	private static final long serialVersionUID = 1L;
	private int empId;
    private int userRoleId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeeViewId that = (EmployeeViewId) o;
        return empId == that.empId && userRoleId == that.userRoleId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(empId, userRoleId);
    }
}
