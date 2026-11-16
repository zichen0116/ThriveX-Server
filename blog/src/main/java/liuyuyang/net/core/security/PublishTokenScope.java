package liuyuyang.net.core.security;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 发布令牌的最小权限白名单。
 * <p>
 * 只允许：建草稿、改草稿、删草稿、上传图片、读取文章/分类/标签，以及令牌自检。
 * 其他管理接口（用户、配置、评论、文件删除等）一律拒绝。
 */
public final class PublishTokenScope {
    private static final Pattern ARTICLE_ID = Pattern.compile("^/api/article/(\\d+)$");
    private static final Pattern ARTICLE_DELETE = Pattern.compile("^/api/article/(\\d+)/(\\d+)$");
    private static final Pattern CATE_ID = Pattern.compile("^/api/cate/(\\d+)$");
    private static final Pattern TAG_ID = Pattern.compile("^/api/tag/(\\d+)$");

    private PublishTokenScope() {
    }

    /**
     * 取出去掉 context-path 和尾部斜杠后的请求路径。
     */
    public static String resolvePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path == null || path.isEmpty()) {
            return "/";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static boolean isAllowed(String method, String path) {
        if (method == null || path == null) {
            return false;
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        switch (method.toUpperCase(Locale.ROOT)) {
            case "POST":
                // 新建文章（控制器会强制为草稿）与图片上传
                return path.equals("/api/article") || path.equals("/api/file");
            case "PATCH":
                // 编辑文章（控制器会要求目标必须是草稿）
                return path.equals("/api/article");
            case "DELETE":
                // 删除文章（控制器会要求目标必须是草稿）
                return ARTICLE_DELETE.matcher(path).matches();
            case "GET":
                // 读取文章/分类/标签，以及令牌自检
                return path.equals("/api/article")
                        || ARTICLE_ID.matcher(path).matches()
                        || path.equals("/api/cate")
                        || CATE_ID.matcher(path).matches()
                        || path.equals("/api/tag")
                        || TAG_ID.matcher(path).matches()
                        || path.equals("/api/publish/check");
            default:
                return false;
        }
    }

    /**
     * 从 DELETE /api/article/{id}/{is_del} 中取出文章 ID，供草稿校验使用。
     */
    public static Integer extractDraftArticleIdForDelete(String method, String path) {
        if (method == null || path == null || !"DELETE".equalsIgnoreCase(method)) {
            return null;
        }
        Matcher matcher = ARTICLE_DELETE.matcher(path);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Integer.valueOf(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
