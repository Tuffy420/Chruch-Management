package com.graceconnect.church.service;

import com.graceconnect.church.dto.DashboardSummary;
import com.graceconnect.church.model.Account;
import com.graceconnect.church.model.ActivityLog;
import com.graceconnect.church.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class DashboardService {

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final AccountRepository accountRepository;
    private final ActivityLogRepository activityLogRepository;

    public DashboardService(FamilyRepository familyRepository, MemberRepository memberRepository,
                            EventRepository eventRepository, AccountRepository accountRepository,
                            ActivityLogRepository activityLogRepository) {
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.accountRepository = accountRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummary getSummary() {
        long totalFamilies = familyRepository.count();
        long totalMembers = memberRepository.count();
        
        LocalDate today = LocalDate.now();
        
        // Birthdays in current month
        long upcomingBirthdays = memberRepository.findAll().stream()
                .filter(m -> m.getDob() != null && m.getDob().getMonthValue() == today.getMonthValue())
                .count();

        // Upcoming events count
        long upcomingEvents = eventRepository.findByDateGreaterThanEqualOrderByDateAsc(today).size();

        // Sum incomes and expenses programmatically
        double income = accountRepository.findByType("INCOME").stream()
                .mapToDouble(Account::getAmount)
                .sum();
        double expense = accountRepository.findByType("EXPENSE").stream()
                .mapToDouble(Account::getAmount)
                .sum();

        // Dynamically compute Treasury Balance as Net Income minus Expense
        double currentTreasury = income - expense;

        // Calculate dynamic Monthly Donations (Income for current calendar month)
        LocalDate currentMonthStart = today.withDayOfMonth(1);
        LocalDate currentMonthEnd = today.withDayOfMonth(today.lengthOfMonth());
        double currentMonthDonations = accountRepository.findByTypeAndDateBetween("INCOME", currentMonthStart, currentMonthEnd).stream()
                .mapToDouble(Account::getAmount)
                .sum();

        // Calculate Monthly Donations Trend (comparing to previous month)
        LocalDate prevMonth = today.minusMonths(1);
        LocalDate prevMonthStart = prevMonth.withDayOfMonth(1);
        LocalDate prevMonthEnd = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth());
        double prevMonthDonations = accountRepository.findByTypeAndDateBetween("INCOME", prevMonthStart, prevMonthEnd).stream()
                .mapToDouble(Account::getAmount)
                .sum();

        double trend = 0.0;
        if (prevMonthDonations > 0) {
            trend = ((currentMonthDonations - prevMonthDonations) / prevMonthDonations) * 100;
        } else if (currentMonthDonations > 0) {
            trend = 100.0;
        }

        // Recents activity audits (excluding logins)
        List<ActivityLog> recentActivities = activityLogRepository.findRecentActivitiesExcludingLogins(PageRequest.of(0, 10));

        // Generate dynamic last 6 months telemetry for charts
        List<String> months = new ArrayList<>();
        List<Double> incomeChart = new ArrayList<>();
        List<Double> expenseChart = new ArrayList<>();

        LocalDate startMonth = today.minusMonths(5);
        for (int i = 0; i < 6; i++) {
            LocalDate mDate = startMonth.plusMonths(i);
            String label = mDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            months.add(label);

            // Fetch dynamic totals from DB for this specific month
            LocalDate start = mDate.withDayOfMonth(1);
            LocalDate end = mDate.withDayOfMonth(mDate.lengthOfMonth());
            
            double monthIncome = accountRepository.findByTypeAndDateBetween("INCOME", start, end).stream()
                    .mapToDouble(Account::getAmount)
                    .sum();
            double monthExpense = accountRepository.findByTypeAndDateBetween("EXPENSE", start, end).stream()
                    .mapToDouble(Account::getAmount)
                    .sum();

            incomeChart.add(monthIncome);
            expenseChart.add(monthExpense);
        }

        // Calculate dynamic fund breakdown balances from database
        Map<String, Double> fundBalances = new HashMap<>();
        List<String> allowedFunds = Arrays.asList(
            "Sunday Worship Offering",
            "General Fund",
            "Missions & Outreach",
            "Building Maintenance",
            "Charity & Welfare"
        );
        for (String fund : allowedFunds) {
            double fundIncome = accountRepository.findAll().stream()
                    .filter(a -> "INCOME".equalsIgnoreCase(a.getType()) && fund.equalsIgnoreCase(a.getFund()))
                    .mapToDouble(Account::getAmount)
                    .sum();
            double fundExpense = accountRepository.findAll().stream()
                    .filter(a -> "EXPENSE".equalsIgnoreCase(a.getType()) && fund.equalsIgnoreCase(a.getFund()))
                    .mapToDouble(Account::getAmount)
                    .sum();
            fundBalances.put(fund, fundIncome - fundExpense);
        }

        return DashboardSummary.builder()
                .totalFamilies(totalFamilies)
                .totalMembers(totalMembers)
                .upcomingBirthdaysCount(upcomingBirthdays)
                .upcomingEventsCount(upcomingEvents)
                .treasuryBalance(currentTreasury)
                .totalIncome(income)
                .totalExpense(expense)
                .monthlyDonations(currentMonthDonations)
                .monthlyDonationsTrend(trend)
                .chartMonths(months)
                .incomeChartData(incomeChart)
                .expenseChartData(expenseChart)
                .recentActivities(recentActivities)
                .fundBalances(fundBalances)
                .build();
    }
}
