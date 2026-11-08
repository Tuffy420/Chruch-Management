package com.graceconnect.church.service;

import com.graceconnect.church.dto.CelebrationItemDto;
import com.graceconnect.church.dto.WhatsAppSendResultDto;
import com.graceconnect.church.dto.WhatsAppTestRequest;
import com.graceconnect.church.model.ActivityLog;
import com.graceconnect.church.model.Member;
import com.graceconnect.church.model.WhatsAppLog;
import com.graceconnect.church.model.WhatsAppSetting;
import com.graceconnect.church.repository.ActivityLogRepository;
import com.graceconnect.church.repository.MemberRepository;
import com.graceconnect.church.repository.WhatsAppLogRepository;
import com.graceconnect.church.repository.WhatsAppSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final MemberRepository memberRepository;
    private final WhatsAppLogRepository whatsAppLogRepository;
    private final WhatsAppSettingRepository whatsAppSettingRepository;
    private final ActivityLogRepository activityLogRepository;
    private final HttpClient httpClient;

    // Curated Scripture Verses & Devotional Reflections
    private record ScriptureDevotional(String reference, String verse, String reflection) {}

    private static final List<ScriptureDevotional> BIRTHDAY_DEVOTIONALS = List.of(
            new ScriptureDevotional(
                    "Jeremiah 29:11",
                    "For I know the plans I have for you, declares the Lord, plans to prosper you and not to harm you, plans to give you hope and a future.",
                    "God has designed every chapter of your life with divine precision. Walk into this new year with great confidence, knowing His purpose for you will unfailingly prevail."
            ),
            new ScriptureDevotional(
                    "Psalm 20:4",
                    "May He give you the desire of your heart and make all your plans succeed.",
                    "When your desires are rooted in God's love, He delights in turning righteous prayers into testimonies. May this year be crowned with answered prayers and divine favor."
            ),
            new ScriptureDevotional(
                    "Numbers 6:24-26",
                    "The Lord bless you and keep you; the Lord make His face shine on you and be gracious to you; the Lord turn His face toward you and give you peace.",
                    "God's countenance is your eternal shield. Through every season, His unmerited favor surrounds you and His peace guards your mind and heart."
            ),
            new ScriptureDevotional(
                    "Psalm 139:14",
                    "I praise you because I am fearfully and wonderfully made; your works are wonderful, I know that full well.",
                    "You were crafted with divine artistry and endowed with unique spiritual gifts. Never doubt how cherished and significant you are in the Father's heart."
            ),
            new ScriptureDevotional(
                    "Isaiah 40:31",
                    "Those who hope in the Lord will renew their strength. They will soar on wings like eagles; they will run and not grow weary, they will walk and not be faint.",
                    "When human energy fades, God's limitless stamina sustains you. May this upcoming year bring spiritual renewal, vision, and supernatural endurance."
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
                    "With Christ as the third cord woven through your marriage, no pressure or season can unravel your bond. Continue holding fast to mutual prayer and selfless grace."
            ),
            new ScriptureDevotional(
                    "1 Corinthians 13:7-8",
                    "Love always protects, always trusts, always hopes, always perseveres. Love never fails.",
                    "Christian marriage is a living portrait of Christ's unconditional commitment to His Church. May your home remain a haven of joyful peace and enduring honor."
            ),
            new ScriptureDevotional(
                    "Colossians 3:14",
                    "And over all these virtues put on love, which binds them all together in perfect unity.",
                    "Unity in covenant is a shining beacon to your community and children. May your partnership continue flourishing with understanding, honor, and deep affection."
            ),
            new ScriptureDevotional(
                    "Proverbs 3:3-4",
                    "Let love and faithfulness never leave you; bind them around your neck, write them on the tablet of your heart. Then you will win favor and a good name in the sight of God and man.",
                    "Integrity, kindness, and devotion are the greatest treasures a family can cultivate. May your generational heritage be filled with divine abundance."
            )
    );

    public WhatsAppService(MemberRepository memberRepository,
                           WhatsAppLogRepository whatsAppLogRepository,
                           WhatsAppSettingRepository whatsAppSettingRepository,
                           ActivityLogRepository activityLogRepository) {
        this.memberRepository = memberRepository;
        this.whatsAppLogRepository = whatsAppLogRepository;
        this.whatsAppSettingRepository = whatsAppSettingRepository;
        this.activityLogRepository = activityLogRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public WhatsAppSetting getSettings() {
        return whatsAppSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> whatsAppSettingRepository.save(
                        WhatsAppSetting.builder()
                                .provider("ULTRAMSG")
                                .apiUrl("")
                                .instanceId("")
                                .apiToken("")
                                .autoSendDaily(true)
                                .pastorSignature("Pastor & GraceConnect Church Family")
                                .build()
                ));
    }

    @Transactional
    public WhatsAppSetting saveSettings(WhatsAppSetting updated) {
        WhatsAppSetting current = getSettings();
        if (updated.getProvider() != null) current.setProvider(updated.getProvider());
        if (updated.getApiUrl() != null) current.setApiUrl(updated.getApiUrl().trim());
        if (updated.getInstanceId() != null) current.setInstanceId(updated.getInstanceId().trim());
        if (updated.getApiToken() != null) current.setApiToken(updated.getApiToken().trim());
        if (updated.getAccountSid() != null) current.setAccountSid(updated.getAccountSid().trim());
        if (updated.getFromPhone() != null) current.setFromPhone(updated.getFromPhone().trim());
        if (updated.getAutoSendDaily() != null) current.setAutoSendDaily(updated.getAutoSendDaily());
        if (updated.getPastorSignature() != null) current.setPastorSignature(updated.getPastorSignature().trim());
        return whatsAppSettingRepository.save(current);
    }

    /**
     * Finds members celebrating birthdays and wedding anniversaries today,
     * builds their personalized scripture devotional blessing, and checks delivery status.
     */
    @Transactional(readOnly = true)
    public List<CelebrationItemDto> getCelebrationsForToday() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        WhatsAppSetting settings = getSettings();
        String pastorSig = settings.getPastorSignature() != null ? settings.getPastorSignature() : "Pastor & GraceConnect Church";

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<Member> allMembers = memberRepository.findAll();
        List<CelebrationItemDto> items = new ArrayList<>();

        for (Member m : allMembers) {
            String sanitizedPhone = sanitizePhone(m.getMobileNumber());
            String familyName = m.getFamily() != null ? m.getFamily().getName() : "GraceConnect Member";

            // 1. Check Birthday Today
            if (m.getDob() != null && m.getDob().getMonthValue() == month && m.getDob().getDayOfMonth() == day) {
                int age = Period.between(m.getDob(), today).getYears();
                ScriptureDevotional devotional = pickDevotional(BIRTHDAY_DEVOTIONALS, m.getId());

                String fullMsg = buildBirthdayMessage(
                        m.getFirstName(), m.getLastName(), age, devotional, pastorSig);

                boolean alreadySent = whatsAppLogRepository.existsByPhoneNumberAndOccasionAndSentAtBetween(
                        sanitizedPhone, "BIRTHDAY", startOfDay, endOfDay);

                items.add(CelebrationItemDto.builder()
                        .memberId(m.getId())
                        .fullName(m.getFirstName() + " " + (m.getLastName() != null ? m.getLastName() : ""))
                        .mobileNumber(m.getMobileNumber())
                        .profilePhoto(m.getProfilePhoto())
                        .familyName(familyName)
                        .occasion("BIRTHDAY")
                        .celebrationDate(m.getDob())
                        .milestoneYears(age)
                        .scriptureRef(devotional.reference())
                        .scriptureText(devotional.verse())
                        .devotionalReflection(devotional.reflection())
                        .fullMessage(fullMsg)
                        .status(alreadySent ? "SENT" : "READY")
                        .build());
            }

            // 2. Check Wedding Anniversary Today
            if (m.getMarriageDate() != null && m.getMarriageDate().getMonthValue() == month && m.getMarriageDate().getDayOfMonth() == day) {
                int years = Period.between(m.getMarriageDate(), today).getYears();
                ScriptureDevotional devotional = pickDevotional(ANNIVERSARY_DEVOTIONALS, m.getId() != null ? m.getId() + 3 : 1);

                String coupleName = m.getFirstName() + (m.getLastName() != null ? " " + m.getLastName() : "");
                String fullMsg = buildAnniversaryMessage(coupleName, familyName, years, devotional, pastorSig);

                boolean alreadySent = whatsAppLogRepository.existsByPhoneNumberAndOccasionAndSentAtBetween(
                        sanitizedPhone, "ANNIVERSARY", startOfDay, endOfDay);

                items.add(CelebrationItemDto.builder()
                        .memberId(m.getId())
                        .fullName(coupleName)
                        .mobileNumber(m.getMobileNumber())
                        .profilePhoto(m.getProfilePhoto())
                        .familyName(familyName)
                        .occasion("ANNIVERSARY")
                        .celebrationDate(m.getMarriageDate())
                        .milestoneYears(years)
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
    @Scheduled(cron = "${graceconnect.whatsapp.cron:0 0 8 * * ?}")
    public void runDailyScheduledWishes() {
        WhatsAppSetting settings = getSettings();
        if (Boolean.TRUE.equals(settings.getAutoSendDaily())) {
            log.info("Executing daily automated WhatsApp birthday & anniversary wishes dispatch at 8:00 AM...");
            autoDispatchToday("SYSTEM_SCHEDULER");
        } else {
            log.info("Daily automated WhatsApp wishes dispatch skipped because autoSendDaily is false.");
        }
    }

    /**
     * Dispatches wishes for all members celebrating today.
     */
    @Transactional
    public WhatsAppSendResultDto autoDispatchToday(String triggeredBy) {
        List<CelebrationItemDto> celebrations = getCelebrationsForToday();
        WhatsAppSetting settings = getSettings();

        int totalFound = celebrations.size();
        int sentCount = 0;
        int failedCount = 0;
        int alreadySentCount = 0;
        List<String> names = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        for (CelebrationItemDto item : celebrations) {
            String sanitizedPhone = sanitizePhone(item.getMobileNumber());

            // Check if already sent today
            boolean alreadySent = whatsAppLogRepository.existsByPhoneNumberAndOccasionAndSentAtBetween(
                    sanitizedPhone, item.getOccasion(), startOfDay, endOfDay);

            if (alreadySent) {
                alreadySentCount++;
                continue;
            }

            // Dispatch message via active provider
            WhatsAppDispatchResult result = dispatchViaProvider(settings, sanitizedPhone, item.getFullMessage());

            // Save Log
            WhatsAppLog logEntry = WhatsAppLog.builder()
                    .recipientName(item.getFullName())
                    .phoneNumber(sanitizedPhone)
                    .occasion(item.getOccasion())
                    .scriptureVerse(item.getScriptureRef())
                    .messageText(item.getFullMessage())
                    .status(result.status())
                    .provider(result.provider())
                    .responsePayload(result.responsePayload())
                    .errorMessage(result.errorMessage())
                    .sentAt(LocalDateTime.now())
                    .build();
            whatsAppLogRepository.save(logEntry);

            if ("SENT".equalsIgnoreCase(result.status()) || "SIMULATED".equalsIgnoreCase(result.status())) {
                sentCount++;
                names.add(item.getFullName() + " (" + item.getOccasion() + ")");
            } else {
                failedCount++;
            }
        }

        // Update settings last run date
        settings.setLastRunDate(today);
        whatsAppSettingRepository.save(settings);

        // Record activity log
        activityLogRepository.save(ActivityLog.builder()
                .action("WhatsApp Auto-Dispatch: " + sentCount + " wishes sent, " + alreadySentCount + " skipped, " + failedCount + " failed")
                .module("WhatsApp")
                .performedBy(triggeredBy != null ? triggeredBy : "System Administrator")
                .timestamp(LocalDateTime.now())
                .build());

        String summary = String.format("Auto-dispatch completed: %d wishes sent (%d already sent today, %d failed out of %d total).",
                sentCount, alreadySentCount, failedCount, totalFound);

        return WhatsAppSendResultDto.builder()
                .totalFound(totalFound)
                .successfullySent(sentCount)
                .alreadySentToday(alreadySentCount)
                .failed(failedCount)
                .providerUsed(settings.getProvider())
                .summaryMessage(summary)
                .dispatchedNames(names)
                .build();
    }

    /**
     * Send wish to a single specific member on demand.
     */
    @Transactional
    public WhatsAppLog sendSingleCelebration(Long memberId, String occasion, String triggeredBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with ID: " + memberId));

        WhatsAppSetting settings = getSettings();
        String pastorSig = settings.getPastorSignature() != null ? settings.getPastorSignature() : "Pastor & GraceConnect Church";
        String sanitizedPhone = sanitizePhone(member.getMobileNumber());

        String fullMessage;
        String scriptureRef;

        if ("ANNIVERSARY".equalsIgnoreCase(occasion)) {
            int years = member.getMarriageDate() != null ? Period.between(member.getMarriageDate(), LocalDate.now()).getYears() : 1;
            ScriptureDevotional dev = pickDevotional(ANNIVERSARY_DEVOTIONALS, member.getId());
            scriptureRef = dev.reference();
            String familyName = member.getFamily() != null ? member.getFamily().getName() : "GraceConnect Family";
            fullMessage = buildAnniversaryMessage(member.getFirstName() + " " + member.getLastName(), familyName, years, dev, pastorSig);
        } else {
            int age = member.getDob() != null ? Period.between(member.getDob(), LocalDate.now()).getYears() : 0;
            ScriptureDevotional dev = pickDevotional(BIRTHDAY_DEVOTIONALS, member.getId());
            scriptureRef = dev.reference();
            fullMessage = buildBirthdayMessage(member.getFirstName(), member.getLastName(), age, dev, pastorSig);
        }

        WhatsAppDispatchResult result = dispatchViaProvider(settings, sanitizedPhone, fullMessage);

        WhatsAppLog logEntry = WhatsAppLog.builder()
                .recipientName(member.getFirstName() + " " + (member.getLastName() != null ? member.getLastName() : ""))
                .phoneNumber(sanitizedPhone)
                .occasion(occasion != null ? occasion.toUpperCase() : "BIRTHDAY")
                .scriptureVerse(scriptureRef)
                .messageText(fullMessage)
                .status(result.status())
                .provider(result.provider())
                .responsePayload(result.responsePayload())
                .errorMessage(result.errorMessage())
                .sentAt(LocalDateTime.now())
                .build();

        whatsAppLogRepository.save(logEntry);

        activityLogRepository.save(ActivityLog.builder()
                .action("Sent WhatsApp " + occasion + " blessing to " + member.getFirstName() + " " + member.getLastName())
                .module("WhatsApp")
                .performedBy(triggeredBy != null ? triggeredBy : "System Administrator")
                .timestamp(LocalDateTime.now())
                .build());

        return logEntry;
    }

    /**
     * Test message sender to verify WhatsApp API credentials.
     */
    @Transactional
    public WhatsAppLog testSend(WhatsAppTestRequest request, String triggeredBy) {
        WhatsAppSetting settings = getSettings();
        String sanitizedPhone = sanitizePhone(request.getPhoneNumber());
        String msg = request.getCustomMessage() != null && !request.getCustomMessage().isBlank()
                ? request.getCustomMessage()
                : "🕊️ *GraceConnect WhatsApp Gateway Test*\n\nYour automated Church WhatsApp wishing service is successfully configured and active!\n\n_\"The Lord bless you and keep you!\" - Numbers 6:24_\n\n— " + settings.getPastorSignature();

        WhatsAppDispatchResult result = dispatchViaProvider(settings, sanitizedPhone, msg);

        WhatsAppLog logEntry = WhatsAppLog.builder()
                .recipientName("Test Recipient")
                .phoneNumber(sanitizedPhone)
                .occasion("TEST")
                .scriptureVerse("Numbers 6:24")
                .messageText(msg)
                .status(result.status())
                .provider(result.provider())
                .responsePayload(result.responsePayload())
                .errorMessage(result.errorMessage())
                .sentAt(LocalDateTime.now())
                .build();

        return whatsAppLogRepository.save(logEntry);
    }

    public Page<WhatsAppLog> getLogs(Pageable pageable) {
        return whatsAppLogRepository.findAllByOrderBySentAtDesc(pageable);
    }

    // --------------------------------------------------------------------------
    // INTERNAL PROVIDER DISPATCH ENGINE
    // --------------------------------------------------------------------------

    private record WhatsAppDispatchResult(String status, String provider, String responsePayload, String errorMessage) {}

    private WhatsAppDispatchResult dispatchViaProvider(WhatsAppSetting settings, String phone, String message) {
        String provider = settings.getProvider() != null ? settings.getProvider().toUpperCase() : "ULTRAMSG";

        try {
            // 1. UltraMsg REST API Gateway
            if ("ULTRAMSG".equalsIgnoreCase(provider)) {
                if (settings.getInstanceId() == null || settings.getInstanceId().isBlank() ||
                    settings.getApiToken() == null || settings.getApiToken().isBlank()) {
                    return new WhatsAppDispatchResult("SIMULATED", "ULTRAMSG",
                            "UltraMsg credentials not configured. Message simulated and logged. Enter Instance ID & Token in Settings to enable live sending.", null);
                }

                String url = settings.getApiUrl() != null && !settings.getApiUrl().isBlank()
                        ? settings.getApiUrl()
                        : "https://api.ultramsg.com/" + settings.getInstanceId() + "/messages/chat";

                String formBody = "token=" + URLEncoder.encode(settings.getApiToken(), StandardCharsets.UTF_8)
                        + "&to=" + URLEncoder.encode(phone, StandardCharsets.UTF_8)
                        + "&body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(formBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return new WhatsAppDispatchResult("SENT", "ULTRAMSG", response.body(), null);
                } else {
                    return new WhatsAppDispatchResult("FAILED", "ULTRAMSG", response.body(), "HTTP " + response.statusCode());
                }
            }

            // 2. Custom Webhook Gateway (e.g. Baileys / WPPConnect / Evolution API / n8n / Zapier)
            if ("WEBHOOK".equalsIgnoreCase(provider) || "CUSTOM_WEBHOOK".equalsIgnoreCase(provider)) {
                if (settings.getApiUrl() == null || settings.getApiUrl().isBlank()) {
                    return new WhatsAppDispatchResult("SIMULATED", "WEBHOOK",
                            "Webhook URL not provided. Message logged in database.", null);
                }

                String jsonPayload = String.format("{\"to\":\"%s\",\"message\":\"%s\",\"token\":\"%s\"}",
                        escapeJson(phone), escapeJson(message), escapeJson(settings.getApiToken() != null ? settings.getApiToken() : ""));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(settings.getApiUrl()))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return new WhatsAppDispatchResult("SENT", "WEBHOOK", response.body(), null);
                } else {
                    return new WhatsAppDispatchResult("FAILED", "WEBHOOK", response.body(), "HTTP " + response.statusCode());
                }
            }

            // 3. Twilio WhatsApp API
            if ("TWILIO".equalsIgnoreCase(provider)) {
                if (settings.getAccountSid() == null || settings.getAccountSid().isBlank() ||
                    settings.getApiToken() == null || settings.getApiToken().isBlank()) {
                    return new WhatsAppDispatchResult("SIMULATED", "TWILIO",
                            "Twilio Account SID or Auth Token missing. Message simulated.", null);
                }

                String fromNumber = settings.getFromPhone() != null && !settings.getFromPhone().isBlank()
                        ? settings.getFromPhone() : "whatsapp:+14155238886";

                String toWhatsApp = phone.startsWith("whatsapp:") ? phone : "whatsapp:" + (phone.startsWith("+") ? phone : "+" + phone);

                String url = "https://api.twilio.com/2010-04-01/Accounts/" + settings.getAccountSid() + "/Messages.json";
                String formBody = "From=" + URLEncoder.encode(fromNumber, StandardCharsets.UTF_8)
                        + "&To=" + URLEncoder.encode(toWhatsApp, StandardCharsets.UTF_8)
                        + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

                String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                        (settings.getAccountSid() + ":" + settings.getApiToken()).getBytes(StandardCharsets.UTF_8));

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", authHeader)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(formBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return new WhatsAppDispatchResult("SENT", "TWILIO", response.body(), null);
                } else {
                    return new WhatsAppDispatchResult("FAILED", "TWILIO", response.body(), "HTTP " + response.statusCode());
                }
            }

            // Fallback / Simulator Mode
            return new WhatsAppDispatchResult("SIMULATED", "SIMULATOR",
                    "Simulated automated dispatch. Configured for direct WhatsApp Web launch and database audit.", null);

        } catch (Exception e) {
            log.error("Error dispatching WhatsApp message to {}: {}", phone, e.getMessage(), e);
            return new WhatsAppDispatchResult("FAILED", provider, null, e.getMessage());
        }
    }

    // --------------------------------------------------------------------------
    // MESSAGE TEMPLATE & SCRIPTURE GENERATORS
    // --------------------------------------------------------------------------

    private ScriptureDevotional pickDevotional(List<ScriptureDevotional> list, Long id) {
        int index = (int) ((id != null ? id : 1) % list.size());
        return list.get(index);
    }

    private String buildBirthdayMessage(String firstName, String lastName, int age,
                                         ScriptureDevotional devotional, String pastorSignature) {
        String milestone = age > 0 ? " on your *" + age + ordinalSuffix(age) + "* Birthday" : "";
        return String.format(
                "✨ *HAPPY BLESSED BIRTHDAY, %s!* 🎂🎉\n\n" +
                "Dear %s,\n" +
                "Your church family at GraceConnect warmly celebrates you today%s! We give profound thanks to Almighty God for the gift of your life and your presence in our fellowship.\n\n" +
                "📖 *God's Living Word for You Today:*\n" +
                "\"%s\"\n" +
                "— *%s*\n\n" +
                "🕊️ *Spiritual Reflection:*\n" +
                "%s\n\n" +
                "🙏 *Our Pastoral Prayer for You:*\n" +
                "May the Lord Almighty enlarge your borders, grant you joyful longevity, divine health, and overflow your coming year with heavenly peace and fruitfulness.\n\n" +
                "With warmest love & blessings,\n" +
                "*%s* ✝️❤️",
                firstName.toUpperCase(),
                firstName,
                milestone,
                devotional.verse(),
                devotional.reference(),
                devotional.reflection(),
                pastorSignature
        );
    }

    private String buildAnniversaryMessage(String coupleName, String familyName, int years,
                                            ScriptureDevotional devotional, String pastorSignature) {
        String milestone = years > 0 ? " *" + years + ordinalSuffix(years) + "* " : " ";
        return String.format(
                "💍 *HAPPY%sWEDDING ANNIVERSARY!* 🕊️🌹\n\n" +
                "Dear %s (%s),\n" +
                "Grace and peace to you in the name of our Lord Jesus Christ! We rejoice with you today as you celebrate God's steadfast faithfulness in your holy covenant of marriage.\n\n" +
                "📖 *God's Living Word for Your Covenant:*\n" +
                "\"%s\"\n" +
                "— *%s*\n\n" +
                "🕊️ *Spiritual Reflection:*\n" +
                "%s\n\n" +
                "🙏 *Our Pastoral Prayer for Your Home:*\n" +
                "May Christ forever remain the unwavering center of your union. May He multiply your love, fill your household with laughter, and establish your family as a testimony of grace for generations.\n\n" +
                "With deep love & continuous prayers,\n" +
                "*%s* ✝️❤️",
                milestone.toUpperCase(),
                coupleName,
                familyName,
                devotional.verse(),
                devotional.reference(),
                devotional.reflection(),
                pastorSignature
        );
    }

    private String sanitizePhone(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.startsWith("+")) {
            return cleaned;
        }
        if (cleaned.length() == 10) {
            return "+91" + cleaned; // Standard 10-digit Indian mobile fallback
        }
        return "+" + cleaned;
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

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
