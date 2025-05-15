import com.ILoveU.dao.AuthorDAO;
import com.ILoveU.dao.impl.AuthorDAOImpl;

public class AuthorDAOTest {
    public static void main(String[] args) {
        AuthorDAO authorDAO = new AuthorDAOImpl();

        System.out.println(authorDAO.countTotalAuthors());
    }
}
