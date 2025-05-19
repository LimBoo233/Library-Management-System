/**
 * 图书管理相关功能
 * 严格按照API文档 /api/books GET接口实现
 */

/**
 * 加载图书列表页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadBooksPage(container) {
    container.innerHTML = `
        <h2>图书管理</h2>
        <button class="btn btn-primary" id="add-book-btn" style="margin-bottom:1rem;">新增图书</button>
        <div class="book-search-bar">
            <input type="text" id="book-search-input" class="form-control" placeholder="搜索书名或作者...">
            <button id="book-search-btn" class="btn btn-primary">搜索</button>
        </div>
        <div id="books-table-container"></div>
        <div id="books-pagination"></div>
    `;
    document.getElementById('add-book-btn').onclick = showAddBookModal;
    // 渲染筛选下拉框
    await renderFilters();
    // 绑定筛选事件
    document.getElementById('filter-tag').addEventListener('change', () => loadBooksTable(1));
    document.getElementById('filter-press').addEventListener('change', () => loadBooksTable(1));
    document.getElementById('book-search-btn').addEventListener('click', () => {
        loadBooksTable(1);
    });
    document.getElementById('book-search-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            loadBooksTable(1);
        }
    });
    loadBooksTable(1);
}

/**
 * 获取标签和出版社列表并渲染筛选下拉框
 * @returns {Promise<{tags: Array, presses: Array}>}
 */
async function renderFilters() {
    const searchBar = document.querySelector('.book-search-bar');
    // 创建筛选容器
    let filterBar = document.getElementById('book-filter-bar');
    if (!filterBar) {
        filterBar = document.createElement('div');
        filterBar.id = 'book-filter-bar';
        filterBar.style.display = 'flex';
        filterBar.style.gap = '1rem';
        filterBar.style.marginBottom = '1rem';
        searchBar.parentNode.insertBefore(filterBar, searchBar);
    }
    filterBar.innerHTML = `
        <select id="filter-tag" class="form-control" style="width:180px">
            <option value="">全部标签</option>
        </select>
        <select id="filter-press" class="form-control" style="width:180px">
            <option value="">全部出版社</option>
        </select>
    `;
    // 获取标签和出版社列表
    let tags = [], presses = [];
    try {
        const tagRes = await fetchAPI('/tags');
        tags = tagRes.data || [];
    } catch {}
    try {
        const pressRes = await fetchAPI('/presses');
        presses = pressRes.data || [];
    } catch {}
    // 渲染标签选项
    const tagSelect = document.getElementById('filter-tag');
    tags.forEach(tag => {
        const opt = document.createElement('option');
        opt.value = tag.id;
        opt.textContent = tag.name;
        tagSelect.appendChild(opt);
    });
    // 渲染出版社选项
    const pressSelect = document.getElementById('filter-press');
    presses.forEach(press => {
        const opt = document.createElement('option');
        opt.value = press.id;
        opt.textContent = press.name;
        pressSelect.appendChild(opt);
    });
    return { tags, presses };
}

/**
 * 加载并渲染图书表格
 * @param {number} page - 当前页码
 */
