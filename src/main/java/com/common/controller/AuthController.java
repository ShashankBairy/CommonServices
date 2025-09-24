package com.common.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.common.dto.JwtResponse;
import com.common.dto.LoginRequest;
import com.common.service.PermissionDataService;
import com.common.service.TokenAuthorization;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/common/auth")
@CrossOrigin("*")
@RequiredArgsConstructor
public class AuthController {

	private final TokenAuthorization authService;
	private final PermissionDataService permissionDataService;

	@PostMapping("/login")
	public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest loginRequest) {
		JwtResponse response = authService.loginAndGenerateLightweightJwt(loginRequest.getEmpid(),
				loginRequest.getPassword());
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/token")
	public ResponseEntity<Map<String, Object>> getUserPermissions(@RequestHeader("Authorization") String authHeader) {

		// The controller's only job is to extract the token from the header
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			// Handle missing or malformed header
			return ResponseEntity.status(401).build();
		}

		String jwt = authHeader.substring(7); // Remove "Bearer " prefix

		// Pass the token directly to the service
		Map<String, Object> permissions = permissionDataService.getPermissions(jwt);

		return ResponseEntity.ok(permissions);
	}
}

