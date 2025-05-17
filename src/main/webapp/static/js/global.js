/**
 * 全局工具函数和配置
 */

// API基础URL
const API_BASE_URL = 'http://localhost:8080/LibrarySystem_war_exploded/api';

/**
 * 发送HTTP请求的通用函数
 * @param {string} url - 请求URL
 * @param {string} method - 请求方法
 * @param {Object} data - 请求数据
 * @returns {Promise} - 返回Promise对象
 */
async function fetchAPI(url, method = 'GET', data = null) {
    try {
        const options = {
            method,
            headers: {
                'Content-Type': 'application/json'
            }
        };

        if (data) {
            options.body = JSON.stringify(data);
        }

        const response = await fetch(`${API_BASE_URL}${url}`, options);

        // 先获取文本内容
        const text = await response.text();

        // 错误时尝试解析错误信息
        if (!response.ok) {
            let errorData;
            try {
                errorData = text ? JSON.parse(text) : {};
            } catch {
                errorData = { message: text || '请求失败' };
            }
            throw new Error(errorData.message || '请求失败');
        }

        // 正常时如果有内容就解析，没有内容就返回空对象
        if (!text) return {};
        return JSON.parse(text);
    } catch (error) {
        console.error('API请求错误:', error);
        throw error;
    }
}

/**
 * 显示错误消息
 * @param {string} message - 错误消息
 * @param {HTMLElement} container - 显示消息的容器
 */
function showError(message, container) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    container.appendChild(errorDiv);
    
    // 3秒后自动移除错误消息
    setTimeout(() => {
        errorDiv.remove();
    }, 3000);
}

/**
 * 显示成功消息
 * @param {string} message - 成功消息
 * @param {HTMLElement} container - 显示消息的容器
 */
function showSuccess(message, container) {
    const successDiv = document.createElement('div');
    successDiv.className = 'success-message';
    successDiv.textContent = message;
    container.appendChild(successDiv);
    
    // 3秒后自动移除成功消息
    setTimeout(() => {
        successDiv.remove();
    }, 3000);
}

/**
 * 显示加载状态
 * @param {HTMLElement} container - 显示加载状态的容器
 * @returns {HTMLElement} - 返回加载状态元素
 */
function showLoading(container) {
    const loadingDiv = document.createElement('div');
    loadingDiv.className = 'loading';
    container.appendChild(loadingDiv);
    return loadingDiv;
}

/**
 * 移除加载状态
 * @param {HTMLElement} loadingElement - 加载状态元素
 */
function removeLoading(loadingElement) {
    if (loadingElement) {
        loadingElement.remove();
    }
}

/**
 * 格式化日期
 * @param {string} dateString - ISO格式的日期字符串
 * @returns {string} - 格式化后的日期字符串
 */
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * 创建分页控件
 * @param {Object} pagination - 分页信息
 * @param {Function} onPageChange - 页码改变回调函数
 * @returns {HTMLElement} - 返回分页控件元素
 */
function createPagination(pagination, onPageChange) {
    const { currentPage, totalPages } = pagination;
    const paginationContainer = document.createElement('ul');
    paginationContainer.className = 'pagination';

    // 上一页按钮
    const prevLi = document.createElement('li');
    const prevLink = document.createElement('a');
    prevLink.href = '#';
    prevLink.textContent = '上一页';
    prevLink.onclick = (e) => {
        e.preventDefault();
        if (currentPage > 1) {
            onPageChange(currentPage - 1);
        }
    };
    prevLi.appendChild(prevLink);
    paginationContainer.appendChild(prevLi);

    // 页码按钮
    for (let i = 1; i <= totalPages; i++) {
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.href = '#';
        a.textContent = i;
        if (i === currentPage) {
            li.className = 'active';
        }
        a.onclick = (e) => {
            e.preventDefault();
            onPageChange(i);
        };
        li.appendChild(a);
        paginationContainer.appendChild(li);
    }

    // 下一页按钮
    const nextLi = document.createElement('li');
    const nextLink = document.createElement('a');
    nextLink.href = '#';
    nextLink.textContent = '下一页';
    nextLink.onclick = (e) => {
        e.preventDefault();
        if (currentPage < totalPages) {
            onPageChange(currentPage + 1);
        }
    };
    nextLi.appendChild(nextLink);
    paginationContainer.appendChild(nextLi);

    return paginationContainer;
} 