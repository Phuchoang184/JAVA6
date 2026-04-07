-- ============================================================
--   LUXURY FASHION E-COMMERCE DATABASE
--   SQL Server 2019+
--   Chuẩn hóa 3NF | Full FK | Index tối ưu
--   Created: 2026-04-01
-- ============================================================

USE master;
GO

IF EXISTS (SELECT name FROM sys.databases WHERE name = N'LuxuryFashionDB')
    DROP DATABASE LuxuryFashionDB;
GO

CREATE DATABASE LuxuryFashionDB
    COLLATE Vietnamese_CI_AS;
GO

USE LuxuryFashionDB;
GO

-- ============================================================
-- 1. ROLES
-- ============================================================
CREATE TABLE Roles (
    RoleId   INT           NOT NULL IDENTITY(1,1),
    RoleName NVARCHAR(50)  NOT NULL,
    CONSTRAINT PK_Roles         PRIMARY KEY (RoleId),
    CONSTRAINT UQ_Roles_Name    UNIQUE (RoleName)
);
GO

-- ============================================================
-- 2. USERS
-- ============================================================
CREATE TABLE Users (
    UserId        INT            NOT NULL IDENTITY(1,1),
    RoleId        INT            NOT NULL,
    FullName      NVARCHAR(150)  NOT NULL,
    Email         NVARCHAR(200)  NOT NULL,
    PasswordHash  NVARCHAR(255)  NOT NULL,
    PhoneNumber   VARCHAR(20)    NULL,
    AvatarUrl     NVARCHAR(500)  NULL,
    IsActive      BIT            NOT NULL DEFAULT 1,
    CreatedAt     DATETIME2(0)   NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt     DATETIME2(0)   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_Users         PRIMARY KEY (UserId),
    CONSTRAINT UQ_Users_Email   UNIQUE (Email),
    CONSTRAINT FK_Users_Roles   FOREIGN KEY (RoleId) REFERENCES Roles(RoleId)
);
GO

-- ============================================================
-- 3. USER ADDRESSES  (3NF: tách địa chỉ riêng)
-- ============================================================
CREATE TABLE UserAddresses (
    AddressId    INT            NOT NULL IDENTITY(1,1),
    UserId       INT            NOT NULL,
    ReceiverName NVARCHAR(150)  NOT NULL,
    PhoneNumber  VARCHAR(20)    NOT NULL,
    Province     NVARCHAR(100)  NOT NULL,
    District     NVARCHAR(100)  NOT NULL,
    Ward         NVARCHAR(100)  NOT NULL,
    StreetDetail NVARCHAR(255)  NOT NULL,
    IsDefault    BIT            NOT NULL DEFAULT 0,
    CONSTRAINT PK_UserAddresses       PRIMARY KEY (AddressId),
    CONSTRAINT FK_UserAddresses_Users FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE
);
GO

-- ============================================================
-- 4. CATEGORIES  (tự tham chiếu cho đa cấp)
-- ============================================================
CREATE TABLE Categories (
    CategoryId   INT            NOT NULL IDENTITY(1,1),
    ParentId     INT            NULL,
    CategoryName NVARCHAR(150)  NOT NULL,
    Slug         NVARCHAR(200)  NOT NULL,
    ImageUrl     NVARCHAR(500)  NULL,
    SortOrder    INT            NOT NULL DEFAULT 0,
    IsActive     BIT            NOT NULL DEFAULT 1,
    CONSTRAINT PK_Categories             PRIMARY KEY (CategoryId),
    CONSTRAINT UQ_Categories_Slug        UNIQUE (Slug),
    CONSTRAINT FK_Categories_Parent      FOREIGN KEY (ParentId) REFERENCES Categories(CategoryId)
);
GO

-- ============================================================
-- 5. BRANDS  (3NF: tách thương hiệu riêng)
-- ============================================================
CREATE TABLE Brands (
    BrandId     INT            NOT NULL IDENTITY(1,1),
    BrandName   NVARCHAR(150)  NOT NULL,
    LogoUrl     NVARCHAR(500)  NULL,
    Description NVARCHAR(MAX)  NULL,
    IsActive    BIT            NOT NULL DEFAULT 1,
    CONSTRAINT PK_Brands       PRIMARY KEY (BrandId),
    CONSTRAINT UQ_Brands_Name  UNIQUE (BrandName)
);
GO

