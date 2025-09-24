package com.common.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PermissionQueryResult {
    private String roleName;
    private String screenName;
    private String permissionName;
}
