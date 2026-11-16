package liuyuyang.net.core.config;

import liuyuyang.net.core.properties.PublishTokenProperties;
import liuyuyang.net.core.utils.PublishTokenUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 在单例初始化完成后把发布令牌摘要交给静态工具类，供拦截器和 CommonUtils 使用。
 */
@Slf4j
@AllArgsConstructor
@EnableConfigurationProperties({PublishTokenProperties.class})
@Configuration
public class PublishTokenConfiguration implements SmartInitializingSingleton {
    private final PublishTokenProperties publishTokenProperties;

    @Override
    public void afterSingletonsInstantiated() {
        PublishTokenUtils.setHashes(publishTokenProperties.getSha256());
        if (PublishTokenUtils.isEnabled()) {
            log.info("发布令牌已启用：仅允许草稿文章创建/编辑/删除、图片上传与分类标签读取");
        } else {
            log.warn("发布令牌未启用：未配置 blog.publish-token.sha256 或 PUBLISH_TOKEN_SHA256");
        }
    }
}