-- ============================================================
-- 6. PRODUCTS
-- ============================================================
CREATE TABLE Products (
    ProductId   INT             NOT NULL IDENTITY(1,1),
    CategoryId  INT             NOT NULL,
    BrandId     INT             NULL,
    ProductName NVARCHAR(300)   NOT NULL,
    Slug        NVARCHAR(350)   NOT NULL,
    Description NVARCHAR(MAX)   NULL,
    BasePrice   DECIMAL(18,2)   NOT NULL,
    SalePrice   DECIMAL(18,2)   NULL,
    ThumbnailUrl NVARCHAR(500)  NULL,
    IsFeatured  BIT             NOT NULL DEFAULT 0,
    IsActive    BIT             NOT NULL DEFAULT 1,
    CreatedAt   DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt   DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_Products              PRIMARY KEY (ProductId),
    CONSTRAINT UQ_Products_Slug         UNIQUE (Slug),
    CONSTRAINT FK_Products_Categories   FOREIGN KEY (CategoryId) REFERENCES Categories(CategoryId),
    CONSTRAINT FK_Products_Brands       FOREIGN KEY (BrandId)    REFERENCES Brands(BrandId),
    CONSTRAINT CK_Products_BasePrice    CHECK (BasePrice >= 0),
    CONSTRAINT CK_Products_SalePrice    CHECK (SalePrice IS NULL OR SalePrice >= 0)
);
GO

-- ============================================================
-- 7. PRODUCT IMAGES  (3NF: tách ảnh riêng)
-- ============================================================
CREATE TABLE ProductImages (
    ImageId    INT            NOT NULL IDENTITY(1,1),
    ProductId  INT            NOT NULL,
    ImageUrl   NVARCHAR(500)  NOT NULL,
    AltText    NVARCHAR(255)  NULL,
    SortOrder  INT            NOT NULL DEFAULT 0,
    CONSTRAINT PK_ProductImages          PRIMARY KEY (ImageId),
    CONSTRAINT FK_ProductImages_Products FOREIGN KEY (ProductId) REFERENCES Products(ProductId) ON DELETE CASCADE
);
GO

-- ============================================================
-- 8. ATTRIBUTES  (Size, Color, Material …)
-- ============================================================
CREATE TABLE Attributes (
    AttributeId   INT           NOT NULL IDENTITY(1,1),
    AttributeName NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_Attributes      PRIMARY KEY (AttributeId),
    CONSTRAINT UQ_Attributes_Name UNIQUE (AttributeName)
);
GO

CREATE TABLE AttributeValues (
    ValueId     INT           NOT NULL IDENTITY(1,1),
    AttributeId INT           NOT NULL,
    ValueText   NVARCHAR(100) NOT NULL,
    CONSTRAINT PK_AttributeValues          PRIMARY KEY (ValueId),
    CONSTRAINT FK_AttributeValues_Attr     FOREIGN KEY (AttributeId) REFERENCES Attributes(AttributeId) ON DELETE CASCADE,
    CONSTRAINT UQ_AttributeValues_Pair     UNIQUE (AttributeId, ValueText)
);
GO

-- ============================================================
-- 9. PRODUCT VARIANTS  (SKU-level: Size M / Color Đen / …)
-- ============================================================
CREATE TABLE ProductVariants (
    VariantId   INT            NOT NULL IDENTITY(1,1),
    ProductId   INT            NOT NULL,
    SKU         VARCHAR(100)   NOT NULL,
    StockQty    INT            NOT NULL DEFAULT 0,
    ExtraPrice  DECIMAL(18,2)  NOT NULL DEFAULT 0,   -- giá cộng thêm so với BasePrice
    ImageUrl    NVARCHAR(500)  NULL,
    IsActive    BIT            NOT NULL DEFAULT 1,
    CONSTRAINT PK_ProductVariants          PRIMARY KEY (VariantId),
    CONSTRAINT UQ_ProductVariants_SKU      UNIQUE (SKU),
    CONSTRAINT FK_ProductVariants_Products FOREIGN KEY (ProductId) REFERENCES Products(ProductId) ON DELETE CASCADE,
    CONSTRAINT CK_ProductVariants_Stock    CHECK (StockQty >= 0)
);
GO

-- Bảng liên kết Variant <-> AttributeValue (N-N)
CREATE TABLE VariantAttributeValues (
    VariantId INT NOT NULL,
    ValueId   INT NOT NULL,
    CONSTRAINT PK_VariantAttributeValues         PRIMARY KEY (VariantId, ValueId),
    CONSTRAINT FK_VAV_Variants FOREIGN KEY (VariantId) REFERENCES ProductVariants(VariantId) ON DELETE CASCADE,
    CONSTRAINT FK_VAV_Values   FOREIGN KEY (ValueId)   REFERENCES AttributeValues(ValueId)
);
GO

