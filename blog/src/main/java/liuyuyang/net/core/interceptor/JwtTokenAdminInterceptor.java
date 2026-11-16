package liuyuyang.net.core.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import liuyuyang.net.core.annotation.NoTokenRequired;
import liuyuyang.net.core.execption.CustomException;
import liuyuyang.net.core.properties.JwtProperties;
import liuyuyang.net.core.security.PublishTokenScope;
import liuyuyang.net.core.utils.BlackListUtils;
import liuyuyang.net.core.utils.IpUtils;
import liuyuyang.net.core.utils.JwtUtils;
import liuyuyang.net.core.utils.PublishTokenUtils;
import liuyuyang.net.model.UserToken;
import liuyuyang.net.web.mapper.UserTokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;

/**
 * jwt令牌校验的拦截器
 */
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {
    @Resource
    private JwtProperties jwtProperties;
    @Resource
    private UserTokenMapper userTokenMapper;
    @Resource
    private BlackListUtils blackListUtils;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());

        // 获取客户端IP
        String ip = IpUtils.getRealIp(request);

        // 检查IP是否在黑名单中
        if (blackListUtils.isBlacklisted(ip)) {
            log.warn("IP {} 已被加入黑名单", ip);
            throw new CustomException("您已被加入黑名单，请稍后再试");
        }

        // 如果是预检请求，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        Method method = handlerMethod.getMethod();

        // 检查方法上是否有@NoTokenRequired注解，如果有就直接放行
        if (method.isAnnotationPresent(NoTokenRequired.class)) {
            return true;
        }

        // 发布令牌：独立于网页登录 token，不做 JWT 解析，只按方法 + 路径做最小权限校验
        if (PublishTokenUtils.matches(token)) {
            String path = PublishTokenScope.resolvePath(request);
            if (!PublishTokenScope.isAllowed(request.getMethod(), path)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                log.warn("发布令牌无权访问：{} {}（指纹 {}）", request.getMethod(), path,
                        PublishTokenUtils.fingerprint(token));
                throw new CustomException(403, "发布令牌无权访问该接口");
            }
            request.setAttribute(PublishTokenUtils.REQUEST_ATTRIBUTE, Boolean.TRUE);
            log.info("发布令牌校验通过：{} {}（指纹 {}）", request.getMethod(), path,
                    PublishTokenUtils.fingerprint(token));
            return true;
        }

        // 校验登录令牌
        try {
            if (token == null || token.trim().isEmpty()) {
                throw new CustomException(401, "请先登录");
            }

            // 处理 Authorization 的 Bearer 前缀
            token = PublishTokenUtils.stripBearer(token);
            if (token.isEmpty()) {
                throw new CustomException(401, "无效或过期的 Token");
            }

            log.info("JWT 校验（指纹 {}）", PublishTokenUtils.fingerprint(token));

            LambdaQueryWrapper<UserToken> userTokenLambdaQueryWrapper = new LambdaQueryWrapper<>();
            userTokenLambdaQueryWrapper.eq(UserToken::getToken, token);
            List<UserToken> userTokens = userTokenMapper.selectList(userTokenLambdaQueryWrapper);

            // 如果跟之前的token相匹配则进一步判断token是否有效
            if (userTokens != null && !userTokens.isEmpty()) {
                JwtUtils.parseJWT(token);
                return true;
            } else {
                throw new CustomException(401, "该账号已在另一台设备登录");
            }
        } catch (Exception ex) {
            log.warn("令牌校验失败：{}", ex.getMessage());
            // 校验失败，响应401状态码
            response.setStatus(401);
            String message = ex.getMessage() != null ? ex.getMessage() : "无效或过期的token";
            throw new CustomException(401, message);
        }
    }
}