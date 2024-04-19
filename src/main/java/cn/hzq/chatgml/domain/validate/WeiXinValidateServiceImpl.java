package cn.hzq.chatgml.domain.validate;

import cn.hutool.core.util.StrUtil;
import cn.hzq.chatgml.application.IWeiXinValidateService;
import cn.hzq.chatgml.infrastructure.util.sdk.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author 黄照权
 * @Date 2024/4/13
 * @Description 微信公众号验签服务实现类
 **/
@Service
@Slf4j
public class WeiXinValidateServiceImpl implements IWeiXinValidateService {

    @Value("${wx.config.token}")
    private String token;

    @Override
    public boolean validateSign(String signature, String timestamp, String nonce) {
        try{
            log.info("微信公众号签名验证开始：【signature:{},timestamp:{},nonce:{}】", signature, timestamp, nonce);
            if (StrUtil.hasBlank(signature, timestamp, nonce)) {
                throw new IllegalArgumentException("验证参数非法");
            }
            boolean success = SignatureUtil.validateSign(token, signature, timestamp, nonce);
            log.info("微信公众号签名验证结束：{}", success ? "成功" : "失败");
            return success;
        }catch (Exception e){
            log.error("微信公众号签名验证失败，【signature:{},timestamp:{},nonce:{}】",signature, timestamp, nonce,e);
            return false;
        }
    }
}
