package com.ILoveU.service;

import com.ILoveU.dto.PageDTO;
import com.ILoveU.model.Press;

public interface PressService {


    /**
     * 分页获取出版社列表。
     *
     * @param page 页码 (通常从1开始计数)
     * @param pageSize 每页显示的记录数
     * @return 一个Map对象，其中包含:
     * - ""data":" 当前页的出版社列表 (List<Press>)
     * - "pagination": 出版社的总记录数 (long)
     */
    PageDTO<Press> getPressesWithPagination(int page, int pageSize);

    /**
     * 根据指定的ID获取单个出版社的详细信息。
     *
     * @param pressId 要查找的出版社的ID
     * @return 找到的Press对象；如果未找到对应ID的出版社，则返回null。
     * (在更完善的设计中，如果未找到，可能会抛出一个自定义的 ResourceNotFoundException)
     */
    Press getPressById(int pressId);

    /**
     * 创建一个新的出版社。
     * Service层应负责在调用DAO之前进行数据校验 (例如，出版社名称不能为空，名称是否已存在等)。
     *
     * @param pressToCreate 包含新出版社信息的Press对象 (通常其ID字段为null或未设置)
     * @return 创建成功后的Press对象 (应包含由数据库生成的ID)；
     * 如果创建失败 (例如，由于校验失败或数据库错误)，则返回null或抛出相应的异常。
     */
    Press createNewPress(Press pressToCreate);

    /**
     * 更新一个已存在的出版社信息。
     * Service层应负责在调用DAO之前进行数据校验，并确认要更新的出版社确实存在。
     *
     * @param pressId 要更新的出版社的ID
     * @param pressDetailsToUpdate 包含要更新的字段信息的Press对象。
     * 此对象的ID字段可以被忽略，或者用于验证它与pressId参数是否匹配。
     * @return 更新成功后的Press对象；
     * 如果未找到对应ID的出版社或更新失败，则返回null或抛出相应的异常。
     */
    Press updateExistingPress(int pressId, Press pressDetailsToUpdate);

    /**
     * 根据指定的ID删除一个出版社。
     * Service层应负责处理删除操作的业务逻辑，例如：
     * - 检查该出版社是否有任何关联的书籍。
     * - 根据业务规则决定是否允许删除 (例如，如果有关联书籍，是禁止删除、级联删除书籍，还是将书籍的出版社设为null/特定值)。
     *
     * @param pressId 要删除的出版社的ID
     * @return 如果删除操作成功，则返回true；
     * 如果删除失败 (例如，出版社未找到，或由于业务规则不允许删除)，则返回false。
     * (在更完善的设计中，如果操作失败或不被允许，可能会抛出自定义异常，如 ResourceNotFoundException 或 OperationForbiddenException)
     */
    boolean deletePressById(int pressId);
}
