package com.txn.smart.pay.hub.sph.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.txn.smart.pay.hub.sph.dto.response.TransactionAnalyticsResponse;
import com.txn.smart.pay.hub.sph.service.TransactionAnalyticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

	private final TransactionAnalyticsService analyticsService;
	
	@GetMapping("/user/{userId}/range")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<List<TransactionAnalyticsResponse>> getUserAnalyticsByRange(
	    @PathVariable String userId,
	    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
	    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
	    
	    List<TransactionAnalyticsResponse> analytics = analyticsService
	        .getUserAnalyticsByDateRange(userId, startDate, endDate);
	    
	    return ResponseEntity.ok(analytics);
	}

	@GetMapping("/user/{userId}/statistics")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
	public ResponseEntity<Map<String, Object>> getUserStatistics(@PathVariable String userId) {
	    Map<String, Object> stats = analyticsService.getUserStatistics(userId);
	    return ResponseEntity.ok(stats);
	}
}