async function loadBooksTable(page) {
    const tableContainer = document.getElementById('books-table-container');
    const paginationContainer = document.getElementById('books-pagination');
    const searchInput = document.getElementById('book-search-input');
    const keyword = searchInput.value.trim();
    const tagId = document.getElementById('filter-tag')?.value;
    const pressId = document.getElementById('filter-press')?.value;

    // 清空内容并显示加载状态
    tableContainer.innerHTML = '';
    paginationContainer.innerHTML = '';
    const loading = showLoading(tableContainer);

    // 构造API请求参数，严格按照API文档
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', 10); // 每页10条
    if (keyword) params.append('search', keyword);
    if (tagId) params.append('tag', tagId);
    if (pressId) params.append('press', pressId);

    try {
        // 请求图书列表
        const data = await fetchAPI(`/books?${params.toString()}`);
        // data结构应为 { data: [...], pagination: {...} }
        renderBooksTable(data.data || [], tableContainer);
        // 渲染分页
        if (data.pagination) {
            const paginationEl = createPagination(data.pagination, loadBooksTable);
            paginationContainer.appendChild(paginationEl);
        }
    } catch (error) {
        showError(error.message || '加载图书列表失败', tableContainer);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 渲染图书表格
 * @param {Array} books - 图书数组
 * @param {HTMLElement} container - 表格容器
 */
function renderBooksTable(books, container) {
    if (!books.length) {
        container.innerHTML = '<p>暂无图书数据。</p>';
        return;
    }
    // 构建表格
    const table = document.createElement('table');
    table.className = 'table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>ID</th>
                <th>书名</th>
                <th>ISBN</th>
                <th>作者</th>
                <th>出版社</th>
                <th>标签</th>
                <th>可借数量</th>
                <th>操作</th>
            </tr>
        </thead>
        <tbody>
            ${books.map(book => `
                <tr>
                    <td>${book.id}</td>
                    <td>${book.title}</td>
                    <td>${book.isbn}</td>
                    <td>${(book.authors||[]).map(a=>a.firstName + (a.lastName?(' ' + a.lastName):'')).join(', ')}</td>
                    <td>${book.press ? book.press.name : ''}</td>
                    <td>${(book.tags||[]).map(t=>t.name).join(', ')}</td>
                    <td>${book.numCopiesAvailable}</td>
                    <td>
                        <button class="btn btn-primary" onclick="showBookDetail(${book.id})">详情</button>
                        <button class="btn btn-primary" onclick="showEditBookModal(${book.id})">编辑</button>
                        <button class="btn btn-danger" onclick="deleteBook(${book.id})">删除</button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

/**
 * 加载并展示图书详情
 * @param {number} bookId - 图书ID
 */
async function showBookDetail(bookId) {
    // 创建弹窗容器
    let modal = document.getElementById('book-detail-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'book-detail-modal';
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
    // 弹窗内容区域
    modal.innerHTML = `<div id="book-detail-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-book-detail" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <div id="book-detail-body"></div>
    </div>`;
    modal.style.display = 'flex';
    // 关闭按钮
    document.getElementById('close-book-detail').onclick = () => {
        modal.style.display = 'none';
    };
    // 加载详情内容
    const detailBody = document.getElementById('book-detail-body');
    detailBody.innerHTML = '';
    const loading = showLoading(detailBody);
    try {
        // 严格按照API文档 /api/books/{bookId} GET
        const data = await fetchAPI(`/books/${bookId}`);
        // 渲染详情
        detailBody.innerHTML = `
            <h3>图书详情</h3>
            <p><b>ID：</b>${data.id}</p>
            <p><b>书名：</b>${data.title}</p>
            <p><b>ISBN：</b>${data.isbn}</p>
            <p><b>作者：</b>${(data.authors||[]).map(a=>a.firstName + (a.lastName?(' ' + a.lastName):'')).join(', ')}</p>
            <p><b>出版社：</b>${data.press ? data.press.name : ''}</p>
            <p><b>标签：</b>${(data.tags||[]).map(t=>t.name).join(', ')}</p>
            <p><b>总藏书数：</b>${data.numCopiesTotal}</p>
            <p><b>可借数量：</b>${data.numCopiesAvailable}</p>
            <p><b>创建时间：</b>${formatDate(data.createdAt)}</p>
            <p><b>更新时间：</b>${formatDate(data.updatedAt)}</p>
        `;
    } catch (error) {
        showError(error.message || '加载图书详情失败', detailBody);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 显示新增图书表单弹窗
 */
async function showAddBookModal() {
    // 创建弹窗容器
    let modal = document.getElementById('book-add-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'book-add-modal';
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
    // 弹窗内容区域
    modal.innerHTML = `<div id="book-add-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-book-add" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>新增图书</h3>
        <form id="add-book-form">
            <div class="form-group">
                <label class="form-label">书名</label>
                <input type="text" class="form-control" name="title" required>
            </div>
            <div class="form-group">
                <label class="form-label">ISBN</label>
                <input type="text" class="form-control" name="isbn" required>
            </div>
            <div class="form-group">
                <label class="form-label">可借数量</label>
                <input type="number" class="form-control" name="numCopiesAvailable" min="1" required>
            </div>
            <div class="form-group">
                <label class="form-label">作者ID（可填多个，用英文逗号分隔）</label>
                <input type="text" class="form-control" name="authorIds" placeholder="如：1,2">
            </div>
            <div class="form-group">
                <label class="form-label">出版社ID</label>
                <input type="number" class="form-control" name="pressId">
            </div>
            <div class="form-group">
                <label class="form-label">标签ID（可填多个，用英文逗号分隔）</label>
                <input type="text" class="form-control" name="tagIds" placeholder="如：1,4">
            </div>
            <button type="submit" class="btn btn-primary">提交</button>
        </form>
        <div id="add-book-message"></div>
    </div>`;
    modal.style.display = 'flex';
    // 关闭按钮
    document.getElementById('close-book-add').onclick = () => {
        modal.style.display = 'none';
    };
    // 表单提交事件
    document.getElementById('add-book-form').onsubmit = async function(e) {
        e.preventDefault();
        const form = e.target;
        const messageDiv = document.getElementById('add-book-message');
        messageDiv.innerHTML = '';
        // 构造请求体，严格按照API文档
        const title = form.title.value.trim();
        const isbn = form.isbn.value.trim();
        const numCopiesAvailable = parseInt(form.numCopiesAvailable.value, 10);
        const authorIds = form.authorIds.value.split(',').map(s=>s.trim()).filter(Boolean).map(Number);
        const pressId = form.pressId.value ? Number(form.pressId.value) : null;
        const tagIds = form.tagIds.value.split(',').map(s=>s.trim()).filter(Boolean).map(Number);
        const reqBody = {
            title,
            isbn,
            numCopiesAvailable,
            authorIds,
            pressId,
            tagIds
        };
        // 清理空数组/空值
        if (!authorIds.length) delete reqBody.authorIds;
        if (!tagIds.length) delete reqBody.tagIds;
        if (!pressId) delete reqBody.pressId;
        try {
            const data = await fetchAPI('/books', 'POST', reqBody);
            showSuccess('新增图书成功', messageDiv);
            // 1秒后关闭弹窗并刷新列表
            setTimeout(() => {
                modal.style.display = 'none';
                loadBooksTable(1);
            }, 1000);
        } catch (error) {
            showError(error.message || '新增图书失败', messageDiv);
        }
    };
}

/**
 * 显示编辑图书表单弹窗
 * @param {number} bookId - 图书ID
 */
async function showEditBookModal(bookId) {
    // 创建弹窗容器
    let modal = document.getElementById('book-edit-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'book-edit-modal';
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
    // 弹窗内容区域
    modal.innerHTML = `<div id="book-edit-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-book-edit" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>编辑图书</h3>
        <form id="edit-book-form"></form>
        <div id="edit-book-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-book-edit').onclick = () => {
        modal.style.display = 'none';
    };
    // 加载原始数据并填充表单
    const form = document.getElementById('edit-book-form');
    const messageDiv = document.getElementById('edit-book-message');
    messageDiv.innerHTML = '';
    form.innerHTML = '';
    const loading = showLoading(form);
    try {
        // 获取原始数据，严格按照API文档
        const data = await fetchAPI(`/books/${bookId}`);
        form.innerHTML = `
            <div class="form-group">
                <label class="form-label">书名</label>
                <input type="text" class="form-control" name="title" value="${data.title}" required>
            </div>
            <div class="form-group">
                <label class="form-label">ISBN</label>
                <input type="text" class="form-control" name="isbn" value="${data.isbn}" required>
            </div>
            <div class="form-group">
                <label class="form-label">可借数量</label>
                <input type="number" class="form-control" name="numCopiesAvailable" min="1" value="${data.numCopiesAvailable}" required>
            </div>
            <div class="form-group">
                <label class="form-label">作者ID（可填多个，用英文逗号分隔）</label>
                <input type="text" class="form-control" name="authorIds" value="${(data.authors||[]).map(a=>a.id).join(',')}">
            </div>
            <div class="form-group">
                <label class="form-label">出版社ID</label>
                <input type="number" class="form-control" name="pressId" value="${data.press ? data.press.id : ''}">
            </div>
            <div class="form-group">
                <label class="form-label">标签ID（可填多个，用英文逗号分隔）</label>
                <input type="text" class="form-control" name="tagIds" value="${(data.tags||[]).map(t=>t.id).join(',')}">
            </div>
            <button type="submit" class="btn btn-primary">保存</button>
        `;
        // 表单提交事件
        form.onsubmit = async function(e) {
            e.preventDefault();
            messageDiv.innerHTML = '';
            // 构造请求体，严格按照API文档
            const title = form.title.value.trim();
            const isbn = form.isbn.value.trim();
            const numCopiesAvailable = parseInt(form.numCopiesAvailable.value, 10);
            const authorIds = form.authorIds.value.split(',').map(s=>s.trim()).filter(Boolean).map(Number);
            const pressId = form.pressId.value ? Number(form.pressId.value) : null;
            const tagIds = form.tagIds.value.split(',').map(s=>s.trim()).filter(Boolean).map(Number);
            const reqBody = {
                title,
                isbn,
                numCopiesAvailable,
                authorIds,
                pressId,
                tagIds
            };
            if (!authorIds.length) delete reqBody.authorIds;
            if (!tagIds.length) delete reqBody.tagIds;
            if (!pressId) delete reqBody.pressId;
            try {
                await fetchAPI(`/books/${bookId}`, 'PUT', reqBody);
                showSuccess('编辑图书成功', messageDiv);
                setTimeout(() => {
                    modal.style.display = 'none';
                    loadBooksTable(1);
                }, 1000);
            } catch (error) {
                showError(error.message || '编辑图书失败', messageDiv);
            }
        };
    } catch (error) {
        showError(error.message || '加载图书信息失败', form);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 删除图书，带确认
 * @param {number} bookId - 图书ID
 */
async function deleteBook(bookId) {
    if (!confirm('确定要删除这本图书吗？')) return;
    try {
        await fetchAPI(`/books/${bookId}`, 'DELETE');
        alert('删除成功');
        loadBooksTable(1);
    } catch (error) {
        alert(error.message || '删除失败');
    }
} 