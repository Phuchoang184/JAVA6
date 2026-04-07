/* =========================================================
   cart.js – VueJS 3 + Axios Shopping Cart Module
   Global mini-cart that slides from the right
   Requires: api.js (LeikaAPI + AuthManager)
   ========================================================= */

const { createApp, ref, computed, onMounted, watch } = Vue;

const CartApp = createApp({
  setup() {
    // ── State ──────────────────────────────────────────────
    const cart = ref({ items: [], totalItems: 0, subtotal: 0, shippingFee: 0, total: 0 });
    const isOpen = ref(false);
    const isLoading = ref(false);
    const notification = ref({ show: false, message: '', type: 'success' });
    const updatingItems = ref(new Set());
    const checkoutLoading = ref(false);

    // ── API (use global LeikaAPI with JWT interceptor) ────
    const api = window.LeikaAPI;

    // ── Helpers: normalize backend CartDto → local shape ──
    function mapCart(dto) {
      if (!dto) return { items: [], totalItems: 0, subtotal: 0, shippingFee: 0, total: 0 };
      return {
        items: (dto.items || []).map(i => ({
          id: i.cartItemId,
          variantId: i.variantId,
          productName: i.productName,
          productImage: i.thumbnailUrl,
          variant: i.variantInfo || '',
          price: i.unitPrice,
          quantity: i.quantity,
          subtotal: i.lineTotal,
          stock: i.stockQty
        })),
        totalItems: dto.totalItems || 0,
        subtotal: dto.subTotal || 0,
        shippingFee: dto.shippingFee || 0,
        total: dto.totalAmount || 0
      };
    }

    // ── Computed ──────────────────────────────────────────
    const cartCount = computed(() => cart.value.totalItems || 0);
    const isEmpty = computed(() => !cart.value.items || cart.value.items.length === 0);
    const freeShippingProgress = computed(() => {
      const threshold = 500000;
      const pct = Math.min((cart.value.subtotal / threshold) * 100, 100);
      return pct;
    });
    const freeShippingRemaining = computed(() => {
      const threshold = 500000;
      const rem = threshold - (cart.value.subtotal || 0);
      return rem > 0 ? rem : 0;
    });

    // ── Auth guard helper ─────────────────────────────────
    function requireAuth(action) {
      if (!window.AuthManager || !window.AuthManager.isAuthenticated()) {
        showNotification('Vui lòng đăng nhập để ' + action, 'error');
        setTimeout(() => { window.location.href = '/login'; }, 1500);
        return false;
      }
      return true;
    }

    // ── API Calls ─────────────────────────────────────────
    async function fetchCart() {
      if (!window.AuthManager || !window.AuthManager.isAuthenticated()) return;
      try {
        isLoading.value = true;
        const { data: resp } = await api.get('/cart');
        cart.value = mapCart(resp.data);
      } catch (err) {
        if (err.response?.status !== 401) {
          showNotification('Không thể tải giỏ hàng', 'error');
        }
      } finally {
        isLoading.value = false;
      }
    }

    async function addToCart(variantId, quantity = 1) {
      if (!requireAuth('thêm vào giỏ hàng')) return false;
      try {
        isLoading.value = true;
        const { data: resp } = await api.post('/cart', { variantId, quantity });
        if (resp.success) {
          cart.value = mapCart(resp.data);
          openCart();
          showNotification(resp.message || 'Đã thêm vào giỏ hàng!', 'success');
          return true;
        } else {
          showNotification(resp.message || 'Có lỗi xảy ra', 'error');
          return false;
        }
      } catch (err) {
        const msg = err.response?.data?.message || 'Có lỗi xảy ra khi thêm giỏ hàng';
        showNotification(msg, 'error');
        return false;
      } finally {
        isLoading.value = false;
      }
    }

    async function updateQuantity(itemId, quantity) {
      if (quantity < 1) { removeItem(itemId); return; }
      updatingItems.value.add(itemId);
      try {
        const { data: resp } = await api.put(`/cart/${itemId}`, null, { params: { quantity } });
        if (resp.success) {
          cart.value = mapCart(resp.data);
        } else {
          showNotification(resp.message || 'Có lỗi xảy ra', 'error');
        }
      } catch (err) {
        showNotification('Không thể cập nhật số lượng', 'error');
      } finally {
        updatingItems.value.delete(itemId);
      }
    }

    async function removeItem(itemId) {
      updatingItems.value.add(itemId);
      try {
        const { data: resp } = await api.delete(`/cart/${itemId}`);
        if (resp.success) {
          cart.value = mapCart(resp.data);
          showNotification('Đã xóa sản phẩm', 'info');
        }
      } catch (err) {
        showNotification('Không thể xóa sản phẩm', 'error');
      } finally {
        updatingItems.value.delete(itemId);
      }
    }

    async function clearCart() {
      if (!confirm('Bạn có chắc muốn xóa toàn bộ giỏ hàng?')) return;
      try {
        await api.delete('/cart');
        cart.value = mapCart(null);
        showNotification('Đã làm trống giỏ hàng', 'info');
      } catch (err) {
        showNotification('Có lỗi xảy ra', 'error');
      }
    }

    /** Place order → POST /api/orders */
    async function checkout(orderData) {
      if (!requireAuth('đặt hàng')) return null;
      try {
        checkoutLoading.value = true;
        const { data: resp } = await api.post('/orders', orderData);
        if (resp.success) {
          cart.value = mapCart(null); // clear local cart
          showNotification(resp.message || 'Đặt hàng thành công!', 'success');
          return resp.data; // OrderDto
        } else {
          showNotification(resp.message || 'Đặt hàng thất bại', 'error');
          return null;
        }
      } catch (err) {
        const msg = err.response?.data?.message || 'Không thể đặt hàng. Vui lòng thử lại.';
        showNotification(msg, 'error');
        return null;
      } finally {
        checkoutLoading.value = false;
      }
    }

    /** Get order history → GET /api/orders */
    async function getOrders() {
      const { data: resp } = await api.get('/orders');
      return resp.data || [];
    }

    // ── UI Helpers ────────────────────────────────────────
    function openCart() { isOpen.value = true; document.body.style.overflow = 'hidden'; }
    function closeCart() { isOpen.value = false; document.body.style.overflow = ''; }
    function toggleCart() { isOpen.value ? closeCart() : openCart(); }

    function isUpdating(id) { return updatingItems.value.has(id); }

    function showNotification(message, type = 'success') {
      notification.value = { show: true, message, type };
      setTimeout(() => { notification.value.show = false; }, 3000);
    }

    function formatPrice(value) {
      if (!value) return '0 ₫';
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
    }

    function getImageUrl(url) {
      if (!url) return '/images/placeholder.jpg';
      return url.startsWith('http') ? url : '/images/products/' + url;
    }

    // Keyboard close
    function handleKeydown(e) { if (e.key === 'Escape') closeCart(); }

    // ── Lifecycle ─────────────────────────────────────────
    onMounted(() => {
      fetchCart();
      document.addEventListener('keydown', handleKeydown);

      // Re-fetch cart when user logs in/out
      window.addEventListener('auth:changed', () => {
        if (window.AuthManager && window.AuthManager.isAuthenticated()) {
          fetchCart();
        } else {
          cart.value = mapCart(null);
        }
      });

      // Global bridge for product pages, checkout, etc.
      window.cartApp = {
        addToCart,
        openCart,
        closeCart,
        toggleCart,
        fetchCart,
        checkout,
        getOrders,
        getCartData: () => cart.value
      };
    });

    return {
      cart, isOpen, isLoading, checkoutLoading, notification, cartCount,
      isEmpty, freeShippingProgress, freeShippingRemaining,
      addToCart, updateQuantity, removeItem, clearCart,
      checkout, getOrders,
      openCart, closeCart, toggleCart, isUpdating,
      formatPrice, getImageUrl
    };
  },

  template: `
    <!-- ═══ CART ICON (Trigger) ═══ -->
    <button
      id="cart-toggle-btn"
      @click="toggleCart"
      class="cart-toggle-btn"
      :class="{ 'has-items': cartCount > 0 }"
      aria-label="Giỏ hàng"
    >
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
        <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/>
        <line x1="3" y1="6" x2="21" y2="6"/>
        <path d="M16 10a4 4 0 01-8 0"/>
      </svg>
      <span v-if="cartCount > 0" class="cart-badge" key="badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
    </button>

    <!-- ═══ OVERLAY ═══ -->
    <transition name="fade">
      <div v-if="isOpen" class="cart-overlay" @click="closeCart" aria-hidden="true"></div>
    </transition>

    <!-- ═══ MINI CART DRAWER ═══ -->
    <transition name="slide-right">
      <aside v-if="isOpen" class="mini-cart" role="dialog" aria-label="Giỏ hàng" aria-modal="true">

        <!-- Header -->
        <div class="mini-cart__header">
          <div class="mini-cart__title">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/>
              <line x1="3" y1="6" x2="21" y2="6"/>
              <path d="M16 10a4 4 0 01-8 0"/>
            </svg>
            <span>Giỏ hàng</span>
            <span v-if="cartCount > 0" class="header-count">({{ cartCount }} sản phẩm)</span>
          </div>
          <button @click="closeCart" class="mini-cart__close" aria-label="Đóng giỏ hàng">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <!-- Free Shipping Bar -->
        <div v-if="!isEmpty" class="shipping-bar">
          <div v-if="freeShippingRemaining > 0" class="shipping-bar__text">
            Mua thêm <strong>{{ formatPrice(freeShippingRemaining) }}</strong> để được <span class="free-tag">MIỄN PHÍ VẬN CHUYỂN</span>
          </div>
          <div v-else class="shipping-bar__text success">
            🎉 Bạn được <strong>MIỄN PHÍ VẬN CHUYỂN</strong>!
          </div>
          <div class="shipping-bar__track">
            <div class="shipping-bar__fill" :style="{ width: freeShippingProgress + '%' }"></div>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="isLoading && isEmpty" class="mini-cart__loading">
          <div class="spinner"></div>
          <span>Đang tải...</span>
        </div>

        <!-- Empty State -->
        <div v-else-if="isEmpty" class="mini-cart__empty">
          <div class="empty-icon">
            <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
              <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/>
              <line x1="3" y1="6" x2="21" y2="6"/>
              <path d="M16 10a4 4 0 01-8 0"/>
            </svg>
          </div>
          <h3>Giỏ hàng trống</h3>
          <p>Hãy khám phá các sản phẩm thời trang mới nhất!</p>
          <a href="/" @click="closeCart" class="btn-shop-now">Mua ngay</a>
        </div>

        <!-- Cart Items -->
        <div v-else class="mini-cart__body">
          <transition-group name="list" tag="ul" class="cart-items-list">
            <li
              v-for="item in cart.items"
              :key="item.id"
              class="cart-item"
              :class="{ 'is-updating': isUpdating(item.id) }"
            >
              <!-- Product Image -->
              <div class="cart-item__img-wrap">
                <img :src="getImageUrl(item.productImage)" :alt="item.productName" class="cart-item__img" loading="lazy" />
                <div v-if="isUpdating(item.id)" class="item-overlay">
                  <div class="spinner spinner--sm"></div>
                </div>
              </div>

              <!-- Product Info -->
              <div class="cart-item__info">
                <h4 class="cart-item__name">{{ item.productName }}</h4>
                <div class="cart-item__meta">
                  <span v-if="item.size && item.size !== ''" class="meta-tag">Size: {{ item.size }}</span>
                  <span v-if="item.color && item.color !== ''" class="meta-tag">{{ item.color }}</span>
                  <span v-if="item.brand" class="meta-tag brand">{{ item.brand }}</span>
                </div>
                <div class="cart-item__price-row">
                  <span class="cart-item__unit-price">{{ formatPrice(item.price) }}</span>
                </div>

                <!-- Quantity Controls -->
                <div class="cart-item__controls">
                  <div class="qty-control">
                    <button
                      @click="updateQuantity(item.id, item.quantity - 1)"
                      :disabled="isUpdating(item.id)"
                      class="qty-btn"
                      :class="{ 'qty-btn--danger': item.quantity === 1 }"
                      :aria-label="item.quantity === 1 ? 'Xóa sản phẩm' : 'Giảm số lượng'"
                    >
                      <svg v-if="item.quantity > 1" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                        <line x1="5" y1="12" x2="19" y2="12"/>
                      </svg>
                      <svg v-else width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
                      </svg>
                    </button>
                    <span class="qty-value">{{ item.quantity }}</span>
                    <button
                      @click="updateQuantity(item.id, item.quantity + 1)"
                      :disabled="isUpdating(item.id) || item.quantity >= item.stock"
                      class="qty-btn"
                      aria-label="Tăng số lượng"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                        <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
                      </svg>
                    </button>
                  </div>
                  <span class="cart-item__subtotal">{{ formatPrice(item.subtotal) }}</span>
                  <button
                    @click="removeItem(item.id)"
                    :disabled="isUpdating(item.id)"
                    class="remove-btn"
                    aria-label="Xóa sản phẩm"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <polyline points="3 6 5 6 21 6"></polyline>
                      <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a1 1 0 011-1h4a1 1 0 011 1v2"></path>
                    </svg>
                  </button>
                </div>
              </div>
            </li>
          </transition-group>
        </div>

        <!-- Footer: Summary + Actions -->
        <div v-if="!isEmpty" class="mini-cart__footer">
          <div class="price-summary">
            <div class="price-row">
              <span>Tạm tính</span>
              <span>{{ formatPrice(cart.subtotal) }}</span>
            </div>
            <div class="price-row">
              <span>Vận chuyển</span>
              <span :class="{ 'free-shipping': cart.shippingFee === 0 }">
                {{ cart.shippingFee === 0 ? 'Miễn phí' : formatPrice(cart.shippingFee) }}
              </span>
            </div>
            <div class="price-row price-row--total">
              <span>Tổng cộng</span>
              <span class="total-amount">{{ formatPrice(cart.total) }}</span>
            </div>
          </div>

          <div class="mini-cart__actions">
            <a href="/cart" @click="closeCart" class="btn-view-cart">Xem giỏ hàng</a>
            <a href="/checkout" class="btn-checkout">Thanh toán ngay →</a>
          </div>

          <button @click="clearCart" class="btn-clear-cart">Xóa toàn bộ giỏ hàng</button>
        </div>

      </aside>
    </transition>

    <!-- ═══ TOAST NOTIFICATION ═══ -->
    <transition name="toast-slide">
      <div
        v-if="notification.show"
        class="toast-notification"
        :class="'toast-' + notification.type"
      >
        <svg v-if="notification.type === 'success'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        <svg v-else-if="notification.type === 'error'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
        <span>{{ notification.message }}</span>
      </div>
    </transition>
  `
});

CartApp.mount('#cart-app');
