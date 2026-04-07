/**
 * Login page Vue app – mounts on #loginApp
 * Loaded globally but only activates when the element exists.
 */
document.addEventListener('DOMContentLoaded', function () {
    if (typeof Vue === 'undefined' || !document.getElementById('loginApp')) return;

    Vue.createApp({
        data() {
            return {
                email: '',
                password: '',
                errorMsg: '',
                loading: false
            };
        },
        methods: {
            async handleLogin() {
                this.loading = true;
                this.errorMsg = '';
                try {
                    const data = await window.AuthManager.login(this.email, this.password);
                    if (data.role === 'ADMIN') {
                        window.location.href = '/admin';
                    } else {
                        window.location.href = '/';
                    }
                } catch (err) {
                    this.errorMsg = err.response?.data?.message || 'Email hoặc mật khẩu không đúng';
                } finally {
                    this.loading = false;
                }
            }
        }
    }).mount('#loginApp');
});
