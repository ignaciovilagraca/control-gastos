package com.xowl.spending.frameworks.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.xowl.spending.frameworks.config.EnvVariables;
import com.xowl.spending.usecases.otp.OtpReceiver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads OTP codes via Gmail API using a refresh token (user mailbox).
 *
 * <p>Required environment variables: {@code CONTROL_GASTOS_GMAIL_CLIENT_ID},
 * {@code CONTROL_GASTOS_GMAIL_CLIENT_SECRET}, {@code CONTROL_GASTOS_GMAIL_REFRESH_TOKEN}.
 *
 * <p>Optional: {@code CONTROL_GASTOS_GMAIL_QUERY} (default filters Prisma OTP mail),
 * {@code CONTROL_GASTOS_GMAIL_OTP_REGEX} (if set, only that pattern is used).
 *
 * <p>Obtain a refresh token once (e.g. <a href="https://developers.google.com/oauthplayground/">OAuth 2.0 Playground</a>
 * with scope {@code https://www.googleapis.com/auth/gmail.modify}) so OTP messages can be moved to Trash after reading.
 */
public class GmailApiOtpReceiver implements OtpReceiver {

    private static final String ENV_CLIENT_ID = "CONTROL_GASTOS_GMAIL_CLIENT_ID";
    private static final String ENV_CLIENT_SECRET = "CONTROL_GASTOS_GMAIL_CLIENT_SECRET";
    private static final String ENV_REFRESH_TOKEN = "CONTROL_GASTOS_GMAIL_REFRESH_TOKEN";
    private static final String ENV_QUERY = "CONTROL_GASTOS_GMAIL_QUERY";
    private static final String ENV_OTP_REGEX = "CONTROL_GASTOS_GMAIL_OTP_REGEX";

    /** Ignore Gmail internalDate this many ms before our "login click" instant (clock skew). */
    private static final long NOT_BEFORE_SLACK_MS = Duration.ofMinutes(3).toMillis();

    private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);

    private final Gmail gmail;
    private final String listQuery;
    private final List<Pattern> otpPatterns;

    public GmailApiOtpReceiver(Gmail gmail, String listQuery, List<Pattern> otpPatterns) {
        this.gmail = gmail;
        this.listQuery = listQuery;
        this.otpPatterns = otpPatterns;
    }

    public static Optional<GmailApiOtpReceiver> fromEnvironment() throws IOException, GeneralSecurityException {
        String clientId = EnvVariables.get(ENV_CLIENT_ID);
        String clientSecret = EnvVariables.get(ENV_CLIENT_SECRET);
        String refreshToken = EnvVariables.get(ENV_REFRESH_TOKEN);
        if (isBlank(clientId) || isBlank(clientSecret) || isBlank(refreshToken)) {
            return Optional.empty();
        }

        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(clientId.trim())
                .setClientSecret(clientSecret.trim())
                .setRefreshToken(refreshToken.trim())
                .build();

        Gmail gmail = new Gmail.Builder(
                        GoogleNetHttpTransport.newTrustedTransport(),
                        GsonFactory.getDefaultInstance(),
                        new HttpCredentialsAdapter(credentials))
                .setApplicationName("control-gastos")
                .build();

        String query = EnvVariables.get(ENV_QUERY);
        if (isBlank(query)) {
            query = "newer_than:30m from:noreply@infomistarjetas.com";
        } else {
            query = query.trim();
        }

        List<Pattern> patterns = new ArrayList<>();
        String regex = EnvVariables.get(ENV_OTP_REGEX);
        if (!isBlank(regex)) {
            patterns.add(Pattern.compile(regex.trim()));
        } else {
            patterns.add(Pattern.compile("\\b(\\d{6})\\b"));
            patterns.add(Pattern.compile("\\b(\\d{3}\\s+\\d{3})\\b"));
            patterns.add(Pattern.compile("(?i)(?:código|codigo|code|otp)\\s*[:.\\s-]+([0-9]{4,8})\\b"));
            patterns.add(Pattern.compile("\\b(\\d{8})\\b"));
        }

        return Optional.of(new GmailApiOtpReceiver(gmail, query, patterns));
    }

    @Override
    public Optional<String> pollAfter(Instant notBefore, Duration maxWait) throws IOException, InterruptedException {
        long deadlineNanos = System.nanoTime() + maxWait.toNanos();
        long notBeforeMs = notBefore.toEpochMilli() - NOT_BEFORE_SLACK_MS;

        while (System.nanoTime() < deadlineNanos) {
            Optional<String> found = fetchLatestMatchingOtp(notBeforeMs);
            if (found.isPresent()) {
                return found;
            }
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        return Optional.empty();
    }

    private Optional<String> fetchLatestMatchingOtp(long notBeforeMs) throws IOException {
        ListMessagesResponse response = gmail.users().messages()
                .list("me")
                .setQ(listQuery)
                .setMaxResults(40L)
                .execute();

        if (response.getMessages() == null || response.getMessages().isEmpty()) {
            return Optional.empty();
        }

        List<Message> fullMessages = new ArrayList<>();
        for (Message ref : response.getMessages()) {
            Message full = gmail.users().messages()
                    .get("me", ref.getId())
                    .setFormat("full")
                    .execute();
            Long internal = full.getInternalDate();
            if (internal != null && internal < notBeforeMs) {
                continue;
            }
            fullMessages.add(full);
        }

        fullMessages.sort(Comparator.comparing(
                m -> m.getInternalDate() == null ? 0L : m.getInternalDate(),
                Comparator.reverseOrder()));

        for (Message full : fullMessages) {
            String haystack = buildHaystack(full);
            Optional<String> otp = firstMatchingOtp(haystack);
            if (otp.isPresent()) {
                trashOtpMessage(full.getId());
                return otp;
            }
        }
        return Optional.empty();
    }

    /** Moves the OTP email to Trash so the same message is not matched on the next run. */
    private void trashOtpMessage(String messageId) {
        if (messageId == null || messageId.isEmpty()) {
            return;
        }
        try {
            gmail.users().messages().trash("me", messageId).execute();
        } catch (IOException ignored) {
            // Non-fatal: OTP was already returned; failures usually mean missing gmail.modify on the refresh token.
        }
    }

    private static String buildHaystack(Message full) {
        String subject = headerValue(full, "Subject");
        String snippet = full.getSnippet() == null ? "" : full.getSnippet();
        String body = extractText(full.getPayload());
        return subject + "\n" + snippet + "\n" + body;
    }

    private static String headerValue(Message msg, String headerName) {
        if (msg.getPayload() == null || msg.getPayload().getHeaders() == null) {
            return "";
        }
        for (MessagePartHeader h : msg.getPayload().getHeaders()) {
            if (headerName.equalsIgnoreCase(h.getName())) {
                return h.getValue() == null ? "" : h.getValue();
            }
        }
        return "";
    }

    private Optional<String> firstMatchingOtp(String haystack) {
        for (Pattern otpPattern : otpPatterns) {
            Matcher matcher = otpPattern.matcher(haystack);
            if (!matcher.find()) {
                continue;
            }
            String raw = matcher.groupCount() >= 1 ? matcher.group(1) : matcher.group();
            String normalized = raw.replaceAll("\\s+", "");
            if (normalized.length() >= 4 && normalized.length() <= 8 && normalized.chars().allMatch(Character::isDigit)) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    private static String extractText(MessagePart part) {
        if (part == null) {
            return "";
        }
        String mime = part.getMimeType();
        if (mime != null && mime.startsWith("multipart/")) {
            StringBuilder sb = new StringBuilder();
            if (part.getParts() != null) {
                for (MessagePart p : nullSafe(part.getParts())) {
                    sb.append(extractText(p));
                }
            }
            return sb.toString();
        }
        if (part.getBody() != null && part.getBody().getData() != null) {
            byte[] decoded = decodeBase64Url(part.getBody().getData());
            String chunk = new String(decoded, StandardCharsets.UTF_8);
            if ("text/html".equalsIgnoreCase(mime)) {
                return chunk.replaceAll("<[^>]+>", " ");
            }
            return chunk;
        }
        return "";
    }

    private static byte[] decodeBase64Url(String data) {
        String trimmed = data.replace("\n", "").replace("\r", "").trim();
        try {
            return Base64.getUrlDecoder().decode(trimmed);
        } catch (IllegalArgumentException e1) {
            try {
                return Base64.getMimeDecoder().decode(trimmed.replace(' ', '+'));
            } catch (IllegalArgumentException e2) {
                return new byte[0];
            }
        }
    }

    private static List<MessagePart> nullSafe(List<MessagePart> parts) {
        return parts == null ? Collections.emptyList() : parts;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
