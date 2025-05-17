/**
 * 用户认证相关功能
 */

/**
 * 加载登录页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadLoginPage(container) {
    // 创建登录表单
    const form = document.createElement('form');
    form.className = 'auth-form';
    form.innerHTML = `
        <h2>用户登录</h2>
        <div class="form-group">
            <label class="form-label" for="account">账号</label>
            <input type="text" class="form-control" id="account" name="account" required>
        </div>
        <div class="form-group">
            <label class="form-label" for="password">密码</label>
            <input type="password" class="form-control" id="password" name="password" required>
        </div>
        <button type="submit" class="btn btn-primary">登录</button>
        <p class="form-footer">
            还没有账号？<a href="#" data-page="register">立即注册</a>
        </p>
    `;

    // 添加表单提交事件
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // 获取表单数据
        const account = form.account.value;
        const password = form.password.value;

        try {
            // 调用登录接口
            const response = await fetchAPI('/auth/login', 'POST', {
                account,
                password
            });

            // 登录成功，保存用户信息
            localStorage.setItem('user', JSON.stringify(response.user));
            
            // 显示成功消息
            showSuccess('登录成功', container);
            
            // 更新导航栏状态
            updateNavbarState();
            
            // 跳转到图书管理页面
            setTimeout(() => {
                loadPage('books');
            }, 1000);
        } catch (error) {
            showError(error.message || '登录失败，请检查账号和密码', container);
        }
    });

    // 清空容器并添加表单
    container.innerHTML = '';
    container.appendChild(form);

    // 绑定"立即注册"跳转事件
    const registerLink = form.querySelector('[data-page="register"]');
    if (registerLink) {
        registerLink.addEventListener('click', (e) => {
            e.preventDefault();
            loadPage('register');
        });
    }
}

/**
 * 加载注册页面
 * @param {HTMLElement} container - 页面容器
 */
async function loadRegisterPage(container) {
    // 创建注册表单
    const form = document.createElement('form');
    form.className = 'auth-form';
    form.innerHTML = `
        <h2>用户注册</h2>
        <div class="form-group">
            <label class="form-label" for="username">显示名称</label>
            <input type="text" class="form-control" id="username" name="username" required>
        </div>
        <div class="form-group">
            <label class="form-label" for="account">登录账号</label>
            <input type="text" class="form-control" id="account" name="account" required>
        </div>
        <div class="form-group">
            <label class="form-label" for="password">密码</label>
            <input type="password" class="form-control" id="password" name="password" required>
        </div>
        <button type="submit" class="btn btn-primary">注册</button>
        <p class="form-footer">
            已有账号？<a href="#" data-page="login">立即登录</a>
        </p>
    `;

    // 添加表单提交事件
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        // 获取表单数据
        const username = form.username.value;
        const account = form.account.value;
        const password = form.password.value;

        try {
            // 调用注册接口
            const response = await fetchAPI('/auth/register', 'POST', {
                username,
                account,
                password
            });

            // 显示成功消息
            showSuccess('注册成功，请登录', container);
            
            // 跳转到登录页面
            setTimeout(() => {
                loadPage('login');
            }, 1000);
        } catch (error) {
            showError(error.message || '注册失败，请稍后重试', container);
        }
    });

    // 清空容器并添加表单
    container.innerHTML = '';
    container.appendChild(form);

    // 绑定"立即登录"跳转事件
    const loginLink = form.querySelector('[data-page="login"]');
    if (loginLink) {
        loginLink.addEventListener('click', (e) => {
            e.preventDefault();
            loadPage('login');
        });
    }
}

/**
 * 更新导航栏状态
 */
function updateNavbarState() {
    const user = JSON.parse(localStorage.getItem('user'));
    const navbarEnd = document.querySelector('.navbar-end');
    
    if (user) {
        // 用户已登录
        navbarEnd.innerHTML = `
            <span class="navbar-item">欢迎，${user.username}</span>
            <a href="#" class="navbar-item" id="logout">退出</a>
        `;
        
        // 添加退出登录事件
        document.getElementById('logout').addEventListener('click', (e) => {
            e.preventDefault();
            localStorage.removeItem('user');
            updateNavbarState();
            loadPage('login');
        });
    } else {
        // 用户未登录
        navbarEnd.innerHTML = `
            <a href="#" class="navbar-item" data-page="login">登录</a>
            <a href="#" class="navbar-item" data-page="register">注册</a>
        `;
        
        // 重新绑定导航事件
        navbarEnd.querySelectorAll('.navbar-item').forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const targetPage = item.getAttribute('data-page');
                loadPage(targetPage);
            });
        });
    }
}

// 页面加载时检查登录状态
document.addEventListener('DOMContentLoaded', () => {
    updateNavbarState();
}); 