-- ============================================================
-- 10. DISCOUNT CODES / COUPONS
-- ============================================================
CREATE TABLE Coupons (
    CouponId      INT            NOT NULL IDENTITY(1,1),
    Code          VARCHAR(50)    NOT NULL,
    DiscountType  VARCHAR(10)    NOT NULL,   -- 'PERCENT' | 'FIXED'
    DiscountValue DECIMAL(18,2)  NOT NULL,
    MaxUses       INT            NULL,
    UsedCount     INT            NOT NULL DEFAULT 0,
    MinOrderValue DECIMAL(18,2)  NOT NULL DEFAULT 0,
    ExpiresAt     DATETIME2(0)   NULL,
    IsActive      BIT            NOT NULL DEFAULT 1,
    CONSTRAINT PK_Coupons           PRIMARY KEY (CouponId),
    CONSTRAINT UQ_Coupons_Code      UNIQUE (Code),
    CONSTRAINT CK_Coupons_Type      CHECK (DiscountType IN ('PERCENT','FIXED')),
    CONSTRAINT CK_Coupons_Value     CHECK (DiscountValue > 0)
);
GO

-- ============================================================
-- 11. ORDERS
-- ============================================================
CREATE TABLE Orders (
    OrderId        INT             NOT NULL IDENTITY(1,1),
    UserId         INT             NULL,           -- NULL nếu guest checkout
    CouponId       INT             NULL,
    AddressSnapshot NVARCHAR(MAX)  NOT NULL,       -- JSON snapshot địa chỉ lúc đặt hàng
    SubTotal       DECIMAL(18,2)   NOT NULL,
    DiscountAmount DECIMAL(18,2)   NOT NULL DEFAULT 0,
    ShippingFee    DECIMAL(18,2)   NOT NULL DEFAULT 0,
    TotalAmount    DECIMAL(18,2)   NOT NULL,
    OrderStatus    NVARCHAR(30)    NOT NULL DEFAULT N'PENDING',
    PaymentMethod  VARCHAR(30)     NOT NULL DEFAULT 'COD',
    PaymentStatus  VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    Note           NVARCHAR(500)   NULL,
    CreatedAt      DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt      DATETIME2(0)    NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_Orders              PRIMARY KEY (OrderId),
    CONSTRAINT FK_Orders_Users        FOREIGN KEY (UserId)    REFERENCES Users(UserId),
    CONSTRAINT FK_Orders_Coupons      FOREIGN KEY (CouponId)  REFERENCES Coupons(CouponId),
    CONSTRAINT CK_Orders_Status       CHECK (OrderStatus IN (N'PENDING',N'CONFIRMED',N'PROCESSING',N'SHIPPED',N'DELIVERED',N'CANCELLED',N'REFUNDED')),
    CONSTRAINT CK_Orders_PayMethod    CHECK (PaymentMethod IN ('COD','BANK_TRANSFER','VNPAY','MOMO','STRIPE')),
    CONSTRAINT CK_Orders_PayStatus    CHECK (PaymentStatus IN ('UNPAID','PAID','REFUNDED'))
);
GO

-- ============================================================
-- 12. ORDER DETAILS
-- ============================================================
CREATE TABLE OrderDetails (
    OrderDetailId INT            NOT NULL IDENTITY(1,1),
    OrderId       INT            NOT NULL,
    VariantId     INT            NULL,           -- NULL nếu variant bị xóa sau
    ProductName   NVARCHAR(300)  NOT NULL,       -- snapshot tên sp
    VariantInfo   NVARCHAR(300)  NULL,           -- snapshot "Size M / Đen"
    SKU           VARCHAR(100)   NULL,
    UnitPrice     DECIMAL(18,2)  NOT NULL,
    Quantity      INT            NOT NULL,
    LineTotal     AS (UnitPrice * Quantity) PERSISTED,
    CONSTRAINT PK_OrderDetails          PRIMARY KEY (OrderDetailId),
    CONSTRAINT FK_OrderDetails_Orders   FOREIGN KEY (OrderId)    REFERENCES Orders(OrderId) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetails_Variants FOREIGN KEY (VariantId)  REFERENCES ProductVariants(VariantId),
    CONSTRAINT CK_OrderDetails_Qty      CHECK (Quantity > 0),
    CONSTRAINT CK_OrderDetails_Price    CHECK (UnitPrice >= 0)
);
GO

