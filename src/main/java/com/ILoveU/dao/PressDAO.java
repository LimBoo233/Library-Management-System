package com.ILoveU.dao;

import com.ILoveU.model.Press;

import java.util.List;

/**
 * PressDAO (Data Access Object) 接口定义了与出版社（Press）实体相关的数据库操作。
 * 它抽象了数据持久化的具体实现，使得服务层可以独立于特定的数据访问技术。
 */
public interface PressDAO {

    /**
     * 分页查询出版社列表。
     *
     * @param page     请求的页码（通常从1开始计数）。如果小于1，实现时应默认为1。
     * @param pageSize 每页期望返回的记录数。如果小于1，实现时应使用一个合理的默认值。
     * @return 包含当前页出版社对象的列表 ({@link List}<{@link Press}>)。
     * 如果查询结果为空或发生错误，应返回一个空列表，而不是null。
     */
    List<Press> findPresses(int page, int pageSize);

    /**
     * 根据指定的ID查找单个出版社。
     *
     * @param pressId 要查找的出版社的唯一ID。
     * @return 如果找到，则返回对应的 {@link Press} 对象；如果未找到具有该ID的出版社，则返回 {@code null}。
     */
    Press findPressById(int pressId);
    // 备选签名: Optional<Press> findPressById(int pressId);

    /**
     * 将一个新的出版社对象持久化到数据库。
     *
     * @param press 要添加的 {@link Press} 对象。此对象的ID字段通常应为null或由数据库自动生成。
     * @return 持久化后的 {@link Press} 对象，通常包含由数据库生成的ID。
     * 如果添加失败（例如，由于数据库约束或连接问题），应返回 {@code null} 或抛出运行时异常。
     */
    Press addPress(Press press);

    /**
     * 更新数据库中已存在的出版社信息。
     * 此方法通常期望传入的 {@link Press} 对象包含一个有效的ID，用于定位要更新的记录。
     *
     * @param press 包含更新后信息的 {@link Press} 对象。其ID应指向一个已存在的出版社。
     * @return 更新成功后的 {@link Press} 对象。
     * 如果具有给定ID的出版社不存在或更新失败，应返回 {@code null} 或抛出运行时异常。
     */
    Press updatePress(Press press);

    /**
     * 根据指定的ID从数据库中删除一个出版社。
     * 实现时需要考虑与该出版社关联的其他数据（如图书）的处理策略，
     * 例如，数据库层面是否有外键约束会阻止删除，或者是否有级联删除的设置。
     *
     * @param pressId 要删除的出版社的唯一ID。
     * @return 如果成功删除出版社，则返回 {@code true}；
     * 如果未找到具有该ID的出版社，或者由于其他原因（如外键约束）导致删除失败，则返回 {@code false}。
     * (或者，可以考虑让此方法返回 {@code void}，并在未找到或删除失败时抛出异常)。
     */
    boolean deletePress(int pressId);
    // 备选签名: void deletePress(int pressId) throws PressNotFoundException, DataIntegrityViolationException;

    /**
     * 统计数据库中所有出版社的总数。
     * 此方法对于实现分页功能非常有用，可以帮助计算总页数。
     *
     * @return 出版社的总记录数 (long)。如果发生错误，应返回0或抛出运行时异常。
     */
    long countTotalPresses();

    /**
     * 检查具有指定名称的出版社是否已存在（不区分大小写）。
     * 这个方法对于在创建新出版社之前验证名称是否唯一很有用。
     *
     * @param name 要检查的出版社名称。
     * @return 如果具有该名称的出版社已存在，则返回 {@code true}；否则返回 {@code false}。
     */
    boolean existsByNameIgnoreCase(String name);

}