package com.ILoveU.service.Impl;

import com.ILoveU.dao.PressDAO;
import com.ILoveU.dao.impl.PressDAOImpl;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.model.Press;
import com.ILoveU.service.PressService;

import java.util.List;

public class PressServiceImpl implements PressService {
    private final PressDAO pressDAO;

    public PressServiceImpl() {
        this.pressDAO = new PressDAOImpl();
    }


    @Override
    public PageDTO<Press> getPressesWithPagination(int page, int pageSize) {
        // 获取当前页数据
        List<Press> data = pressDAO.findPresses(page, pageSize);
        // 获取总条数
        long total = pressDAO.countTotalPresses();
        // 组装分页对象
        return new PageDTO<>(data, total, page, pageSize);
    }

    @Override
    public Press getPressById(int pressId) {
        return pressDAO.findPressById(pressId);
    }

    @Override
    public Press createNewPress(Press pressToCreate) {
        return null;
    }

    @Override
    public Press updateExistingPress(int pressId, Press pressDetailsToUpdate) {
        return null;
    }

    @Override
    public boolean deletePressById(int pressId) {
        return false;
    }

}
