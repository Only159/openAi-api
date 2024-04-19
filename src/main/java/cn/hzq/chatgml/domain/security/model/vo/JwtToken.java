package cn.hzq.chatgml.domain.security.model.vo;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * @author 黄照权
 * @Date 2024/3/21
 * @Description Token的对象信息
 **/
public class JwtToken implements AuthenticationToken {
    private String jwt;

    public JwtToken(String jwt) {
        this.jwt = jwt;
    }

    /**
     * 等同于账户
     */
    @Override
    public Object getPrincipal() {
        return jwt;
    }

    /**
     * 等同于密码
     */
    @Override
    public Object getCredentials() {
        return jwt;
    }
}
