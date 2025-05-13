package com.ILoveU.dao;

import com.ILoveU.model.Press;

import java.util.List;

public interface PressDAO {

    public List<Press> findPresses(int page, int pageSize);

    public Press findPressById(int pressId);

    public Press addPress(Press press);

    public Press updatePress(Press press);

    public boolean deletePress(int pressId);


    public long countTotalPresses();

}
