-- ============================================================
--   LUXURY FASHION DB – OPTIMIZED QUERIES, VIEWS & STORED PROCS
-- ============================================================
USE LuxuryFashionDB;
GO

-- ============================================================
-- A. VIEWS (pre-built cho Thymeleaf / API)
-- ============================================================

-- A1. Danh sách sản phẩm kèm thông tin đầy đủ
CREATE OR ALTER VIEW vw_ProductList AS
SELECT
    p.ProductId,
    p.ProductName,
    p.Slug,
    p.BasePrice,
    p.SalePrice,
    COALESCE(p.SalePrice, p.BasePrice)                              AS EffectivePrice,
    ROUND(
        CASE WHEN p.SalePrice IS NOT NULL
             THEN (1 - p.SalePrice / p.BasePrice) * 100
             ELSE 0 END, 0)                                         AS DiscountPercent,
    p.ThumbnailUrl,
    p.IsFeatured,
    c.CategoryId,
    c.CategoryName,
    b.BrandId,
    b.BrandName,
    COALESCE(r.AvgRating, 0)                                        AS AvgRating,
    COALESCE(r.ReviewCount, 0)                                      AS ReviewCount,
    COALESCE(v.TotalStock, 0)                                       AS TotalStock
FROM Products p
JOIN Categories  c ON c.CategoryId = p.CategoryId
LEFT JOIN Brands b ON b.BrandId    = p.BrandId
LEFT JOIN (
    SELECT ProductId,
           CAST(AVG(CAST(Rating AS FLOAT)) AS DECIMAL(3,1)) AS AvgRating,
           COUNT(*) AS ReviewCount
    FROM Reviews WHERE IsApproved = 1
    GROUP BY ProductId
) r ON r.ProductId = p.ProductId
LEFT JOIN (
    SELECT ProductId, SUM(StockQty) AS TotalStock
    FROM ProductVariants WHERE IsActive = 1
    GROUP BY ProductId
) v ON v.ProductId = p.ProductId
WHERE p.IsActive = 1;
GO

-- A2. Chi tiết đơn hàng (dùng cho trang order history)
CREATE OR ALTER VIEW vw_OrderSummary AS
SELECT
    o.OrderId,
    o.UserId,
    u.FullName      AS CustomerName,
    u.Email         AS CustomerEmail,
    o.SubTotal,
    o.DiscountAmount,
    o.ShippingFee,
    o.TotalAmount,
    o.OrderStatus,
    o.PaymentMethod,
    o.PaymentStatus,
    o.CreatedAt,
    c.Code          AS CouponCode,
    (SELECT COUNT(*) FROM OrderDetails od WHERE od.OrderId = o.OrderId) AS ItemCount
FROM Orders o
LEFT JOIN Users   u ON u.UserId   = o.UserId
LEFT JOIN Coupons c ON c.CouponId = o.CouponId;
GO

-- A3. Số lượng items trong Cart (dùng cho badge giỏ hàng)
CREATE OR ALTER VIEW vw_CartItemCount AS
SELECT
    CartId,
    UserId,
    SUM(Quantity) AS TotalItems
FROM Cart
JOIN CartItems USING (CartId)
-- SQL Server không dùng USING, viết lại:
--  Đây chỉ là ví dụ logic – dùng inline query bên dưới
WHERE 1=0; -- placeholder
GO

DROP VIEW IF EXISTS vw_CartItemCount;
GO

-- ============================================================
-- B. STORED PROCEDURES
-- ============================================================

