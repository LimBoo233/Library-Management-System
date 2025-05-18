/**
 * 导航栏交互逻辑
 */

document.addEventListener('DOMContentLoaded', () => {
    // 获取所有导航项
    const navItems = document.querySelectorAll('.navbar-item');
    
    // 为每个导航项添加点击事件
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            
            // 获取目标页面
            const targetPage = item.getAttribute('data-page');
            
            // 移除所有导航项的active类
            navItems.forEach(navItem => navItem.classList.remove('active'));
            
            // 为当前点击的导航项添加active类
            item.classList.add('active');
            
            // 加载对应的页面内容
            loadPage(targetPage);
        });
    });
});

/**
 * 加载页面内容
 * @param {string} pageName - 页面名称
 */
async function loadPage(pageName) {
    const contentContainer = document.getElementById('content');
    
    // 显示加载状态
    const loadingElement = showLoading(contentContainer);
    
    try {
        // 根据页面名称加载对应的内容
        switch (pageName) {
            case 'books':
                await loadBooksPage(contentContainer);
                break;
            case 'authors':
                await loadAuthorsPage(contentContainer);
                break;
            case 'presses':
                await loadPressesPage(contentContainer);
                break;
            case 'tags':
                await loadTagsPage(contentContainer);
                break;
            case 'loans':
                await loadLoansPage(contentContainer);
                break;
            case 'login':
                await loadLoginPage(contentContainer);
                break;
            case 'register':
                await loadRegisterPage(contentContainer);
                break;
            default:
                contentContainer.innerHTML = '<h2>页面不存在</h2>';
        }
    } catch (error) {
        console.error('加载页面失败:', error);
        showError('加载页面失败，请稍后重试', contentContainer);
    } finally {
        // 移除加载状态
        removeLoading(loadingElement);
    }
}

// 页面加载函数将在后续实现
async function loadBooksPage(container) {
    container.innerHTML = '<h2>图书管理</h2><p>正在开发中...</p>';
}

async function loadAuthorsPage(container) {
    container.innerHTML = '<h2>作者管理</h2><p>正在开发中...</p>';
}

async function loadPressesPage(container) {
    container.innerHTML = '<h2>出版社管理</h2><p>正在开发中...</p>';
}

async function loadTagsPage(container) {
    container.innerHTML = '<h2>标签管理</h2><p>正在开发中...</p>';
}

async function loadLoansPage(container) {
    container.innerHTML = '<h2>借还管理</h2><p>正在开发中...</p>';
}

async function loadLoginPage(container) {
    container.innerHTML = '<h2>用户登录</h2><p>正在开发中...</p>';
}

async function loadRegisterPage(container) {
    container.innerHTML = '<h2>用户注册</h2><p>正在开发中...</p>';
} 