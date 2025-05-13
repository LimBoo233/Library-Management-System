import com.ILoveU.model.User;
import com.ILoveU.util.HibernateUtil;
import org.hibernate.Session;

public class TestHibernate {
    public static void main(String[] args) {
        Session session = HibernateUtil.getSessionFactory().openSession();

        User user = new User();
        user.setName("Rosmontis");
        user.setAccount("rosmontis123");
        user.setPassword("123456");

        session.beginTransaction();
        session.save(user);
        session.getTransaction().commit();

        session.close();
        HibernateUtil.shutdown();
    }
}