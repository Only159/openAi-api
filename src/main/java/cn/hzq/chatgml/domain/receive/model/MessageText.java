package cn.hzq.chatgml.domain.receive.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author 黄照权
 * @Date 2024/4/14
 * @Description 微信公共号消息文本
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageText {
    // 消息ID
    private String msgId;
    // 开发者微信号
    private String toUserName;
    // 发送方OpenId
    private String fromUserName;
    // 消息创建时间
    private String createTime;
    // 消息类型
    private String msgType;
    // 文本消息内容
    private String content;
}
