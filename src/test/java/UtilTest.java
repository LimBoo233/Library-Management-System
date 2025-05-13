import com.ILoveU.util.HibernateUtil;
import org.hibernate.SessionFactory;

public class UtilTest {

    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    }

}
