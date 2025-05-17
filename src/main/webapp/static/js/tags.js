/**
 * 标签管理相关功能
 * 严格按照API文档 /api/tags 相关接口实现
 */

/**
 * 加载标签列表页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadTagsPage(container) {
    // 页面基础结构：搜索框、表格、分页
    container.innerHTML = `
        <h2>标签管理</h2>
        <button class="btn btn-primary" id="add-tag-btn" style="margin-bottom:1rem;">新增标签</button>
        <div class="tag-search-bar">
            <input type="text" id="tag-search-input" class="form-control" placeholder="搜索标签名...">
            <button id="tag-search-btn" class="btn btn-primary">搜索</button>
        </div>
        <div id="tags-table-container"></div>
        <div id="tags-pagination"></div>
    `;
    document.getElementById('add-tag-btn').onclick = showAddTagModal;
    document.getElementById('tag-search-btn').addEventListener('click', () => {
        loadTagsTable(1);
    });
    document.getElementById('tag-search-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            loadTagsTable(1);
        }
    });
    loadTagsTable(1);
}

/**
 * 加载并渲染标签表格
 * @param {number} page - 当前页码
 */
async function loadTagsTable(page) {
    const tableContainer = document.getElementById('tags-table-container');
    const paginationContainer = document.getElementById('tags-pagination');
    const searchInput = document.getElementById('tag-search-input');
    const keyword = searchInput.value.trim();

    // 清空内容并显示加载状态
    tableContainer.innerHTML = '';
    paginationContainer.innerHTML = '';
    const loading = showLoading(tableContainer);

    // 构造API请求参数，严格按照API文档
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', 10); // 每页10条
    if (keyword) params.append('search', keyword);

    try {
        // 请求标签列表
        const data = await fetchAPI(`/tags?${params.toString()}`);
        // data结构应为 { data: [...], pagination: {...} }
        renderTagsTable(data.data || [], tableContainer);
        // 渲染分页
        if (data.pagination) {
            const paginationEl = createPagination(data.pagination, loadTagsTable);
            paginationContainer.appendChild(paginationEl);
        }
    } catch (error) {
        showError(error.message || '加载标签列表失败', tableContainer);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 渲染标签表格
 * @param {Array} tags - 标签数组
 * @param {HTMLElement} container - 表格容器
 */
function renderTagsTable(tags, container) {
    if (!tags.length) {
        container.innerHTML = '<p>暂无标签数据。</p>';
        return;
    }
    // 构建表格
    const table = document.createElement('table');
    table.className = 'table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>ID</th>
                <th>名称</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
            ${tags.map(tag => `
                <tr>
                    <td>${tag.id}</td>
                    <td>${tag.name || ''}</td>
                    <td>
                        <button class="btn btn-primary" onclick="showTagDetail(${tag.id})">详情</button>
                        <button class="btn btn-primary" onclick="showEditTagModal(${tag.id})">编辑</button>
                        <button class="btn btn-danger" onclick="deleteTag(${tag.id})">删除</button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

/**
 * 显示标签详情弹窗
 * @param {number} tagId - 标签ID
 */
async function showTagDetail(tagId) {
    let modal = document.getElementById('tag-detail-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'tag-detail-modal';
        modal.style.position = 'fixed';
        modal.style.top = '0';
        modal.style.left = '0';
        modal.style.width = '100vw';
        modal.style.height = '100vh';
        modal.style.background = 'rgba(0,0,0,0.4)';
        modal.style.display = 'flex';
        modal.style.alignItems = 'center';
        modal.style.justifyContent = 'center';
        modal.style.zIndex = '9999';
        document.body.appendChild(modal);
    }
    modal.innerHTML = `<div id="tag-detail-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-tag-detail" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <div id="tag-detail-body"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-tag-detail').onclick = () => {
        modal.style.display = 'none';
    };
    const detailBody = document.getElementById('tag-detail-body');
    detailBody.innerHTML = '';
    const loading = showLoading(detailBody);
    try {
        const data = await fetchAPI(`/tags/${tagId}`);
        detailBody.innerHTML = `
            <h3>标签详情</h3>
            <p><b>ID：</b>${data.id}</p>
            <p><b>名称：</b>${data.name || ''}</p>
        `;
    } catch (error) {
        showError(error.message || '加载标签详情失败', detailBody);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 显示新增标签表单弹窗
 */
async function showAddTagModal() {
    let modal = document.getElementById('tag-add-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'tag-add-modal';
        modal.style.position = 'fixed';
        modal.style.top = '0';
        modal.style.left = '0';
        modal.style.width = '100vw';
        modal.style.height = '100vh';
        modal.style.background = 'rgba(0,0,0,0.4)';
        modal.style.display = 'flex';
        modal.style.alignItems = 'center';
        modal.style.justifyContent = 'center';
        modal.style.zIndex = '9999';
        document.body.appendChild(modal);
    }
    modal.innerHTML = `<div id="tag-add-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-tag-add" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>新增标签</h3>
        <form id="add-tag-form">
            <div class="form-group">
                <label class="form-label">名称</label>
                <input type="text" class="form-control" name="name" required>
            </div>
            <button type="submit" class="btn btn-primary">提交</button>
        </form>
        <div id="add-tag-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-tag-add').onclick = () => {
        modal.style.display = 'none';
    };
    document.getElementById('add-tag-form').onsubmit = async function(e) {
        e.preventDefault();
        const form = e.target;
        const messageDiv = document.getElementById('add-tag-message');
        messageDiv.innerHTML = '';
        const reqBody = {
            name: form.name.value.trim()
        };
        try {
            await fetchAPI('/tags', 'POST', reqBody);
            showSuccess('新增标签成功', messageDiv);
            setTimeout(() => {
                modal.style.display = 'none';
                loadTagsTable(1);
            }, 1000);
        } catch (error) {
            showError(error.message || '新增标签失败', messageDiv);
        }
    };
}

/**
 * 显示编辑标签表单弹窗
 * @param {number} tagId - 标签ID
 */
async function showEditTagModal(tagId) {
    let modal = document.getElementById('tag-edit-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'tag-edit-modal';
        modal.style.position = 'fixed';
        modal.style.top = '0';
        modal.style.left = '0';
        modal.style.width = '100vw';
        modal.style.height = '100vh';
        modal.style.background = 'rgba(0,0,0,0.4)';
        modal.style.display = 'flex';
        modal.style.alignItems = 'center';
        modal.style.justifyContent = 'center';
        modal.style.zIndex = '9999';
        document.body.appendChild(modal);
    }
    modal.innerHTML = `<div id="tag-edit-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-tag-edit" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>编辑标签</h3>
        <form id="edit-tag-form"></form>
        <div id="edit-tag-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-tag-edit').onclick = () => {
        modal.style.display = 'none';
    };
    const form = document.getElementById('edit-tag-form');
    const messageDiv = document.getElementById('edit-tag-message');
    messageDiv.innerHTML = '';
    form.innerHTML = '';
    const loading = showLoading(form);
    try {
        const data = await fetchAPI(`/tags/${tagId}`);
        form.innerHTML = `
            <div class="form-group">
                <label class="form-label">名称</label>
                <input type="text" class="form-control" name="name" value="${data.name || ''}" required>
            </div>
            <button type="submit" class="btn btn-primary">保存</button>
        `;
        form.onsubmit = async function(e) {
            e.preventDefault();
            messageDiv.innerHTML = '';
            const reqBody = {
                name: form.name.value.trim()
            };
            try {
                await fetchAPI(`/tags/${tagId}`, 'PUT', reqBody);
                showSuccess('编辑标签成功', messageDiv);
                setTimeout(() => {
                    modal.style.display = 'none';
                    loadTagsTable(1);
                }, 1000);
            } catch (error) {
                showError(error.message || '编辑标签失败', messageDiv);
            }
        };
    } catch (error) {
        showError(error.message || '加载标签信息失败', form);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 删除标签，带确认
 * @param {number} tagId - 标签ID
 */
async function deleteTag(tagId) {
    if (!confirm('确定要删除该标签吗？')) return;
    try {
        await fetchAPI(`/tags/${tagId}`, 'DELETE');
        alert('删除成功');
        loadTagsTable(1);
    } catch (error) {
        alert(error.message || '删除失败');
    }
} 