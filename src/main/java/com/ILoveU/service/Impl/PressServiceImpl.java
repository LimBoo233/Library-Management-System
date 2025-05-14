package com.ILoveU.service.Impl;

import com.ILoveU.dao.BookDAO;
import com.ILoveU.dao.PressDAO;
import com.ILoveU.dao.impl.BookDAOImpl;
import com.ILoveU.dao.impl.PressDAOImpl;
import com.ILoveU.dto.PageDTO;
import com.ILoveU.dto.PressDTO;
import com.ILoveU.exception.*;
import com.ILoveU.model.Press;
import com.ILoveU.service.PressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class PressServiceImpl implements PressService {

    private static final Logger logger = LoggerFactory.getLogger(PressServiceImpl.class);
    private final PressDAO pressDAO;
    private final BookDAO bookDAO;

    public PressServiceImpl() {
        this.pressDAO = new PressDAOImpl();
        this.bookDAO = new BookDAOImpl();
    }


    @Override
    public PageDTO<PressDTO> getPressesWithPagination(int page, int pageSize)
            throws ValidationException {

        if (page <= 0 || pageSize <= 0) {
            logger.error("页码和每页大小必须大于0");
            throw new ValidationException("页码和每页大小必须大于0");
        }

        logger.debug("正在获取出版社分页数据 - 页码: {}, 每页大小: {}", page, pageSize);

        // 获取当前页数据
        List<Press> data = pressDAO.findPresses(page, pageSize);
        // 获取总条数
        long total = pressDAO.countTotalPresses();
        // 组装分页对象
        List<PressDTO> dataDTO = data.
                stream().
                map(press -> new PressDTO(press.getPressId(), press.getName())).
                collect(Collectors.toList());

        return new PageDTO<>(dataDTO, total, page, pageSize);
    }

    @Override
    public PressDTO getPressById(int pressId) throws ResourceNotFoundException {
        logger.info("开始查询出版社信息，pressId: {}", pressId);

        Press press = pressDAO.findPressById(pressId);

        if (press == null) {
            logger.warn("未找到出版社信息，pressId: {}", pressId);
            throw new ResourceNotFoundException("未找到该出版社");
        }

        logger.info("成功查询到出版社信息，pressId: {}, name: {}", pressId, press.getName());
        return new PressDTO(press.getPressId(), press.getName());
    }

    @Override
    public PressDTO createNewPress(PressDTO pressToCreateDTO) throws ValidationException, DuplicateResourceException, OperationFailedException {

        if (pressToCreateDTO == null) {
            logger.error("创建出版社时，PressDTO参数不能为空");
            throw new ValidationException("创建出版社时，PressDTO参数不能为空");
        }

        try {
            if (pressDAO.existsByNameIgnoreCase(pressToCreateDTO.getName())) {
                logger.error("创建出版社时失败，出版社名称已存在: {}", pressToCreateDTO.getName());
                throw new DuplicateResourceException("出版社名称已存在: " + pressToCreateDTO.getName());
            }

            Press press = new Press();
            press.setName(pressToCreateDTO.getName());

            // get press_id
            press = pressDAO.addPress(press);

            return new PressDTO(press.getPressId(), press.getName());

        } catch (Exception e) {
            logger.error("创建出版社时，检查出版社名称是否存在时发生意外错误: {}", e.getMessage(), e);
            throw new OperationFailedException("创建出版社时，检查出版社名称是否存在时发生意外错误: " + e.getMessage());
        }
    }

    @Override
    public PressDTO updateExistingPress(int pressId, PressDTO pressDetailsToUpdateDTO) throws ResourceNotFoundException, ValidationException, DuplicateResourceException, OperationFailedException {
        // 校验数据
        if (pressDetailsToUpdateDTO == null) {
            logger.error("更新出版社时失败，PressDTO参数不能为空");
            throw new ValidationException("更新出版社时，PressDTO参数不能为空");
        }


        if (pressDetailsToUpdateDTO.getName() == null) {
            logger.error("更新出版社时失败，出版社名称不能为空");
            throw new ValidationException("更新出版社时，出版社名称不能为空");
        }

        String newName = pressDetailsToUpdateDTO.getName().trim();

        // 尝试获取出版社信息
        Press press = null;
        try {
            press = pressDAO.findPressById(pressId);
        } catch (Exception e) {
            logger.error("更新出版社时，检查出版社名称是否存在时发生意外错误: {}", e.getMessage(), e);
            throw new OperationFailedException("更新出版社时，检查出版社名称是否存在时发生意外错误: " + e.getMessage());
        }

        if (press == null) {
            logger.error("更新出版社时失败，找到出版社信息，pressId: {}", pressId);
            throw new ResourceNotFoundException("未找到该出版社");
        }

        if (pressDAO.existsByNameIgnoreCase(newName)) {
            logger.error("更新出版社ID {} 时，检查出版社名称 '{}' 是否存在时发生错误。", pressId, newName);
            throw new DuplicateResourceException("出版社名称已存在: " + newName);
        }

        // 更新出版社信息
        press.setName(newName);
        try {
            press = pressDAO.updatePress(press);
            logger.info("出版社ID {} 已成功更新，新名称为 '{}'。", pressId, newName);
        } catch (Exception e) {
            logger.error("更新出版社ID {} 到数据库时失败。", pressId, e);
            throw new OperationFailedException("更新出版社信息到数据库时发生错误。", e);
        }

        return new PressDTO(press.getPressId(), press.getName());
    }


    @Override
    public void deletePressById(int pressId) {
        logger.info("尝试删除出版社，ID: {}", pressId);

        // 1. 检查出版社是否存在
        Press pressToDelete;
        try {
            pressToDelete = pressDAO.findPressById(pressId);
        } catch (Exception e) {
            logger.error("删除出版社ID {} 时，查找出版社失败。", pressId, e);
            throw new OperationFailedException("查找待删除出版社时发生错误。", e);
        }

        if (pressToDelete == null) {
            logger.warn("删除出版社失败：未找到ID为 {} 的出版社。", pressId);
            throw new ResourceNotFoundException("未找到ID为 " + pressId + " 的出版社，无法删除。");
        }

        // 2. 检查该出版社是否有关联的书籍 (核心业务逻辑)
        long bookCount;
        try {
            // bookDAO 需要有一个方法来统计某个出版社的书籍数量
            bookCount = bookDAO.countBooksByPressId(pressId);
        } catch (Exception e) {
            logger.error("删除出版社ID {} 时，检查关联书籍数量失败。", pressId, e);
            throw new OperationFailedException("检查出版社关联书籍时发生错误。", e);
        }

        if (bookCount > 0) {
            logger.warn("删除出版社ID {} 失败：该出版社尚有关联的书籍 {} 本。", pressId, bookCount);
            throw new OperationForbiddenException("无法删除出版社ID " + pressId + "，因为它尚有关联的书籍。请先处理这些书籍。");
        }

        // 3. 如果没有关联书籍，则执行删除操作
        try {
            boolean deleted = pressDAO.deletePress(pressId); // DAO的deletePress返回boolean
            if (!deleted) {
                // 如果DAO返回false，可能意味着记录在检查后到删除前被其他事务删除了，或者DAO内部有其他逻辑
                logger.warn("删除出版社ID {} 操作在DAO层未成功执行（可能已被删除或发生未知问题）。", pressId);
                // 这种情况可以视为一种操作失败，或者如果确定是“未找到”则抛出ResourceNotFoundException
                // 但因为我们前面已经检查过pressToDelete不为null，所以这里更倾向于是操作执行层面的问题
                throw new OperationFailedException("删除出版社ID " + pressId + " 操作未成功完成。");
            }
            logger.info("出版社ID {} 已成功删除。", pressId);
        } catch (Exception e) { // 捕获DAO层可能抛出的其他运行时异常，例如数据库约束异常（虽然理论上不应该发生，因为我们检查了书籍）
            logger.error("删除出版社ID {} 时发生数据库错误。", pressId, e);
            throw new OperationFailedException("删除出版社时发生数据库错误。", e);
        }
    }

}
