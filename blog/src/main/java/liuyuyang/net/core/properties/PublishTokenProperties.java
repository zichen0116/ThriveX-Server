package liuyuyang.net.core.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 发布令牌配置。
 * <p>
 * 服务端只保存令牌的 SHA-256 摘要，不保存令牌明文。原始令牌由使用者保存在密码保险库中，
 * 通过 {@code Authorization: Bearer <token>} 传给服务端。
 */
@Component
@ConfigurationProperties(prefix = "blog.publish-token")
@Data
public class PublishTokenProperties {
    /**
     * 发布令牌的 SHA-256 十六进制摘要，多个摘要用英文逗号分隔；为空表示禁用发布令牌。
     */
    private String sha256;
}
