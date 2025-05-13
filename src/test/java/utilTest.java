import com.ILoveU.util.HibernateUtil;
import org.hibernate.SessionFactory;

import java.sql.Connection;

public class utilTest {

    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    }

}
