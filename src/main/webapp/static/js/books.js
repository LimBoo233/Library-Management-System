/**
 * 图书管理相关功能
 * 严格按照API文档 /api/books GET接口实现
 */

/**
 * 加载图书列表页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadBooksPage(container) {
    // 页面基础结构：搜索框、表格、分页
    container.innerHTML = `
        <h2>图书管理</h2>
        <div class="book-search-bar">
            <input type="text" id="book-search-input" class="form-control" placeholder="搜索书名或作者...">
            <button id="book-search-btn" class="btn btn-primary">搜索</button>
        </div>
        <div id="books-table-container"></div>
        <div id="books-pagination"></div>
    `;

    // 绑定搜索按钮事件
    document.getElementById('book-search-btn').addEventListener('click', () => {
        loadBooksTable(1); // 搜索时重置到第1页
    });
    // 回车也能触发搜索
    document.getElementById('book-search-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            loadBooksTable(1);
        }
    });

    // 初始加载第一页
    loadBooksTable(1);
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
                        <button class="btn btn-primary" onclick="alert('TODO: 查看详情')">详情</button>
                    </td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
} 