package com.ILoveU.service.Impl;

import com.ILoveU.dao.AuthorDAO;
import com.ILoveU.dao.BookDAO;
import com.ILoveU.dao.impl.AuthorDAOImpl;
import com.ILoveU.dao.impl.BookDAOImpl;
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
    private final BookDAO bookDAO;

    public AuthorServiceImpl() {
        this.authorDAO = new AuthorDAOImpl();
        this.bookDAO = new BookDAOImpl();
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
            logger.warn("未找到作者，ID: {}", authorId);
            throw new ResourceNotFoundException("未找到ID为 " + authorId + " 的作者。");
        }
        return convertToAuthorDTO(author);
    }

    @Override
    public AuthorDTO createAuthor(AuthorDTO authorDTO)
            throws ValidationException, DuplicateResourceException, OperationFailedException {

        // 1. 校验数据
        if (authorDTO == null) {
            logger.error("创建作者时失败，authorDTO参数不能为空");
            throw new ValidationException("更新作者时，AuthorDTO参数不能为空");
        }

        if (authorDTO.getFirstName() == null || authorDTO.getLastName() == null) {
            logger.error("创建作者时失败，dto必要参数为空");
            throw new ValidationException("更新作者时，dto必要参数为空");
        }

        String newFirstName = authorDTO.getFirstName().trim();
        String newLastName = authorDTO.getLastName().trim();
        String newBio = (authorDTO.getBio() != null) ? authorDTO.getBio().trim() : null;

        logger.info("尝试创建新作者: {} {}", newFirstName, newLastName);

        if (newFirstName.isEmpty() || newLastName.isEmpty()) {
            logger.error("创建作者时失败，dto必要参数为空");
            throw new ValidationException("更新作者时，dto必要参数为空");
        }


        try {
            if (authorDAO.existsByNameIgnoreCase(authorDTO.getFirstName(), authorDTO.getLastName())) {
                logger.warn("创建作者失败：作者 '{} {}' 已存在。", newFirstName, newLastName);
                throw new DuplicateResourceException("作者 '" + newFirstName + " " + newLastName + "' 已存在。");
            }
        } catch (Exception e) {
            logger.error("创建作者时，检查作者名称是否存在时发生意外错误: {}", e.getMessage(), e);
            throw new OperationFailedException("创建作者时，检查作者名称是否存在时发生意外错误: " + e.getMessage());
        }

        // 2. 创建新作者
        Author newAuthor = new Author();
        newAuthor.setFirstName(newFirstName);
        newAuthor.setLastName(newLastName);
        newAuthor.setBio(newBio);

        newAuthor = authorDAO.addAuthor(newAuthor);

        Author savedAuthor;
        try {
            savedAuthor = authorDAO.addAuthor(newAuthor);
            if (savedAuthor == null || savedAuthor.getAuthorId() == null) {
                throw new OperationFailedException("创建作者后未能获取有效的作者信息。");
            }
        } catch (Exception e) {
            logger.error("创建作者 '{} {}' 时发生数据库错误。", newFirstName, newLastName, e);
            throw new OperationFailedException("创建作者时发生数据库错误。", e);
        }

        return convertToAuthorDTO(savedAuthor);
    }

    @Override
    public AuthorDTO updateAuthor(int authorId, AuthorDTO authorDTO)
            throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException {

        // 1. 校验数据
        if (authorDTO == null) {
            throw new ValidationException("更新作者的请求数据不能为空。");
        }

        if (authorDTO.getFirstName() == null || authorDTO.getLastName() == null) {
            throw new ValidationException("更新作者时，dto必要参数为空");
        }

        String newFirstName = authorDTO.getFirstName().trim();
        String newLastName = authorDTO.getLastName().trim();
        String newBio = null;
        if (authorDTO.getBio() != null) {
            newBio = authorDTO.getBio().trim();
        }

        if (newFirstName.isEmpty() || newLastName.isEmpty()) {
            throw new ValidationException("更新作者时，dto必要参数为空");
        }

        // 2. 检查作者是否存在
        Author authorToUpdate;
        try {
            authorToUpdate = authorDAO.findAuthorById(authorId);
        } catch (Exception e) {
            logger.error("更新作者ID {} 时查找失败。", authorId, e);
            throw new OperationFailedException("查找待更新作者时发生错误。", e);
        }

        if (authorToUpdate == null) {
            throw new ResourceNotFoundException("未找到ID为 " + authorId + " 的作者，无法更新。");
        }

        // 3. 若修改名字
        boolean nameHasChanged = !newFirstName.equalsIgnoreCase(authorToUpdate.getFirstName()) ||
                !newLastName.equalsIgnoreCase(authorToUpdate.getLastName());

        if (nameHasChanged) {
            try {
                // 检查新的firstName和lastName组合是否已被其他作者使用
                if (authorDAO.existsByNameIgnoreCase(newFirstName, newLastName)) {
                    logger.warn("更新作者ID {} 失败：姓名 '{} {}' 已被的其他作者使用。",
                            authorId, newFirstName, newLastName);
                    throw new DuplicateResourceException("姓名组合 '" + newFirstName + " " + newLastName + "' 已被其他作者使用。");
                }
            } catch (Exception e) {
                logger.error("更新作者ID {} 时检查名称唯一性失败。", authorId, e);
                throw new OperationFailedException("检查作者名称唯一性时发生错误。", e);
            }
            authorToUpdate.setFirstName(newFirstName);
            authorToUpdate.setLastName(newLastName);
        }




        // 3. 更新作者信息
        authorToUpdate.setFirstName(newFirstName);
        authorToUpdate.setLastName(newLastName);
        if (newBio != null && !newBio.isEmpty()) {
            authorToUpdate.setBio(newBio);
        }

        Author updatedAuthor;
        try {
            updatedAuthor = authorDAO.updateAuthor(authorToUpdate);
            if (updatedAuthor == null) {
                throw new OperationFailedException("更新作者后未能获取有效的作者信息。");
            }
        } catch (Exception e) {
            logger.error("更新作者ID {} 到数据库时失败。", authorId, e);
            throw new OperationFailedException("更新作者信息到数据库时发生错误。", e);
        }

        return convertToAuthorDTO(updatedAuthor);
    }

    @Override
    public void deleteAuthor(int authorId)
            throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException {
        logger.info("尝试删除作者，ID: {}", authorId);
        Author authorToDelete;
        try {
            authorToDelete = authorDAO.findAuthorById(authorId);
        } catch (Exception e) {
            logger.error("删除作者ID {} 时查找失败。", authorId, e);
            throw new OperationFailedException("查找待删除作者时发生错误。", e);
        }

        if (authorToDelete == null) {
            throw new ResourceNotFoundException("未找到ID为 " + authorId + " 的作者，无法删除。");
        }

        long bookCount;
        try {
            // 依赖BookDAO来检查作者是否有关联的书籍
            bookCount = bookDAO.countBooksByAuthorId(authorId);
        } catch (Exception e) {
            logger.error("删除作者ID {} 时检查关联书籍失败。", authorId, e);
            throw new OperationFailedException("检查作者关联书籍时发生错误。", e);
        }

        if (bookCount > 0) {
            logger.warn("删除作者ID {} 失败：该作者尚著有 {} 本书籍。", authorId, bookCount);
            throw new OperationForbiddenException("无法删除该作者，他/她尚著有 " + bookCount + " 本书籍。");
        }

        try {
            if (!authorDAO.deleteAuthor(authorId)) {
                logger.warn("删除作者ID {} 操作在DAO层未成功执行。", authorId);
                // 如果DAO的deleteAuthor返回false，且前面已确认作者存在且无关联书籍，
                // 这可能指示一个不期望的状态或DAO内部问题。
                throw new OperationFailedException("删除作者ID " + authorId + " 操作未成功完成。");
            }
            logger.info("作者ID {} 已成功删除。", authorId);
        } catch (Exception e) {
            logger.error("删除作者ID {} 时发生数据库错误。", authorId, e);
            throw new OperationFailedException("删除作者时发生数据库错误。", e);
        }
    }

}
