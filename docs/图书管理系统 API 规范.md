## 基本信息

- **Base URL 前缀**：`/api`
    
- **协议**：HTTPS（建议）
    
- **请求/响应格式**：JSON
    
- **默认头部**：`Content-Type: application/json`
    

---

## 一、标准响应格式

### 1. 错误响应（所有 4xx/5xx）

```
{
  "timestamp": "2025-05-12T17:44:00Z",  // UTC 时间，ISO8601 格式
  "status": 404,                          // HTTP 状态码
  "error": "Not Found",                 // 状态描述
  "message": "Book with ID 999 not found.", // 具体错误信息
  "path": "/api/books/999"
  // 可选: "errors": [ { "field":"isbn","message":"格式非法" }, ... ]
}
```

### 2. 分页列表响应（GET 列表）

```
{
  "data": [ /* 资源对象数组 */ ],
  "pagination": {
    "currentPage": 1,   // 当前页（1 起）
    "pageSize": 10,     // 每页条数
    "totalPages": 5,    // 总页数
    "totalItems": 48    // 总记录数
  }
}
```

---

## 二、认证与用户 (`/api/auth`)

### 1. POST `/api/auth/register`

**用途**：用户注册（创建新账号）。

**请求体**：

```
{
  "username": "张三",    // 显示名称
  "account": "zhangsan", // 登录账号
  "password": "pass1234"  // 密码
}
```

**成功响应 (201 Created)**：

```
{
  "id": 5,
  "username": "张三",
  "account": "zhangsan"
}
```

**错误响应 (400 Bad Request)**：字段校验失败或账号已存在。

---

### 2. POST `/api/auth/login`

**用途**：用户登录，获取 JWT。

**请求体**：

```
{
  "account": "zhangsan",
  "password": "pass1234"
}
```

**成功响应 (200 OK)**：

```
{
  "token": "<JWT>...",
  "user": {
    "id": 5,
    "username": "张三",
    "account": "zhangsan"
  }
}
```

**错误响应 (401 Unauthorized)**：凭据无效。

---

## 三、图书管理 (`/api/books`)

所有接口均需在请求头携带 `Authorization: Bearer <token>`。

### 1. POST `/api/books`

**用途**：新增图书。

**请求体**：

```
{
  "title": "Clean Code",
  "isbn": "9780132350884",
  "numCopiesAvailable": 10,
  "authorIds": [3],
  "pressId": 6,
  "tagIds": [1,4]
}
```

**成功响应 (201 Created)**：

```
{
  "id": 124,
  "title": "Clean Code",
  "isbn": "9780132350884",
  "numCopiesTotal": 10,
  "numCopiesAvailable": 10,
  "createdAt": "2025-05-12T17:44:00Z",
  "updatedAt": "2025-05-12T17:44:00Z",
  "authors": [ { "id":3, "firstName":"Robert","lastName":"Martin" } ],
  "press": { "id":6, "name":"Prentice Hall" },
  "tags": [ {"id":1,"name":"Programming"}, {"id":4,"name":"Refactoring"} ]
}
```

**错误响应**：400/401。

---

### 2. GET `/api/books`

**用途**：获取图书列表（分页、搜索、过滤）。

**查询参数**：

- `page`（页码，默认1）
    
- `size`（每页条数，默认10）
    
- `search`（标题/作者关键词）
    
- `tag`（标签ID）
    
- `press`（印刷厂ID）
    

**成功响应 (200 OK)**：分页列表格式，`data` 数组内为简化 Book 对象。

---

### 3. GET `/api/books/{bookId}`

**用途**：获取指定图书详情。

**示例请求**：

```
GET /api/books/124 HTTP/1.1
Host: your-domain.com
Authorization: Bearer <token>
```

**成功响应 (200 OK)**：完整 Book 对象，示例：

```
{
  "id": 124,
  "title": "Clean Code",
  "isbn": "9780132350884",
  "numCopiesTotal": 10,
  "numCopiesAvailable": 10,
  "createdAt": "2025-05-12T17:44:00Z",
  "updatedAt": "2025-05-12T17:44:00Z",
  "authors": [ { "id":3, "firstName":"Robert","lastName":"Martin" } ],
  "press": { "id":6, "name":"Prentice Hall" },
  "tags": [ {"id":1,"name":"Programming"}, {"id":4,"name":"Refactoring"} ]
}
```

**错误响应 (404 Not Found)**：

```
{
  "timestamp": "2025-05-12T17:45:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Book with ID 999 not found.",
  "path": "/api/books/999"
}
```

---

### 4. PUT `/api/books/{bookId}`

**用途**：更新整本图书。

**请求体**：与 POST 相同，所有可更新字段必填。

**成功响应 (200 OK)**：更新后的完整 Book 对象。

---

### 5. DELETE `/api/books/{bookId}`

**用途**：删除图书（需无在借记录）。

**成功响应 (204 No Content)**。

**错误响应**：400/404/401。

---

## 四、作者管理 (`/api/authors`)

接口与图书类似，支持 `GET` 列表、`GET /{id}`、`POST`、`PUT /{id}`、`DELETE /{id}`，返回 Author 对象：

```
{
  "id":4,
  "firstName":"Martin",
  "lastName":"Fowler",
  "bio":"...",
  "createdAt":"2025-05-12T17:44:00Z",
  "updatedAt":"2025-05-12T17:44:00Z"
}
```

---

## 五、出版社管理 (`/api/presses`)

支持 `GET` 列表、`GET /{id}`、`POST`、`PUT /{id}`、`DELETE /{id}`，Press 对象仅含：

```
{ "id":6, "name":"Prentice Hall" }
```

---

## 六、标签管理 (`/api/tags`)

支持同 Authors，Tag 对象：

```
{ "id":1, "name":"Programming" }
```

---

## 七、借还管理 (`/api/loans`)

### 1. POST `/api/loans/checkout`

**用途**：借出图书。

**请求体**：

```
{ "userId":5, "bookId":124 }
```

**成功响应 (201 Created)**：新 Loan 对象：

```
{
  "id":501,
  "userId":5,
  "bookId":124,
  "checkoutDate":"2025-05-12T17:44:00Z",
  "dueDate":"2025-05-26T17:44:00Z",
  "returnDate":null,
  "isOverdue":false
}
```

### 2. POST `/api/loans/return`

**用途**：归还图书。

**请求体**：

```
{ "loanId":501 }
```

**成功响应 (200 OK)**：更新后的 Loan 对象。

---

### 3. GET `/api/loans`（可选）

**用途**：查询借阅记录，支持 `?userId=5` 或分页。

**成功响应**：分页列表，`data` 数组内为 Loan 对象。

---

_以上即为最终 API 文档，前后端可据此无歧义地开发和联调。_