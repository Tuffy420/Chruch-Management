package com.graceconnect.church.service;

import com.graceconnect.church.model.Member;
import com.graceconnect.church.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CelebrationSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(CelebrationSchedulerService.class);

    private final MemberRepository memberRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public CelebrationSchedulerService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // Runs every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void checkAndSendCelebrationWishes() {
        logger.info("Starting automated Daily Birthday & Marriage Anniversary wishes scheduler...");
        LocalDate today = LocalDate.now();
        List<Member> members = memberRepository.findAll();

        int birthdayWishesSent = 0;
        int anniversaryWishesSent = 0;

        for (Member m : members) {
            // 1. Check Birthday
            if (m.getDob() != null 
                && m.getDob().getMonthValue() == today.getMonthValue() 
                && m.getDob().getDayOfMonth() == today.getDayOfMonth()) {
                
                sendBirthdayWish(m);
                birthdayWishesSent++;
            }

            // 2. Check Anniversary
            if ("Married".equalsIgnoreCase(m.getMarriageStatus()) 
                && m.getMarriageDate() != null 
                && m.getMarriageDate().getMonthValue() == today.getMonthValue() 
                && m.getMarriageDate().getDayOfMonth() == today.getDayOfMonth()) {
                
                sendAnniversaryWish(m);
                anniversaryWishesSent++;
            }
        }

        logger.info("Daily celebration scheduler completed. Birthday wishes sent: {}, Anniversary wishes sent: {}", 
                birthdayWishesSent, anniversaryWishesSent);
    }

    private void sendBirthdayWish(Member m) {
        String subject = "Happy Birthday, " + m.getFirstName() + "! 🎂";
        String body = "Dear " + m.getFirstName() + " " + m.getLastName() + ",\n\n"
                    + "GraceConnect Church wishes you a highly blessed, joyful, and healthy Birthday!\n"
                    + "May this year bring abundance of peace, grace, and happiness to your life.\n\n"
                    + "Warm blessings,\n"
                    + "GraceConnect Church Administration";

        triggerEmail(m.getEmail(), m.getFirstName() + " " + m.getLastName(), subject, body, "Birthday");
    }

    private void sendAnniversaryWish(Member m) {
        String subject = "Happy Marriage Anniversary! 💖";
        int years = LocalDate.now().getYear() - m.getMarriageDate().getYear();
        String yearSuffix = getOrdinalSuffix(years);
        String body = "Dear " + m.getFirstName() + " " + m.getLastName() + ",\n\n"
                    + "GraceConnect Church wishes you and your household a blessed and happy " 
                    + years + yearSuffix + " Marriage Anniversary!\n"
                    + "May God continue to bless your union with love, strong bonds, and joy.\n\n"
                    + "Warm blessings,\n"
                    + "GraceConnect Church Administration";

        triggerEmail(m.getEmail(), m.getFirstName() + " " + m.getLastName(), subject, body, "Marriage Anniversary");
    }

    private void triggerEmail(String toEmail, String fullName, String subject, String body, String type) {
        // Beautiful console logging for visual confirmation in server logs
        System.out.println("==================================================================");
        System.out.println("[AUTOMATED " + type.toUpperCase() + " WISH SENT]");
        System.out.println("To: " + fullName + " (" + toEmail + ")");
        System.out.println("Subject: " + subject);
        System.out.println("Message:\n" + body);
        System.out.println("==================================================================");

        if (mailSender != null && toEmail != null && !toEmail.trim().isEmpty()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(body);
                message.setFrom("no-reply@graceconnect.org");
                mailSender.send(message);
                logger.info("Successfully dispatched real SMTP email to {}", toEmail);
            } catch (Exception e) {
                logger.error("Failed to dispatch real SMTP email to {}: {}", toEmail, e.getMessage());
            }
        }
    }

    private String getOrdinalSuffix(int value) {
        if (value >= 11 && value <= 13) {
            return "th";
        }
        switch (value % 10) {
            case 1:  return "st";
            case 2:  return "nd";
            case 3:  return "rd";
            default: return "th";
        }
    }
}
