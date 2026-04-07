-- data.sql
-- Seed data for LEIKA LUXURY Fashion Shop
-- This file runs on every startup; use MERGE/IF NOT EXISTS to avoid duplicates.

-- ======================== ROLES ========================
IF NOT EXISTS (SELECT 1 FROM Roles WHERE RoleName = 'USER')
    INSERT INTO Roles (RoleName) VALUES ('USER');
IF NOT EXISTS (SELECT 1 FROM Roles WHERE RoleName = 'ADMIN')
    INSERT INTO Roles (RoleName) VALUES ('ADMIN');

-- ======================== BRANDS ========================
IF NOT EXISTS (SELECT 1 FROM Brands WHERE BrandName = 'LEIKA')
    INSERT INTO Brands (BrandName, LogoUrl, Description, IsActive) VALUES (N'LEIKA', NULL, N'Thương hiệu thời trang cao cấp Việt Nam', 1);
IF NOT EXISTS (SELECT 1 FROM Brands WHERE BrandName = 'Chanel')
    INSERT INTO Brands (BrandName, LogoUrl, Description, IsActive) VALUES (N'Chanel', NULL, N'Nhà mốt huyền thoại nước Pháp', 1);
IF NOT EXISTS (SELECT 1 FROM Brands WHERE BrandName = 'Dior')
    INSERT INTO Brands (BrandName, LogoUrl, Description, IsActive) VALUES (N'Dior', NULL, N'Biểu tượng của sự sang trọng', 1);
IF NOT EXISTS (SELECT 1 FROM Brands WHERE BrandName = 'Gucci')
    INSERT INTO Brands (BrandName, LogoUrl, Description, IsActive) VALUES (N'Gucci', NULL, N'Thương hiệu thời trang Italia', 1);
IF NOT EXISTS (SELECT 1 FROM Brands WHERE BrandName = 'Zara')
    INSERT INTO Brands (BrandName, LogoUrl, Description, IsActive) VALUES (N'Zara', NULL, N'Thời trang nhanh hàng đầu thế giới', 1);

-- ======================== CATEGORIES ========================
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'thoi-trang-nu')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Thời trang nữ', 'thoi-trang-nu', 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=500&q=80', 1, 1, NULL);
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'thoi-trang-nam')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Thời trang nam', 'thoi-trang-nam', 'https://images.unsplash.com/photo-1480455624313-e29b44bbfde1?w=500&q=80', 2, 1, NULL);
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'phu-kien')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Phụ kiện', 'phu-kien', 'https://images.unsplash.com/photo-1584916201218-f4181f08ce49?w=500&q=80', 3, 1, NULL);

-- Sub-categories: Thời trang nữ
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'dam-nu')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Đầm', 'dam-nu', NULL, 1, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'thoi-trang-nu'));
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'ao-nu')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Áo nữ', 'ao-nu', NULL, 2, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'thoi-trang-nu'));
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'chan-vay')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Chân váy', 'chan-vay', NULL, 3, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'thoi-trang-nu'));
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'quan-nu')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Quần nữ', 'quan-nu', NULL, 4, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'thoi-trang-nu'));

-- Sub-categories: Thời trang nam
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'ao-nam')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Áo nam', 'ao-nam', NULL, 1, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'thoi-trang-nam'));
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'quan-nam')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Quần nam', 'quan-nam', NULL, 2, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'thoi-trang-nam'));

-- Sub-categories: Phụ kiện
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'tui-xach')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Túi xách', 'tui-xach', NULL, 1, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'phu-kien'));
IF NOT EXISTS (SELECT 1 FROM Categories WHERE Slug = 'giay-dep')
    INSERT INTO Categories (CategoryName, Slug, ImageUrl, SortOrder, IsActive, ParentId) VALUES (N'Giày dép', 'giay-dep', NULL, 2, 1, (SELECT CategoryId FROM Categories WHERE Slug = 'phu-kien'));

