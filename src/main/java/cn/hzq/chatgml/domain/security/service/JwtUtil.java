package cn.hzq.chatgml.domain.security.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author 黄照权
 * @Date 2024/3/21
 * @Description 获取JwtToken，获取JwtToken中封装的信息，判断JwtToken是否存在
 **/
public class JwtUtil {
    // 创建默认的秘钥和算法，供无参的构造方法使用
    private static final String defaultBase64EncodedSecretKey = "B*B^";
    private static final SignatureAlgorithm defaultSignatureAlgorithm = SignatureAlgorithm.HS256;
    public JwtUtil() {
        this(defaultBase64EncodedSecretKey, defaultSignatureAlgorithm);
    }
    private final String base64EncodedSecretKey;
    private final SignatureAlgorithm signatureAlgorithm;

    /**
     * 传递密钥和算法
     * @param secretKey 密钥
     * @param signatureAlgorithm 算法
     */
    public JwtUtil(String secretKey, SignatureAlgorithm signatureAlgorithm) {
        this.base64EncodedSecretKey = Base64.encodeBase64String(secretKey.getBytes());
        this.signatureAlgorithm = signatureAlgorithm;
    }

    /**
     * 这里就是产生jwt字符串的地方
     * @param issuer 签发人 一般都是username或者userId
     * @param ttlMillis 生存时间（单位：毫秒）
     * @param claims 还想要在jwt中存储的一些非隐私信息
     * @return JwtToken对应的字符串
     */
    public String encode(String issuer, long ttlMillis, Map<String, Object> claims) {
        if (claims == null) {
            claims = new HashMap<>();
        }
        //获取当前时间
        long nowMillis = System.currentTimeMillis();

        JwtBuilder builder = Jwts.builder()
                // 荷载部分，想要存储的额外信息
                .setClaims(claims)
                // 这个是JWT的唯一标识，一般设置成唯一的，使用UUID生成随机的唯一ID
                .setId(UUID.randomUUID().toString())
                // 签发时间
                .setIssuedAt(new Date(nowMillis))
                // 签发人，也就是JWT是给谁的（逻辑上一般都是username或者userId）
                .setSubject(issuer)
                // 设置生成jwt使用的算法和秘钥
                .signWith(signatureAlgorithm, base64EncodedSecretKey);
        if (ttlMillis >= 0) {
            //4. 设置过期时间，这个也是使用毫秒生成的，使用当前时间+前面传入的持续时间生成
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }
        //返回构建的jwt字符串
        return builder.compact();
    }
    /**
     * 获取jwtToken中的载荷部分
     * @param jwtToken jwtToken字符串
     * @return 荷载部分所有的键值对
     */
    public Claims decode(String jwtToken) {
        // 得到 DefaultJwtParser
        return Jwts.parser()
                // 设置签名的秘钥
                .setSigningKey(base64EncodedSecretKey)
                // 设置需要解析的 jwt
                .parseClaimsJws(jwtToken)
                .getBody();
    }

    /**
     * 验证token是否存在
     * @param jwtToken jwtToken字符串
     * @return boolean
     */
    public boolean isVerify(String jwtToken) {
        // 这个是官方的校验规则，这里只写了一个”校验算法“，可以自己加
        Algorithm algorithm = null;
        switch (signatureAlgorithm) {
            case HS256:
                algorithm = Algorithm.HMAC256(Base64.decodeBase64(base64EncodedSecretKey));
                break;
            default:
                throw new RuntimeException("不支持该算法");
        }
        JWTVerifier verifier = JWT.require(algorithm).build();
        verifier.verify(jwtToken);
        // 校验不通过会抛出异常
        // TODO 判断合法的标准：1. 头部和荷载部分没有篡改过。2. 没有过期
        return true;
    }
}
