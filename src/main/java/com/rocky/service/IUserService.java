package com.rocky.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rocky.dto.LoginFormDTO;
import com.rocky.dto.Result;
import com.rocky.entity.User;

import javax.servlet.http.HttpSession;


public interface IUserService extends IService<User> {

    public Result sendCode (String phone, HttpSession session);

    public Result login(LoginFormDTO loginForm, HttpSession session);

    public User createUserWithPhone(String phone);

}
