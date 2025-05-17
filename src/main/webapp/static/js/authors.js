/**
 * 作者管理相关功能
 * 严格按照API文档 /api/authors GET接口实现
 */

/**
 * 加载作者列表页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadAuthorsPage(container) {
    // 页面基础结构：搜索框、表格、分页
    container.innerHTML = `
        <h2>作者管理</h2>
        <button class="btn btn-primary" id="add-author-btn" style="margin-bottom:1rem;">新增作者</button>
        <div class="author-search-bar">
            <input type="text" id="author-search-input" class="form-control" placeholder="搜索作者名...">
            <button id="author-search-btn" class="btn btn-primary">搜索</button>
        </div>
        <div id="authors-table-container"></div>
        <div id="authors-pagination"></div>
    `;
    document.getElementById('add-author-btn').onclick = showAddAuthorModal;
    document.getElementById('author-search-btn').addEventListener('click', () => {
        loadAuthorsTable(1);
    });
    document.getElementById('author-search-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            loadAuthorsTable(1);
        }
    });
    loadAuthorsTable(1);
}

/**
 * 加载并渲染作者表格
 * @param {number} page - 当前页码
 */
async function loadAuthorsTable(page) {
    const tableContainer = document.getElementById('authors-table-container');
    const paginationContainer = document.getElementById('authors-pagination');
    const searchInput = document.getElementById('author-search-input');
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
        // 请求作者列表
        const data = await fetchAPI(`/authors?${params.toString()}`);
        // data结构应为 { data: [...], pagination: {...} }
        renderAuthorsTable(data.data || [], tableContainer);
        // 渲染分页
        if (data.pagination) {
            const paginationEl = createPagination(data.pagination, loadAuthorsTable);
            paginationContainer.appendChild(paginationEl);
        }
    } catch (error) {
        showError(error.message || '加载作者列表失败', tableContainer);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 渲染作者表格
 * @param {Array} authors - 作者数组
 * @param {HTMLElement} container - 表格容器
 */
function renderAuthorsTable(authors, container) {
    if (!authors.length) {
        container.innerHTML = '<p>暂无作者数据。</p>';
        return;
    }
    // 构建表格
    const table = document.createElement('table');
    table.className = 'table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>ID</th>
                <th>名</th>
                <th>姓</th>
                <th>简介</th>
                <th>创建时间</th>
                <th>更新时间</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
            ${authors.map(author => `
                <tr>
                    <td>${author.id}</td>
                    <td>${author.firstName || ''}</td>
                    <td>${author.lastName || ''}</td>
                    <td>${author.bio || ''}</td>
                    <td>${author.createdAt ? formatDate(author.createdAt) : ''}</td>
                    <td>${author.updatedAt ? formatDate(author.updatedAt) : ''}</td>
                    <td>
                        <button class="btn btn-primary" onclick="showAuthorDetail(${author.id})">详情</button>
                        <button class="btn btn-primary" onclick="showEditAuthorModal(${author.id})">编辑</button>
                        <button class="btn btn-danger" onclick="deleteAuthor(${author.id})">删除</button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

/**
 * 显示作者详情弹窗
 * @param {number} authorId - 作者ID
 */
async function showAuthorDetail(authorId) {
    let modal = document.getElementById('author-detail-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'author-detail-modal';
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
    modal.innerHTML = `<div id="author-detail-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-author-detail" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <div id="author-detail-body"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-author-detail').onclick = () => {
        modal.style.display = 'none';
    };
    const detailBody = document.getElementById('author-detail-body');
    detailBody.innerHTML = '';
    const loading = showLoading(detailBody);
    try {
        const data = await fetchAPI(`/authors/${authorId}`);
        detailBody.innerHTML = `
            <h3>作者详情</h3>
            <p><b>ID：</b>${data.id}</p>
            <p><b>名：</b>${data.firstName || ''}</p>
            <p><b>姓：</b>${data.lastName || ''}</p>
            <p><b>简介：</b>${data.bio || ''}</p>
            <p><b>创建时间：</b>${data.createdAt ? formatDate(data.createdAt) : ''}</p>
            <p><b>更新时间：</b>${data.updatedAt ? formatDate(data.updatedAt) : ''}</p>
        `;
    } catch (error) {
        showError(error.message || '加载作者详情失败', detailBody);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 显示新增作者表单弹窗
 */
async function showAddAuthorModal() {
    let modal = document.getElementById('author-add-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'author-add-modal';
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
    modal.innerHTML = `<div id="author-add-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-author-add" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>新增作者</h3>
        <form id="add-author-form">
            <div class="form-group">
                <label class="form-label">名</label>
                <input type="text" class="form-control" name="firstName" required>
            </div>
            <div class="form-group">
                <label class="form-label">姓</label>
                <input type="text" class="form-control" name="lastName">
            </div>
            <div class="form-group">
                <label class="form-label">简介</label>
                <textarea class="form-control" name="bio"></textarea>
            </div>
            <button type="submit" class="btn btn-primary">提交</button>
        </form>
        <div id="add-author-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-author-add').onclick = () => {
        modal.style.display = 'none';
    };
    document.getElementById('add-author-form').onsubmit = async function(e) {
        e.preventDefault();
        const form = e.target;
        const messageDiv = document.getElementById('add-author-message');
        messageDiv.innerHTML = '';
        const reqBody = {
            firstName: form.firstName.value.trim(),
            lastName: form.lastName.value.trim(),
            bio: form.bio.value.trim()
        };
        if (!reqBody.lastName) delete reqBody.lastName;
        if (!reqBody.bio) delete reqBody.bio;
        try {
            await fetchAPI('/authors', 'POST', reqBody);
            showSuccess('新增作者成功', messageDiv);
            setTimeout(() => {
                modal.style.display = 'none';
                loadAuthorsTable(1);
            }, 1000);
        } catch (error) {
            showError(error.message || '新增作者失败', messageDiv);
        }
    };
}

/**
 * 显示编辑作者表单弹窗
 * @param {number} authorId - 作者ID
 */
async function showEditAuthorModal(authorId) {
    let modal = document.getElementById('author-edit-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'author-edit-modal';
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
    modal.innerHTML = `<div id="author-edit-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-author-edit" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>编辑作者</h3>
        <form id="edit-author-form"></form>
        <div id="edit-author-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-author-edit').onclick = () => {
        modal.style.display = 'none';
    };
    const form = document.getElementById('edit-author-form');
    const messageDiv = document.getElementById('edit-author-message');
    messageDiv.innerHTML = '';
    form.innerHTML = '';
    const loading = showLoading(form);
    try {
        const data = await fetchAPI(`/authors/${authorId}`);
        form.innerHTML = `
            <div class="form-group">
                <label class="form-label">名</label>
                <input type="text" class="form-control" name="firstName" value="${data.firstName || ''}" required>
            </div>
            <div class="form-group">
                <label class="form-label">姓</label>
                <input type="text" class="form-control" name="lastName" value="${data.lastName || ''}">
            </div>
            <div class="form-group">
                <label class="form-label">简介</label>
                <textarea class="form-control" name="bio">${data.bio || ''}</textarea>
            </div>
            <button type="submit" class="btn btn-primary">保存</button>
        `;
        form.onsubmit = async function(e) {
            e.preventDefault();
            messageDiv.innerHTML = '';
            const reqBody = {
                firstName: form.firstName.value.trim(),
                lastName: form.lastName.value.trim(),
                bio: form.bio.value.trim()
            };
            if (!reqBody.lastName) delete reqBody.lastName;
            if (!reqBody.bio) delete reqBody.bio;
            try {
                await fetchAPI(`/authors/${authorId}`, 'PUT', reqBody);
                showSuccess('编辑作者成功', messageDiv);
                setTimeout(() => {
                    modal.style.display = 'none';
                    loadAuthorsTable(1);
                }, 1000);
            } catch (error) {
                showError(error.message || '编辑作者失败', messageDiv);
            }
        };
    } catch (error) {
        showError(error.message || '加载作者信息失败', form);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 删除作者，带确认
 * @param {number} authorId - 作者ID
 */
async function deleteAuthor(authorId) {
    if (!confirm('确定要删除该作者吗？')) return;
    try {
        await fetchAPI(`/authors/${authorId}`, 'DELETE');
        alert('删除成功');
        loadAuthorsTable(1);
    } catch (error) {
        alert(error.message || '删除失败');
    }
} 