-- B1. Lấy danh sách sản phẩm theo category (có phân trang)
CREATE OR ALTER PROCEDURE sp_GetProductsByCategory
    @CategoryId  INT,
    @PageNumber  INT = 1,
    @PageSize    INT = 12,
    @SortBy      VARCHAR(20) = 'newest'     -- newest | price_asc | price_desc | rating
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Offset INT = (@PageNumber - 1) * @PageSize;

    SELECT
        p.ProductId, p.ProductName, p.Slug,
        p.BasePrice, p.SalePrice,
        COALESCE(p.SalePrice, p.BasePrice)              AS EffectivePrice,
        p.ThumbnailUrl, p.IsFeatured,
        b.BrandName,
        COALESCE(r.AvgRating,    0)                    AS AvgRating,
        COALESCE(r.ReviewCount,  0)                    AS ReviewCount,
        COALESCE(v.TotalStock,   0)                    AS TotalStock
    FROM Products p
    JOIN Categories c   ON c.CategoryId = p.CategoryId
    LEFT JOIN Brands  b ON b.BrandId    = p.BrandId
    LEFT JOIN (
        SELECT ProductId,
               CAST(AVG(CAST(Rating AS FLOAT)) AS DECIMAL(3,1)) AS AvgRating,
               COUNT(*) AS ReviewCount
        FROM Reviews WHERE IsApproved = 1
        GROUP BY ProductId
    ) r ON r.ProductId = p.ProductId
    LEFT JOIN (
        SELECT ProductId, SUM(StockQty) AS TotalStock
        FROM ProductVariants WHERE IsActive = 1
        GROUP BY ProductId
    ) v ON v.ProductId = p.ProductId
    WHERE p.IsActive = 1
      AND (p.CategoryId = @CategoryId
           OR c.ParentId = @CategoryId)   -- include sub-categories
    ORDER BY
        CASE WHEN @SortBy = 'price_asc'  THEN COALESCE(p.SalePrice, p.BasePrice) END ASC,
        CASE WHEN @SortBy = 'price_desc' THEN COALESCE(p.SalePrice, p.BasePrice) END DESC,
        CASE WHEN @SortBy = 'rating'     THEN COALESCE(r.AvgRating, 0)          END DESC,
        CASE WHEN @SortBy = 'newest'     THEN p.ProductId                        END DESC,
        p.ProductId DESC
    OFFSET @Offset ROWS FETCH NEXT @PageSize ROWS ONLY;
END;
GO

-- B2. Thêm/cập nhật Cart Item
CREATE OR ALTER PROCEDURE sp_UpsertCartItem
    @UserId    INT         = NULL,
    @SessionId VARCHAR(128) = NULL,
    @VariantId INT,
    @Quantity  INT         = 1
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    -- Tìm hoặc tạo Cart
    DECLARE @CartId INT;
    SELECT @CartId = CartId FROM Cart
    WHERE  (UserId = @UserId AND @UserId IS NOT NULL)
        OR (SessionId = @SessionId AND @SessionId IS NOT NULL);

    IF @CartId IS NULL
    BEGIN
        INSERT INTO Cart (UserId, SessionId) VALUES (@UserId, @SessionId);
        SET @CartId = SCOPE_IDENTITY();
    END;

    -- Kiểm tra tồn kho
    DECLARE @Stock INT;
    SELECT @Stock = StockQty FROM ProductVariants WHERE VariantId = @VariantId AND IsActive = 1;
    IF @Stock IS NULL OR @Stock < @Quantity
    BEGIN
        ROLLBACK;
        RAISERROR(N'Sản phẩm không đủ số lượng trong kho.', 16, 1);
        RETURN;
    END;

    -- Upsert CartItem
    IF EXISTS (SELECT 1 FROM CartItems WHERE CartId = @CartId AND VariantId = @VariantId)
        UPDATE CartItems SET Quantity = Quantity + @Quantity
        WHERE CartId = @CartId AND VariantId = @VariantId;
    ELSE
        INSERT INTO CartItems (CartId, VariantId, Quantity) VALUES (@CartId, @VariantId, @Quantity);

    UPDATE Cart SET UpdatedAt = SYSUTCDATETIME() WHERE CartId = @CartId;
    COMMIT;
END;
GO

