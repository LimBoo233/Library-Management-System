import com.ILoveU.service.UserService;
import com.ILoveU.service.Impl.UserServiceImpl;

import java.util.Map;

public class UserServiceTest {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        Map<String, Object> result = (Map<String, Object>) userService.registerUser(
                "Amiya",
                "Amiya123",
                "123456awa"
        );

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

    }
}
