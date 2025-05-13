package com.ILoveU.dao;

import com.ILoveU.model.User;

public interface UserDAO {

    /**
     * add new user to database (for register)
     * @param user the user to be added, not include userId (automatically created by database)
     * @return return User if succeed，else return null
     */
    public User addUser(User user);

    public User findUserById(int id);

    public User findUserByAccount(String account);

    public boolean isAccountExists(String account);

}
