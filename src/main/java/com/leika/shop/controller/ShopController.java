package com.leika.shop.controller;

import com.leika.shop.dto.ProductDto;
import com.leika.shop.entity.Product;
import com.leika.shop.repository.ProductRepository;
import com.leika.shop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @GetMapping("/")
    public String home(Model model) {
        List<ProductDto> featured = productService.getFeaturedProducts();
        List<ProductDto> newArrivals = productService.getNewArrivals(8);
        model.addAttribute("products", newArrivals);
        model.addAttribute("featuredProducts", featured);
        model.addAttribute("pageTitle", "Trang chủ - LEIKA LUXURY");
        return "index";
    }

    @GetMapping("/shop")
    public String shop(Model model) {
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands", productService.getAllBrands());
        model.addAttribute("pageTitle", "Sản phẩm - LEIKA LUXURY");
        return "shop";
    }

    @GetMapping("/collections")
    public String collections(Model model) {
        model.addAttribute("pageTitle", "Bộ sưu tập - LEIKA LUXURY");
        return "collections";
    }

    @GetMapping("/sale")
    public String sale(Model model) {
        model.addAttribute("pageTitle", "Khuyến mãi - LEIKA LUXURY");
        return "sale";
    }

    @GetMapping("/new-arrivals")
    public String newArrivals(Model model) {
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands", productService.getAllBrands());
        model.addAttribute("pageTitle", "Hàng mới về - LEIKA LUXURY");
        model.addAttribute("initialSort", "newest");
        model.addAttribute("shopHeroEyebrow", "New Arrivals");
        model.addAttribute("shopHeroTitle", "Thiết kế mới nhất vừa cập bến boutique LEIKA.");
        model.addAttribute("shopHeroDescription", "Khám phá những thiết kế vừa lên kệ với tinh thần tối giản, đường cắt chuẩn mực và bảng màu thanh lịch dành cho tủ đồ cao cấp.");
        return "shop";
    }

    @GetMapping("/category/{segment}")
    public String categoryLanding(@PathVariable String segment, Model model) {
        Map<String, String> categorySlugMap = Map.of(
                "nu", "thoi-trang-nu",
                "nam", "thoi-trang-nam"
        );

        Map<String, String> categoryTitleMap = Map.of(
                "nu", "Thời trang nữ - LEIKA LUXURY",
                "nam", "Thời trang nam - LEIKA LUXURY"
        );

        Map<String, String> categoryHeroMap = Map.of(
                "nu", "Tuyển chọn dành cho quý cô hiện đại, ưu tiên chất liệu mềm rủ và phom dáng tinh gọn.",
                "nam", "Tinh thần menswear cao cấp với phom dáng chỉn chu, hiện đại và giàu tính ứng dụng."
        );

        String normalizedSegment = segment == null ? "" : segment.toLowerCase();
        String categorySlug = categorySlugMap.get(normalizedSegment);
        if (categorySlug == null) {
            return "redirect:/shop";
        }

        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("brands", productService.getAllBrands());
        model.addAttribute("pageTitle", categoryTitleMap.get(normalizedSegment));
        model.addAttribute("initialCategorySlug", categorySlug);
        model.addAttribute("shopHeroEyebrow", normalizedSegment.equals("nu") ? "Women Edit" : "Men Edit");
        model.addAttribute("shopHeroTitle", normalizedSegment.equals("nu") ? "Chạm vào tinh thần thanh lịch của LEIKA cho phái đẹp." : "Nhịp điệu lịch lãm dành cho quý ông theo đuổi sự tối giản." );
        model.addAttribute("shopHeroDescription", categoryHeroMap.get(normalizedSegment));
        return "shop";
    }

    @GetMapping("/policy/{type}")
    public String policyPage(@PathVariable String type, Model model) {
        Map<String, Object> page = buildPolicyPage(type);
        if (page == null) {
            return "redirect:/";
        }

        model.addAttribute("pageTitle", page.get("pageTitle"));
        model.addAttribute("policyLabel", page.get("label"));
        model.addAttribute("policyTitle", page.get("title"));
        model.addAttribute("policyLead", page.get("lead"));
        model.addAttribute("policyUpdatedAt", "Cập nhật lần cuối: 02.04.2026");
        model.addAttribute("policySections", page.get("sections"));
        model.addAttribute("activePolicyPath", page.get("path"));
        model.addAttribute("policyNavigation", buildPolicyNavigation());
        return "policy";
    }

    @GetMapping("/guide/buying")
    public String buyingGuide(Model model) {
        Map<String, Object> page = buildBuyingGuidePage();
        model.addAttribute("pageTitle", page.get("pageTitle"));
        model.addAttribute("policyLabel", page.get("label"));
        model.addAttribute("policyTitle", page.get("title"));
        model.addAttribute("policyLead", page.get("lead"));
        model.addAttribute("policyUpdatedAt", "Cập nhật lần cuối: 02.04.2026");
        model.addAttribute("policySections", page.get("sections"));
        model.addAttribute("activePolicyPath", page.get("path"));
        model.addAttribute("policyNavigation", buildPolicyNavigation());
        return "policy";
    }

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        ProductDto product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("pageTitle", product.getProductName() + " - LEIKA LUXURY");
        return "product-detail";
    }

    @GetMapping("/cart")
    public String cartPage(Model model) {
        model.addAttribute("pageTitle", "Giỏ hàng - LEIKA LUXURY");
        return "cart";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "Đăng nhập - LEIKA LUXURY");
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("pageTitle", "Đăng ký - LEIKA LUXURY");
        return "register";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model) {
        model.addAttribute("pageTitle", "Thanh toán - LEIKA LUXURY");
        return "checkout";
    }

    @GetMapping("/payment/vnpay")
    public String vnpayMockPage(Model model) {
        model.addAttribute("pageTitle", "Thanh toán VNPAY - LEIKA LUXURY");
        return "payment-vnpay";
    }

    @GetMapping("/profile")
    public String profilePage(Model model) {
        model.addAttribute("pageTitle", "Tài khoản - LEIKA LUXURY");
        return "profile";
    }

    @GetMapping("/orders")
    public String ordersPage(Model model) {
        model.addAttribute("pageTitle", "Đơn hàng - LEIKA LUXURY");
        return "orders";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Admin Dashboard - LEIKA");
        return "admin/dashboard";
    }

        private List<Map<String, String>> buildPolicyNavigation() {
        return List.of(
            navItem("Chính sách đổi trả", "/policy/return"),
            navItem("Chính sách bảo mật", "/policy/privacy"),
            navItem("Điều khoản sử dụng", "/policy/terms"),
            navItem("Hướng dẫn mua hàng", "/guide/buying")
        );
        }

        private Map<String, String> navItem(String label, String href) {
        return Map.of(
            "label", label,
            "href", href
        );
        }

        private Map<String, Object> buildPolicyPage(String type) {
        return switch (type) {
            case "return" -> page(
                "/policy/return",
                "Chính sách đổi trả - LEIKA LUXURY",
                "Return Policy",
                "Chính sách đổi trả",
                "LEIKA thiết kế trải nghiệm hậu mua hàng tương xứng với tiêu chuẩn boutique cao cấp: rõ ràng, lịch thiệp và ưu tiên sự an tâm của khách hàng trong từng đơn hàng.",
                List.of(
                    section("Thời gian tiếp nhận", "LEIKA hỗ trợ tiếp nhận yêu cầu đổi hoặc trả trong vòng 07 ngày kể từ thời điểm đơn hàng được giao thành công. Sản phẩm cần giữ nguyên tình trạng ban đầu, chưa qua sử dụng, còn đầy đủ tem, nhãn, phụ kiện đi kèm và hóa đơn mua hàng điện tử hoặc mã đơn.") ,
                    section("Điều kiện áp dụng", "Chúng tôi ưu tiên xử lý cho các trường hợp sản phẩm lỗi kỹ thuật, giao sai mẫu, sai kích cỡ theo đơn hoặc chưa đáp ứng tiêu chuẩn hoàn thiện đã công bố. Các sản phẩm thuộc nhóm ưu đãi đặc biệt, phụ kiện cá nhân hoặc thiết kế đặt riêng có thể không thuộc phạm vi đổi trả, trừ khi phát sinh lỗi từ LEIKA.") ,
                    section("Quy trình thực hiện", "Khách hàng vui lòng liên hệ bộ phận Chăm sóc khách hàng qua hotline hoặc email trong thời gian quy định, đồng thời cung cấp mã đơn hàng và hình ảnh sản phẩm. Sau khi xác minh, LEIKA sẽ hướng dẫn gửi hoàn và thông báo phương án phù hợp gồm đổi size, đổi sang thiết kế khác hoặc hoàn tiền theo chính sách.") ,
                    section("Hoàn tiền", "Khoản hoàn tiền, nếu phát sinh, sẽ được thực hiện về đúng phương thức thanh toán ban đầu trong vòng 07 đến 14 ngày làm việc kể từ khi LEIKA hoàn tất kiểm định sản phẩm hoàn trả. Trong mọi tình huống, chúng tôi luôn ưu tiên trao đổi minh bạch để đảm bảo trải nghiệm xứng tầm với kỳ vọng của khách hàng.")
                )
            );
            case "privacy" -> page(
                "/policy/privacy",
                "Chính sách bảo mật - LEIKA LUXURY",
                "Privacy Notice",
                "Chính sách bảo mật",
                "Sự tin tưởng là một phần của trải nghiệm xa xỉ. LEIKA cam kết quản lý dữ liệu cá nhân cẩn trọng, đúng mục đích và với tiêu chuẩn bảo mật tương xứng cùng vị thế thương hiệu.",
                List.of(
                    section("Phạm vi dữ liệu thu thập", "LEIKA có thể thu thập các thông tin cần thiết như họ tên, số điện thoại, email, địa chỉ nhận hàng, lịch sử mua sắm và tương tác trên nền tảng nhằm phục vụ vận hành đơn hàng, chăm sóc khách hàng và cá nhân hóa trải nghiệm mua sắm.") ,
                    section("Mục đích sử dụng", "Thông tin của khách hàng được sử dụng để xác nhận giao dịch, xử lý thanh toán, hỗ trợ hậu mãi, cập nhật bộ sưu tập mới hoặc ưu đãi nếu khách hàng đồng ý nhận tin. LEIKA không khai thác dữ liệu ngoài phạm vi cần thiết cho dịch vụ và trải nghiệm thương hiệu.") ,
                    section("Bảo mật và lưu trữ", "Dữ liệu được lưu trữ trên hạ tầng có kiểm soát truy cập và các lớp bảo vệ phù hợp với hệ thống thương mại điện tử. Chỉ những bộ phận có thẩm quyền mới được phép tiếp cận thông tin cần thiết để hoàn thành nghiệp vụ liên quan.") ,
                    section("Quyền của khách hàng", "Khách hàng có thể yêu cầu rà soát, cập nhật hoặc ngừng sử dụng dữ liệu cho mục đích tiếp thị bất kỳ lúc nào bằng cách liên hệ LEIKA. Chúng tôi tôn trọng quyền riêng tư và luôn phản hồi các yêu cầu hợp lệ trong thời gian hợp lý.")
                )
            );
            case "terms" -> page(
                "/policy/terms",
                "Điều khoản sử dụng - LEIKA LUXURY",
                "Terms of Service",
                "Điều khoản sử dụng",
                "Khi truy cập và mua sắm tại LEIKA, khách hàng đồng ý với những nguyên tắc vận hành cơ bản giúp trải nghiệm mua hàng trực tuyến luôn minh bạch, chuẩn xác và nhất quán.",
                List.of(
                    section("Tính chính xác của thông tin", "LEIKA nỗ lực duy trì hình ảnh, mô tả, mức giá và tình trạng tồn kho ở trạng thái cập nhật nhất. Tuy nhiên, trong một số tình huống kỹ thuật hiếm gặp, thông tin có thể thay đổi mà chưa kịp đồng bộ tức thời. Chúng tôi bảo lưu quyền điều chỉnh các sai lệch phát sinh để đảm bảo giao dịch đúng thực tế.") ,
                    section("Xác nhận đơn hàng", "Đơn hàng chỉ được xem là hoàn tất khi khách hàng nhận được thông báo xác nhận từ LEIKA và giao dịch đáp ứng các điều kiện kiểm tra cần thiết. Trong trường hợp phát hiện bất thường về giá, thanh toán hoặc tồn kho, LEIKA có thể chủ động liên hệ để điều chỉnh hoặc hủy đơn phù hợp.") ,
                    section("Quyền sở hữu nội dung", "Toàn bộ hình ảnh, nội dung biên tập, thiết kế nhận diện và tài sản trí tuệ hiển thị trên nền tảng thuộc quyền sở hữu của LEIKA hoặc đối tác hợp pháp. Mọi hành vi sao chép, tái sử dụng hoặc khai thác cho mục đích thương mại khi chưa được cho phép đều không được chấp thuận.")
                )
            );
            default -> null;
        };
        }

        private Map<String, Object> buildBuyingGuidePage() {
        return page(
            "/guide/buying",
            "Hướng dẫn mua hàng - LEIKA LUXURY",
            "Shopping Guide",
            "Hướng dẫn mua hàng",
            "Từ lúc chọn thiết kế đến khi hoàn tất thanh toán, LEIKA tối ưu hành trình mua sắm để khách hàng dễ dàng đưa ra lựa chọn chính xác và thanh lịch.",
            List.of(
                section("Bước 1: Chọn thiết kế phù hợp", "Khách hàng có thể khám phá theo danh mục, bộ sưu tập hoặc khu vực sale để tìm đúng dòng sản phẩm mong muốn. Mỗi thiết kế đều đi kèm hình ảnh, thông tin chất liệu, phom dáng và mức giá để hỗ trợ quyết định nhanh chóng.") ,
                section("Bước 2: Kiểm tra biến thể", "Tại trang chi tiết sản phẩm, vui lòng chọn kích cỡ hoặc biến thể còn hàng trước khi thêm vào giỏ. Nếu cần tư vấn sâu hơn về form mặc, đội ngũ LEIKA luôn sẵn sàng hỗ trợ qua hotline và các kênh mạng xã hội chính thức.") ,
                section("Bước 3: Hoàn tất giỏ hàng", "Sau khi thêm sản phẩm, khách hàng có thể rà soát lại số lượng, mã ưu đãi và chi phí giao hàng tại giỏ hàng. Hệ thống sẽ giữ trải nghiệm thanh toán gọn, rõ và hạn chế tối đa các thao tác không cần thiết.") ,
                section("Bước 4: Thanh toán và theo dõi đơn", "LEIKA hỗ trợ các phương thức thanh toán trực tuyến phù hợp với nhu cầu phổ biến. Sau khi đặt hàng thành công, khách hàng có thể đăng nhập để theo dõi trạng thái đơn, lịch sử mua sắm và các cập nhật hậu mãi liên quan.")
            )
        );
        }

        private Map<String, Object> page(String path, String pageTitle, String label, String title, String lead, List<Map<String, String>> sections) {
        Map<String, Object> page = new LinkedHashMap<>();
        page.put("path", path);
        page.put("pageTitle", pageTitle);
        page.put("label", label);
        page.put("title", title);
        page.put("lead", lead);
        page.put("sections", sections);
        return page;
        }

        private Map<String, String> section(String title, String body) {
        return Map.of(
            "title", title,
            "body", body
        );
        }
}
