package org.delcom.ifs25041springbootcashflow.framework.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionCookieSignerTest {

    @Test
    void roundTripsUserId() {
        SessionCookieSigner signer = new SessionCookieSigner("rahasia-uji");
        assertEquals(42, signer.verify(signer.sign(42)).orElseThrow().intValue());
    }

    @Test
    void rejectsForgedUserIdWithOriginalSignature() {
        SessionCookieSigner signer = new SessionCookieSigner("rahasia-uji");
        String signature = signer.sign(2).substring(2);
        assertTrue(signer.verify("1." + signature).isEmpty());
    }

    @Test
    void rejectsPlainUserIdAsUsedByOldCookies() {
        assertTrue(new SessionCookieSigner("k").verify("1").isEmpty());
    }

    @Test
    void rejectsMalformedValues() {
        SessionCookieSigner signer = new SessionCookieSigner("k");
        assertTrue(signer.verify(null).isEmpty());
        assertTrue(signer.verify("").isEmpty());
        assertTrue(signer.verify(".abc").isEmpty());
        assertTrue(signer.verify("abc.def").isEmpty());
        assertTrue(signer.verify("1.@@@").isEmpty());
        assertTrue(signer.verify("1.").isEmpty());
    }

    @Test
    void differentSecretsDoNotAcceptEachOthersCookies() {
        String cookie = new SessionCookieSigner("satu").sign(5);
        assertTrue(new SessionCookieSigner("dua").verify(cookie).isEmpty());
    }

    @Test
    void blankSecretFallsBackToRandomKeyPerInstance() {
        SessionCookieSigner first = new SessionCookieSigner("  ");
        SessionCookieSigner second = new SessionCookieSigner(null);

        assertEquals(7, first.verify(first.sign(7)).orElseThrow().intValue());
        assertNotEquals(first.sign(7), second.sign(7));
        assertTrue(second.verify(first.sign(7)).isEmpty());
    }
}