-- ============================================================
-- 13. CART
-- ============================================================
CREATE TABLE Cart (
    CartId     INT           NOT NULL IDENTITY(1,1),
    UserId     INT           NULL,           -- registered user
    SessionId  VARCHAR(128)  NULL,           -- guest session
    CreatedAt  DATETIME2(0)  NOT NULL DEFAULT SYSUTCDATETIME(),
    UpdatedAt  DATETIME2(0)  NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_Cart        PRIMARY KEY (CartId),
    CONSTRAINT FK_Cart_Users  FOREIGN KEY (UserId) REFERENCES Users(UserId) ON DELETE CASCADE,
    CONSTRAINT CK_Cart_Owner  CHECK (UserId IS NOT NULL OR SessionId IS NOT NULL)
);
GO

-- ============================================================
-- 14. CART ITEMS
-- ============================================================
CREATE TABLE CartItems (
    CartItemId INT            NOT NULL IDENTITY(1,1),
    CartId     INT            NOT NULL,
    VariantId  INT            NOT NULL,
    Quantity   INT            NOT NULL DEFAULT 1,
    AddedAt    DATETIME2(0)   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_CartItems           PRIMARY KEY (CartItemId),
    CONSTRAINT UQ_CartItems_Pair      UNIQUE (CartId, VariantId),
    CONSTRAINT FK_CartItems_Cart      FOREIGN KEY (CartId)    REFERENCES Cart(CartId) ON DELETE CASCADE,
    CONSTRAINT FK_CartItems_Variants  FOREIGN KEY (VariantId) REFERENCES ProductVariants(VariantId),
    CONSTRAINT CK_CartItems_Qty       CHECK (Quantity > 0)
);
GO

-- ============================================================
-- 15. PRODUCT REVIEWS
-- ============================================================
CREATE TABLE Reviews (
    ReviewId  INT            NOT NULL IDENTITY(1,1),
    ProductId INT            NOT NULL,
    UserId    INT            NOT NULL,
    OrderId   INT            NULL,           -- chứng minh đã mua hàng
    Rating    TINYINT        NOT NULL,
    Title     NVARCHAR(200)  NULL,
    Body      NVARCHAR(MAX)  NULL,
    IsApproved BIT           NOT NULL DEFAULT 0,
    CreatedAt DATETIME2(0)   NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_Reviews             PRIMARY KEY (ReviewId),
    CONSTRAINT FK_Reviews_Products    FOREIGN KEY (ProductId) REFERENCES Products(ProductId) ON DELETE CASCADE,
    CONSTRAINT FK_Reviews_Users       FOREIGN KEY (UserId)    REFERENCES Users(UserId),
    CONSTRAINT FK_Reviews_Orders      FOREIGN KEY (OrderId)   REFERENCES Orders(OrderId),
    CONSTRAINT CK_Reviews_Rating      CHECK (Rating BETWEEN 1 AND 5),
    CONSTRAINT UQ_Reviews_UserProduct UNIQUE (UserId, ProductId, OrderId)
);
GO

-- ============================================================
-- 16. WISHLISTS
-- ============================================================
CREATE TABLE Wishlists (
    WishlistId INT          NOT NULL IDENTITY(1,1),
    UserId     INT          NOT NULL,
    ProductId  INT          NOT NULL,
    AddedAt    DATETIME2(0) NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT PK_Wishlists           PRIMARY KEY (WishlistId),
    CONSTRAINT UQ_Wishlists_Pair      UNIQUE (UserId, ProductId),
    CONSTRAINT FK_Wishlists_Users     FOREIGN KEY (UserId)    REFERENCES Users(UserId) ON DELETE CASCADE,
    CONSTRAINT FK_Wishlists_Products  FOREIGN KEY (ProductId) REFERENCES Products(ProductId) ON DELETE CASCADE
);
GO

-- ============================================================
-- INDEXES  (performance-critical)
-- ============================================================

-- Users
CREATE INDEX IX_Users_RoleId       ON Users(RoleId);
CREATE INDEX IX_Users_Email        ON Users(Email);       -- đã UNIQUE, thêm IX để cover-scan

-- Products
CREATE INDEX IX_Products_CategoryId  ON Products(CategoryId);
CREATE INDEX IX_Products_BrandId     ON Products(BrandId);
CREATE INDEX IX_Products_IsActive    ON Products(IsActive) INCLUDE (ProductId, ProductName, BasePrice, SalePrice, ThumbnailUrl);
CREATE INDEX IX_Products_IsFeatured  ON Products(IsFeatured, IsActive) INCLUDE (ProductId, ProductName, BasePrice, SalePrice, ThumbnailUrl);
CREATE INDEX IX_Products_Price       ON Products(SalePrice, BasePrice);

-- Product Variants
CREATE INDEX IX_ProductVariants_ProductId ON ProductVariants(ProductId);
CREATE INDEX IX_ProductVariants_SKU       ON ProductVariants(SKU);        -- đã UNIQUE

