package cn.hzq.chatgml.infrastructure.util.sdk;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.crypto.SecureUtil;

import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * @Author HZQ
 * @Date  2024/4/14
 * @Description 微信公众号签名工具类
*/
public class SignatureUtil {


    /**
     * @Author HZQ
     * @Date  2024/4/14
     * @Description 校验签名  用 Hutool 简化sha1加密操作
     * @param  token  开发者填写的任意值
     * @param signature 微信加密签名，结合了开发者填写的token和请求中的timestamp，nonce
     * @param nonce 随机戳
     * @param timestamp 时间戳
     * @return 签名是否校验成功
    */
    public static boolean validateSign(String token, String signature, String timestamp, String nonce){
        try{
            // 1. 将token timestamp ,nonce 三个参数进行字典排序
            String[] tmp = {token, timestamp, nonce};
            Arrays.sort(tmp);
            // 2. 将参数参数字符串拼接进行sha1加密
            String sha1 = SecureUtil.sha1(ArrayUtil.join(tmp, ""));
            // 3. 开发者获得加密后的字符串与signature进行比对，标识该请求来源与微信
            return sha1.equals(signature);
        }catch (Exception e){
            return false;
        }
    }
}
