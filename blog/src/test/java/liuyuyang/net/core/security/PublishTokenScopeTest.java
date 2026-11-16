package liuyuyang.net.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PublishTokenScopeTest {

    @Test
    public void allowsDraftPublishingOperations() {
        assertTrue(PublishTokenScope.isAllowed("POST", "/api/article"));
        assertTrue(PublishTokenScope.isAllowed("POST", "/api/article/"));
        assertTrue(PublishTokenScope.isAllowed("PATCH", "/api/article"));
        assertTrue(PublishTokenScope.isAllowed("DELETE", "/api/article/12/0"));
        assertTrue(PublishTokenScope.isAllowed("POST", "/api/file"));

        assertTrue(PublishTokenScope.isAllowed("GET", "/api/article"));
        assertTrue(PublishTokenScope.isAllowed("GET", "/api/article/12"));
        assertTrue(PublishTokenScope.isAllowed("GET", "/api/cate"));
        assertTrue(PublishTokenScope.isAllowed("GET", "/api/cate/68"));
        assertTrue(PublishTokenScope.isAllowed("GET", "/api/tag/3"));
        assertTrue(PublishTokenScope.isAllowed("GET", "/api/publish/check"));
    }

    @Test
    public void rejectsEverythingElse() {
        assertFalse(PublishTokenScope.isAllowed("PATCH", "/api/user"));
        assertFalse(PublishTokenScope.isAllowed("DELETE", "/api/file"));
        assertFalse(PublishTokenScope.isAllowed("POST", "/api/article/import"));
        assertFalse(PublishTokenScope.isAllowed("GET", "/api/env-config"));
        assertFalse(PublishTokenScope.isAllowed("DELETE", "/api/article/batch"));
        assertFalse(PublishTokenScope.isAllowed("PUT", "/api/article"));
        assertFalse(PublishTokenScope.isAllowed("GET", "/api/article/12/like"));
    }

    @Test
    public void extractsDeleteArticleIdOnlyForDeletePattern() {
        assertEquals(Integer.valueOf(42),
                PublishTokenScope.extractDraftArticleIdForDelete("DELETE", "/api/article/42/0"));
        assertNull(PublishTokenScope.extractDraftArticleIdForDelete("GET", "/api/article/42/0"));
        assertNull(PublishTokenScope.extractDraftArticleIdForDelete("DELETE", "/api/article/batch"));
        assertNull(PublishTokenScope.extractDraftArticleIdForDelete("DELETE", "/api/article/42"));
    }
}