-- Orders
CREATE INDEX IX_Orders_UserId       ON Orders(UserId);
CREATE INDEX IX_Orders_Status       ON Orders(OrderStatus) INCLUDE (OrderId, UserId, TotalAmount, CreatedAt);
CREATE INDEX IX_Orders_CreatedAt    ON Orders(CreatedAt DESC) INCLUDE (OrderId, UserId, OrderStatus, TotalAmount);
CREATE INDEX IX_Orders_CouponId     ON Orders(CouponId);

-- Order Details
CREATE INDEX IX_OrderDetails_OrderId   ON OrderDetails(OrderId);
CREATE INDEX IX_OrderDetails_VariantId ON OrderDetails(VariantId);

-- Cart
CREATE INDEX IX_Cart_UserId    ON Cart(UserId);
CREATE INDEX IX_Cart_Session   ON Cart(SessionId);

-- Cart Items
CREATE INDEX IX_CartItems_CartId    ON CartItems(CartId);
CREATE INDEX IX_CartItems_VariantId ON CartItems(VariantId);

-- Reviews
CREATE INDEX IX_Reviews_ProductId ON Reviews(ProductId, IsApproved) INCLUDE (Rating);
CREATE INDEX IX_Reviews_UserId    ON Reviews(UserId);

-- Categories
CREATE INDEX IX_Categories_ParentId ON Categories(ParentId);
CREATE INDEX IX_Categories_Slug     ON Categories(Slug);    -- đã UNIQUE

-- Wishlists
CREATE INDEX IX_Wishlists_UserId    ON Wishlists(UserId);
CREATE INDEX IX_Wishlists_ProductId ON Wishlists(ProductId);

GO

-- ============================================================
-- DỮ LIỆU MẪU
-- ============================================================

-- ---- ROLES ----
INSERT INTO Roles (RoleName) VALUES
    (N'ADMIN'),
    (N'STAFF'),
    (N'CUSTOMER');
GO

-- ---- USERS ----
-- Mật khẩu: Admin@123 (BCrypt hash – thay bằng hash thực khi deploy)
INSERT INTO Users (RoleId, FullName, Email, PasswordHash, PhoneNumber, IsActive) VALUES
    (1, N'Nguyễn Admin',   'admin@luxuryfashion.vn',    '$2a$12$Kj8XGMq0lrBz2UvHsWc7e.Kv1V9NcDfOHxT3pNmP0YwA3rQlZkRiS', '0901000001', 1),
    (2, N'Trần Nhân Viên', 'staff@luxuryfashion.vn',   '$2a$12$Kj8XGMq0lrBz2UvHsWc7e.Kv1V9NcDfOHxT3pNmP0YwA3rQlZkRiS', '0901000002', 1),
    (3, N'Lê Thị Hoa',     'hoa.le@gmail.com',         '$2a$12$Kj8XGMq0lrBz2UvHsWc7e.Kv1V9NcDfOHxT3pNmP0YwA3rQlZkRiS', '0901000003', 1),
    (3, N'Phạm Minh Tuấn', 'tuan.pham@gmail.com',      '$2a$12$Kj8XGMq0lrBz2UvHsWc7e.Kv1V9NcDfOHxT3pNmP0YwA3rQlZkRiS', '0901000004', 1),
    (3, N'Đinh Thị Mai',   'mai.dinh@gmail.com',       '$2a$12$Kj8XGMq0lrBz2UvHsWc7e.Kv1V9NcDfOHxT3pNmP0YwA3rQlZkRiS', '0901000005', 1);
GO

-- ---- USER ADDRESSES ----
INSERT INTO UserAddresses (UserId, ReceiverName, PhoneNumber, Province, District, Ward, StreetDetail, IsDefault) VALUES
    (3, N'Lê Thị Hoa',     '0901000003', N'Hà Nội',         N'Cầu Giấy',    N'Dịch Vọng Hậu', N'Số 18 Trần Thái Tông', 1),
    (4, N'Phạm Minh Tuấn', '0901000004', N'TP Hồ Chí Minh', N'Quận 1',      N'Phường Bến Nghé', N'123 Lê Lai', 1),
    (5, N'Đinh Thị Mai',   '0901000005', N'TP Hồ Chí Minh', N'Quận 3',      N'Phường 6',        N'45 Võ Văn Tần', 1);
GO