-- ======================== PRODUCTS ========================
IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'dam-midi-hoa-nhi')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'dam-nu'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'LEIKA'),
        N'Đầm Midi Hoa Đồng Nội', 'dam-midi-hoa-nhi',
        N'Thiết kế bay bổng với họa tiết hoa đồng nội lãng mạn, chất liệu voan lụa cao cấp mát mẻ, cổ V quyến rũ phù hợp đi chơi, đi làm.',
        890000.00, 750000.00,
        'https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'ao-so-mi-lua-trang')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'ao-nu'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'LEIKA'),
        N'Sơ Mi Lụa Cổ Bèo | Pearl Ruffle Silk Blouse', 'ao-so-mi-lua-trang',
        N'Quyến rũ với chất lụa ngọc trai mềm mịn như làn da, không nhăn. Cổ bèo tiểu thư nữ tính, tạo điểm nhấn thanh lịch tối giản.',
        690000.00, NULL,
        'https://images.unsplash.com/photo-1604695573706-53170668f6a6?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'chan-vay-but-chi')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'chan-vay'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'LEIKA'),
        N'Chân Váy Bút Chì Thanh Lịch', 'chan-vay-but-chi',
        N'Tôn vinh vóc dáng hoàn hảo với thiết kế lưng cao ôm sát, đường viền may tinh tế mở ra phong cách sang trọng hiện đại.',
        550000.00, NULL,
        'https://images.unsplash.com/photo-1582142306909-195724d33ffc?w=500&q=80',
        0, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'tui-xach-da-bo-mini')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'tui-xach'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'Chanel'),
        N'Túi Mini Da Thật | Petite Leather Handbag', 'tui-xach-da-bo-mini',
        N'Túi nhỏ gọn tinh tế làm từ da bò thật 100%, phong cách Hàn Quốc đẳng cấp, điểm nhấn hoàn hảo cho mọi set đồ.',
        1250000.00, 990000.00,
        'https://images.unsplash.com/photo-1584916201218-f4181f08ce49?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'blazer-oversize-nu')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'ao-nu'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'Zara'),
        N'Blazer Oversize Thanh Lịch Nữ', 'blazer-oversize-nu',
        N'Blazer oversize dáng suông thanh lịch, phù hợp cho cả công sở lẫn dạo phố, mang đến phong cách thời thượng đầy cá tính.',
        1290000.00, 990000.00,
        'https://images.unsplash.com/photo-1591369822096-ffd140ec948f?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'ao-polo-classic-nam')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'ao-nam'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'LEIKA'),
        N'Polo Nam Classic | Urban Essence Polo', 'ao-polo-classic-nam',
        N'Áo polo cổ bẻ chất liệu cotton pima cao cấp, form regular thoải mái, tôn dáng phong cách năng động hiện đại.',
        450000.00, NULL,
        'https://images.unsplash.com/photo-1586363104862-3a5e2ab60d99?w=500&q=80',
        0, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'quan-tay-nam-slim')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'quan-nam'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'Gucci'),
        N'Quần Tây Hiện Đại | Signature Slim-fit Trouser', 'quan-tay-nam-slim',
        N'Quần tây ôm vừa phải, chất liệu cao cấp co giãn, phong cách chuyên nghiệp dành cho quý ông hiện đại.',
        790000.00, 690000.00,
        'https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'giay-cao-got-mui-nhon')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'giay-dep'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'Dior'),
        N'Giày Mũi Nhọn Thanh Lịch | Pointed Stiletto Heel', 'giay-cao-got-mui-nhon',
        N'Giày cao gót mũi nhọn kiểu dáng tinh tế, gót nhọn 7cm thanh mảnh, tôn đôi chân thon gọn hoàn hảo.',
        1890000.00, 1490000.00,
        'https://images.unsplash.com/photo-1543163521-1bf539c55dd2?w=500&q=80',
        1, 1
    );

