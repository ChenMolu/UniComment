package com.rocky.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocky.dto.LoginFormDTO;
import com.rocky.dto.Result;
import com.rocky.entity.User;
import com.rocky.mapper.UserMapper;
import com.rocky.service.IUserService;
import com.rocky.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;


@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {



    @Resource
    private UserMapper userMapper;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机格式校验未通过!");
        }

        String code = RandomUtil.randomNumbers(6);

        session.setAttribute("code",code);
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
        Object code  = session.getAttribute("code");
        if(!code.equals(loginForm.getCode())){
            return Result.fail("验证码错误");
        }

        User user = query().eq("phone",phone).one();
        log.debug(user.toString());
        if(user == null){
            user  = createUserWithPhone(phone);
        }

        session.setAttribute("user",user);
        return Result.ok();
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
