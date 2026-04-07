/**
 * Register page Vue app – mounts on #registerApp
 * Loaded globally but only activates when the element exists.
 */
document.addEventListener('DOMContentLoaded', function () {
    if (typeof Vue === 'undefined' || !document.getElementById('registerApp')) return;

    Vue.createApp({
        data() {
            return {
                fullName: '',
                email: '',
                phone: '',
                password: '',
                confirmPassword: '',
                errorMsg: '',
                successMsg: '',
                loading: false
            };
        },
        methods: {
            async handleRegister() {
                this.errorMsg = '';
                this.successMsg = '';

                if (this.password !== this.confirmPassword) {
                    this.errorMsg = 'Mật khẩu xác nhận không khớp';
                    return;
                }

                this.loading = true;
                try {
                    await window.AuthManager.register({
                        fullName: this.fullName,
                        email: this.email,
                        phone: this.phone,
                        password: this.password
                    });
                    this.successMsg = 'Đăng ký thành công! Đang chuyển hướng...';
                    setTimeout(() => { window.location.href = '/'; }, 1500);
                } catch (err) {
                    this.errorMsg = err.response?.data?.message || 'Đã có lỗi xảy ra, vui lòng thử lại';
                } finally {
                    this.loading = false;
                }
            }
        }
    }).mount('#registerApp');
});
