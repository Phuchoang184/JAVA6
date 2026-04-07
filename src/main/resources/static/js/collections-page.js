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

    const CURATED_COLLECTIONS = [
        {
            key: 'spring-summer-2026',
            eyebrow: 'Collection 01',
            title: 'Spring Summer 2026',
            subtitle: 'Xuân Hè 2026',
            story: 'Nhẹ, mỏng và có độ rủ vừa đủ. Bộ sưu tập này đi theo tinh thần resort tối giản, dành cho những ngày thành phố nhiều nắng nhưng vẫn cần sự chỉn chu của LEIKA.',
            note: 'Silk touch, linen blend, floral rhythm',
            image: 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?w=1200&q=80',
            mobileHeight: '26rem',
            desktopHeight: '34rem',
            categorySlugs: ['dam-nu', 'ao-nu', 'chan-vay', 'quan-nu']
        },
        {
            key: 'luxury-office',
            eyebrow: 'Collection 02',
            title: 'Luxury Office',
            subtitle: 'Công Sở Xa Xỉ',
            story: 'Phom blazer mềm, chân váy thẳng và bảng màu trung tính tạo nên một tủ đồ công sở sang nhưng không nặng nề. Đây là nhịp điệu của quyền lực mềm.',
            note: 'Structured silhouette, precise tailoring, day-to-night ease',
            image: 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=1200&q=80',
            mobileHeight: '22rem',
            desktopHeight: '28rem',
            categorySlugs: ['ao-nu', 'chan-vay', 'quan-nam', 'ao-nam']
        },
        {
            key: 'city-after-hours',
            eyebrow: 'Collection 03',
            title: 'City After Hours',
            subtitle: 'Dạ Tiệc Thành Thị',
            story: 'Những thiết kế có độ bóng vừa phải, đường cắt sắc và điểm nhấn phụ kiện dành cho bữa tối, gallery opening hay một cuộc hẹn cần sự xuất hiện tinh tế.',
            note: 'Gloss finish, black tie undertone, polished confidence',
            image: 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=1200&q=80',
            mobileHeight: '30rem',
            desktopHeight: '38rem',
            categorySlugs: ['dam-nu', 'tui-xach', 'giay-dep']
        },
        {
            key: 'weekend-resort',
            eyebrow: 'Collection 04',
            title: 'Weekend Resort',
            subtitle: 'Du Hành Cuối Tuần',
            story: 'Dành cho chuyển động tự nhiên: culottes rộng, áo mềm, sandal cao gót nhẹ và túi mini. Một bộ sưu tập mang tinh thần tự do nhưng vẫn sang.',
            note: 'Light packing, effortless layering, soft neutrals',
            image: 'https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=1200&q=80',
            mobileHeight: '24rem',
            desktopHeight: '30rem',
            categorySlugs: ['quan-nu', 'ao-nu', 'giay-dep', 'tui-xach']
        }
    ];

    function flattenCategories(categories) {
        return (categories || []).flatMap(category => [
            category,
            ...flattenCategories(category.subCategories || [])
        ]);
    }

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

    document.addEventListener('DOMContentLoaded', function () {
        const mountPoint = document.getElementById('collectionsPageApp');
        if (!mountPoint || typeof Vue === 'undefined') {
            return;
        }

        Vue.createApp({
            data() {
                return {
                    collections: CURATED_COLLECTIONS,
                    allProducts: [],
                    categoryMap: {},
                    parentChildMap: {},
                    activeCollectionKey: CURATED_COLLECTIONS[0].key,
                    activeProducts: [],
                    loading: true
                };
            },
            computed: {
                activeCollection() {
                    return this.collections.find(collection => collection.key === this.activeCollectionKey) || this.collections[0];
                }
            },
            mounted() {
                const params = new URLSearchParams(window.location.search);
                const requestedCollection = params.get('collection');
                if (requestedCollection && this.collections.some(collection => collection.key === requestedCollection)) {
                    this.activeCollectionKey = requestedCollection;
                }
                this.loadCatalog();
            },
            methods: {
                async loadCatalog() {
                    this.loading = true;
                    try {
                        const [categoryResponse, productResponse] = await Promise.all([
                            axios.get('/api/products/categories'),
                            axios.get('/api/products', { params: { size: 100, page: 0, sortBy: 'newest' } })
                        ]);

                        const allCategories = flattenCategories(categoryResponse.data.data || []);

                        // Map slug → categoryId including parent → all child IDs
                        this.categoryMap = allCategories.reduce((map, category) => {
                            map[category.slug] = category.categoryId;
                            return map;
                        }, {});

                        // Build parent → child IDs map for fallback matching
                        this.parentChildMap = (categoryResponse.data.data || []).reduce((map, parent) => {
                            map[parent.categoryId] = (parent.subCategories || []).map(sub => sub.categoryId);
                            return map;
                        }, {});

                        this.allProducts = (productResponse.data.data.content || []).map(product => ({
                            ...product,
                            nameParts: splitProductName(product),
                            displayImageUrl: resolveImage(product)
                        }));

                        this.selectCollection(this.activeCollection);
                    } catch (error) {
                        console.error('Failed to load collection catalog', error);
                    } finally {
                        this.loading = false;
                    }
                },
                collectionProducts(collection) {
                    const categoryIds = [];
                    (collection.categorySlugs || []).forEach(slug => {
                        const id = this.categoryMap[slug];
                        if (!id) return;
                        categoryIds.push(id);
                        // If this is a parent, also include its children
                        const children = this.parentChildMap?.[id] || [];
                        children.forEach(childId => categoryIds.push(childId));
                    });

                    const uniqueIds = [...new Set(categoryIds)];
                    return this.allProducts.filter(product => uniqueIds.includes(product.categoryId));
                },
                selectCollection(collection) {
                    this.activeCollectionKey = collection.key;
                    this.activeProducts = this.collectionProducts(collection);
                    const nextUrl = new URL(window.location.href);
                    nextUrl.searchParams.set('collection', collection.key);
                    window.history.replaceState({}, '', nextUrl);
                },
                productHref(product) {
                    return '/products/' + product.productId;
                },
                formatPrice(value) {
                    return formatPrice(value);
                },
                cardHeight(collection, index) {
                    if (window.innerWidth >= 1024) {
                        return { minHeight: collection.desktopHeight, height: collection.desktopHeight };
                    }
                    if (window.innerWidth >= 768 && index % 2 === 0) {
                        return { minHeight: collection.desktopHeight, height: collection.desktopHeight };
                    }
                    return { minHeight: collection.mobileHeight, height: collection.mobileHeight };
                }
            }
        }).mount('#collectionsPageApp');
    });
})();