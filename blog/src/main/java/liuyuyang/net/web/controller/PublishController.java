package liuyuyang.net.web.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import liuyuyang.net.core.utils.PublishTokenUtils;
import liuyuyang.net.core.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 发布令牌自检接口，便于自动化脚本在正式发布前确认令牌和权限是否可用。
 */
@Api(tags = "发布令牌")
@RestController
@RequestMapping("/publish")
public class PublishController {

    @GetMapping("/check")
    @ApiOperation("校验发布令牌并返回可用权限")
    @ApiOperationSupport(order = 1)
    public Result<Map<String, Object>> check() {
        boolean publishToken = PublishTokenUtils.isCurrentRequestPublishToken();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tokenType", publishToken ? "publish" : "admin");
        data.put("scopes", publishToken
                ? Arrays.asList(
                        "article:create-draft",
                        "article:update-draft",
                        "article:delete-draft",
                        "file:upload",
                        "cate:read",
                        "tag:read")
                : Collections.singletonList("admin:*"));
        return Result.success(data);
    }
}
