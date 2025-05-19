/**
 * 借还管理相关功能
 * 严格按照API文档 /api/loans 相关接口实现
 */

/**
 * 加载借还管理页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadLoansPage(container) {
    container.innerHTML = `
        <h2>借还管理</h2>
        <button class="btn btn-primary" id="loan-checkout-btn" style="margin-bottom:1rem;">借书</button>
        <button class="btn btn-primary" id="loan-return-btn" style="margin-bottom:1rem;margin-left:1rem;">还书</button>
        <div class="loan-search-bar">
            <input type="text" id="loan-user-id-input" class="form-control" placeholder="按用户ID查询借阅记录..." style="width:200px;display:inline-block;">
            <button id="loan-search-btn" class="btn btn-primary">查询</button>
        </div>
        <div id="loans-table-container"></div>
        <div id="loans-pagination"></div>
    `;
    document.getElementById('loan-checkout-btn').onclick = showLoanCheckoutModal;
    document.getElementById('loan-return-btn').onclick = showLoanReturnModal;
    document.getElementById('loan-search-btn').addEventListener('click', () => {
        loadLoansTable(1);
    });
    document.getElementById('loan-user-id-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            loadLoansTable(1);
        }
    });
    loadLoansTable(1);
}

/**
 * 加载并渲染借阅记录表格
 * @param {number} page - 当前页码
 */
async function loadLoansTable(page) {
    const tableContainer = document.getElementById('loans-table-container');
    const paginationContainer = document.getElementById('loans-pagination');
    const userIdInput = document.getElementById('loan-user-id-input');
    const userId = userIdInput.value.trim();

    // 清空内容并显示加载状态
    tableContainer.innerHTML = '';
    paginationContainer.innerHTML = '';
    const loading = showLoading(tableContainer);

    // 构造API请求参数，严格按照API文档
    const params = new URLSearchParams();
    params.append('page', page);
    params.append('size', 10); // 每页10条
    if (userId) params.append('userId', userId);

    try {
        // 请求借阅记录列表
        const data = await fetchAPI(`/loans?${params.toString()}`);
        // data结构应为 { data: [...], pagination: {...} }
        renderLoansTable(data.data || [], tableContainer);
        // 渲染分页
        if (data.pagination) {
            const paginationEl = createPagination(data.pagination, loadLoansTable);
            paginationContainer.appendChild(paginationEl);
        }
    } catch (error) {
        showError(error.message || '加载借阅记录失败', tableContainer);
    } finally {
        removeLoading(loading);
    }
}

/**
 * 渲染借阅记录表格
 * @param {Array} loans - 借阅记录数组
 * @param {HTMLElement} container - 表格容器
 */
function renderLoansTable(loans, container) {
    if (!loans.length) {
        container.innerHTML = '<p>暂无借阅记录。</p>';
        return;
    }
    // 构建表格
    const table = document.createElement('table');
    table.className = 'table';
    table.innerHTML = `
        <thead>
            <tr>
                <th>ID</th>
                <th>用户ID</th>
                <th>图书ID</th>
                <th>借出时间</th>
                <th>应还时间</th>
                <th>归还时间</th>
                <th>是否逾期</th>
            </tr>
        </thead>
        <tbody>
            ${loans.map(loan => `
                <tr>
                    <td style="text-align: center;">${loan.id}</td>
                    <td style="text-align: center;">${loan.userId}</td>
                    <td style="text-align: center;">${loan.bookId}</td>
                    <td style="text-align: center;">${loan.checkoutDate ? formatDate(loan.checkoutDate).split(' ')[0] : ''}</td>
                    <td style="text-align: center;">${loan.dueDate ? formatDate(loan.dueDate).split(' ')[0] : ''}</td>
                    <td style="text-align: center;">${loan.returnDate ? formatDate(loan.returnDate).split(' ')[0] : ''}</td>
                    <td style="text-align: center;">${loan.isOverdue ? '是' : '否'}</td>
                </tr>
            `).join('')}
        </tbody>
    `;
    container.innerHTML = '';
    container.appendChild(table);
}

/**
 * 显示借书弹窗
 */
async function showLoanCheckoutModal() {
    let modal = document.getElementById('loan-checkout-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'loan-checkout-modal';
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
    modal.innerHTML = `<div id="loan-checkout-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-loan-checkout" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>借书</h3>
        <form id="loan-checkout-form">
            <div class="form-group">
                <label class="form-label">用户ID</label>
                <input type="number" class="form-control" name="userId" required>
            </div>
            <div class="form-group">
                <label class="form-label">图书ID</label>
                <input type="number" class="form-control" name="bookId" required>
            </div>
            <button type="submit" class="btn btn-primary">提交</button>
        </form>
        <div id="loan-checkout-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-loan-checkout').onclick = () => {
        modal.style.display = 'none';
    };
    document.getElementById('loan-checkout-form').onsubmit = async function(e) {
        e.preventDefault();
        const form = e.target;
        const messageDiv = document.getElementById('loan-checkout-message');
        messageDiv.innerHTML = '';
        const reqBody = {
            userId: Number(form.userId.value),
            bookId: Number(form.bookId.value)
        };
        try {
            await fetchAPI('/loans/checkout', 'POST', reqBody);
            showSuccess('借书成功', messageDiv);
            setTimeout(() => {
                modal.style.display = 'none';
                loadLoansTable(1);
            }, 1000);
        } catch (error) {
            showError(error.message || '借书失败', messageDiv);
        }
    };
}

/**
 * 显示还书弹窗
 */
async function showLoanReturnModal() {
    let modal = document.getElementById('loan-return-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'loan-return-modal';
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
    modal.innerHTML = `<div id="loan-return-content" style="background:#fff;padding:2rem;min-width:350px;max-width:90vw;border-radius:8px;position:relative;box-shadow:0 2px 8px rgba(0,0,0,0.2)">
        <button id="close-loan-return" style="position:absolute;top:10px;right:10px;font-size:18px;background:none;border:none;cursor:pointer;">×</button>
        <h3>还书</h3>
        <form id="loan-return-form">
            <div class="form-group">
                <label class="form-label">借阅记录ID</label>
                <input type="number" class="form-control" name="loanId" required>
            </div>
            <button type="submit" class="btn btn-primary">提交</button>
        </form>
        <div id="loan-return-message"></div>
    </div>`;
    modal.style.display = 'flex';
    document.getElementById('close-loan-return').onclick = () => {
        modal.style.display = 'none';
    };
    document.getElementById('loan-return-form').onsubmit = async function(e) {
        e.preventDefault();
        const form = e.target;
        const messageDiv = document.getElementById('loan-return-message');
        messageDiv.innerHTML = '';
        const reqBody = {
            loanId: Number(form.loanId.value)
        };
        try {
            await fetchAPI('/loans/return', 'POST', reqBody);
            showSuccess('还书成功', messageDiv);
            setTimeout(() => {
                modal.style.display = 'none';
                loadLoansTable(1);
            }, 1000);
        } catch (error) {
            showError(error.message || '还书失败', messageDiv);
        }
    };
} 