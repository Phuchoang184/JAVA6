(function () {
    const NAME_TRANSLATIONS = {
        'dam-midi-hoa-nhi': 'Wildflower Midi Dress',
        'chan-vay-but-chi': 'Refined Pencil Skirt',
        'blazer-oversize-nu': 'Relaxed Power Blazer',
        'vay-midi-xep-ly': 'Pleated Midi Skirt',
        'quan-culottes-ong-rong': 'Wide-leg Culottes',
        'tui-xach-da-bo-mini': 'Petite Leather Handbag',
        'ao-len-cashmere-nu': 'Lightweight Cashmere Knit',
        'giay-cao-got-mui-nhon': 'Pointed Stiletto Heel',
        'ao-polo-classic-nam': 'Urban Essence Polo',
        'quan-tay-nam-slim': 'Signature Slim-fit Trouser',
        'blazer-nam-structured': 'Structured Tailored Blazer',
        'giay-loafer-da-nam': 'Classic Leather Loafer'
    };

    const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=900&q=80';

    function splitProductName(product) {
        const rawName = (product?.productName || '').trim();
        if (!rawName) {
            return { primary: 'LEIKA Signature', secondary: 'LEIKA Edition' };
        }

        if (rawName.includes('|')) {
            const parts = rawName.split('|').map(part => part.trim()).filter(Boolean);
            return {
                primary: parts[0] || rawName,
                secondary: parts[1] || 'LEIKA Edition'
            };
        }

        return {
            primary: rawName,
            secondary: NAME_TRANSLATIONS[product?.slug] || 'LEIKA Edition'
        };
    }

    function resolveImage(product) {
        return product?.displayImageUrl || product?.thumbnailUrl || product?.variants?.find(variant => variant.imageUrl)?.imageUrl || FALLBACK_IMAGE;
    }

    function formatPrice(value) {
        if (!value) {
            return '0₫';
        }
        return new Intl.NumberFormat('vi-VN').format(value) + '₫';
    }

    function flattenCategories(categories) {
        return (categories || []).flatMap(category => [
            category,
            ...flattenCategories(category.subCategories || [])
        ]);
    }

    document.addEventListener('DOMContentLoaded', function () {
        const mountPoint = document.getElementById('shopPageApp');
        if (!mountPoint || typeof Vue === 'undefined') {
            return;
        }

        Vue.createApp({
            data() {
                return {
                    products: [],
                    categories: [],
                    brands: [],
                    keyword: '',
                    selectedCategory: null,
                    selectedBrand: null,
                    minPrice: null,
                    maxPrice: null,
                    sortBy: 'newest',
                    currentPage: 0,
                    totalPages: 0,
                    totalElements: 0,
                    loading: true,
                    quickViewOpen: false,
                    quickViewLoading: false,
                    quickProduct: null,
                    quickQuantity: 1,
                    quickSelectedVariantId: null,
                    productCache: {}
                };
            },
            computed: {
                visiblePages() {
                    const pages = [];
                    const start = Math.max(0, this.currentPage - 2);
                    const end = Math.min(this.totalPages, start + 5);
                    for (let page = start; page < end; page += 1) {
                        pages.push(page);
                    }
                    return pages;
                },
                activeFilterCount() {
                    return [this.selectedCategory, this.selectedBrand, this.minPrice, this.maxPrice, this.keyword]
                        .filter(value => value !== null && value !== '').length;
                },
                selectedQuickVariant() {
                    return this.quickProduct?.variants?.find(variant => variant.variantId === this.quickSelectedVariantId) || null;
                }
            },
            mounted() {
                const mountEl = document.getElementById('shopPageApp');
                const initialCategorySlug = mountEl?.dataset?.initialCategory || '';
                const initialSort = mountEl?.dataset?.initialSort || '';

                const params = new URLSearchParams(window.location.search);
                this.keyword = params.get('keyword') || '';
                this.sortBy = params.get('sort') || initialSort || 'newest';

                this.loadFilters().then(() => {
                    const categorySlug = params.get('category') || initialCategorySlug;
                    if (categorySlug) {
                        const foundCategory = flattenCategories(this.categories).find(category => category.slug === categorySlug);
                        if (foundCategory) {
                            this.selectedCategory = foundCategory.categoryId;
                        }
                    }
                    this.loadProducts();
                });
            },
            methods: {
                async loadFilters() {
                    try {
                        const [categoryResponse, brandResponse] = await Promise.all([
                            axios.get('/api/products/categories'),
                            axios.get('/api/products/brands')
                        ]);
                        this.categories = categoryResponse.data.data || [];
                        this.brands = brandResponse.data.data || [];
                    } catch (error) {
                        console.error('Failed to load shop filters', error);
                    }
                },
                async loadProducts() {
                    this.loading = true;
                    try {
                        const params = {
                            page: this.currentPage,
                            size: 12,
                            sortBy: this.sortBy
                        };

                        if (this.keyword) params.keyword = this.keyword;
                        if (this.selectedCategory) params.categoryId = this.selectedCategory;
                        if (this.selectedBrand) params.brandId = this.selectedBrand;
                        if (this.minPrice !== null) params.minPrice = this.minPrice;
                        if (this.maxPrice !== null) params.maxPrice = this.maxPrice;

                        const response = await axios.get('/api/products', { params });
                        const page = response.data.data;
                        this.products = (page.content || []).map(product => this.normalizeProduct(product));
                        this.totalPages = page.totalPages || 0;
                        this.totalElements = page.totalElements || 0;
                    } catch (error) {
                        console.error('Failed to load shop products', error);
                    } finally {
                        this.loading = false;
                    }
                },
                normalizeProduct(product) {
                    return {
                        ...product,
                        nameParts: splitProductName(product),
                        displayImageUrl: resolveImage(product),
                        stockLabel: this.getStockLabel(product.totalStockQty)
                    };
                },
                getStockLabel(stockQty) {
                    if (!stockQty || stockQty <= 0) {
                        return 'Tạm hết hàng';
                    }
                    if (stockQty <= 5) {
                        return 'Chỉ còn ' + stockQty + ' sản phẩm';
                    }
                    return 'Có sẵn ' + stockQty + ' sản phẩm';
                },
                filterCategory(categoryId) {
                    this.selectedCategory = categoryId;
                    this.currentPage = 0;
                    this.loadProducts();
                },
                filterBrand(brandId) {
                    this.selectedBrand = brandId;
                    this.currentPage = 0;
                    this.loadProducts();
                },
                filterPrice(minPrice, maxPrice) {
                    this.minPrice = minPrice;
                    this.maxPrice = maxPrice;
                    this.currentPage = 0;
                    this.loadProducts();
                },
                resetFilters() {
                    this.keyword = '';
                    this.selectedCategory = null;
                    this.selectedBrand = null;
                    this.minPrice = null;
                    this.maxPrice = null;
                    this.sortBy = 'newest';
                    this.currentPage = 0;
                    this.loadProducts();
                },
                changePage(page) {
                    if (page < 0 || page >= this.totalPages) {
                        return;
                    }
                    this.currentPage = page;
                    this.loadProducts();
                    window.scrollTo({ top: 0, behavior: 'smooth' });
                },
                getPrice(product) {
                    return formatPrice(product.salePrice || product.basePrice);
                },
                getBasePrice(product) {
                    return formatPrice(product.basePrice);
                },
                hasDiscount(product) {
                    return !!(product.salePrice && product.basePrice && product.salePrice < product.basePrice);
                },
                productHref(product) {
                    return '/products/' + product.productId;
                },
                resolveImage(product) {
                    return resolveImage(product);
                },
                async quickAdd(product) {
                    if (!product.defaultVariantId) {
                        await this.openQuickView(product);
                        return;
                    }
                    if (window.cartApp) {
                        await window.cartApp.addToCart(product.defaultVariantId, 1);
                    } else {
                        window.location.href = this.productHref(product);
                    }
                },
                async openQuickView(product) {
                    this.quickViewOpen = true;
                    this.quickViewLoading = true;
                    document.body.style.overflow = 'hidden';

                    try {
                        if (!this.productCache[product.productId]) {
                            const response = await axios.get('/api/products/' + product.productId);
                            this.productCache[product.productId] = this.normalizeProduct(response.data.data);
                        }

                        this.quickProduct = this.productCache[product.productId];
                        this.quickQuantity = 1;

                        const firstAvailableVariant = this.quickProduct.variants?.find(variant => variant.stockQty > 0);
                        this.quickSelectedVariantId = firstAvailableVariant?.variantId || this.quickProduct.defaultVariantId || null;
                    } catch (error) {
                        console.error('Failed to open quick view', error);
                        this.quickProduct = this.normalizeProduct(product);
                        this.quickSelectedVariantId = product.defaultVariantId || null;
                    } finally {
                        this.quickViewLoading = false;
                    }
                },
                closeQuickView() {
                    this.quickViewOpen = false;
                    this.quickProduct = null;
                    this.quickSelectedVariantId = null;
                    this.quickQuantity = 1;
                    document.body.style.overflow = '';
                },
                incrementQuickQuantity() {
                    if (!this.selectedQuickVariant || this.quickQuantity >= this.selectedQuickVariant.stockQty) {
                        return;
                    }
                    this.quickQuantity += 1;
                },
                decrementQuickQuantity() {
                    if (this.quickQuantity > 1) {
                        this.quickQuantity -= 1;
                    }
                },
                async addQuickViewToCart() {
                    if (!this.quickSelectedVariantId || !window.cartApp) {
                        return;
                    }
                    await window.cartApp.addToCart(this.quickSelectedVariantId, this.quickQuantity);
                    this.closeQuickView();
                }
            }
        }).mount('#shopPageApp');
    });
})();