package com.rocky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocky.dto.LoginFormDTO;
import com.rocky.dto.Result;
import com.rocky.dto.UserDTO;
import com.rocky.entity.User;
import com.rocky.mapper.UserMapper;
import com.rocky.service.IUserService;
import com.rocky.utils.RedisConstants;
import com.rocky.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.rocky.utils.RedisConstants.*;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Resource
    private UserMapper userMapper;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号格式
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机格式校验未通过!");
        }
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        //将验证码存入到redis中，并设置有效时间
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("验证码生成成功："+code);
        return Result.ok("验证码发送成功！");
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号格式
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机格式校验未通过!");
        }
        //校验验证码是否正确
        Object cachecode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY+phone);
        String code = loginForm.getCode();
        if(cachecode == null && !cachecode.toString().equals(code)){
            return Result.fail("验证码错误");
        }

        User user = query().eq("phone",phone).one();
        log.debug(user.toString());
        if(user == null){
            user  = createUserWithPhone(phone);
        }

        // 保存用户信息到 redis中
        // 随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        // 将User对象转为HashMap存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //StringRedisTemplate只支持string类型，而id是Long类型，所以会出现转换异常
        //转换Map时转化成String类型
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 存储
        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        // 设置token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    @Override
    public User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName("ali");
        save(user);
        log.debug("添加用户成功"+user.toString());
        return user;
    }

}