-- ======================== NEW PRODUCTS ========================
IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'vay-midi-xep-ly')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'chan-vay'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'LEIKA'),
        N'Váy Midi Xếp Ly | Pleated Midi Skirt', 'vay-midi-xep-ly',
        N'Thiết kế xếp ly mềm mại bay bổng, chất liệu voan cao cấp rủ nhẹ nhàng, tạo dáng thanh thoát khi di chuyển.',
        780000.00, 650000.00,
        'https://images.unsplash.com/photo-1583496661160-fb5886a0aaaa?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'blazer-nam-structured')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'ao-nam'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'Gucci'),
        N'Blazer Nam Cấu Trúc | Structured Tailored Blazer', 'blazer-nam-structured',
        N'Blazer dáng ôm cấu trúc vai hoàn hảo, lớp lót lụa mát, phom dáng chuẩn Ý cho phong cách chuyên nghiệp.',
        2450000.00, 1990000.00,
        'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'giay-loafer-da-nam')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'giay-dep'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'LEIKA'),
        N'Giày Loafer Da | Classic Leather Loafer', 'giay-loafer-da-nam',
        N'Giày loafer da bò nguyên tấm, đường may thủ công tinh xảo, đế cao su tự nhiên chống trượt, phù hợp công sở và dạo phố.',
        1690000.00, NULL,
        'https://images.unsplash.com/photo-1614252369475-531eba835eb1?w=500&q=80',
        0, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'ao-len-cashmere-nu')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'ao-nu'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'Chanel'),
        N'Áo Len Cashmere Mỏng | Lightweight Cashmere Knit', 'ao-len-cashmere-nu',
        N'Áo len cashmere siêu mỏng, mềm mịn ôm nhẹ cơ thể, phối dễ dàng với mọi outfit từ công sở đến dạo phố.',
        1350000.00, 1090000.00,
        'https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=500&q=80',
        1, 1
    );

IF NOT EXISTS (SELECT 1 FROM Products WHERE Slug = 'quan-culottes-ong-rong')
    INSERT INTO Products (CategoryId, BrandId, ProductName, Slug, Description, BasePrice, SalePrice, ThumbnailUrl, IsFeatured, IsActive)
    VALUES (
        (SELECT CategoryId FROM Categories WHERE Slug = 'quan-nu'),
        (SELECT BrandId FROM Brands WHERE BrandName = 'Zara'),
        N'Quần Culottes Ống Rộng | Wide-leg Culottes', 'quan-culottes-ong-rong',
        N'Quần culottes ống rộng phom dáng thoải mái, chất liệu linen pha thoáng mát, lưng cao tôn dáng hiệu quả.',
        650000.00, 520000.00,
        'https://images.unsplash.com/photo-1594633312681-425c7b97ccd1?w=500&q=80',
        0, 1
    );

-- ======================== PRODUCT VARIANTS ========================
-- Đầm Midi Hoa Nhí
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-DMH-S')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'dam-midi-hoa-nhi'), 'LEIKA-DMH-S', 25, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-DMH-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'dam-midi-hoa-nhi'), 'LEIKA-DMH-M', 30, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-DMH-L')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'dam-midi-hoa-nhi'), 'LEIKA-DMH-L', 20, 0, NULL, 1);

-- Áo Sơ Mi Lụa
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-SML-S')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'ao-so-mi-lua-trang'), 'LEIKA-SML-S', 15, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-SML-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'ao-so-mi-lua-trang'), 'LEIKA-SML-M', 20, 0, NULL, 1);

-- Chân Váy Bút Chì
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-CVB-S')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'chan-vay-but-chi'), 'LEIKA-CVB-S', 30, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-CVB-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'chan-vay-but-chi'), 'LEIKA-CVB-M', 25, 0, NULL, 1);

-- Túi Xách Da Bò
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'CHA-TXD-ONE')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'tui-xach-da-bo-mini'), 'CHA-TXD-ONE', 15, 0, NULL, 1);

-- Blazer Oversize
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'ZAR-BLZ-S')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'blazer-oversize-nu'), 'ZAR-BLZ-S', 10, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'ZAR-BLZ-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'blazer-oversize-nu'), 'ZAR-BLZ-M', 12, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'ZAR-BLZ-L')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'blazer-oversize-nu'), 'ZAR-BLZ-L', 8, 0, NULL, 1);

-- Áo Polo Nam
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-POL-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'ao-polo-classic-nam'), 'LEIKA-POL-M', 40, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-POL-L')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'ao-polo-classic-nam'), 'LEIKA-POL-L', 35, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-POL-XL')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'ao-polo-classic-nam'), 'LEIKA-POL-XL', 20, 0, NULL, 1);

-- Quần Tây Nam
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'GUC-QTN-30')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'quan-tay-nam-slim'), 'GUC-QTN-30', 15, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'GUC-QTN-32')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'quan-tay-nam-slim'), 'GUC-QTN-32', 20, 0, NULL, 1);

