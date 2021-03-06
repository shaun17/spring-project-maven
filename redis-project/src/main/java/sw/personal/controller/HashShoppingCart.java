package sw.personal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class HashShoppingCart {

    private final String userid = UUID.randomUUID().toString().replace("-", "");

    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping("/addcart")
    public void addCart(@NotNull int number){
        String proId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForHash().put(userid,proId, number+"");
    }
    @GetMapping("/cart")
    public Map cart(){
        return  redisTemplate.opsForHash().entries(userid);
    }
    @GetMapping("/delcart")
    public Long delCart(String proId){
        Long delete = redisTemplate.opsForHash().delete(userid, proId);
        return delete;
    }

}
