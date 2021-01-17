package com.community.service.impl;

import com.community.entity.LoginTicket;
import com.community.entity.User;
import com.community.mapper.LoginTicketMapper;
import com.community.mapper.UserMapper;
import com.community.service.UserService;
import com.community.utils.CommonUtil;
import com.community.utils.Constant;
import com.community.utils.MailClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final MailClient mailClient;
    private final TemplateEngine templateEngine;
    private final LoginTicketMapper loginTicketMapper;

    public UserServiceImpl(UserMapper userMapper, MailClient mailClient, TemplateEngine templateEngine, LoginTicketMapper loginTicketMapper) {
        this.userMapper = userMapper;
        this.mailClient = mailClient;
        this.templateEngine = templateEngine;
        this.loginTicketMapper = loginTicketMapper;
    }

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 根据用户 id 查询用户
     */
    @Override
    public User selectById(int id) {
        return userMapper.selectById(id);
    }


    /**
     * 根据用户名查询用户
     */
    @Override
    public User selectByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * 根据邮箱查询用户
     */
    @Override
    public User selectByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    /**
     * 添加用户
     */
    @Override
    public int insertUser(User user) {
        return userMapper.insertUser(user);
    }


    /**
     * 修改用户状态
     */
    @Override
    public int updateStatus(int userId, int status) {
        return userMapper.updateStatus(userId, status);
    }

    /**
     * 注册用户
     */
    @Override
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();
        // 空值判断
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("UsernameMessage", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("PasswordMessage", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("EmailMessage", "邮箱不能为空!");
            return map;
        }
        // 验证账号
        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser != null) {
            map.put("UsernameMessage", "该账号已存在!");
            return map;
        }
        // 验证邮箱
        dbUser = userMapper.selectByEmail(user.getEmail());
        if (dbUser != null) {
            map.put("EmailMessage", "该邮箱已被注册!");
            return map;
        }
        // 注册用户
        user.setSalt(CommonUtil.generateUUID().substring(0, 5));
        user.setPassword(CommonUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommonUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/activation/user_id/code
        String url = domain + contextPath + "activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "Community-激活账号", content);

        return map;
    }

    /**
     * 激活用户
     */
    @Override
    public int activation(int userId, String code) {
        User dbUser = userMapper.selectById(userId);
        if (dbUser.getStatus() == 1) {
            return Constant.ACTIVATION_REPEAT;
        } else if (dbUser.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            return Constant.ACTIVATION_SUCCESS;
        } else {
            return Constant.ACTIVATION_FAILURE;
        }
    }

    /**
     * 用户登录
     *
     * @param username 登录账号
     * @param password 明文密码。数据库里存的是加密后的密码
     * @param expired  凭证过期时间
     */
    @Override
    public Map<String, Object> login(String username, String password, int expired) {
        Map<String, Object> map = new HashMap<>();

        // 空值判断
        if (StringUtils.isBlank(username)) map.put("UsernameMessage", "账号不能为空!");
        if (StringUtils.isBlank(password)) map.put("PasswordMessage", "密码不能为空!");

        // 账号验证
        User dbUser = userMapper.selectByUsername(username);
        if (dbUser == null) {
            map.put("UsernameMessage", "该账号不存在!");
            return map;
        }
        if (dbUser.getStatus() == 0) {
            map.put("UsernameMessage", "该账号未激活!");
            return map;
        }
        // 密码验证
        password = CommonUtil.md5(password + dbUser.getSalt());
        if (!dbUser.getPassword().equals(password)) {
            map.put("PasswordMessage", "密码错误!");
            return map;
        }

        // 生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(dbUser.getId());
        loginTicket.setTicket(CommonUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expired * 1000L));
        loginTicketMapper.insertLoginTicket(loginTicket);

        map.put("ticket", loginTicket.getTicket());

        return map;
    }

    /**
     * 用户退出
     */
    @Override
    public int logout(String ticket) {
        return loginTicketMapper.updateStatus(ticket, 1);
    }

    /**
     * 根据 ticket 查询用户
     */
    @Override
    public LoginTicket selectByTicket(String ticket) {
        return loginTicketMapper.selectByTicket(ticket);
    }

}
