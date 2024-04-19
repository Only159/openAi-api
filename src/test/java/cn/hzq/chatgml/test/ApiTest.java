package cn.hzq.chatgml.test;

import cn.hzq.chatgml.domain.security.service.JwtUtil;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 黄照权
 * @Date 2024/3/21
 * @Description 单元测试
 **/
public class ApiTest {
    @Test
    public void test_jwt() {
        // 以tom作为秘钥，以HS256加密
        JwtUtil util = new JwtUtil("tom", SignatureAlgorithm.HS256);
        Map<String, Object> map = new HashMap<>();
        map.put("username", "hzq");
        map.put("password", "123");
        map.put("age", 100);
        String jwtToken = util.encode("hzq", 30000, map);

        util.decode(jwtToken).forEach((key, value) -> System.out.println(key + ": " + value));
    }

}
