package liuyuyang.net.core.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublishTokenUtilsTest {
    private static final String TOKEN = "muse-publish-token-for-unit-test";

    @AfterEach
    public void tearDown() {
        PublishTokenUtils.setHashes(null);
    }

    @Test
    public void sha256MatchesStandardHexVector() {
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
                PublishTokenUtils.sha256Hex("test"));
    }

    @Test
    public void matchesRawAndBearerToken() {
        PublishTokenUtils.setHashes(PublishTokenUtils.sha256Hex(TOKEN));

        assertTrue(PublishTokenUtils.matches(TOKEN));
        assertTrue(PublishTokenUtils.matches("Bearer " + TOKEN));
        assertTrue(PublishTokenUtils.matches("bearer " + TOKEN));
    }

    @Test
    public void supportsMultipleHashesForRotation() {
        String nextToken = TOKEN + "-next";
        PublishTokenUtils.setHashes(PublishTokenUtils.sha256Hex(TOKEN) + ","
                + PublishTokenUtils.sha256Hex(nextToken).toUpperCase());

        assertTrue(PublishTokenUtils.matches(TOKEN));
        assertTrue(PublishTokenUtils.matches(nextToken));
    }

    @Test
    public void rejectsWrongEmptyAndDisabledTokens() {
        PublishTokenUtils.setHashes(PublishTokenUtils.sha256Hex(TOKEN));

        assertFalse(PublishTokenUtils.matches("wrong-token"));
        assertFalse(PublishTokenUtils.matches(""));
        assertFalse(PublishTokenUtils.matches(null));

        PublishTokenUtils.setHashes(null);
        assertFalse(PublishTokenUtils.matches(TOKEN));
    }

    @Test
    public void ignoresMalformedHashes() {
        PublishTokenUtils.setHashes("not-a-sha256," + PublishTokenUtils.sha256Hex(TOKEN));

        assertTrue(PublishTokenUtils.matches(TOKEN));
    }
}