-- ---- BRANDS ----
INSERT INTO Brands (BrandName, Description, IsActive) VALUES
    (N'LEIKA',       N'Thương hiệu thời trang cao cấp Việt Nam', 1),
    (N'VASCARA',     N'Thương hiệu thời trang nữ hàng đầu',     1),
    (N'IVY MODA',    N'Phong cách hiện đại, thanh lịch',        1),
    (N'OWEN',        N'Thời trang công sở nam cao cấp',         1),
    (N'ELISE',       N'Thời trang nữ duyên dáng, nữ tính',     1);
GO

-- ---- CATEGORIES ----
INSERT INTO Categories (ParentId, CategoryName, Slug, SortOrder, IsActive) VALUES
    -- Cấp 1
    (NULL, N'Thời Trang Nữ',     'thoi-trang-nu',     1, 1),
    (NULL, N'Thời Trang Nam',     'thoi-trang-nam',    2, 1),
    (NULL, N'Phụ Kiện',          'phu-kien',          3, 1),
    -- Cấp 2 – Nữ
    (1,    N'Áo Nữ',             'ao-nu',             1, 1),
    (1,    N'Quần Nữ',           'quan-nu',           2, 1),
    (1,    N'Váy – Đầm',         'vay-dam',           3, 1),
    (1,    N'Đồ Bộ Nữ',         'do-bo-nu',          4, 1),
    -- Cấp 2 – Nam
    (2,    N'Áo Nam',            'ao-nam',            1, 1),
    (2,    N'Quần Nam',          'quan-nam',          2, 1),
    (2,    N'Vest – Blazer',     'vest-blazer',       3, 1),
    -- Cấp 2 – Phụ Kiện
    (3,    N'Túi Xách',          'tui-xach',          1, 1),
    (3,    N'Thắt Lưng',         'that-lung',         2, 1),
    (3,    N'Khăn – Mũ',        'khan-mu',           3, 1);
GO

-- ---- PRODUCTS ----
INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, IsFeatured, IsActive) VALUES
    -- Váy
    (6, 1, N'Đầm Midi Hoa Nhí Cổ V', 'dam-midi-hoa-nhi-co-v',
        N'Đầm midi dáng xòe, họa tiết hoa nhí tinh tế, cổ V thanh lịch. Chất liệu vải lụa cao cấp.', 1290000, 890000, 1, 1),
    (6, 5, N'Đầm Ren Nữ Tính Tay Phồng', 'dam-ren-nu-tinh-tay-phong',
        N'Đầm ren cao cấp, tay phồng nhẹ, thiết kế lãng mạn phù hợp dự tiệc.', 1590000, 1290000, 1, 1),
    (6, 3, N'Váy Wrap Caro Thanh Lịch', 'vay-wrap-caro-thanh-lich',
        N'Váy wrap cổ điển họa tiết caro nhỏ, dây buộc eo tôn dáng.', 850000, NULL, 0, 1),
    -- Áo nữ
    (4, 1, N'Áo Sơ Mi Lụa Trắng Cổ Bèo', 'ao-so-mi-lua-trang-co-beo',
        N'Áo sơ mi lụa cao cấp, cổ bèo nhẹ nhàng, phối đồ linh hoạt.', 790000, 690000, 1, 1),
    (4, 2, N'Áo Thun Oversize Wash Basic', 'ao-thun-oversize-wash-basic',
        N'Áo thun oversize chất cotton wash, màu tone earth hiện đại.', 450000, 390000, 0, 1),
    -- Áo nam
    (8, 4, N'Áo Sơ Mi Nam Oxford Trắng', 'ao-so-mi-nam-oxford-trang',
        N'Áo sơ mi công sở cao cấp, vải Oxford không nhàu, form slim fit.', 690000, NULL, 1, 1),
    (8, 4, N'Áo Polo Nam Premium Pique', 'ao-polo-nam-premium-pique',
        N'Áo polo chất liệu Pique cao cấp, thêu logo nhỏ tinh tế.', 590000, 490000, 0, 1),
    -- Túi
    (11, 1, N'Túi Tote Da Thật Hạt Sen', 'tui-tote-da-that-hat-sen',
        N'Túi tote da thật, khóa từ, ngăn chia tiện lợi, phong cách minimalist cao cấp.', 2490000, 1990000, 1, 1),
    (11, 2, N'Clutch Dự Tiệc Da PU Ánh Kim', 'clutch-du-tiec-da-pu-anh-kim',
        N'Clutch da PU ánh kim, đính đá tinh xảo, dây đeo kim loại tháo rời.', 890000, 750000, 0, 1),
    -- Vest
    (10, 4, N'Vest Nam 2 Nút Kẻ Sọc Lịch Lãm', 'vest-nam-2-nut-ke-soc-lich-lam',
        N'Vest 2 nút kẻ sọc mỏng, lót trong cao cấp, form dáng âu lịch sự.', 3490000, 2890000, 1, 1);
