package com.naelir.spadhtview;

import java.util.Base64;

/**
 * Validates time-based read tokens for GET endpoints.
 *
 * <p>A valid token is a <b>URL-safe Base64</b> (no padding) encoded string whose
 * decoded bytes represent the current epoch time in milliseconds (a {@code long},
 * big-endian 8 bytes).  URL-safe Base64 uses {@code -} and {@code _} instead of
 * {@code +} and {@code /}, and omits {@code =} padding – so the token can be
 * appended to a URL query string without any percent-encoding.
 *
 * <p>JavaScript counterpart (in index.html):
 * <pre>
 *   function genToken() {
 *       const buf = new ArrayBuffer(8);
 *       const view = new DataView(buf);
 *       view.setBigInt64(0, BigInt(Date.now()), false); // big-endian
 *       return btoa(String.fromCharCode(...new Uint8Array(buf)))
 *           .replace(/\+/g, '-')
 *           .replace(/\//g, '_')
 *           .replace(/=+$/, '');
 *   }
 * </pre>
 */
public final class TokenValidator {

    /** Allowed clock-skew window in milliseconds (1 minute). */
    private static final long WINDOW_MS = 60_000L;

    private TokenValidator() {}

    /**
     * Returns {@code true} when {@code token} is a Base64-encoded big-endian
     * {@code long} whose value is within {@link #WINDOW_MS} of now.
     */
    public static boolean isValidReadToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(token);
            if (bytes.length != 8) {
                return false;
            }
            long timestamp = 0L;
            for (byte b : bytes) {
                timestamp = (timestamp << 8) | (b & 0xFF);
            }
            long diff = Math.abs(System.currentTimeMillis() - timestamp);
            return diff <= WINDOW_MS;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
