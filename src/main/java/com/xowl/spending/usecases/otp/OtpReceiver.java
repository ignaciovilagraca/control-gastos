package com.xowl.spending.usecases.otp;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Retrieves a one-time code sent by email (e.g. Prisma login). */
public interface OtpReceiver {

    /**
     * Polls until an OTP appears in a message whose internal date is at or after {@code notBefore},
     * or until {@code maxWait} elapses.
     */
    Optional<String> pollAfter(Instant notBefore, Duration maxWait) throws IOException, InterruptedException;
}
