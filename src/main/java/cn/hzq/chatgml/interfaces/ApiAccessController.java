package cn.hzq.chatgml.interfaces;

import cn.hzq.chatgml.domain.security.service.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


/**
 * @author 黄照权
 * @Date 2024/3/21
 * @Description API 访问准入管理 当访问OpenAi接口时，需要进行验证
 **/
@RestController
public class ApiAccessController {

    private Logger logger = LoggerFactory.getLogger(ApiAccessController.class);

    @RequestMapping("/authorize")
    public ResponseEntity<Map<String,String>> authorize(String username,String password){
        Map<String, String> map = new HashMap<>();
        //模拟账号密码校验
        if (!"hzq".equals(username) || !"159357".equals(password)){
            map.put("msg","用户名或密码错误");
            return ResponseEntity.ok(map);
        }
        //验证通过生成token
        JwtUtil jwtUtil = new JwtUtil();
        Map<String, Object> chaim = new HashMap<>();
        chaim.put("username",username);
        String jwtToken = jwtUtil.encode(username, 5 * 60 * 1000, chaim);
        map.put("msg","授权成功");
        map.put("token", jwtToken);
        return ResponseEntity.ok(map);
    }

    @RequestMapping("/verify")
    public ResponseEntity<String> verify(String token) {
        logger.info("验证 token：{}", token);
        return ResponseEntity.status(HttpStatus.OK).body("verify success!");
    }

    @RequestMapping("/success")
    public String success(){
        return "test success by hzq";
    }

}
