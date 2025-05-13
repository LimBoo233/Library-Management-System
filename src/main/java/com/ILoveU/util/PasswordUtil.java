package com.ILoveU.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {

    private static final String HASH_ALGORITHM = "SHA-256";

    public static String hashPassword(String password) {
        // String saltedPassword = salt + password;
        try {
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedBytes = md.digest(password.getBytes());
            // 将字节数组转换为十六进制字符串表示
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 这种异常理论上不应该发生，因为SHA-256是标准算法
            Log.Instance().severe("密码哈希算法未找到: " + e.getMessage());
            throw new RuntimeException("password hash transformation failed ", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        // use same hash algorithm
        String newHashedPassword = hashPassword(plainPassword);
        return newHashedPassword.equals(hashedPassword);
    }

}