-- B3. Đặt hàng từ giỏ hàng
CREATE OR ALTER PROCEDURE sp_CreateOrderFromCart
    @UserId      INT,
    @AddressId   INT,
    @CouponCode  VARCHAR(50) = NULL,
    @PaymentMethod VARCHAR(30) = 'COD',
    @Note        NVARCHAR(500) = NULL,
    @OrderId     INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;
    BEGIN TRANSACTION;

    -- Lấy CartId
    DECLARE @CartId INT;
    SELECT @CartId = CartId FROM Cart WHERE UserId = @UserId;
    IF @CartId IS NULL BEGIN ROLLBACK; RAISERROR(N'Giỏ hàng trống.', 16, 1); RETURN; END;

    -- Tính SubTotal
    DECLARE @SubTotal DECIMAL(18,2);
    SELECT @SubTotal = SUM(
        ci.Quantity * (COALESCE(p.SalePrice, p.BasePrice) + pv.ExtraPrice)
    )
    FROM CartItems ci
    JOIN ProductVariants pv ON pv.VariantId = ci.VariantId
    JOIN Products p         ON p.ProductId  = pv.ProductId
    WHERE ci.CartId = @CartId;

    IF @SubTotal IS NULL BEGIN ROLLBACK; RAISERROR(N'Giỏ hàng trống.', 16, 1); RETURN; END;

    -- Xử lý coupon
    DECLARE @CouponId INT = NULL, @Discount DECIMAL(18,2) = 0;
    IF @CouponCode IS NOT NULL
    BEGIN
        SELECT @CouponId = CouponId,
               @Discount = CASE DiscountType
                               WHEN 'PERCENT' THEN @SubTotal * DiscountValue / 100
                               WHEN 'FIXED'   THEN DiscountValue
                           END
        FROM Coupons
        WHERE Code      = @CouponCode
          AND IsActive  = 1
          AND (ExpiresAt IS NULL OR ExpiresAt > SYSUTCDATETIME())
          AND (MaxUses IS NULL OR UsedCount < MaxUses)
          AND MinOrderValue <= @SubTotal;
    END;

    -- Snapshot địa chỉ
    DECLARE @AddrSnap NVARCHAR(MAX);
    SELECT @AddrSnap = CONCAT(
        N'{"receiver":"', ReceiverName,
        N'","phone":"',   PhoneNumber,
        N'","address":"', StreetDetail, N', ', Ward, N', ', District, N', ', Province, N'"}'
    )
    FROM UserAddresses WHERE AddressId = @AddressId AND UserId = @UserId;
    IF @AddrSnap IS NULL BEGIN ROLLBACK; RAISERROR(N'Địa chỉ không hợp lệ.', 16, 1); RETURN; END;

    DECLARE @Total DECIMAL(18,2) = @SubTotal - @Discount;

    -- Tạo Order
    INSERT INTO Orders (UserId, CouponId, AddressSnapshot, SubTotal, DiscountAmount, TotalAmount, PaymentMethod, Note)
    VALUES (@UserId, @CouponId, @AddrSnap, @SubTotal, @Discount, @Total, @PaymentMethod, @Note);
    SET @OrderId = SCOPE_IDENTITY();

    -- Chuyển CartItems → OrderDetails & trừ tồn kho
    INSERT INTO OrderDetails (OrderId, VariantId, ProductName, VariantInfo, SKU, UnitPrice, Quantity)
    SELECT
        @OrderId,
        pv.VariantId,
        p.ProductName,
        (SELECT STRING_AGG(CONCAT(a.AttributeName, ': ', av.ValueText), ' / ')
         FROM VariantAttributeValues vav
         JOIN AttributeValues av ON av.ValueId     = vav.ValueId
         JOIN Attributes      a  ON a.AttributeId  = av.AttributeId
         WHERE vav.VariantId = pv.VariantId),
        pv.SKU,
        COALESCE(p.SalePrice, p.BasePrice) + pv.ExtraPrice,
        ci.Quantity
    FROM CartItems ci
    JOIN ProductVariants pv ON pv.VariantId = ci.VariantId
    JOIN Products        p  ON p.ProductId  = pv.ProductId
    WHERE ci.CartId = @CartId;

    -- Trừ tồn kho
    UPDATE pv
    SET    pv.StockQty = pv.StockQty - ci.Quantity
    FROM   ProductVariants pv
    JOIN   CartItems       ci ON ci.VariantId = pv.VariantId
    WHERE  ci.CartId = @CartId;

    -- Tăng UsedCount của coupon
    IF @CouponId IS NOT NULL
        UPDATE Coupons SET UsedCount = UsedCount + 1 WHERE CouponId = @CouponId;

    -- Xóa Cart
    DELETE FROM CartItems WHERE CartId = @CartId;
    DELETE FROM Cart      WHERE CartId = @CartId;

    COMMIT;
END;
GO

