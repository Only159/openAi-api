package cn.hzq.chatgml.interfaces;


import cn.hzq.chatgml.application.IWeiXinValidateService;
import cn.hzq.chatgml.domain.receive.model.MessageText;
import cn.hzq.chatgml.infrastructure.util.sdk.XmlUtil;
import cn.hzq.chatgml.model.ChatCompletionRequest;
import cn.hzq.chatgml.model.ChatCompletionSyncResponse;
import cn.hzq.chatgml.model.Model;
import cn.hzq.chatgml.model.Role;
import cn.hzq.chatgml.session.Configuration;
import cn.hzq.chatgml.session.OpenAiSession;
import cn.hzq.chatgml.session.OpenAiSessionFactory;
import cn.hzq.chatgml.session.defaults.DefaultOpenAiSessionFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * @author 黄照权
 * @Date 2024/4/13
 * @Description 微信公众号，请求处理服务
 **/
@Slf4j
@RestController
@RequestMapping("/wx/portal/{appid}")
public class WeiXinPortalController {

    @Value("${wx.config.originalid}")
    private String originalId;


    @Resource
    private IWeiXinValidateService weiXinValidateService;
    // openAi会话
    private final OpenAiSession openAiSession;
    // gpt对话缓存任务
    private final Cache<String, GptTaskInfo> gptTaskCache;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    private Map<String, String> chatGPTMap = new ConcurrentHashMap<>();

    public WeiXinPortalController() {

        // 1. 创建 OpenAi 会话
       /* Configuration configuration = Configuration
                .builder()
                .apiHost(aipHost)
                .apiSecretKey(aipKey)
                .level(HttpLoggingInterceptor.Level.BODY)
                .build();*/
        // 1. 配置文件
        Configuration configuration = new Configuration();
        configuration.setApiHost("https://open.bigmodel.cn/");
        configuration.setApiSecretKey("42e670f4c94232aafa9d2a6666ac286c.nLj8y9ZMex0HvaFB");
        configuration.setLevel(HttpLoggingInterceptor.Level.BODY);
        // 2. 会话工厂
        OpenAiSessionFactory factory = new DefaultOpenAiSessionFactory(configuration);
        // 3. 开启会话
        this.openAiSession = factory.openSession();

        // 创建 GPT 对话任务缓存
        this.gptTaskCache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();

        log.info("开始 openAiSession");
    }

    /**
     * 处理微信服务器发来的get请求，进行签名的验证
     *
     * @param signature 签名
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param echostr   随机字符串
     * @return 若验签成功则返回 {@code echostr}, 否则返回 null
     */
    @GetMapping
    public String validateSign(@RequestParam(value = "signature", required = false) String signature,
                               @RequestParam(value = "timestamp", required = false) String timestamp,
                               @RequestParam(value = "nonce", required = false) String nonce,
                               @RequestParam(value = "echostr", required = false) String echostr) {
        boolean success = weiXinValidateService.validateSign(signature, timestamp, nonce);
        return success ? echostr : null;
    }

    /**
     * 接收用户消息
     *
     * @param signature    签名
     * @param timestamp    时间戳
     * @param nonce        随机数
     * @param openid       OpenID
     * @param xmlStructure XML 结构
     * @return 公众号回复内容
     */
    @PostMapping
    public String receiveUserMessage(@RequestParam String signature,
                                     @RequestParam String timestamp,
                                     @RequestParam String nonce,
                                     @RequestParam String openid,
                                     @RequestBody String xmlStructure) throws InterruptedException {

        // 验签, 确认请求来源
        boolean valid = weiXinValidateService.validateSign(signature, timestamp, nonce);
        if (!valid) {
            return null;
        }
        // 提取用户消息
        MessageText messageText = XmlUtil.xmlToBean(xmlStructure, MessageText.class);
        String content = messageText.getContent();
        log.info("微信公众号接收用户消息: [OpenID: {}, Content: {}]", openid, content);

        // 获取 GPT 对话任务, 若不存在则创建并缓存
        GptTaskInfo gptTaskInfo = gptTaskCache.getIfPresent(openid);
        if (gptTaskInfo == null) {
            gptTaskInfo = new GptTaskInfo();
            gptTaskInfo.setFuture(submitChatGptTask(openid, content));
            gptTaskInfo.setRetryTimes(0);
            gptTaskCache.put(openid, gptTaskInfo);
        }

        // 处理 GPT 对话任务, 获取响应信息
        String response = null;
        try {
            Future<String> future = gptTaskInfo.getFuture();
            gptTaskInfo.addRetryTimes();
            // 每次请求的前 4 秒可回复消息, 超过则触发重试机制
            response = future.get(4, TimeUnit.SECONDS);
            log.info("AI 回复用户消息: [OpenID: {}, Response: {}]", openid, response);
            gptTaskCache.invalidate(openid);
        } catch (TimeoutException e) {
            // 利用微信公众号重试机制, 保证 3 次重试内回复消息即可
            if (gptTaskInfo.getRetryTimes() == 3) {
                response = "AI 仍在思考中, 请重新发送您的问题";
            } else if (gptTaskInfo.getRetryTimes() == 6) {
                response = "AI 被难住了, 换个问题试试吧";
                gptTaskCache.invalidate(openid);
            } else {
                // 使本次响应时间大于 5 秒, 触发重试机制
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.error("处理 GPT 对话任务异常: [OpenID: {}, Content: {}]", openid, content, e);
            response = "AI 处理异常, 请稍后重试";
            gptTaskCache.invalidate(openid);
        }

        // 构建回复消息
        MessageText responseMessage = MessageText
                .builder()
                .content(response)
                .toUserName(openid)
                .fromUserName(originalId)
                .createTime(String.valueOf(System.currentTimeMillis() / 1000))
                .msgType("text")
                .build();
        return XmlUtil.beanToXml(responseMessage);

    }


    /**
     * 提交 ChatGPT 任务
     */
    private Future<String> submitChatGptTask(String openid, String content) {
        return taskExecutor.submit(() -> {
            try {
                // OpenAI 请求
                // 1. 创建请求参数
                ChatCompletionRequest request = new ChatCompletionRequest();
                request.setModel(Model.GLM_4);
                request.setPrompt(new ArrayList<ChatCompletionRequest.Prompt>() {
                    private static final long serialVersionUID = -7988151926241837899L;

                    {
                        add(ChatCompletionRequest.Prompt.builder()
                                .role(Role.user.getCode())
                                .content(content)
                                .build());
                    }
                });
                request.setTools(new ArrayList<ChatCompletionRequest.Tool>() {
                    private static final long serialVersionUID = -7988151926241837899L;

                    {
                        add(ChatCompletionRequest.Tool.builder()
                                .type(ChatCompletionRequest.Tool.Type.web_search)
                                .webSearch(ChatCompletionRequest.Tool.WebSearch.builder().enable(true).searchQuery(content).build())
                                .build());
                    }
                });

                // 2. 发起请求
                ChatCompletionSyncResponse chatCompletionSyncResponse = openAiSession.completionsSync(request);

                String content1 = chatCompletionSyncResponse.getChoices().get(0).getMessage().getContent();
                return content1;
            } catch (Exception e) {
                log.error("OpenAI 请求异常: [OpenID: {}, Content: {}]", openid, content, e);

                return "请求异常, 请联系开发者";
            }
        });
    }

    /**
     * ChatGPT 任务信息
     */
    @Data
    private static class GptTaskInfo {

        private Future<String> future;
        private Integer retryTimes;

        public void addRetryTimes() {
            retryTimes++;
        }

    }

}
