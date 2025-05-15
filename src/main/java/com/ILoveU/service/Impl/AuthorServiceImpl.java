package com.ILoveU.service.Impl;

import com.ILoveU.dao.AuthorDAO;
import com.ILoveU.dao.impl.AuthorDAOImpl;
import com.ILoveU.dto.AuthorDTO;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.exception.*;
import com.ILoveU.model.Author;
import com.ILoveU.service.AuthorService;
import com.ILoveU.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorServiceImpl implements AuthorService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorServiceImpl.class);
    private final AuthorDAO authorDAO;

    public AuthorServiceImpl() {
        this.authorDAO = new AuthorDAOImpl();
    }


    @Override
    public PageDTO<AuthorDTO> getAuthors(String nameKeyword, int page, int pageSize) throws ValidationException {
        // 校验1: 页码和每页大小
        if (page <= 0 || pageSize <= 0) {
            logger.warn("无效的分页参数 - page: {}, pageSize: {}", page, pageSize);
            throw new ValidationException("页码和每页大小必须大于0。");
        }

        // 校验2: nameKeyword 处理 (如果为空，DAO层会处理为查询所有)
        if (nameKeyword != null && nameKeyword.trim().isEmpty()) {
            nameKeyword = null;
        }
        if (nameKeyword != null) {
            nameKeyword = nameKeyword.trim();
        }


        logger.debug("正在获取作者分页数据 - 关键词: '{}', 页码: {}, 每页大小: {}", nameKeyword, page, pageSize);
        List<Author> authors = authorDAO.findAuthorsByNameKeyword(nameKeyword, page, pageSize);
        long totalAuthors = authorDAO.countAuthorsByNameKeyword(nameKeyword);

        // 将Author实体列表转换为AuthorDTO列表
        List<AuthorDTO> authorDTOs = authors.stream()
                .map(this::convertToAuthorDTO)
                .collect(Collectors.toList());

        return new PageDTO<>(authorDTOs, totalAuthors, page, pageSize);
    }

    private AuthorDTO convertToAuthorDTO(Author author) {
        if (author == null) {
            return null;
        }
        return new AuthorDTO(
                author.getAuthorId(),
                author.getFirstName(),
                author.getLastName(),
                author.getBio(),
                // 假设DateUtil可以安全地将Timestamp转换为ISO8601字符串
                author.getCreatedAt() != null ? DateUtil.formatTimestampToISOString(author.getCreatedAt()) : null,
                author.getUpdatedAt() != null ? DateUtil.formatTimestampToISOString(author.getUpdatedAt()) : null
        );
    }

    @Override
    public AuthorDTO getAuthorById(int authorId) throws ResourceNotFoundException {
        logger.info("开始查询作者信息，authorId: {}", authorId);

        Author author;
        try {
            author = authorDAO.findAuthorById(authorId);
        } catch (Exception e) {
            logger.error("Service层通过ID {} 查询作者时发生数据库错误。", authorId, e);
            throw new OperationFailedException("查询作者信息失败，请稍后再试。", e);
        }

        if (author == null) {
            logger.warn("未找到作者信息，authorId: {}", authorId);
            throw new ResourceNotFoundException("未找到该作者");
        }

        logger.info("成功查询到作者信息，authorId: {}, name: {} {}", authorId, author.getFirstName(), author.getLastName());

        return new AuthorDTO(author.getAuthorId(), author.getFirstName(), author.getLastName(), author.getBio(), author.getCreatedAt().toString(), author.getUpdatedAt().toString());
    }

    @Override
    public AuthorDTO createAuthor(AuthorDTO authorDTO)
            throws ValidationException, DuplicateResourceException, OperationFailedException {

        // 1. 校验数据
        if (authorDTO == null) {
            logger.error("创建作者时失败，authorDTO参数不能为空");
            throw new ValidationException("更新作者时，AuthorDTO参数不能为空");
        }

        if (authorDTO.getFirstName() == null || authorDTO.getLastName() == null || authorDTO.getBio() == null || authorDTO.getCreatedAt() == null || authorDTO.getUpdatedAt() == null) {
            logger.error("创建作者时失败，dto必要参数为空");
            throw new ValidationException("更新作者时，dto必要参数为空");
        }

        String newFirstName = authorDTO.getFirstName().trim();
        String newLastName = authorDTO.getLastName().trim();
        String newBio = authorDTO.getBio().trim();
        String newCreatedAt = authorDTO.getCreatedAt().trim();
        String newUpdatedAt = authorDTO.getUpdatedAt().trim();

        if (newFirstName.isEmpty() || newLastName.isEmpty() || newBio.isEmpty() || newCreatedAt.isEmpty() || newUpdatedAt.isEmpty()) {
            logger.error("创建作者时失败，dto必要参数为空");
            throw new ValidationException("更新作者时，dto必要参数为空");
        }


        try {
            if (authorDAO.existsByNameIgnoreCase(authorDTO.getFirstName(), authorDTO.getLastName())) {
                logger.error("作者已存在: {} {}", authorDTO.getFirstName(), authorDTO.getLastName());
                throw new DuplicateResourceException("作者已存在: " + authorDTO.getFirstName() + " " + authorDTO.getLastName());
            }

            Author author = new Author();
            author.setFirstName(authorDTO.getFirstName());
            author.setLastName(authorDTO.getLastName());
            author.setBio(authorDTO.getBio());

            author = authorDAO.addAuthor(author);

            return new AuthorDTO(author.getAuthorId(), author.getFirstName(), author.getLastName(), author.getBio(), author.getCreatedAt().toString(), author.getUpdatedAt().toString());
        } catch (Exception e) {
            logger.error("创建作者时，检查作者名称是否存在时发生意外错误: {}", e.getMessage(), e);
            throw new OperationFailedException("创建作者时，检查作者名称是否存在时发生意外错误: " + e.getMessage());
        }
    }

    @Override
    public AuthorDTO updateAuthor(int authorId, AuthorDTO authorDTO)
            throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException {

        // 1. 校验数据
        if (authorDTO == null) {
            logger.error("更新作者时失败，authorDTO参数不能为空");
            throw new ValidationException("更新作者时，AuthorDTO参数不能为空");
        }

        if (authorDTO.getFirstName() == null || authorDTO.getLastName() == null || authorDTO.getBio() == null || authorDTO.getCreatedAt() == null || authorDTO.getUpdatedAt() == null) {
            logger.error("更新作者时失败，dto必要参数为空");
            throw new ValidationException("更新作者时，dto必要参数为空");
        }

        String newFirstName = authorDTO.getFirstName().trim();
        String newLastName = authorDTO.getLastName().trim();
        String newBio = authorDTO.getBio().trim();
        String newCreatedAt = authorDTO.getCreatedAt().trim();
        String newUpdatedAt = authorDTO.getUpdatedAt().trim();

        if (newFirstName.isEmpty() || newLastName.isEmpty() || newBio.isEmpty() || newCreatedAt.isEmpty() || newUpdatedAt.isEmpty()) {
            logger.error("更新作者时失败，dto必要参数为空");
            throw new ValidationException("更新作者时，dto必要参数为空");
        }

        // 2. 检查作者是否存在
        Author author;
        try {

            if (authorDAO.existsByNameIgnoreCase(newFirstName, newLastName)) {
                logger.error("更新作者ID {} 时，检查作者名称 '{}' 存在时发生错误。", authorId, newFirstName + " " + newLastName);
                throw new DuplicateResourceException("作者已存在: " + newFirstName + " " + newLastName);
            }

            author = authorDAO.findAuthorById(authorId);

        } catch (Exception e) {
            logger.error("更新作者时，检查作者名称是否存在时发生意外错误: {}", e.getMessage(), e);
            throw new OperationFailedException("更新作者时，检查作者名称是否存在时发生意外错误: " + e.getMessage());
        }

        if (author == null) {
            logger.error("更新作者时失败，未找到作者信息，authorId: {}", authorId);
            throw new ResourceNotFoundException("未找到该作者");
        }

        // 3. 更新作者信息
        author.setFirstName(newFirstName);
        author.setLastName(newLastName);
        author.setBio(newBio);
        author.setCreatedAt(Timestamp.valueOf(newCreatedAt));
        author.setUpdatedAt(Timestamp.valueOf(newUpdatedAt));

        try {
            author = authorDAO.updateAuthor(author);
        } catch (Exception e) {
            logger.error("更新作者时，更新作者信息时发生意外错误: {}", e.getMessage(), e);
            throw new OperationFailedException("更新作者时，更新作者信息时发生意外错误: " + e.getMessage());
        }

        return new AuthorDTO(author.getAuthorId(), author.getFirstName(), author.getLastName(), author.getBio(), author.getCreatedAt().toString(), author.getUpdatedAt().toString());
    }

    @Override
    public void deleteAuthor(int authorId)
            throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException {
        logger.info("尝试删除作者，ID: {}", authorId);

        // 1. 检查作者是否存在
        Author authorToDelete;
        try {
            authorToDelete = authorDAO.findAuthorById(authorId);
        } catch (Exception e) {
            logger.error("删除作者ID {} 时，查找作者失败。", authorId, e);
            throw new OperationFailedException("查找待删除作者时发生错误。", e);
        }

        if (authorToDelete == null) {
            logger.warn("删除作者失败：未找到ID为 {} 的作者。", authorId);
            throw new ResourceNotFoundException("未找到ID为 " + authorId + " 的作者，无法删除。");
        }

        try {
            boolean deleted = authorDAO.deleteAuthor(authorId);
            if (!deleted) {
                logger.warn("删除出版社ID {} 操作在DAO层未成功执行（可能已被删除或发生未知问题）。", authorId);
                throw new OperationFailedException("删除出版社ID " + authorId + " 操作未成功完成。");
            }
        } catch (Exception e) {
            logger.error("删除作者ID {} 时发生数据库错误。", authorId, e);
            throw new OperationFailedException("删除作者时发生数据库错误。", e);
        }
    }

}