GO

-- ---- ATTRIBUTES ----
INSERT INTO Attributes (AttributeName) VALUES
    (N'Kích cỡ'),
    (N'Màu sắc'),
    (N'Chất liệu');
GO

INSERT INTO AttributeValues (AttributeId, ValueText) VALUES
    -- Kích cỡ (AttributeId = 1)
    (1, 'XS'), (1, 'S'),  (1, 'M'),  (1, 'L'),  (1, 'XL'), (1, 'XXL'),
    -- Màu sắc (AttributeId = 2)
    (2, N'Trắng'), (2, N'Đen'),  (2, N'Kem'),  (2, N'Hồng'), (2, N'Be'), (2, N'Navy'),
    -- Chất liệu (AttributeId = 3)
    (3, N'Lụa'), (3, N'Cotton'), (3, N'Ren'), (3, N'Da thật'), (3, N'Da PU');
GO

-- ---- PRODUCT VARIANTS ----
-- SP1: Đầm Midi Hoa Nhí (S/Hồng, M/Hồng, L/Hồng, M/Kem)
INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice) VALUES
    (1, 'DAM-MIDI-001-S-HONG',  15, 0),
    (1, 'DAM-MIDI-001-M-HONG',  20, 0),
    (1, 'DAM-MIDI-001-L-HONG',  12, 0),
    (1, 'DAM-MIDI-001-M-KEM',   18, 0),
    -- SP2: Đầm Ren (S/Đen, M/Đen, L/Đen)
    (2, 'DAM-REN-002-S-DEN',    10, 0),
    (2, 'DAM-REN-002-M-DEN',    14, 0),
    (2, 'DAM-REN-002-L-DEN',    8,  0),
    -- SP4: Áo Sơ Mi Lụa (S/Trắng, M/Trắng, L/Trắng)
    (4, 'ASM-LUA-004-S-TRANG',  25, 0),
    (4, 'ASM-LUA-004-M-TRANG',  30, 0),
    (4, 'ASM-LUA-004-L-TRANG',  20, 0),
    -- SP6: Áo Sơ Mi Nam (S/Trắng, M/Trắng, L/Trắng, XL/Trắng)
    (6, 'ASM-NAM-006-S-TRANG',  20, 0),
    (6, 'ASM-NAM-006-M-TRANG',  35, 0),
    (6, 'ASM-NAM-006-L-TRANG',  30, 0),
    (6, 'ASM-NAM-006-XL-TRANG', 15, 0),
    -- SP8: Túi Tote Da (Kem, Đen, Be)
    (8, 'TUI-TOTE-008-KEM',     8,  0),
    (8, 'TUI-TOTE-008-DEN',     10, 0),
    (8, 'TUI-TOTE-008-BE',      6,  100000),
    -- SP10: Vest (S, M, L, XL)
    (10,'VEST-SOC-010-S',       5,  0),
    (10,'VEST-SOC-010-M',       8,  0),
    (10,'VEST-SOC-010-L',       7,  0),
    (10,'VEST-SOC-010-XL',      4,  200000);
GO

-- Variant – AttributeValue links (chọn các cặp tiêu biểu)
-- VariantId 1: S / Hồng
INSERT INTO VariantAttributeValues VALUES (1,2),(1,10);  -- S, Hồng
INSERT INTO VariantAttributeValues VALUES (2,3),(2,10);  -- M, Hồng
INSERT INTO VariantAttributeValues VALUES (3,4),(3,10);  -- L, Hồng
INSERT INTO VariantAttributeValues VALUES (4,3),(4,11);  -- M, Kem
INSERT INTO VariantAttributeValues VALUES (5,2),(5,8);   -- S, Đen
INSERT INTO VariantAttributeValues VALUES (6,3),(6,8);   -- M, Đen
INSERT INTO VariantAttributeValues VALUES (7,4),(7,8);   -- L, Đen
GO

-- ---- COUPONS ----
INSERT INTO Coupons (Code, DiscountType, DiscountValue, MaxUses, MinOrderValue, ExpiresAt, IsActive) VALUES
    ('WELCOME10',  'PERCENT', 10,   500,  0,       '2026-12-31', 1),
    ('SUMMER20',   'PERCENT', 20,   200,  500000,  '2026-06-30', 1),
    ('FREESHIP',   'FIXED',   30000, 1000, 0,      '2026-12-31', 1),
    ('VIP500K',    'FIXED',   500000, 50,  3000000,'2026-12-31', 1);
