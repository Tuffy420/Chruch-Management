package com.graceconnect.church.service;

import com.graceconnect.church.dto.CelebrationItemDto;
import com.graceconnect.church.dto.EmailSendResultDto;
import com.graceconnect.church.dto.EmailTestRequest;
import com.graceconnect.church.model.ActivityLog;
import com.graceconnect.church.model.EmailLog;
import com.graceconnect.church.model.EmailSetting;
import com.graceconnect.church.model.Member;
import com.graceconnect.church.repository.ActivityLogRepository;
import com.graceconnect.church.repository.EmailLogRepository;
import com.graceconnect.church.repository.EmailSettingRepository;
import com.graceconnect.church.repository.MemberRepository;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final MemberRepository memberRepository;
    private final EmailLogRepository emailLogRepository;
    private final EmailSettingRepository emailSettingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final JavaMailSender defaultMailSender;

    private record ScriptureDevotional(String reference, String verse, String reflection) {}

    private static final List<ScriptureDevotional> BIRTHDAY_DEVOTIONALS = List.of(
            new ScriptureDevotional(
                    "Jeremiah 29:11",
                    "For I know the plans I have for you, declares the Lord, plans to prosper you and not to harm you, plans to give you hope and a future.",
                    "God has designed every chapter of your life with divine precision. Walk into this new year with great confidence, knowing His purpose for your peace, longevity, and spiritual fruitfulness will unfailingly prevail."
            ),
            new ScriptureDevotional(
                    "Psalm 20:4",
                    "May He give you the desire of your heart and make all your plans succeed.",
                    "When your desires are rooted in God's love, He delights in turning righteous prayers into testimonies. May this coming year be crowned with answered prayers and divine favor."
            ),
            new ScriptureDevotional(
                    "Numbers 6:24-26",
                    "The Lord bless you and keep you; the Lord make His face shine on you and be gracious to you; the Lord turn His face toward you and give you peace.",
                    "God's countenance is your eternal shield. Through every season, His unmerited favor surrounds you and His heavenly peace guards your mind and heart."
            ),
            new ScriptureDevotional(
                    "Psalm 139:14",
                    "I praise you because I am fearfully and wonderfully made; your works are wonderful, I know that full well.",
                    "You were crafted with divine artistry and endowed with unique spiritual gifts. Never doubt how cherished and deeply valued you are in the Father's heart."
            ),
            new ScriptureDevotional(
                    "Isaiah 40:31",
                    "Those who hope in the Lord will renew their strength. They will soar on wings like eagles; they will run and not grow weary, they will walk and not be faint.",
                    "When human energy fades, God's limitless stamina sustains you. May this upcoming year bring spiritual renewal, soaring vision, and supernatural endurance."
            ),
            new ScriptureDevotional(
                    "Zephaniah 3:17",
                    "The Lord your God is with you, the Mighty Warrior who saves. He will take great delight in you; in his love he will rejoice over you with singing.",
                    "The Lord rejoices over your life with gladness! Step into this new birthday year in the freedom and assurance of being fiercely loved and protected by God."
            ),
            new ScriptureDevotional(
                    "Philippians 1:6",
                    "Being confident of this, that he who began a good work in you will carry it on to completion until the day of Christ Jesus.",
                    "The Almighty never leaves His masterpiece incomplete. The grace that has guided you thus far will faithfully bring every righteous calling to fulfillment."
            )
    );

    private static final List<ScriptureDevotional> ANNIVERSARY_DEVOTIONALS = List.of(
            new ScriptureDevotional(
                    "Ecclesiastes 4:12",
                    "Though one may be overpowered, two can defend themselves. A cord of three strands is not quickly broken.",
                    "With Christ as the third cord woven through your marriage, no pressure or earthly season can unravel your bond. Continue holding fast to mutual prayer, honor, and selfless grace."
            ),
            new ScriptureDevotional(
                    "1 Corinthians 13:7-8",
                    "Love always protects, always trusts, always hopes, always perseveres. Love never fails.",
                    "Christian marriage is a living portrait of Christ's unconditional commitment to His Church. May your home remain a haven of joyful peace, patient forgiveness, and enduring honor."
            ),
            new ScriptureDevotional(
                    "Colossians 3:14",
                    "And over all these virtues put on love, which binds them all together in perfect unity.",
                    "Unity in covenant is a shining beacon to your community and children. May your partnership continue flourishing with understanding, joy, and deep affection."
            ),
            new ScriptureDevotional(
                    "Proverbs 3:3-4",
                    "Let love and faithfulness never leave you; bind them around your neck, write them on the tablet of your heart. Then you will win favor and a good name in the sight of God and man.",
                    "Integrity, kindness, and devotion are the greatest treasures a family can cultivate. May your generational heritage be filled with divine abundance."
            )
    );

    public EmailService(MemberRepository memberRepository,
                        EmailLogRepository emailLogRepository,
                        EmailSettingRepository emailSettingRepository,
                        ActivityLogRepository activityLogRepository,
                        JavaMailSender defaultMailSender) {
        this.memberRepository = memberRepository;
        this.emailLogRepository = emailLogRepository;
        this.emailSettingRepository = emailSettingRepository;
        this.activityLogRepository = activityLogRepository;
        this.defaultMailSender = defaultMailSender;
    }

    public EmailSetting getSettings() {
        return emailSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> emailSettingRepository.save(
                        EmailSetting.builder()
                                .smtpHost("smtp.gmail.com")
                                .smtpPort(587)
                                .fromEmail("blessings@graceconnect.org")
                                .fromName("GraceConnect Church")
                                .pastorSignature("Pastor & GraceConnect Church Family")
                                .autoSendDaily(true)
                                .build()
                ));
    }

    @Transactional
    public EmailSetting saveSettings(EmailSetting updated) {
        EmailSetting current = getSettings();
        if (updated.getSmtpHost() != null) current.setSmtpHost(updated.getSmtpHost().trim());
        if (updated.getSmtpPort() != null) current.setSmtpPort(updated.getSmtpPort());
        if (updated.getSmtpUsername() != null) current.setSmtpUsername(updated.getSmtpUsername().trim());
        if (updated.getSmtpPassword() != null) current.setSmtpPassword(updated.getSmtpPassword().trim());
        if (updated.getFromEmail() != null) current.setFromEmail(updated.getFromEmail().trim());
        if (updated.getFromName() != null) current.setFromName(updated.getFromName().trim());
        if (updated.getPastorSignature() != null) current.setPastorSignature(updated.getPastorSignature().trim());
        if (updated.getAutoSendDaily() != null) current.setAutoSendDaily(updated.getAutoSendDaily());
        return emailSettingRepository.save(current);
    }

    /**
     * Retrieves members celebrating today with their personalized scripture and email delivery status.
     */
    @Transactional(readOnly = true)
    public List<CelebrationItemDto> getCelebrationsForToday() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        EmailSetting settings = getSettings();
        String pastorSig = settings.getPastorSignature() != null ? settings.getPastorSignature() : "Pastor & GraceConnect Church Family";

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<Member> allMembers = memberRepository.findAll();
        List<CelebrationItemDto> items = new ArrayList<>();

        for (Member m : allMembers) {
            String familyName = m.getFamily() != null ? m.getFamily().getName() : "GraceConnect Member";
            String email = (m.getEmail() != null && !m.getEmail().isBlank()) ? m.getEmail().trim()
                    : (m.getFamily() != null ? m.getFamily().getEmail() : "");

            // 1. Birthday Today
            if (m.getDob() != null && m.getDob().getMonthValue() == month && m.getDob().getDayOfMonth() == day) {
                int age = Period.between(m.getDob(), today).getYears();
                ScriptureDevotional devotional = pickDevotional(BIRTHDAY_DEVOTIONALS, m.getId());

                String subject = "✨ Happy Blessed Birthday, " + m.getFirstName() + "! 🎂 | GraceConnect Church";
                String fullMsg = buildBirthdayPlainMessage(m.getFirstName(), age, devotional, pastorSig);

                boolean alreadySent = !email.isBlank() && emailLogRepository.existsByRecipientEmailAndOccasionAndSentAtBetween(
                        email, "BIRTHDAY", startOfDay, endOfDay);

                items.add(CelebrationItemDto.builder()
                        .memberId(m.getId())
                        .fullName(m.getFirstName() + " " + (m.getLastName() != null ? m.getLastName() : ""))
                        .email(email)
                        .mobileNumber(m.getMobileNumber())
                        .profilePhoto(m.getProfilePhoto())
                        .familyName(familyName)
                        .occasion("BIRTHDAY")
                        .celebrationDate(m.getDob())
                        .milestoneYears(age)
                        .emailSubject(subject)
                        .scriptureRef(devotional.reference())
                        .scriptureText(devotional.verse())
                        .devotionalReflection(devotional.reflection())
                        .fullMessage(fullMsg)
                        .status(alreadySent ? "SENT" : "READY")
                        .build());
            }

            // 2. Wedding Anniversary Today
            if (m.getMarriageDate() != null && m.getMarriageDate().getMonthValue() == month && m.getMarriageDate().getDayOfMonth() == day) {
                int years = Period.between(m.getMarriageDate(), today).getYears();
                ScriptureDevotional devotional = pickDevotional(ANNIVERSARY_DEVOTIONALS, m.getId() != null ? m.getId() + 3 : 1);

                String coupleName = m.getFirstName() + (m.getLastName() != null ? " " + m.getLastName() : "");
                String subject = "💍 Happy Wedding Anniversary, " + coupleName + "! 🕊️ | GraceConnect Church";
                String fullMsg = buildAnniversaryPlainMessage(coupleName, familyName, years, devotional, pastorSig);

                boolean alreadySent = !email.isBlank() && emailLogRepository.existsByRecipientEmailAndOccasionAndSentAtBetween(
                        email, "ANNIVERSARY", startOfDay, endOfDay);

                items.add(CelebrationItemDto.builder()
                        .memberId(m.getId())
                        .fullName(coupleName)
                        .email(email)
                        .mobileNumber(m.getMobileNumber())
                        .profilePhoto(m.getProfilePhoto())
                        .familyName(familyName)
                        .occasion("ANNIVERSARY")
                        .celebrationDate(m.getMarriageDate())
                        .milestoneYears(years)
                        .emailSubject(subject)
                        .scriptureRef(devotional.reference())
                        .scriptureText(devotional.verse())
                        .devotionalReflection(devotional.reflection())
                        .fullMessage(fullMsg)
                        .status(alreadySent ? "SENT" : "READY")
                        .build());
            }
        }

        return items;
    }

    /**
     * Automatic Cron: Runs daily at 8:00 AM server time.
     */
    @Scheduled(cron = "${graceconnect.email.cron:0 0 8 * * ?}")
    public void runDailyScheduledEmailWishes() {
        EmailSetting settings = getSettings();
        if (Boolean.TRUE.equals(settings.getAutoSendDaily())) {
            log.info("Executing daily automated email birthday & anniversary blessings dispatch at 8:00 AM...");
            autoDispatchToday("SYSTEM_SCHEDULER");
        } else {
            log.info("Daily automated email dispatch skipped because autoSendDaily is false.");
        }
    }

    /**
     * Batch dispatches emails to all members celebrating today.
     */
    @Transactional
    public EmailSendResultDto autoDispatchToday(String triggeredBy) {
        List<CelebrationItemDto> celebrations = getCelebrationsForToday();
        EmailSetting settings = getSettings();

        int totalFound = celebrations.size();
        int sentCount = 0;
        int failedCount = 0;
        int alreadySentCount = 0;
        List<String> names = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        for (CelebrationItemDto item : celebrations) {
            String recipientEmail = item.getEmail();
            if (recipientEmail == null || recipientEmail.isBlank()) {
                failedCount++;
                continue;
            }

            boolean alreadySent = emailLogRepository.existsByRecipientEmailAndOccasionAndSentAtBetween(
                    recipientEmail, item.getOccasion(), startOfDay, endOfDay);

            if (alreadySent) {
                alreadySentCount++;
                continue;
            }

            // Build HTML Message
            String htmlContent = buildHtmlEmailCard(
                    item.getOccasion(), item.getFullName(), item.getMilestoneYears(),
                    item.getScriptureRef(), item.getScriptureText(), item.getDevotionalReflection(),
                    settings.getPastorSignature()
            );

            // Dispatch Email
            EmailDispatchStatus dispatchStatus = dispatchEmail(settings, recipientEmail, item.getEmailSubject(), htmlContent, item.getFullMessage());

            EmailLog logEntry = EmailLog.builder()
                    .recipientName(item.getFullName())
                    .recipientEmail(recipientEmail)
                    .occasion(item.getOccasion())
                    .subject(item.getEmailSubject())
                    .scriptureVerse(item.getScriptureRef())
                    .messageBody(item.getFullMessage())
                    .status(dispatchStatus.status())
                    .errorMessage(dispatchStatus.errorMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
            emailLogRepository.save(logEntry);

            if ("SENT".equalsIgnoreCase(dispatchStatus.status()) || "SIMULATED".equalsIgnoreCase(dispatchStatus.status())) {
                sentCount++;
                names.add(item.getFullName() + " <" + recipientEmail + ">");
            } else {
                failedCount++;
            }
        }

        settings.setLastRunDate(today);
        emailSettingRepository.save(settings);

        activityLogRepository.save(ActivityLog.builder()
                .action("Email Auto-Dispatch: " + sentCount + " blessings sent, " + alreadySentCount + " skipped, " + failedCount + " failed")
                .module("Email")
                .performedBy(triggeredBy != null ? triggeredBy : "System Administrator")
                .timestamp(LocalDateTime.now())
                .build());

        String summary = String.format("Email dispatch complete: %d sent, %d already received, %d failed out of %d celebrants.",
                sentCount, alreadySentCount, failedCount, totalFound);

        return EmailSendResultDto.builder()
                .totalFound(totalFound)
                .successfullySent(sentCount)
                .alreadySentToday(alreadySentCount)
                .failed(failedCount)
                .summaryMessage(summary)
                .dispatchedNames(names)
                .build();
    }

    /**
     * Send email blessing to a single member on demand.
     */
    @Transactional
    public EmailLog sendSingleCelebration(Long memberId, String occasion, String triggeredBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with ID: " + memberId));

        EmailSetting settings = getSettings();
        String pastorSig = settings.getPastorSignature() != null ? settings.getPastorSignature() : "Pastor & GraceConnect Church Family";

        String recipientEmail = (member.getEmail() != null && !member.getEmail().isBlank()) ? member.getEmail().trim()
                : (member.getFamily() != null ? member.getFamily().getEmail() : "");

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("No registered email address for member: " + member.getFirstName() + " " + member.getLastName());
        }

        String fullName = member.getFirstName() + " " + (member.getLastName() != null ? member.getLastName() : "");
        String subject;
        String scriptureRef;
        String verseText;
        String reflection;
        String plainMessage;
        int milestone;

        if ("ANNIVERSARY".equalsIgnoreCase(occasion)) {
            milestone = member.getMarriageDate() != null ? Period.between(member.getMarriageDate(), LocalDate.now()).getYears() : 1;
            ScriptureDevotional dev = pickDevotional(ANNIVERSARY_DEVOTIONALS, member.getId());
            scriptureRef = dev.reference();
            verseText = dev.verse();
            reflection = dev.reflection();
            subject = "💍 Happy Wedding Anniversary, " + fullName + "! 🕊️ | GraceConnect Church";
            String familyName = member.getFamily() != null ? member.getFamily().getName() : "GraceConnect Family";
            plainMessage = buildAnniversaryPlainMessage(fullName, familyName, milestone, dev, pastorSig);
        } else {
            milestone = member.getDob() != null ? Period.between(member.getDob(), LocalDate.now()).getYears() : 0;
            ScriptureDevotional dev = pickDevotional(BIRTHDAY_DEVOTIONALS, member.getId());
            scriptureRef = dev.reference();
            verseText = dev.verse();
            reflection = dev.reflection();
            subject = "✨ Happy Blessed Birthday, " + member.getFirstName() + "! 🎂 | GraceConnect Church";
            plainMessage = buildBirthdayPlainMessage(member.getFirstName(), milestone, dev, pastorSig);
        }

        String htmlContent = buildHtmlEmailCard(occasion, fullName, milestone, scriptureRef, verseText, reflection, pastorSig);
        EmailDispatchStatus dispatchStatus = dispatchEmail(settings, recipientEmail, subject, htmlContent, plainMessage);

        EmailLog logEntry = EmailLog.builder()
                .recipientName(fullName)
                .recipientEmail(recipientEmail)
                .occasion(occasion != null ? occasion.toUpperCase() : "BIRTHDAY")
                .subject(subject)
                .scriptureVerse(scriptureRef)
                .messageBody(plainMessage)
                .status(dispatchStatus.status())
                .errorMessage(dispatchStatus.errorMessage())
                .sentAt(LocalDateTime.now())
                .build();

        emailLogRepository.save(logEntry);

        activityLogRepository.save(ActivityLog.builder()
                .action("Sent Email " + occasion + " blessing to " + fullName + " (" + recipientEmail + ")")
                .module("Email")
                .performedBy(triggeredBy != null ? triggeredBy : "System Administrator")
                .timestamp(LocalDateTime.now())
                .build());

        return logEntry;
    }

    /**
     * Send test verification email to confirm SMTP settings.
     */
    @Transactional
    public EmailLog testSend(EmailTestRequest request, String triggeredBy) {
        EmailSetting settings = getSettings();
        String recipient = request.getRecipientEmail() != null ? request.getRecipientEmail().trim() : "";
        if (recipient.isBlank()) {
            throw new IllegalArgumentException("Recipient email address is required.");
        }

        String subject = request.getSubject() != null && !request.getSubject().isBlank()
                ? request.getSubject()
                : "🕊️ GraceConnect Church Email Server Verification Test";

        String scriptureRef = "Numbers 6:24-26";
        String verseText = "The Lord bless you and keep you; the Lord make His face shine on you and be gracious to you; the Lord turn His face toward you and give you peace.";
        String reflection = "This is a verification test confirming that GraceConnect Church email delivery service is active and properly connected.";

        String htmlContent = buildHtmlEmailCard("TEST", "Church Administrator", 0, scriptureRef, verseText, reflection, settings.getPastorSignature());
        String plain = "GraceConnect Church Email Gateway Test:\n\n" + verseText + "\n— " + scriptureRef + "\n\n" + settings.getPastorSignature();

        EmailDispatchStatus dispatchStatus = dispatchEmail(settings, recipient, subject, htmlContent, plain);

        EmailLog logEntry = EmailLog.builder()
                .recipientName("Church Administrator")
                .recipientEmail(recipient)
                .occasion("TEST")
                .subject(subject)
                .scriptureVerse(scriptureRef)
                .messageBody(plain)
                .status(dispatchStatus.status())
                .errorMessage(dispatchStatus.errorMessage())
                .sentAt(LocalDateTime.now())
                .build();

        return emailLogRepository.save(logEntry);
    }

    public Page<EmailLog> getLogs(Pageable pageable) {
        return emailLogRepository.findAllByOrderBySentAtDesc(pageable);
    }

    // --------------------------------------------------------------------------
    // INTERNAL EMAIL SENDER
    // --------------------------------------------------------------------------

    private record EmailDispatchStatus(String status, String errorMessage) {}

    private EmailDispatchStatus dispatchEmail(EmailSetting settings, String toEmail, String subject, String htmlContent, String plainContent) {
        try {
            JavaMailSender mailSender = resolveMailSender(settings);
            if (mailSender == null) {
                log.info("No active SMTP credentials configured. Email wish simulated and logged for: {}", toEmail);
                return new EmailDispatchStatus("SIMULATED", "SMTP credentials not configured. Message simulated, logged in database, and available for mailto app launch.");
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String fromAddr = (settings.getFromEmail() != null && !settings.getFromEmail().isBlank())
                    ? settings.getFromEmail() : "blessings@graceconnect.org";
            String fromName = (settings.getFromName() != null && !settings.getFromName().isBlank())
                    ? settings.getFromName() : "GraceConnect Church";

            helper.setFrom(fromAddr, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainContent, htmlContent);

            mailSender.send(message);
            log.info("Successfully sent celebration email to {}", toEmail);
            return new EmailDispatchStatus("SENT", null);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            return new EmailDispatchStatus("FAILED", e.getMessage());
        }
    }

    private JavaMailSender resolveMailSender(EmailSetting settings) {
        // 1. If dynamic database settings have username and password, build dynamic mail sender
        if (settings.getSmtpUsername() != null && !settings.getSmtpUsername().isBlank() &&
            settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank()) {
            JavaMailSenderImpl impl = new JavaMailSenderImpl();
            impl.setHost(settings.getSmtpHost() != null ? settings.getSmtpHost() : "smtp.gmail.com");
            impl.setPort(settings.getSmtpPort() != null ? settings.getSmtpPort() : 587);
            impl.setUsername(settings.getSmtpUsername());
            impl.setPassword(settings.getSmtpPassword());

            Properties props = impl.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            return impl;
        }

        // 2. Check if defaultMailSender has credentials configured via environment variables
        if (defaultMailSender instanceof JavaMailSenderImpl impl) {
            if (impl.getUsername() != null && !impl.getUsername().isBlank() &&
                impl.getPassword() != null && !impl.getPassword().isBlank()) {
                return defaultMailSender;
            }
        }

        return null;
    }

    // --------------------------------------------------------------------------
    // HTML & PLAIN TEXT TEMPLATE BUILDERS
    // --------------------------------------------------------------------------

    private ScriptureDevotional pickDevotional(List<ScriptureDevotional> list, Long id) {
        int index = (int) ((id != null ? id : 1) % list.size());
        return list.get(index);
    }

    private String buildHtmlEmailCard(String occasion, String name, int milestone,
                                       String scriptureRef, String scriptureText, String reflection,
                                       String pastorSignature) {
        boolean isBirthday = "BIRTHDAY".equalsIgnoreCase(occasion);
        String headerTitle = isBirthday ? "✨ Heavenly Birthday Blessing ✨" : "💍 Covenant Wedding Anniversary 🕊️";
        String subtitle = isBirthday ? "Celebrating the Gift of Your Life" : "Rejoicing in God's Faithfulness in Your Holy Matrimony";
        String accentColor = isBirthday ? "#8b5cf6" : "#f43f5e";
        String milestoneBadge = milestone > 0
                ? (isBirthday ? milestone + ordinalSuffix(milestone) + " Birthday" : milestone + ordinalSuffix(milestone) + " Anniversary")
                : "";

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 24px; background-color: #f8fafc; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #e2e8f0;">
                    
                    <!-- Header Banner -->
                    <div style="background: linear-gradient(135deg, #4f46e5 0%, %s 100%%); padding: 36px 24px; text-align: center; color: #ffffff;">
                        <span style="font-size: 28px; display: block; margin-bottom: 8px;">✝️</span>
                        <h1 style="margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px;">%s</h1>
                        <p style="margin: 6px 0 0; font-size: 13px; opacity: 0.9;">%s</p>
                    </div>

                    <!-- Main Body -->
                    <div style="padding: 32px 28px; color: #1e293b;">
                        <p style="font-size: 16px; line-height: 1.6; margin-top: 0;">
                            Dear <strong>%s</strong>,
                        </p>
                        <p style="font-size: 14.5px; line-height: 1.6; color: #475569;">
                            Your church family at <strong>GraceConnect</strong> rejoices with you today %s! We give profound thanks to Almighty God for His steadfast love and grace in your life.
                        </p>

                        <!-- Scripture Quote Box -->
                        <div style="margin: 24px 0; background: #f8fafc; border-left: 4px solid %s; padding: 18px 20px; border-radius: 0 12px 12px 0;">
                            <span style="font-size: 11px; font-weight: 700; letter-spacing: 1px; color: %s; text-transform: uppercase; display: block; margin-bottom: 6px;">
                                📖 God's Living Word for You:
                            </span>
                            <blockquote style="margin: 0; font-size: 15px; font-style: italic; line-height: 1.6; color: #1e293b;">
                                "%s"
                            </blockquote>
                            <span style="display: block; margin-top: 8px; font-size: 13px; font-weight: 700; color: #64748b; text-align: right;">
                                — %s
                            </span>
                        </div>

                        <!-- Devotional Reflection -->
                        <div style="margin-bottom: 22px;">
                            <span style="font-size: 13px; font-weight: 700; color: #0f172a; display: block; margin-bottom: 6px;">
                                🕊️ Spiritual Reflection:
                            </span>
                            <p style="margin: 0; font-size: 14px; line-height: 1.6; color: #475569;">
                                %s
                            </p>
                        </div>

                        <!-- Pastoral Prayer -->
                        <div style="background: rgba(139, 92, 246, 0.05); border: 1px dashed rgba(139, 92, 246, 0.3); border-radius: 12px; padding: 16px; margin-bottom: 24px;">
                            <span style="font-size: 13px; font-weight: 700; color: #6d28d9; display: block; margin-bottom: 4px;">
                                🙏 Our Pastoral Prayer:
                            </span>
                            <p style="margin: 0; font-size: 13.5px; line-height: 1.6; color: #334155;">
                                May the Lord Almighty enlarge your borders, grant you robust health, guard your going out and coming in, and shower His peace upon your household throughout this coming year.
                            </p>
                        </div>

                        <p style="margin: 0; font-size: 14px; line-height: 1.6; color: #475569;">
                            With heartfelt love & continuous prayers,<br>
                            <strong style="color: #0f172a;">%s</strong>
                        </p>
                    </div>

                    <!-- Footer -->
                    <div style="background: #f1f5f9; padding: 16px 24px; text-align: center; font-size: 11.5px; color: #64748b; border-top: 1px solid #e2e8f0;">
                        GraceConnect Church Family &bull; Celebrating Life & Covenant in Christ
                    </div>

                </div>
            </body>
            </html>
            """.formatted(
                accentColor,
                headerTitle,
                subtitle,
                name,
                milestoneBadge.isEmpty() ? "" : "on your <strong>" + milestoneBadge + "</strong>",
                accentColor,
                accentColor,
                scriptureText,
                scriptureRef,
                reflection,
                pastorSignature
        );
    }

    private String buildBirthdayPlainMessage(String name, int age, ScriptureDevotional devotional, String pastorSig) {
        String milestone = age > 0 ? " on your " + age + ordinalSuffix(age) + " Birthday" : "";
        return String.format(
                "✨ HAPPY BLESSED BIRTHDAY, %s! 🎂\n\n" +
                "Dear %s,\n" +
                "Your church family at GraceConnect warmly celebrates you today%s! We give profound thanks to Almighty God for the gift of your life and your fellowship.\n\n" +
                "📖 God's Living Word for You Today:\n" +
                "\"%s\"\n" +
                "— %s\n\n" +
                "🕊️ Spiritual Reflection:\n" +
                "%s\n\n" +
                "🙏 Our Pastoral Prayer for You:\n" +
                "May the LORD Almighty enlarge your borders, grant you robust health, and shower His heavenly peace upon you and your loved ones throughout this coming year.\n\n" +
                "With warmest love & blessings,\n" +
                "%s ✝️❤️",
                name.toUpperCase(),
                name,
                milestone,
                devotional.verse(),
                devotional.reference(),
                devotional.reflection(),
                pastorSig
        );
    }

    private String buildAnniversaryPlainMessage(String name, String familyName, int years, ScriptureDevotional devotional, String pastorSig) {
        String milestone = years > 0 ? " on your " + years + ordinalSuffix(years) + " Wedding Anniversary" : " on your Wedding Anniversary";
        return String.format(
                "💍 HAPPY WEDDING ANNIVERSARY, %s! 🕊️\n\n" +
                "Dear %s (%s),\n" +
                "Grace and peace to you in the name of our Lord Jesus Christ! We rejoice with you today%s as you celebrate God's steadfast faithfulness in your holy covenant of marriage.\n\n" +
                "📖 God's Living Word for Your Covenant:\n" +
                "\"%s\"\n" +
                "— %s\n\n" +
                "🕊️ Spiritual Reflection:\n" +
                "%s\n\n" +
                "🙏 Our Pastoral Prayer for Your Home:\n" +
                "May Christ forever remain the unwavering cornerstone of your marriage. May He multiply your love, fill your home with laughter, and establish your family as a testimony of grace for generations.\n\n" +
                "With deep love & continuous prayers,\n" +
                "%s ✝️❤️",
                name.toUpperCase(),
                name,
                familyName,
                milestone,
                devotional.verse(),
                devotional.reference(),
                devotional.reflection(),
                pastorSig
        );
    }

    private String ordinalSuffix(int n) {
        if (n >= 11 && n <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}

