package com.kiran.service;


import com.kiran.controller.dto.UserLogDTO.UserLogDTO;
import com.kiran.dao.UserLogDao;
import com.kiran.model.entity.UserLogEntity;
import com.kiran.translator.UserLogTranslator;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kiran
 * @since 9/5/17
 */


public class UserLogService {

    @Autowired
    private UserLogDao userLogDao;

    @Autowired
    private UserLogTranslator userLogTranslator;

    public UserLogEntity createUserLog(UserLogDTO userLogDTO) {

        UserLogEntity newUserLog = userLogTranslator.dtoToEntity(userLogDTO);

        return userLogDao.save(newUserLog);
    }

    public List<UserLogEntity> readAllUsers() {
        List<UserLogEntity> list = new ArrayList<UserLogEntity>();
        for (UserLogEntity item :  userLogDao.findAll()) {
            list.add(item);
        }
        return list;
    }

    public Iterable<UserLogEntity> readUserByFirstName(final String userName) {
        return userLogDao.findByUserName(userName);
    }


}
