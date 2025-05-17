package com.ILoveU.service.Impl;

import com.ILoveU.dao.BookDAO;
import com.ILoveU.dao.TagDAO;
import com.ILoveU.dao.impl.BookDAOImpl;
import com.ILoveU.dao.impl.TagDAOImpl;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.dto.TagDTO;
import com.ILoveU.dto.ApiErrorResponse; // 用于 ValidationException 的 FieldErrorDetail
import com.ILoveU.exception.*;
import com.ILoveU.model.Tag;

import com.ILoveU.service.TagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class TagServiceImpl implements TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagServiceImpl.class);
    private final TagDAO tagDAO;
    private final BookDAO bookDAO; // 用于检查标签是否被书籍使用

    public TagServiceImpl() {
        this.tagDAO = new TagDAOImpl();
        this.bookDAO = new BookDAOImpl();
    }

    // 通过构造函数注入DAO实例
    public TagServiceImpl(TagDAO tagDAO, BookDAO bookDAO) {
        this.tagDAO = tagDAO;
        this.bookDAO = bookDAO;
    }

    /**
     * 将 Tag 实体转换为 TagDTO。
     * @param tag Tag 实体
     * @return TagDTO 对象，如果输入为null则返回null
     */
    private TagDTO convertToTagDTO(Tag tag) {
        if (tag == null) {
            return null;
        }
        return new TagDTO(tag.getTagId(), tag.getName());
    }

    @Override
    public PageDTO<TagDTO> getTags(int page, int pageSize) throws ValidationException, OperationFailedException {
        if (page <= 0) {
            throw new ValidationException("页码必须是正整数。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("page", "页码必须大于0")));
        }
        if (pageSize <= 0) {
            throw new ValidationException("每页大小必须是正整数。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("pageSize", "每页大小必须大于0")));
        }

        logger.debug("获取标签分页列表 - 页码: {}, 每页大小: {}", page, pageSize);
        List<Tag> tags;
        long totalTags;

        try {
            tags = tagDAO.findTags(page, pageSize);
            totalTags = tagDAO.countTotalTags();
        } catch (Exception e) {
            logger.error("Service层获取标签列表时发生数据库错误。", e);
            throw new OperationFailedException("获取标签列表失败，请稍后再试。", e);
        }

        List<TagDTO> tagDTOs = tags.stream()
                .map(this::convertToTagDTO)
                .collect(Collectors.toList());

        return new PageDTO<>(tagDTOs, totalTags, page, pageSize);
    }

    @Override
    public List<TagDTO> getAllTags() throws OperationFailedException {
        logger.debug("获取所有标签列表。");
        List<Tag> tags;
        try {
            tags = tagDAO.findAllTags();
        } catch (Exception e) {
            logger.error("Service层获取所有标签时发生数据库错误。", e);
            throw new OperationFailedException("获取所有标签失败，请稍后再试。", e);
        }
        return tags.stream()
                .map(this::convertToTagDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TagDTO getTagById(int tagId) throws ResourceNotFoundException, OperationFailedException {
        logger.info("查询标签信息，ID: {}", tagId);
        Tag tag;
        try {
            tag = tagDAO.findTagById(tagId);
        } catch (Exception e) {
            logger.error("Service层通过ID {} 查询标签时发生数据库错误。", tagId, e);
            throw new OperationFailedException("查询标签信息失败，请稍后再试。", e);
        }

        if (tag == null) {
            logger.warn("未找到标签，ID: {}", tagId);
            throw new ResourceNotFoundException("未找到ID为 " + tagId + " 的标签。");
        }
        return convertToTagDTO(tag);
    }

    @Override
    public TagDTO createTag(TagDTO tagDTO)
            throws ValidationException, DuplicateResourceException, OperationFailedException {
        if (tagDTO == null || tagDTO.getName() == null || tagDTO.getName().trim().isEmpty()) {
            throw new ValidationException("标签名称不能为空。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("name", "标签名称不能为空")));
        }
        String tagName = tagDTO.getName().trim();
        logger.info("尝试创建新标签: {}", tagName);

        try {
            if (tagDAO.existsByNameIgnoreCase(tagName)) {
                logger.warn("创建标签失败：标签名称 '{}' 已存在。", tagName);
                throw new DuplicateResourceException("标签名称 '" + tagName + "' 已存在。");
            }
        } catch (Exception e) {
            logger.error("创建标签时检查名称唯一性失败。", e);
            throw new OperationFailedException("检查标签名称唯一性时发生错误。", e);
        }

        Tag newTag = new Tag();
        newTag.setName(tagName);
        // ID 由数据库自动生成

        Tag savedTag;
        try {
            savedTag = tagDAO.addTag(newTag);
            if (savedTag == null || savedTag.getTagId() == null) {
                throw new OperationFailedException("创建标签后未能获取有效的标签信息。");
            }
        } catch (Exception e) {
            logger.error("创建标签 '{}' 时发生数据库错误。", tagName, e);
            throw new OperationFailedException("创建标签时发生数据库错误。", e);
        }
        return convertToTagDTO(savedTag);
    }

    @Override
    public TagDTO updateTag(int tagId, TagDTO tagDTO)
            throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException {
        if (tagDTO == null || tagDTO.getName() == null || tagDTO.getName().trim().isEmpty()) {
            throw new ValidationException("标签名称不能为空。", Collections.singletonList(new ApiErrorResponse.FieldErrorDetail("name", "标签名称不能为空")));
        }
        String newTagName = tagDTO.getName().trim();
        logger.info("尝试更新标签ID: {} 为新名称: {}", tagId, newTagName);

        Tag tagToUpdate;
        try {
            tagToUpdate = tagDAO.findTagById(tagId);
        } catch (Exception e) {
            logger.error("更新标签ID {} 时查找失败。", tagId, e);
            throw new OperationFailedException("查找待更新标签时发生错误。", e);
        }

        if (tagToUpdate == null) {
            throw new ResourceNotFoundException("未找到ID为 " + tagId + " 的标签，无法更新。");
        }

        // 检查新名称是否与现有名称不同，并且是否与其他标签冲突
        if (!newTagName.equalsIgnoreCase(tagToUpdate.getName())) {
            try {
                // 检查新的tagName是否已被其他标签使用
                Tag existingTagWithNewName = tagDAO.findTagByNameIgnoreCase(newTagName);
                if (existingTagWithNewName != null && !existingTagWithNewName.getTagId().equals(tagId)) {
                    logger.warn("更新标签ID {} 失败：新名称 '{}' 已被ID为 {} 的其他标签使用。",
                            tagId, newTagName, existingTagWithNewName.getTagId());
                    throw new DuplicateResourceException("标签名称 '" + newTagName + "' 已被其他标签使用。");
                }
            } catch (Exception e) {
                logger.error("更新标签ID {} 时检查名称唯一性失败。", tagId, e);
                throw new OperationFailedException("检查标签名称唯一性时发生错误。", e);
            }
            tagToUpdate.setName(newTagName);

            try {
                Tag updatedTag = tagDAO.updateTag(tagToUpdate);
                if (updatedTag == null) {
                    throw new OperationFailedException("更新标签后未能获取有效的标签信息。");
                }
                return convertToTagDTO(updatedTag);
            } catch (Exception e) {
                logger.error("更新标签ID {} 到数据库时失败。", tagId, e);
                throw new OperationFailedException("更新标签信息到数据库时发生错误。", e);
            }
        } else {
            logger.info("标签ID {} 的名称 ('{}') 未发生变化，不执行数据库更新。", tagId, newTagName);
            return convertToTagDTO(tagToUpdate); // 名称未变，直接返回当前DTO
        }
    }

    @Override
    public void deleteTag(int tagId)
            throws ResourceNotFoundException, OperationForbiddenException, OperationFailedException {
        logger.info("尝试删除标签，ID: {}", tagId);
        Tag tagToDelete;
        try {
            tagToDelete = tagDAO.findTagById(tagId);
        } catch (Exception e) {
            logger.error("删除标签ID {} 时查找失败。", tagId, e);
            throw new OperationFailedException("查找待删除标签时发生错误。", e);
        }

        if (tagToDelete == null) {
            throw new ResourceNotFoundException("未找到ID为 " + tagId + " 的标签，无法删除。");
        }

        // 核心业务规则：检查该标签是否仍被任何书籍使用
        long bookCount;
        try {
            // 依赖BookDAO来检查标签是否有关联的书籍
            bookCount = bookDAO.countBooksByTagId(tagId); // 你需要在BookDAO中实现此方法
        } catch (Exception e) {
            logger.error("删除标签ID {} 时检查关联书籍失败。", tagId, e);
            throw new OperationFailedException("检查标签关联书籍时发生错误。", e);
        }

        if (bookCount > 0) {
            logger.warn("删除标签ID {} 失败：该标签尚被 {} 本书籍使用。", tagId, bookCount);
            throw new OperationForbiddenException("无法删除该标签，它尚被 " + bookCount + " 本书籍使用。");
        }

        try {
            if (!tagDAO.deleteTag(tagId)) {
                logger.warn("删除标签ID {} 操作在DAO层未成功执行。", tagId);
                throw new OperationFailedException("删除标签ID " + tagId + " 操作未成功完成。");
            }
            logger.info("标签ID {} 已成功删除。", tagId);
        } catch (Exception e) {
            logger.error("删除标签ID {} 时发生数据库错误。", tagId, e);
            throw new OperationFailedException("删除标签时发生数据库错误。", e);
        }
    }
}