package com.ILoveU.dao;

import com.ILoveU.exception.OperationFailedException;
import com.ILoveU.model.Tag;

import java.util.List;
import java.util.Set;
import java.util.Collections;

/**
 * TagDAO (Data Access Object) 接口定义了与标签（Tag）实体相关的数据库操作。
 * 方法在发生错误或未找到数据时，通常返回null或空集合。
 * 具体的错误应由DAO的实现类记录日志。
 */
public interface TagDAO {

    /**
     * 将一个新的标签对象持久化到数据库。
     *
     * @param tag 要添加的 {@link Tag} 对象。其ID字段通常应为null或由数据库自动生成。
     * @return 持久化后的 {@link Tag} 对象，通常包含由数据库生成的ID。
     * 如果添加过程中发生数据库错误或违反约束（如名称唯一性），则返回 {@code null}。
     */
    Tag addTag(Tag tag);

    /**
     * 根据指定的ID查找单个标签。
     *
     * @param tagId 要查找的标签的唯一ID。
     * @return 如果找到，则返回对应的 {@link Tag} 对象；
     * 如果未找到具有该ID的标签或查询过程中发生数据库错误，则返回 {@code null}。
     */
    Tag findTagById(int tagId);
    // 备选签名: Optional<Tag> findTagById(int tagId); // Optional本身可以表示缺失，但错误情况仍需Service层判断

    /**
     * 根据一组ID查找多个标签。
     *
     * @param tagIds 一组标签ID。
     * @return 包含找到的 {@link Tag} 对象的列表；如果某些ID未找到，则列表中不包含它们。
     * 如果查询过程中发生数据库错误，则返回空列表 ({@link Collections#emptyList()})。
     */
    List<Tag> findTagsByIds(Set<Integer> tagIds);


    /**
     * 更新数据库中已存在的标签信息。
     *
     * @param tag 包含更新后信息的 {@link Tag} 对象。其ID应指向一个已存在的标签。
     * @return 更新成功后的受Hibernate Session管理的 {@link Tag} 对象。
     * 如果具有给定ID的标签不存在或更新过程中发生数据库错误或违反约束，则返回 {@code null}。
     */
    Tag updateTag(Tag tag);

    /**
     * 根据指定的ID从数据库中删除一个标签。
     * Service层在调用此方法前，应已处理完所有业务规则检查。
     *
     * @param tagId 要删除的标签的唯一ID。
     * @return 如果成功删除标签，则返回 {@code true}；
     * 如果未找到具有该ID的标签或删除失败（如因外键约束或数据库错误），则返回 {@code false}。
     */
    boolean deleteTag(int tagId);

    /**
     * 分页查询标签列表。
     *
     * @param page     请求的页码（通常从1开始计数）。
     * @param pageSize 每页期望返回的记录数。
     * @return 包含当前页标签对象的列表 ({@link List}<{@link Tag}>)。
     * 如果查询结果为空或发生错误，则返回空列表 ({@link Collections#emptyList()})。
     */
    List<Tag> findTags(int page, int pageSize);

    /**
     * 获取所有标签的列表 (不分页)。
     *
     * @return 包含所有标签对象的列表。
     * 如果查询过程中发生数据库错误，则返回空列表 ({@link Collections#emptyList()})。
     */
    List<Tag> findAllTags();


    /**
     * 统计数据库中所有标签的总数。
     *
     * @return 标签的总记录数 (long)。如果查询过程中发生数据库错误，则返回 {@code 0L}。
     */
    long countTotalTags();

    /**
     * 检查具有指定名称的标签是否已存在（通常不区分大小写）。
     *
     * @param name 要检查的标签名称。
     * @return 如果具有该名称的标签已存在，则返回 {@code true}；
     * 如果不存在或查询过程中发生数据库错误，则返回 {@code false}。
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * 根据名称查找标签（不区分大小写）。
     * 标签名应该是唯一的。
     *
     * @param name 要查找的标签名称。
     * @return 如果找到，则返回对应的 {@link Tag} 对象；
     * 如果未找到或查询过程中发生数据库错误，则返回 {@code null}。
     */
    Tag findTagByNameIgnoreCase(String name);

}