-- Giày Cao Gót
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'DIO-GCG-36')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'giay-cao-got-mui-nhon'), 'DIO-GCG-36', 10, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'DIO-GCG-37')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'giay-cao-got-mui-nhon'), 'DIO-GCG-37', 12, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'DIO-GCG-38')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'giay-cao-got-mui-nhon'), 'DIO-GCG-38', 8, 0, NULL, 1);

-- Váy Midi Xếp Ly
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-VML-S')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'vay-midi-xep-ly'), 'LEIKA-VML-S', 20, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-VML-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'vay-midi-xep-ly'), 'LEIKA-VML-M', 25, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-VML-L')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'vay-midi-xep-ly'), 'LEIKA-VML-L', 15, 0, NULL, 1);

-- Blazer Nam Structured
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'GUC-BLN-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'blazer-nam-structured'), 'GUC-BLN-M', 12, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'GUC-BLN-L')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'blazer-nam-structured'), 'GUC-BLN-L', 10, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'GUC-BLN-XL')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'blazer-nam-structured'), 'GUC-BLN-XL', 8, 0, NULL, 1);

-- Giày Loafer Da
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-GLF-40')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'giay-loafer-da-nam'), 'LEIKA-GLF-40', 15, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-GLF-42')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'giay-loafer-da-nam'), 'LEIKA-GLF-42', 18, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'LEIKA-GLF-43')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'giay-loafer-da-nam'), 'LEIKA-GLF-43', 12, 0, NULL, 1);

-- Áo Len Cashmere
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'CHA-ALC-S')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'ao-len-cashmere-nu'), 'CHA-ALC-S', 10, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'CHA-ALC-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'ao-len-cashmere-nu'), 'CHA-ALC-M', 15, 0, NULL, 1);

-- Quần Culottes Ống Rộng
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'ZAR-QCL-S')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'quan-culottes-ong-rong'), 'ZAR-QCL-S', 20, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'ZAR-QCL-M')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'quan-culottes-ong-rong'), 'ZAR-QCL-M', 25, 0, NULL, 1);
IF NOT EXISTS (SELECT 1 FROM ProductVariants WHERE SKU = 'ZAR-QCL-L')
    INSERT INTO ProductVariants (ProductId, SKU, StockQty, ExtraPrice, ImageUrl, IsActive)
    VALUES ((SELECT ProductId FROM Products WHERE Slug = 'quan-culottes-ong-rong'), 'ZAR-QCL-L', 18, 0, NULL, 1);

-- ======================== USERS (seed accounts) ========================
-- Admin: admin@leika.vn / Admin@123
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'admin@leika.vn')
    INSERT INTO Users (RoleId, FullName, Email, PasswordHash, PhoneNumber, IsActive)
    VALUES (
        (SELECT RoleId FROM Roles WHERE RoleName = 'ADMIN'),
        N'Admin LEIKA', 'admin@leika.vn',
        '$2a$10$m6IseXm2m5KZ1rcz9Ebxg.8Hl32ykzS9ICioFfIv5CZtHs8QixrsW',
        '0901234567', 1
    );
-- User: user@leika.vn / User@123
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'user@leika.vn')
    INSERT INTO Users (RoleId, FullName, Email, PasswordHash, PhoneNumber, IsActive)
    VALUES (
        (SELECT RoleId FROM Roles WHERE RoleName = 'USER'),
        N'Nguyễn Văn A', 'user@leika.vn',
        '$2a$10$ZjXjdkU8loOBTFumyWJVLO91zH/9rSm8QxNraaRnSwUTxYCI6L4tq',
        '0909876543', 1
    );
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'linh.tran@leika.vn')
    INSERT INTO Users (RoleId, FullName, Email, PasswordHash, PhoneNumber, IsActive)
    VALUES (
        (SELECT RoleId FROM Roles WHERE RoleName = 'USER'),
        N'Trần Linh', 'linh.tran@leika.vn',
        '$2a$10$ZjXjdkU8loOBTFumyWJVLO91zH/9rSm8QxNraaRnSwUTxYCI6L4tq',
        '0912345678', 1
    );