-- B4. Doanh thu theo tháng (dashboard admin)
CREATE OR ALTER PROCEDURE sp_RevenueByMonth
    @Year INT = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET @Year = COALESCE(@Year, YEAR(SYSUTCDATETIME()));

    SELECT
        MONTH(o.CreatedAt)               AS [Month],
        COUNT(DISTINCT o.OrderId)        AS TotalOrders,
        SUM(o.TotalAmount)               AS Revenue,
        SUM(o.DiscountAmount)            AS TotalDiscount,
        AVG(o.TotalAmount)               AS AvgOrderValue
    FROM Orders o
    WHERE YEAR(o.CreatedAt) = @Year
      AND o.OrderStatus NOT IN (N'CANCELLED', N'REFUNDED')
      AND o.PaymentStatus = 'PAID'
    GROUP BY MONTH(o.CreatedAt)
    ORDER BY [Month];
END;
GO

-- ============================================================
-- C. TRIGGERS
-- ============================================================

-- C1. Tự cập nhật UpdatedAt khi Products thay đổi
CREATE OR ALTER TRIGGER trg_Products_UpdatedAt
ON Products AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Products
    SET UpdatedAt = SYSUTCDATETIME()
    WHERE ProductId IN (SELECT ProductId FROM inserted);
END;
GO

-- C2. Tự cập nhật UpdatedAt khi Orders thay đổi
CREATE OR ALTER TRIGGER trg_Orders_UpdatedAt
ON Orders AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    UPDATE Orders
    SET UpdatedAt = SYSUTCDATETIME()
    WHERE OrderId IN (SELECT OrderId FROM inserted);
END;
GO

-- C3. Không cho trừ tồn kho xuống âm
CREATE OR ALTER TRIGGER trg_ProductVariants_NoNegativeStock
ON ProductVariants AFTER UPDATE AS
BEGIN
    SET NOCOUNT ON;
    IF EXISTS (SELECT 1 FROM inserted WHERE StockQty < 0)
    BEGIN
        ROLLBACK;
        RAISERROR(N'Không thể trừ tồn kho xuống giá trị âm.', 16, 1);
    END;
END;
GO

-- ============================================================
-- D. OPTIMIZED QUERY SAMPLES
-- ============================================================

-- D1. Trang chủ: featured products (covering index đã có)
SELECT TOP 8
    ProductId, ProductName, Slug, BasePrice, SalePrice, ThumbnailUrl
FROM Products WITH (NOLOCK)
WHERE IsActive = 1 AND IsFeatured = 1
ORDER BY ProductId DESC;
GO

-- D2. Full-text search sản phẩm (giả lập LIKE; nên dùng FTS trong prod)
-- CREATE FULLTEXT CATALOG ft_catalog AS DEFAULT;
-- CREATE FULLTEXT INDEX ON Products(ProductName, Description) KEY INDEX PK_Products;
-- Sau đó dùng: WHERE CONTAINS(ProductName, @keyword)

-- D3. Gợi ý sản phẩm cùng danh mục (dùng trên trang chi tiết)
-- Giả sử @ProductId = 1, @CategoryId = 6
--
-- SELECT TOP 4 p.ProductId, p.ProductName, p.Slug, p.ThumbnailUrl,
--              COALESCE(p.SalePrice, p.BasePrice) AS EffectivePrice
-- FROM   Products p
-- WHERE  p.CategoryId = @CategoryId
--   AND  p.ProductId  <> @ProductId
--   AND  p.IsActive   = 1
-- ORDER BY NEWID();    -- random – production: dùng collaborative filtering

-- D4. Bảng điều khiển – top 5 sản phẩm bán chạy
SELECT TOP 5
    p.ProductId, p.ProductName, p.ThumbnailUrl,
    SUM(od.Quantity)                AS TotalSold,
    SUM(od.LineTotal)               AS TotalRevenue
FROM OrderDetails od
JOIN ProductVariants pv ON pv.VariantId = od.VariantId
JOIN Products        p  ON p.ProductId  = pv.ProductId
JOIN Orders          o  ON o.OrderId    = od.OrderId
WHERE o.OrderStatus NOT IN (N'CANCELLED', N'REFUNDED')
GROUP BY p.ProductId, p.ProductName, p.ThumbnailUrl
ORDER BY TotalSold DESC;
GO

PRINT N'[OK] Views, Stored Procs, Triggers & Queries tạo thành công!';
GO
