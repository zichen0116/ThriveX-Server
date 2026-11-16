package liuyuyang.net.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 发布令牌工具：只保存 SHA-256 摘要，使用恒定时间比较，避免在日志中输出令牌明文。
 */
public final class PublishTokenUtils {
    private static final Logger log = LoggerFactory.getLogger(PublishTokenUtils.class);

    /**
     * 请求属性：标记当前请求已通过发布令牌校验。
     */
    public static final String REQUEST_ATTRIBUTE = PublishTokenUtils.class.getName() + ".ACTIVE";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SHA256_HEX_PATTERN = "[0-9a-f]{64}";

    private static volatile List<String> tokenHashes = Collections.emptyList();

    private PublishTokenUtils() {
    }

    /**
     * 设置允许的令牌 SHA-256 摘要，支持英文逗号分隔多个（用于轮换期间的平滑过渡）。
     */
    public static void setHashes(String rawHashes) {
        if (rawHashes == null || rawHashes.trim().isEmpty()) {
            tokenHashes = Collections.emptyList();
            return;
        }

        Set<String> hashes = new LinkedHashSet<>();
        for (String part : rawHashes.split(",")) {
            String hash = part.trim().toLowerCase(Locale.ROOT);
            if (hash.isEmpty()) {
                continue;
            }
            if (!hash.matches(SHA256_HEX_PATTERN)) {
                log.warn("忽略格式错误的发布令牌 SHA-256 配置：应为 64 位十六进制字符串");
                continue;
            }
            hashes.add(hash);
        }
        tokenHashes = Collections.unmodifiableList(new ArrayList<>(hashes));
    }

    public static boolean isEnabled() {
        return !tokenHashes.isEmpty();
    }

    /**
     * 判断请求头中的令牌是否匹配任一已配置的发布令牌。
     */
    public static boolean matches(String rawToken) {
        String token = stripBearer(rawToken);
        if (token.isEmpty() || !isEnabled()) {
            return false;
        }

        byte[] actual = sha256Hex(token).getBytes(StandardCharsets.US_ASCII);
        for (String expected : tokenHashes) {
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), actual)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 当前请求是否使用发布令牌。优先读取拦截器写入的标记，未经过拦截器的公开接口再回退到请求头校验。
     */
    public static boolean isCurrentRequestPublishToken() {
        HttpServletRequest request = CommonUtils.getRequest();
        if (request == null) {
            return false;
        }
        if (Boolean.TRUE.equals(request.getAttribute(REQUEST_ATTRIBUTE))) {
            return true;
        }
        return matches(request.getHeader("Authorization"));
    }

    /**
     * 返回令牌摘要的前 8 位，仅用于日志排查，不能反推出令牌。
     */
    public static String fingerprint(String rawToken) {
        String token = stripBearer(rawToken);
        if (token.isEmpty()) {
            return "-";
        }
        return sha256Hex(token).substring(0, 8);
    }

    public static String stripBearer(String rawToken) {
        if (rawToken == null) {
            return "";
        }
        String token = rawToken.trim();
        if (token.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            token = token.substring(BEARER_PREFIX.length()).trim();
        }
        return token;
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", e);
        }
    }
}
