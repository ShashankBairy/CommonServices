package com.common.entity;
 
import java.util.List;
 
import org.hibernate.annotations.Immutable;
 
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
 
@Entity
@Data
@Immutable
@Table(name = "sce_user_admin")
public class EmployeeView {
 
    @EmbeddedId
    private EmployeeViewId id;
 
    @Column(name = "first_name")
    private String firstName;
 
    @Column(name = "last_name")
    private String lastName;
 
    @Column(name = "password")
    private String password;
 
    @Column(name = "email")
    private String email;
 
    @Column(name = "designation_name")
    private String designationName;
 
    @Column(name = "role_name")
    private String roleName;
 
    // REMOVED screenName and permissionName fields
 
    // NEW field to map the 'screen_permission' JSON column
    // CHANGE THIS BACK TO STRING
    @Column(name = "screen_permission", columnDefinition = "json")
    private String screenPermission;
}