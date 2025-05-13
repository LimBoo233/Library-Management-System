import com.ILoveU.dao.UserDAO;
import com.ILoveU.dao.impl.UserDaoImpl;
import com.ILoveU.model.User;

public class UserDaoTest {
    public static void main(String[] args) {
        UserDAO userDAO = new UserDaoImpl();
        boolean isAccountExists = userDAO.isAccountExists("rosmontis123");
        System.out.println(isAccountExists ? "Account Exists XD" : "Account Not Exists TxT ");

        User rosmontis = userDAO.findUserByAccount("rosmontis123");
        System.out.println("(｀・ω・´)User name:" + rosmontis.getName());

        User amiya = userDAO.findUserById(4);
        System.out.println("(｀・ω・´)find user by id: " + amiya.getName());
    }
}
