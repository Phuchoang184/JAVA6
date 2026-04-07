(function () {
    const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1529139574466-a303027c1d8b?w=900&q=80';
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
        return product?.displayImageUrl || product?.thumbnailUrl || FALLBACK_IMAGE;
    }

    function formatPrice(value) {
        if (!value) {
            return '0₫';
        }
        return new Intl.NumberFormat('vi-VN').format(value) + '₫';
    }

    function calculateDiscount(product) {
        if (!product?.salePrice || !product?.basePrice || product.salePrice >= product.basePrice) {
            return 0;
        }
        return Math.round(((product.basePrice - product.salePrice) / product.basePrice) * 100);
    }

    document.addEventListener('DOMContentLoaded', function () {
        const mountPoint = document.getElementById('salePageApp');
        if (!mountPoint || typeof Vue === 'undefined') {
            return;
        }

        Vue.createApp({
            data() {
                return {
                    products: [],
                    loading: true,
                    activeTab: 'all',
                    countdown: { days: '00', hours: '00', minutes: '00', seconds: '00' },
                    countdownTimer: null,
                    flashEndsAt: new Date(Date.now() + (72 * 60 * 60 * 1000)),
                    quickViewOpen: false,
                    quickViewLoading: false,
                    quickProduct: null,
                    quickSelectedVariantId: null,
                    productCache: {}
                };
            },
            computed: {
                tabs() {
                    return [
                        { key: 'all', label: 'Flash picks' },
                        { key: 'sale30', label: 'Sale 30%' },
                        { key: 'sale50', label: 'Sale 50%' },
                        { key: 'fixed199', label: 'Đồng giá 199k' }
                    ];
                },
                filteredProducts() {
                    if (this.activeTab === 'sale30') {
                        return this.products.filter(product => product.discountPercent >= 30 && product.discountPercent < 50);
                    }
                    if (this.activeTab === 'sale50') {
                        return this.products.filter(product => product.discountPercent >= 50);
                    }
                    if (this.activeTab === 'fixed199') {
                        return this.products.filter(product => (product.salePrice || product.basePrice || 0) <= 199000);
                    }
                    return this.products;
                },
                flashFeatured() {
                    return [...this.products]
                        .sort((left, right) => (right.discountPercent || 0) - (left.discountPercent || 0))
                        .slice(0, 3);
                },
                quickSelectedVariant() {
                    return this.quickProduct?.variants?.find(v => v.variantId === this.quickSelectedVariantId) || null;
                }
            },
            mounted() {
                this.loadSaleProducts();
                this.updateCountdown();
                this.countdownTimer = window.setInterval(this.updateCountdown, 1000);
                this._escHandler = (e) => { if (e.key === 'Escape') this.closeQuickView(); };
                document.addEventListener('keydown', this._escHandler);
            },
            beforeUnmount() {
                if (this.countdownTimer) {
                    window.clearInterval(this.countdownTimer);
                }
                document.removeEventListener('keydown', this._escHandler);
            },
            methods: {
                async loadSaleProducts() {
                    this.loading = true;
                    try {
                        const response = await axios.get('/api/products/sale');
                        this.products = (response.data.data || []).map(product => ({
                            ...product,
                            discountPercent: product.discountPercent || calculateDiscount(product),
                            nameParts: splitProductName(product),
                            displayImageUrl: resolveImage(product)
                        }));
                    } catch (error) {
                        console.error('Failed to load sale products', error);
                    } finally {
                        this.loading = false;
                    }
                },
                updateCountdown() {
                    const distance = this.flashEndsAt.getTime() - Date.now();
                    if (distance <= 0) {
                        this.countdown = { days: '00', hours: '00', minutes: '00', seconds: '00' };
                        return;
                    }

                    const days = Math.floor(distance / (1000 * 60 * 60 * 24));
                    const hours = Math.floor((distance / (1000 * 60 * 60)) % 24);
                    const minutes = Math.floor((distance / (1000 * 60)) % 60);
                    const seconds = Math.floor((distance / 1000) % 60);

                    this.countdown = {
                        days: String(days).padStart(2, '0'),
                        hours: String(hours).padStart(2, '0'),
                        minutes: String(minutes).padStart(2, '0'),
                        seconds: String(seconds).padStart(2, '0')
                    };
                },
                formatPrice(value) {
                    return formatPrice(value);
                },
                hasDiscount(product) {
                    return !!(product.salePrice && product.basePrice && product.salePrice < product.basePrice);
                },
                tabCount(key) {
                    if (key === 'sale30') {
                        return this.products.filter(product => product.discountPercent >= 30 && product.discountPercent < 50).length;
                    }
                    if (key === 'sale50') {
                        return this.products.filter(product => product.discountPercent >= 50).length;
                    }
                    if (key === 'fixed199') {
                        return this.products.filter(product => (product.salePrice || product.basePrice || 0) <= 199000).length;
                    }
                    return this.products.length;
                },
                scarcityLabel(product) {
                    const stockQty = product.totalStockQty || 0;
                    if (stockQty <= 0) {
                        return 'Đã bán hết';
                    }
                    if (stockQty <= 5) {
                        return 'Chỉ còn ' + stockQty + ' sản phẩm cuối cùng';
                    }
                    if (stockQty <= 12) {
                        return 'Còn ' + stockQty + ' sản phẩm trong kho';
                    }
                    return 'Flash deal đang được săn mạnh';
                },
                scarcityProgress(product) {
                    const stockQty = Math.max(0, Math.min(product.totalStockQty || 0, 20));
                    return Math.max(12, ((20 - stockQty) / 20) * 100);
                },
                async quickAdd(product) {
                    if (!product.defaultVariantId || !window.cartApp) {
                        window.location.href = '/products/' + product.productId;
                        return;
                    }
                    await window.cartApp.addToCart(product.defaultVariantId, 1);
                },
                async openQuickView(product) {
                    this.quickViewOpen = true;
                    this.quickViewLoading = true;
                    document.body.style.overflow = 'hidden';
                    try {
                        if (!this.productCache[product.productId]) {
                            const response = await axios.get('/api/products/' + product.productId);
                            const data = response.data.data;
                            this.productCache[product.productId] = {
                                ...data,
                                discountPercent: data.discountPercent || calculateDiscount(data),
                                nameParts: splitProductName(data),
                                displayImageUrl: resolveImage(data)
                            };
                        }
                        this.quickProduct = this.productCache[product.productId];
                        const firstAvailable = this.quickProduct.variants?.find(v => v.stockQty > 0);
                        this.quickSelectedVariantId = firstAvailable?.variantId || this.quickProduct.defaultVariantId || null;
                    } catch (error) {
                        console.error('Failed to open quick view', error);
                        this.quickProduct = product;
                        this.quickSelectedVariantId = product.defaultVariantId || null;
                    } finally {
                        this.quickViewLoading = false;
                    }
                },
                closeQuickView() {
                    this.quickViewOpen = false;
                    this.quickProduct = null;
                    this.quickSelectedVariantId = null;
                    document.body.style.overflow = '';
                },
                async addQuickViewToCart() {
                    if (!this.quickSelectedVariantId || !window.cartApp) return;
                    await window.cartApp.addToCart(this.quickSelectedVariantId, 1);
                    this.closeQuickView();
                }
            }
        }).mount('#salePageApp');
    });
})();