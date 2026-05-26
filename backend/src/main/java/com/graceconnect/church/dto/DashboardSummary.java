package com.graceconnect.church.dto;

import com.graceconnect.church.model.ActivityLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummary {
    private long totalFamilies;
    private long totalMembers;
    private long upcomingBirthdaysCount;
    private long upcomingEventsCount;
    private double treasuryBalance;
    
    // Financial overview
    private double totalIncome;
    private double totalExpense;
    private double monthlyDonations;
    private double monthlyDonationsTrend;
    
    // Chart details (last 6 months)
    private List<String> chartMonths;
    private List<Double> incomeChartData;
    private List<Double> expenseChartData;

    // Recent audits
    private List<ActivityLog> recentActivities;

    // Fund breakdown balances
    private java.util.Map<String, Double> fundBalances;
}