IF NOT EXISTS (SELECT 1 FROM Users WHERE Email = 'minh.nguyen@leika.vn')
    INSERT INTO Users (RoleId, FullName, Email, PasswordHash, PhoneNumber, IsActive)
    VALUES (
        (SELECT RoleId FROM Roles WHERE RoleName = 'USER'),
        N'Nguyễn Minh', 'minh.nguyen@leika.vn',
        '$2a$10$ZjXjdkU8loOBTFumyWJVLO91zH/9rSm8QxNraaRnSwUTxYCI6L4tq',
        '0923456789', 1
    );

-- ======================== REVIEWS ========================
INSERT INTO Reviews (ProductId, UserId, OrderId, Rating, Title, Body, IsApproved)
SELECT p.ProductId, u.UserId, NULL, s.Rating, s.Title, s.Body, 1
FROM (VALUES
    ('dam-midi-hoa-nhi', 'user@leika.vn', 5, N'Form váy rất tôn dáng', N'Chất voan nhẹ, mặc lên mềm và bay. Màu hoa lên ảnh đẹp, đi làm hay đi chơi đều hợp.'),
    ('dam-midi-hoa-nhi', 'linh.tran@leika.vn', 4, N'Nữ tính và dễ mặc', N'Đường may ổn, phần eo lên dáng gọn. Mặc cả ngày vẫn thoải mái và không bị bí.'),
    ('dam-midi-hoa-nhi', 'minh.nguyen@leika.vn', 5, N'Đúng kiểu thanh lịch', N'Váy rủ đẹp, cổ V vừa phải nên nhìn sang mà không quá cầu kỳ. Giá sale khá đáng mua.'),
    ('ao-so-mi-lua-trang', 'user@leika.vn', 5, N'Lụa mềm và đứng form', N'Áo nhẹ, mịn và ít nhăn hơn mình nghĩ. Cổ bèo tạo điểm nhấn đẹp khi mặc với chân váy công sở.'),
    ('ao-so-mi-lua-trang', 'linh.tran@leika.vn', 4, N'Mặc lên rất sáng da', N'Chất vải mát, tay áo rơi đẹp. Phù hợp đi họp hoặc phối cùng quần âu đều ổn.'),
    ('ao-so-mi-lua-trang', 'minh.nguyen@leika.vn', 5, N'Thanh lịch đúng mô tả', N'Áo không quá bóng nên nhìn sang hơn. Mình giặt nhẹ vẫn giữ phom và màu trắng sạch.'),
    ('chan-vay-but-chi', 'user@leika.vn', 4, N'Dáng váy ôm gọn', N'Lưng cao tôn dáng tốt, mặc với áo sơ mi rất hợp. Chất vải đủ dày để lên dáng đẹp.'),
    ('chan-vay-but-chi', 'linh.tran@leika.vn', 5, N'Rất hợp đi làm', N'Chân váy ôm vừa phải chứ không quá chật. Đường may sạch, bước đi vẫn thoải mái.'),
    ('chan-vay-but-chi', 'minh.nguyen@leika.vn', 4, N'Tối giản nhưng sang', N'Kiểu basic dễ phối đồ, mặc lên gọn người. Nếu có thêm màu nữa thì sẽ càng dễ chọn.'),
    ('tui-xach-da-bo-mini', 'user@leika.vn', 5, N'Túi nhỏ nhưng rất xinh', N'Da mềm, cầm chắc tay và form túi cứng cáp. Đi tiệc hay đi cafe đều hợp.'),
    ('tui-xach-da-bo-mini', 'linh.tran@leika.vn', 4, N'Đủ đựng đồ cơ bản', N'Túi mini nhưng vẫn để vừa điện thoại, son và ví nhỏ. Màu da nhìn sang và dễ phối.'),
    ('tui-xach-da-bo-mini', 'minh.nguyen@leika.vn', 5, N'Phụ kiện nâng set đồ', N'Khóa và quai chắc chắn, hoàn thiện tốt. Mình thích nhất là kiểu dáng gọn nhưng vẫn nổi bật.'),
    ('blazer-oversize-nu', 'user@leika.vn', 5, N'Blazer lên form rất đẹp', N'Dáng oversize vừa phải, không bị thùng thình. Mặc với quần jeans hay chân váy đều thời thượng.'),
    ('blazer-oversize-nu', 'linh.tran@leika.vn', 4, N'Chất vải đứng dáng', N'Vai áo và thân áo lên chuẩn, mặc trông có gu hơn hẳn. Hợp cho cả công sở lẫn đi chơi.'),
    ('blazer-oversize-nu', 'minh.nguyen@leika.vn', 5, N'Rất đáng tiền', N'Lớp vải dày vừa, không bị nóng khó chịu. Kiểu dáng hiện đại và dễ phối nhiều outfit.'),
    ('ao-polo-classic-nam', 'user@leika.vn', 4, N'Áo polo nam tính', N'Chất cotton mặc thoáng, cổ áo giữ phom khá tốt. Màu sắc basic nên phối đồ nhanh.'),
    ('ao-polo-classic-nam', 'linh.tran@leika.vn', 5, N'Form regular dễ mặc', N'Áo lên dáng gọn, không quá ôm. Mặc đi làm casual hay cuối tuần đều hợp.'),
    ('ao-polo-classic-nam', 'minh.nguyen@leika.vn', 4, N'Ổn trong tầm giá', N'Chất liệu mềm và thoải mái khi vận động. Mình đánh giá cao phần đường may cổ áo.'),
    ('quan-tay-nam-slim', 'user@leika.vn', 5, N'Quần đứng dáng đẹp', N'Ống quần slim vừa phải nên mặc trẻ trung nhưng vẫn lịch sự. Chất vải co giãn nhẹ, ngồi lâu vẫn dễ chịu.'),
    ('quan-tay-nam-slim', 'linh.tran@leika.vn', 4, N'Phù hợp môi trường công sở', N'Lên form gọn chân và khá tôn dáng. Phối với polo hoặc sơ mi đều ổn.'),
    ('quan-tay-nam-slim', 'minh.nguyen@leika.vn', 5, N'Nhìn chuyên nghiệp', N'Màu quần sang, đường ly giữ được lâu. Đây là mẫu quần dễ mặc nhất mình mua gần đây.'),
    ('giay-cao-got-mui-nhon', 'user@leika.vn', 5, N'Kiểu dáng rất sang', N'Mũi nhọn thanh thoát, đi lên chân nhìn gọn và dài hơn. Gót 7cm vừa đẹp vừa không quá khó đi.'),
    ('giay-cao-got-mui-nhon', 'linh.tran@leika.vn', 4, N'Lên outfit rất đẹp', N'Giày ôm chân khá chắc, đi tiệc nhìn nổi bật. Chất liệu và đường hoàn thiện tốt.'),
    ('giay-cao-got-mui-nhon', 'minh.nguyen@leika.vn', 5, N'Đáng mua cho dịp đặc biệt', N'Mẫu này chụp ảnh rất đẹp, phần gót thanh nhưng vẫn tạo cảm giác ổn định khi đi.'),
    ('vay-midi-xep-ly', 'user@leika.vn', 5, N'Xếp ly mềm và bay', N'Chân váy di chuyển rất đẹp, mặc lên nhẹ người. Phần lưng cao giúp tổng thể thanh thoát hơn.'),
    ('vay-midi-xep-ly', 'linh.tran@leika.vn', 4, N'Dễ phối nhiều kiểu áo', N'Mình phối với sơ mi và áo knit đều hợp. Màu sắc nhã, mặc đi làm vẫn lịch sự.'),
    ('vay-midi-xep-ly', 'minh.nguyen@leika.vn', 5, N'Nữ tính nhưng hiện đại', N'Ly váy không bị thô, rủ rất tự nhiên. Đây là mẫu mặc lên trông gọn và có điểm nhấn.'),
    ('blazer-nam-structured', 'user@leika.vn', 5, N'Chuẩn phong cách lịch lãm', N'Phom vai rõ, mặc lên đứng dáng và sang. Chất liệu tạo cảm giác cao cấp ngay từ lần thử đầu.'),
    ('blazer-nam-structured', 'linh.tran@leika.vn', 4, N'Rất hợp đi sự kiện', N'Áo blazer cắt may tốt, lên người gọn và mạnh mẽ. Phối với quần tây nhìn rất chỉnh chu.'),
    ('blazer-nam-structured', 'minh.nguyen@leika.vn', 5, N'Mặc rất ra dáng', N'Lớp lót mịn, cử động không bị cứng. Đây là kiểu blazer mặc vào là thấy chỉn chu ngay.'),
    ('giay-loafer-da-nam', 'user@leika.vn', 4, N'Giày loafer tinh tế', N'Da mềm, mang vào êm và không bị cấn nhiều. Dễ phối cả quần tây lẫn jeans.'),
    ('giay-loafer-da-nam', 'linh.tran@leika.vn', 5, N'Nhìn gọn và sang chân', N'Giày hoàn thiện đẹp, đường may chắc chắn. Mình thích vì mang cả ngày vẫn khá thoải mái.'),
    ('giay-loafer-da-nam', 'minh.nguyen@leika.vn', 4, N'Phù hợp công sở', N'Thiết kế classic nên không sợ lỗi mốt. Đế bám tốt, đi lại trong ngày khá an tâm.'),
    ('ao-len-cashmere-nu', 'user@leika.vn', 5, N'Mềm và nhẹ đúng ý', N'Áo len mỏng nhưng ấm vừa phải, mặc ôm nhẹ rất tôn dáng. Phối với quần âu nhìn sang.'),
    ('ao-len-cashmere-nu', 'linh.tran@leika.vn', 4, N'Chất liệu dễ chịu', N'Mặc trực tiếp trên da vẫn mềm, không ngứa. Form áo gọn và lên màu đẹp.'),
    ('ao-len-cashmere-nu', 'minh.nguyen@leika.vn', 5, N'Dễ mặc hằng ngày', N'Mẫu này rất tiện vì phối với váy hay quần đều ổn. Chất len nhìn cao cấp hơn giá bán.'),
    ('quan-culottes-ong-rong', 'user@leika.vn', 4, N'Thoải mái mà vẫn tôn dáng', N'Ống quần rộng vừa phải, đi lại rất thoáng. Lưng cao nên mặc lên nhìn chân dài hơn.'),
    ('quan-culottes-ong-rong', 'linh.tran@leika.vn', 5, N'Phom đẹp và dễ ứng dụng', N'Quần nhẹ, đứng dáng và không bị thô. Phối với áo ôm hoặc sơ mi đều rất đẹp.'),
    ('quan-culottes-ong-rong', 'minh.nguyen@leika.vn', 4, N'Mặc cả ngày vẫn dễ chịu', N'Chất liệu mát, phù hợp thời tiết nóng. Đây là mẫu quần mình thấy vừa tiện vừa thời trang.')
) AS s(ProductSlug, UserEmail, Rating, Title, Body)
JOIN Products p ON p.Slug = s.ProductSlug
JOIN Users u ON u.Email = s.UserEmail
WHERE NOT EXISTS (
    SELECT 1
    FROM Reviews r
    WHERE r.ProductId = p.ProductId
      AND r.UserId = u.UserId
      AND r.OrderId IS NULL
);

-- ======================== COUPONS ========================
IF NOT EXISTS (SELECT 1 FROM Coupons WHERE Code = 'WELCOME10')
    INSERT INTO Coupons (Code, DiscountType, DiscountValue, MaxUses, UsedCount, MinOrderValue, ExpiresAt, IsActive)
    VALUES ('WELCOME10', 'PERCENT', 10, 1000, 0, 200000, '2026-12-31 23:59:59', 1);
IF NOT EXISTS (SELECT 1 FROM Coupons WHERE Code = 'SAVE50K')
    INSERT INTO Coupons (Code, DiscountType, DiscountValue, MaxUses, UsedCount, MinOrderValue, ExpiresAt, IsActive)
    VALUES ('SAVE50K', 'FIXED', 50000, 500, 0, 500000, '2026-12-31 23:59:59', 1);

IF NOT EXISTS (SELECT 1 FROM Coupons WHERE Code = 'LEIKA10')
    INSERT INTO Coupons (Code, DiscountType, DiscountValue, MaxUses, UsedCount, MinOrderValue, ExpiresAt, IsActive)
    VALUES ('LEIKA10', 'PERCENT', 10, 5000, 0, 0, '2026-12-31 23:59:59', 1);
