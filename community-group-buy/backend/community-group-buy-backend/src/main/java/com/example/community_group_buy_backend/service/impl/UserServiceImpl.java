package com.example.community_group_buy_backend.service.impl;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.example.community_group_buy_backend.entity.User;
import com.example.community_group_buy_backend.mapper.UserMapper;
import com.example.community_group_buy_backend.service.UserService;
import com.example.community_group_buy_backend.vo.UserVO;

@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public List<UserVO> list() {
        return userMapper.findAll().stream().map(this::toVO).toList();
    }

    @Override
    public UserVO get(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO update(User user) {
        userMapper.update(user);
        return get(user.getUserId());
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        userMapper.updateStatus(userId, status);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
