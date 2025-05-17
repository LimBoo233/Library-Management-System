/**
 * 出版社管理相关功能
 * 严格按照API文档 /api/presses 相关接口实现
 */

/**
 * 加载出版社列表页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadPressesPage(container) {
    // 页面基础结构：搜索框、表格、分页
    container.innerHTML = `
        <h2>出版社管理</h2>
        <button class="btn btn-primary" id="add-press-btn" style="margin-bottom:1rem;">新增出版社</button>
        <div class="press-search-bar">
            <input type="text" id="press-search-input" class="form-control" placeholder="搜索出版社名...">
            <button id="press-search-btn" class="btn btn-primary">搜索</button>
        </div>
        <div id="presses-table-container"></div>
        <div id="presses-pagination"></div>
    `;
    document.getElementById('add-press-btn').onclick = showAddPressModal;
    document.getElementById('press-search-btn').addEventListener('click', () => {
        loadPressesTable(1);
    });
    document.getElementById('press-search-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            loadPressesTable(1);
        }
    });
    loadPressesTable(1);
}

/**
 * 加载并渲染出版社表格
 * @param {number} page - 当前页码
 */
async function loadPressesTable(page) {
    const tableContainer = document.getElementById('presses-table-container');
    const paginationContainer = document.getElementById('presses-pagination');
    const searchInput = document.getElementById('press-search-input');
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
        // 请求出版社列表
        const data = await fetchAPI(`/presses?${params.toString()}`);
        // data结构应为 { data: [...], pagination: {...} }
        renderPressesTable(data.data || [], tableContainer);
        // 渲染分页
        if (data.pagination) {
            const paginationEl = createPagination(data.pagination, loadPressesTable);
            paginationContainer.appendChild(paginationEl);
        }
    } catch (error) {
        showError(error.message || '加载出版社列表失败', tableContainer);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 渲染出版社表格
 * @param {Array} presses - 出版社数组
 * @param {HTMLElement} container - 表格容器
 */
function renderPressesTable(presses, container) {
    if (!presses.length) {
        container.innerHTML = '<p>暂无出版社数据。</p>';
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
            ${presses.map(press => `
                <tr>
                    <td>${press.id}</td>
                    <td>${press.name || ''}</td>
                    <td>
                        <button class="btn btn-primary" onclick="showPressDetail(${press.id})">详情</button>
                        <button class="btn btn-primary" onclick="showEditPressModal(${press.id})">编辑</button>
                        <button class="btn btn-danger" onclick="deletePress(${press.id})">删除</button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

/**
 * 显示出版社详情弹窗
 * @param {number} pressId - 出版社ID
 */
async function showPressDetail(pressId) {
    let modal = document.getElementById('press-detail-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'press-detail-modal';
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
    modal.innerHTML = `<div id="press-detail-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-press-detail" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <div id="press-detail-body"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-press-detail').onclick = () => {
        modal.style.display = 'none';
    };
    const detailBody = document.getElementById('press-detail-body');
    detailBody.innerHTML = '';
    const loading = showLoading(detailBody);
    try {
        const data = await fetchAPI(`/presses/${pressId}`);
        detailBody.innerHTML = `
            <h3>出版社详情</h3>
            <p><b>ID：</b>${data.id}</p>
            <p><b>名称：</b>${data.name || ''}</p>
        `;
    } catch (error) {
        showError(error.message || '加载出版社详情失败', detailBody);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 显示新增出版社表单弹窗
 */
async function showAddPressModal() {
    let modal = document.getElementById('press-add-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'press-add-modal';
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
    modal.innerHTML = `<div id="press-add-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-press-add" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>新增出版社</h3>
        <form id="add-press-form">
            <div class="form-group">
                <label class="form-label">名称</label>
                <input type="text" class="form-control" name="name" required>
            </div>
            <button type="submit" class="btn btn-primary">提交</button>
        </form>
        <div id="add-press-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-press-add').onclick = () => {
        modal.style.display = 'none';
    };
    document.getElementById('add-press-form').onsubmit = async function(e) {
        e.preventDefault();
        const form = e.target;
        const messageDiv = document.getElementById('add-press-message');
        messageDiv.innerHTML = '';
        const reqBody = {
            name: form.name.value.trim()
        };
        try {
            await fetchAPI('/presses', 'POST', reqBody);
            showSuccess('新增出版社成功', messageDiv);
            setTimeout(() => {
                modal.style.display = 'none';
                loadPressesTable(1);
            }, 1000);
        } catch (error) {
            showError(error.message || '新增出版社失败', messageDiv);
        }
    };
}

/**
 * 显示编辑出版社表单弹窗
 * @param {number} pressId - 出版社ID
 */
async function showEditPressModal(pressId) {
    let modal = document.getElementById('press-edit-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'press-edit-modal';
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
    modal.innerHTML = `<div id="press-edit-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-press-edit" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>编辑出版社</h3>
        <form id="edit-press-form"></form>
        <div id="edit-press-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-press-edit').onclick = () => {
        modal.style.display = 'none';
    };
    const form = document.getElementById('edit-press-form');
    const messageDiv = document.getElementById('edit-press-message');
    messageDiv.innerHTML = '';
    form.innerHTML = '';
    const loading = showLoading(form);
    try {
        const data = await fetchAPI(`/presses/${pressId}`);
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
                await fetchAPI(`/presses/${pressId}`, 'PUT', reqBody);
                showSuccess('编辑出版社成功', messageDiv);
                setTimeout(() => {
                    modal.style.display = 'none';
                    loadPressesTable(1);
                }, 1000);
            } catch (error) {
                showError(error.message || '编辑出版社失败', messageDiv);
            }
        };
    } catch (error) {
        showError(error.message || '加载出版社信息失败', form);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 删除出版社，带确认
 * @param {number} pressId - 出版社ID
 */
async function deletePress(pressId) {
    if (!confirm('确定要删除该出版社吗？')) return;
    try {
        await fetchAPI(`/presses/${pressId}`, 'DELETE');
        alert('删除成功');
        loadPressesTable(1);
    } catch (error) {
        alert(error.message || '删除失败');
    }
} 