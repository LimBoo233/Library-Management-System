import com.ILoveU.dao.AuthorDAO;
import com.ILoveU.dao.BookDAO;
import com.ILoveU.dao.PressDAO;
import com.ILoveU.dao.TagDAO;
import com.ILoveU.dao.impl.AuthorDAOImpl;
import com.ILoveU.dao.impl.BookDAOImpl;
import com.ILoveU.dao.impl.PressDAOImpl;
import com.ILoveU.dao.impl.TagDAOImpl;
import com.ILoveU.model.Author;
import com.ILoveU.model.Book;
import com.ILoveU.model.Press;
import com.ILoveU.model.Tag;
import com.ILoveU.util.HibernateUtil;

import java.util.HashSet;
import java.util.Set;

public class BookDAOTest {

    public static void main(String[] args) {
        // 1. 实例化所有需要的DAO
        BookDAO bookDAO = new BookDAOImpl();
        PressDAO pressDAO = new PressDAOImpl();
        AuthorDAO authorDAO = new AuthorDAOImpl();
        TagDAO tagDAO = new TagDAOImpl();

        System.out.println("--- 开始 BookDAO addBook 方法简单测试 ---");

        Press testPress = null;
        Author testAuthor = null;
        Tag testTag = null;

        try {

            // 3. 创建一个新的 Book 对象并设置属性
            Book newBook = new Book();
            newBook.setIsbn("2998887776635"); // 使用一个唯一的ISBN进行测试
            newBook.setTitle("我的第二本测试图书");
            newBook.setPublishYear(2025);
            newBook.setNumCopiesTotal(20);
            newBook.setNumCopiesAvailable(18);

            // 4. 关联已保存的出版社、作者和标签
            testPress = pressDAO.findPressById(1);
            newBook.setPress(testPress); // 关联出版社

            testAuthor = authorDAO.findAuthorById(1);
            Set<Author> authorsForBook = new HashSet<>();
            authorsForBook.add(testAuthor); // 添加作者
            newBook.setAuthors(authorsForBook);

            testTag = tagDAO.findTagById(1);
            Set<Tag> tagsForBook = new HashSet<>();
            tagsForBook.add(testTag); // 添加标签
            newBook.setTags(tagsForBook);

            // 5. 调用 bookDAO.addBook() 保存图书
            Book savedBook = bookDAO.addBook(newBook);
            if (savedBook != null) {
                System.out.println("图书添加成功，ID: " + savedBook.getBookId());
            } else {
                System.out.println("图书添加失败");
            }


        } catch (Exception e) {
            System.out.println("BookDAOTest 发生严重错误: " +  e);
            e.printStackTrace();
        } finally {
            HibernateUtil.shutdown();
            System.out.println("结束");
        }

    }
}
