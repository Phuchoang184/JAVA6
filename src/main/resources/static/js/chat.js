/* =========================================================
   chat.js – LEIKA AI Chatbox Vue 3 Application
   ========================================================= */
(function () {
    'use strict';

    // ── Storage Keys ───────────────────────────────────────
    const HISTORY_KEY     = 'leika_chat_history';
    const PREFS_KEY       = 'leika_chat_prefs';
    const UNREAD_KEY      = 'leika_chat_unread';
    const SESSION_KEY     = 'leika_session_id';
    const CONV_KEY        = 'leika_conv_id';
    const MAX_HISTORY     = 30;
    const MAX_CONTEXT_MSG = 10; // messages sent to backend as context

    // ── UUID helper ─────────────────────────────────────────
    function uuidv4() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            return (c === 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
    }

    // ── Helpers ─────────────────────────────────────────────
    function formatPrice(value) {
        if (value == null) return '';
        return new Intl.NumberFormat('vi-VN').format(value) + 'đ';
    }

    function nowTime() {
        return new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    }

    // Tiny Web-Audio notification sound (no external file needed)
    function playChime() {
        try {
            const ctx = new (window.AudioContext || window.webkitAudioContext)();
            const osc  = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.type = 'sine';
            osc.frequency.setValueAtTime(1047, ctx.currentTime);      // C6
            osc.frequency.setValueAtTime(1319, ctx.currentTime + 0.1); // E6
            gain.gain.setValueAtTime(0.12, ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.5);
            osc.start(ctx.currentTime);
            osc.stop(ctx.currentTime + 0.5);
        } catch (_) { /* ignore if AudioContext blocked */ }
    }

    // ── Vue App ──────────────────────────────────────────────
    const ChatApp = Vue.createApp({

        data() {
            return {
                isOpen:     false,
                isTyping:   false,
                inputText:  '',
                messages:   [],
                unreadCount: 0,

                // Session & conversation IDs for server-side memory
                sessionId:      null,
                conversationId: null,

                // Quick-reply chips (refreshed per message)
                chips: [
                    { icon: '✨', text: 'Gợi ý outfit đi tiệc' },
                    { icon: '🔥', text: 'Sản phẩm bán chạy' },
                    { icon: '🏷️', text: 'Khuyến mãi hôm nay' },
                    { icon: '↩️', text: 'Chính sách đổi trả' },
                ],

                // Remembered user preferences (style, budget, color)
                preferences: {},

                // Detected product context from the current page
                currentProductId: null,

                soundEnabled: true,
            };
        },

        computed: {
            hasMessages() { return this.messages.length > 0; },
            badgeVisible() { return !this.isOpen && this.unreadCount > 0; },
            sendDisabled()  { return this.isTyping || !this.inputText.trim(); },
        },

        mounted() {
            this.loadState();
            this.detectPageContext();
            this.initSession();

            // Show welcome bubble after a short delay if no history
            if (this.messages.length === 0) {
                setTimeout(() => this.pushBotMsg(
                    'Xin chào! Tôi là trợ lý thời trang LEIKA 💎\n' +
                    'Tôi có thể giúp bạn tìm trang phục phù hợp, tư vấn style hoặc giải đáp mọi thắc mắc. Bạn cần hỗ trợ gì ạ?',
                    null,
                    [
                        { icon: '✨', text: 'Gợi ý outfit' },
                        { icon: '🏷️', text: 'Sản phẩm bán chạy' },
                        { icon: '❔', text: 'Chính sách đổi trả' },
                    ]
                ), 800);
            }

            // Update badge button class when open state changes
            this.$watch('isOpen', (val) => {
                const btn = document.getElementById('chat-toggle');
                if (btn) btn.classList.toggle('is-open', val);
                if (val) {
                    this.unreadCount = 0;
                    this.saveState();
                    this.$nextTick(() => {
                        this.scrollToBottom(true);
                        this.$refs.chatInput && this.$refs.chatInput.focus();
                    });
                }
            });
        },

        methods: {

            // ── Open / Close ─────────────────────────────────
            toggle() {
                this.isOpen = !this.isOpen;
            },

            // ── Send message (from input or chip) ────────────
            async sendMessage(textOverride) {
                const text = (textOverride ?? this.inputText).trim();
                if (!text || this.isTyping) return;

                this.inputText = '';

                // Push user bubble
                this.messages.push({
                    id:     Date.now(),
                    isUser: true,
                    text,
                    time:   nowTime(),
                });
                this.saveState();
                this.$nextTick(() => this.scrollToBottom());

                // Extract any preference hints from message
                this.detectPreferences(text);

                this.isTyping = true;
                this.$nextTick(() => this.scrollToBottom());

                try {
                    // Build history context only when server memory is NOT available
                    const historyContext = this.conversationId ? [] : this.messages
                        .slice(-MAX_CONTEXT_MSG - 1, -1)
                        .map(m => ({
                            role:    m.isUser ? 'user' : 'assistant',
                            content: m.text,
                        }));

                    const response = await window.api.post('/chat', {
                        message:          text,
                        sessionId:        this.sessionId,
                        conversationId:   this.conversationId,
                        history:          historyContext,
                        currentProductId: this.currentProductId,
                        preferences:      JSON.stringify(this.preferences),
                    });

                    const data = response.data.data || response.data;
                    this.isTyping = false;

                    // Push bot reply
                    this.pushBotMsg(data.message, data.products, this.normaliseChips(data.suggestions));

                    if (this.soundEnabled) playChime();
                    if (!this.isOpen) {
                        this.unreadCount++;
                        this.saveState();
                    }

                } catch (err) {
                    this.isTyping = false;
                    this.pushBotMsg(
                        'Xin lỗi, tôi đang bận một chút 🙏 Bạn thử lại sau nhé!',
                        null,
                        [{ icon: '🔄', text: 'Thử lại' }, { icon: '📞', text: 'Liên hệ hỗ trợ' }]
                    );
                }
            },

            // ── Push a bot message ────────────────────────────
            pushBotMsg(text, products, chips) {
                this.messages.push({
                    id:       Date.now(),
                    isUser:   false,
                    text,
                    products: products && products.length ? products : null,
                    chips:    chips || [],
                    time:     nowTime(),
                });

                if (chips && chips.length) this.chips = chips;

                // Trim stored history
                if (this.messages.length > MAX_HISTORY) {
                    this.messages.splice(0, this.messages.length - MAX_HISTORY);
                }

                this.saveState();
                this.$nextTick(() => this.scrollToBottom());
            },

            // ── Handle chip click ─────────────────────────────
            onChip(chip) {
                this.sendMessage(chip.text);
            },

            // ── Keyboard: Enter sends, Shift+Enter = newline ──
            onKeydown(e) {
                if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    this.sendMessage();
                }
            },

            // ── Auto-resize textarea ──────────────────────────
            onInput() {
                const el = this.$refs.chatInput;
                if (!el) return;
                el.style.height = 'auto';
                el.style.height = Math.min(el.scrollHeight, 110) + 'px';
            },

            // ── Scroll chat to bottom ─────────────────────────
            scrollToBottom(instant) {
                const el = this.$refs.msgList;
                if (!el) return;
                if (instant) {
                    el.scrollTop = el.scrollHeight;
                } else {
                    el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' });
                }
            },

            // ── Clear chat history ────────────────────────────
            clearHistory() {
                if (!confirm('Xóa toàn bộ lịch sử trò chuyện?')) return;
                // Clear server-side memory
                if (this.conversationId) {
                    window.api.delete('/chat/history/' + this.conversationId).catch(() => {});
                }
                this.messages = [];
                this.preferences = {};
                // Generate a fresh conversation ID
                this.conversationId = uuidv4();
                localStorage.setItem(CONV_KEY, this.conversationId);
                localStorage.removeItem(HISTORY_KEY);
                localStorage.removeItem(PREFS_KEY);
                // Re-show welcome
                this.pushBotMsg(
                    'Lịch sử trò chuyện đã được xóa! Tôi có thể giúp gì cho bạn? 😊',
                    null,
                    [
                        { icon: '✨', text: 'Gợi ý outfit' },
                        { icon: '🔥', text: 'Sản phẩm bán chạy' },
                    ]
                );
            },

            // ── Toggle sound ──────────────────────────────────
            toggleSound() {
                this.soundEnabled = !this.soundEnabled;
            },

            // ── Detect product page context ───────────────────
            detectPageContext() {
                // Convention: product detail pages expose window.currentProductId
                this.currentProductId = window.currentProductId || null;
            },

            // ── Initialise persistent session & conversation IDs ──
            initSession() {
                // Session ID: persists across page loads (browser session until cleared)
                let sid = localStorage.getItem(SESSION_KEY);
                if (!sid) {
                    sid = uuidv4();
                    localStorage.setItem(SESSION_KEY, sid);
                }
                this.sessionId = sid;

                // Conversation ID: scoped to one chat conversation
                let cid = localStorage.getItem(CONV_KEY);
                if (!cid) {
                    cid = uuidv4();
                    localStorage.setItem(CONV_KEY, cid);
                }
                this.conversationId = cid;
            },

            // ── Fire-and-forget behavior tracking ────────────
            trackBehavior(productId, actionType) {
                if (!productId || !this.sessionId) return;
                window.api.post('/track-behavior', {
                    sessionId:  this.sessionId,
                    productId:  productId,
                    actionType: actionType || 'VIEW',
                }).catch(() => { /* silent fail – tracking is best-effort */ });
            },

            // ── Naively extract preferences from text ─────────
            detectPreferences(text) {
                const lower = text.toLowerCase();
                // Budget hints
                const budgetMatch = lower.match(/(\d{2,3})\s*k|(\d{3,})\s*nghìn|(\d{1,3})\s*triệu/);
                if (budgetMatch) {
                    let budget = 0;
                    if (budgetMatch[1]) budget = parseInt(budgetMatch[1]) * 1000;
                    else if (budgetMatch[2]) budget = parseInt(budgetMatch[2]) * 1000;
                    else if (budgetMatch[3]) budget = parseInt(budgetMatch[3]) * 1000000;
                    if (budget > 0) this.preferences.budget = budget;
                }
                // Color hints
                const colors = ['đen','trắng','đỏ','xanh','vàng','hồng','nâu','be','nude','xám','tím','cam'];
                colors.forEach(c => { if (lower.includes(c)) this.preferences.color = c; });
                // Style hints
                const styles = ['công sở','tiệc','dạo phố','thể thao','casual','tối giản','sexy','dễ thương','vintage','thanh lịch'];
                styles.forEach(s => { if (lower.includes(s)) this.preferences.style = s; });

                if (Object.keys(this.preferences).length) {
                    localStorage.setItem(PREFS_KEY, JSON.stringify(this.preferences));
                }
            },

            // ── Persist state to localStorage ─────────────────
            saveState() {
                try {
                    const toSave = this.messages.slice(-MAX_HISTORY);
                    localStorage.setItem(HISTORY_KEY, JSON.stringify(toSave));
                    localStorage.setItem(UNREAD_KEY, String(this.unreadCount));
                } catch (_) { /* quota exceeded – silent */ }
            },

            // ── Restore state from localStorage ───────────────
            loadState() {
                try {
                    const h = localStorage.getItem(HISTORY_KEY);
                    if (h) this.messages = JSON.parse(h);
                    const p = localStorage.getItem(PREFS_KEY);
                    if (p) this.preferences = JSON.parse(p);
                    this.unreadCount = parseInt(localStorage.getItem(UNREAD_KEY) || '0');
                } catch (_) {
                    this.messages = [];
                }
            },

            // ── Normalise backend suggestions → chip objects ───
            normaliseChips(suggestions) {
                if (!suggestions || !suggestions.length) return [];
                const icons = ['💬', '👗', '✨', '🛍️', '❤️', '🔍'];
                return suggestions.slice(0, 4).map((text, i) => ({
                    icon: icons[i % icons.length],
                    text: typeof text === 'string' ? text : text.text || '',
                }));
            },

            // ── Formatting helpers ────────────────────────────
            formatPrice,
        },
    });

    // ── Mount ─────────────────────────────────────────────────
    const mountEl = document.getElementById('leika-chat-app');
    if (mountEl) {
        const instance = ChatApp.mount('#leika-chat-app');
        window.leikaChat = instance;
    }

})();
