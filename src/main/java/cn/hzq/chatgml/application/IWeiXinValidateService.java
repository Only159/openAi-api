package cn.hzq.chatgml.application;

/**
 * @author 黄照权
 * @Date 2024/4/13
 * @Description 微信公众号验签服务
 **/
public interface IWeiXinValidateService {
    boolean validateSign(String signature, String timestamp, String nonce);
}
