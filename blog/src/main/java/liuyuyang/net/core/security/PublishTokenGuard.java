package liuyuyang.net.core.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import liuyuyang.net.core.execption.CustomException;
import liuyuyang.net.model.ArticleConfig;
import liuyuyang.net.web.mapper.ArticleConfigMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 发布令牌的草稿边界校验：只能修改或删除已经是草稿的文章。
 */
@Component
public class PublishTokenGuard {
    @Resource
    private ArticleConfigMapper articleConfigMapper;

    public void assertDraft(Integer articleId) {
        if (articleId == null) {
            throw new CustomException(400, "文章 ID 不能为空");
        }

        ArticleConfig config = articleConfigMapper.selectOne(
                new LambdaQueryWrapper<ArticleConfig>()
                        .eq(ArticleConfig::getArticleId, articleId)
                        .last("LIMIT 1"));

        if (config == null) {
            throw new CustomException(404, "文章不存在或缺少配置");
        }
        if (!Boolean.TRUE.equals(config.getIsDraft())) {
            throw new CustomException(403, "发布令牌只能修改或删除草稿文章");
        }
    }
}