GO

-- ---- ORDERS ----
INSERT INTO Orders (UserId, CouponId, AddressSnapshot, SubTotal, DiscountAmount, ShippingFee, TotalAmount, OrderStatus, PaymentMethod, PaymentStatus) VALUES
(3, 1,
 N'{"receiver":"Lê Thị Hoa","phone":"0901000003","address":"Số 18 Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội"}',
 1580000, 158000, 0, 1422000, N'DELIVERED', 'COD', 'PAID'),
(4, NULL,
 N'{"receiver":"Phạm Minh Tuấn","phone":"0901000004","address":"123 Lê Lai, Bến Nghé, Q1, TP.HCM"}',
 2890000, 0, 30000, 2920000, N'SHIPPED', 'VNPAY', 'PAID'),
(3, 3,
 N'{"receiver":"Lê Thị Hoa","phone":"0901000003","address":"Số 18 Trần Thái Tông, Dịch Vọng Hậu, Cầu Giấy, Hà Nội"}',
 890000, 30000, 0, 860000, N'PENDING', 'COD', 'UNPAID'),
(5, NULL,
 N'{"receiver":"Đinh Thị Mai","phone":"0901000005","address":"45 Võ Văn Tần, P6, Q3, TP.HCM"}',
 3490000, 500000, 0, 2990000, N'CONFIRMED', 'MOMO', 'PAID');
GO

-- ---- ORDER DETAILS ----
INSERT INTO OrderDetails (OrderId, VariantId, ProductName, VariantInfo, SKU, UnitPrice, Quantity) VALUES
-- Order 1: Đầm Midi M/Hồng + Áo Sơ Mi M/Trắng
(1, 2,  N'Đầm Midi Hoa Nhí Cổ V',      N'M / Hồng',   'DAM-MIDI-001-M-HONG',  890000, 1),
(1, 9,  N'Áo Sơ Mi Lụa Trắng Cổ Bèo',  N'M / Trắng',  'ASM-LUA-004-M-TRANG',  690000, 1),
-- Order 2: Vest M
(2, 19, N'Vest Nam 2 Nút Kẻ Sọc',       N'M',          'VEST-SOC-010-M',       2890000, 1),
-- Order 3: Túi Tote Kem
(3, 15, N'Túi Tote Da Thật Hạt Sen',    N'Kem',        'TUI-TOTE-008-KEM',     1990000, 1),
-- Order 4: Vest XL
(4, 21, N'Vest Nam 2 Nút Kẻ Sọc',       N'XL',         'VEST-SOC-010-XL',      3090000, 1);
GO

-- ---- CART ----
INSERT INTO Cart (UserId, SessionId) VALUES
    (3,  NULL),     -- CartId 1
    (4,  NULL),     -- CartId 2
    (NULL, 'sess_guest_abc123');  -- CartId 3 (khách vãng lai)
GO

-- ---- CART ITEMS ----
INSERT INTO CartItems (CartId, VariantId, Quantity) VALUES
    (1, 3,  1),   -- Đầm Midi L/Hồng
    (1, 16, 1),   -- Túi Tote Đen
    (2, 12, 2),   -- Áo Sơ Mi Nam M/Trắng
    (3, 6,  1);   -- Đầm Ren M/Đen (guest)
GO

-- ---- REVIEWS ----
INSERT INTO Reviews (ProductId, UserId, OrderId, Rating, Title, Body, IsApproved) VALUES
    (1, 3, 1, 5, N'Sản phẩm tuyệt vời!',    N'Vải mềm mịn, may đẹp, đúng với mô tả, giao hàng nhanh. Sẽ ủng hộ shop tiếp!', 1),
    (4, 3, 1, 4, N'Áo đẹp, hơi nhỏ cỡ',    N'Chất lụa rất mịn, nhưng cỡ hơi nhỏ hơn thường. Nên lấy cỡ to hơn 1 size.', 1),
    (10,4, 2, 5, N'Vest chuẩn form',         N'Mặc đi phỏng vấn rất chuyên nghiệp, chất liệu không bị nhăn.', 1);
GO

-- ---- WISHLISTS ----
INSERT INTO Wishlists (UserId, ProductId) VALUES
    (3, 2),   -- Hoa yêu thích Đầm Ren
    (3, 8),   -- Hoa yêu thích Túi Tote
    (4, 10),  -- Tuấn yêu thích Vest
    (5, 6),   -- Mai yêu thích Áo Sơ Mi Nam
    (5, 1);   -- Mai yêu thích Đầm Midi
GO

PRINT N'[OK] Database LuxuryFashionDB tạo thành công!';
GO
