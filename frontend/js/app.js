// Point of Sale Frontend Application Logic

// Auto-detect API base URL: if served from same origin (e.g. Nginx/Spring Boot), use relative URL; otherwise default to localhost:8080
const API_BASE_URL = (window.location.port === "8080" || window.location.port === "80" || window.location.port === "443" || window.location.port === "")
    ? window.location.origin
    : "http://localhost:8080";

const TAX_RATE = 0.08; // 8%

// App State
let state = {
    token: localStorage.getItem("pos_jwt") || null,
    user: JSON.parse(localStorage.getItem("pos_user") || "null"),
    products: [],
    cart: [],
    categories: new Set()
};

// Bootstrap Modal Instances
let loginModal, checkoutModal, receiptModal, newProductModal, adjustStockModal;

document.addEventListener("DOMContentLoaded", () => {
    // Initialize Modals
    loginModal = new bootstrap.Modal(document.getElementById("loginModal"));
    checkoutModal = new bootstrap.Modal(document.getElementById("checkoutModal"));
    receiptModal = new bootstrap.Modal(document.getElementById("receiptModal"));
    newProductModal = new bootstrap.Modal(document.getElementById("newProductModal"));
    adjustStockModal = new bootstrap.Modal(document.getElementById("adjustStockModal"));

    setupTabNavigation();
    setupSearchAndFilter();
    updateAuthUI();

    // Load initial catalog
    loadProducts();
});

// ==========================================
// 1. API HELPER WITH JWT INJECTION
// ==========================================
async function apiRequest(endpoint, method = "GET", body = null) {
    const headers = {
        "Content-Type": "application/json"
    };

    if (state.token) {
        headers["Authorization"] = `Bearer ${state.token}`;
    }

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method,
            headers,
            body: body ? JSON.stringify(body) : null
        });

        if (response.status === 401) {
            showAlert("Session expired or unauthorized. Please log in.", "danger");
            logout();
            throw new Error("Unauthorized");
        }

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const errorMsg = (data && data.message) ? data.message : `Error ${response.status}: ${response.statusText}`;
            throw new Error(errorMsg);
        }

        return data;
    } catch (err) {
        if (err.message !== "Unauthorized") {
            showAlert(err.message, "danger");
        }
        throw err;
    }
}

function showAlert(message, type = "info") {
    const container = document.getElementById("alertContainer");
    const alertId = "alert-" + Date.now();
    const alertHtml = `
        <div id="${alertId}" class="alert alert-${type} alert-dismissible fade show shadow-sm" role="alert">
            <i class="bi bi-info-circle-fill me-2"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    container.innerHTML = alertHtml;
    setTimeout(() => {
        const elem = document.getElementById(alertId);
        if (elem) elem.remove();
    }, 5000);
}

// ==========================================
// 2. AUTHENTICATION & RBAC
// ==========================================
function updateAuthUI() {
    const userBadge = document.getElementById("userBadge");
    const userName = document.getElementById("userName");
    const roleBadge = document.getElementById("roleBadge");
    const authBtn = document.getElementById("authBtn");

    if (state.token && state.user) {
        userBadge.classList.remove("d-none");
        userName.textContent = state.user.fullName || state.user.username;
        const primaryRole = (state.user.roles && state.user.roles.length > 0)
            ? state.user.roles[0].replace("ROLE_", "")
            : "USER";
        roleBadge.textContent = primaryRole;
        authBtn.innerHTML = `<i class="bi bi-box-arrow-right me-1"></i> Logout`;
        authBtn.className = "btn btn-outline-danger btn-sm";
    } else {
        userBadge.classList.add("d-none");
        authBtn.innerHTML = `<i class="bi bi-box-arrow-in-right me-1"></i> Login`;
        authBtn.className = "btn btn-outline-light btn-sm";
    }
}

function handleAuthAction() {
    if (state.token) {
        logout();
    } else {
        document.getElementById("loginError").classList.add("d-none");
        loginModal.show();
    }
}

async function handleLoginSubmit(e) {
    e.preventDefault();
    const username = document.getElementById("loginUsername").value.trim();
    const password = document.getElementById("loginPassword").value.trim();
    const errorDiv = document.getElementById("loginError");

    try {
        const res = await apiRequest("/api/v1/auth/login", "POST", { username, password });
        state.token = res.token;
        state.user = {
            id: res.id,
            username: res.username,
            fullName: res.fullName,
            email: res.email,
            roles: res.roles
        };
        localStorage.setItem("pos_jwt", state.token);
        localStorage.setItem("pos_user", JSON.stringify(state.user));

        loginModal.hide();
        showAlert(`Welcome back, ${state.user.fullName}!`, "success");
        updateAuthUI();
        loadProducts();
    } catch (err) {
        errorDiv.textContent = err.message || "Invalid username or password";
        errorDiv.classList.remove("d-none");
    }
}

function logout() {
    state.token = null;
    state.user = null;
    localStorage.removeItem("pos_jwt");
    localStorage.removeItem("pos_user");
    updateAuthUI();
    showAlert("Logged out successfully.", "info");
}

// ==========================================
// 3. NAVIGATION & TAB SWITCHING
// ==========================================
function setupTabNavigation() {
    const tabs = document.querySelectorAll("#mainNavTabs .nav-link");
    tabs.forEach(tab => {
        tab.addEventListener("click", () => {
            tabs.forEach(t => t.classList.remove("active"));
            tab.classList.add("active");

            document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.add("d-none"));
            const targetPane = document.getElementById(tab.getAttribute("data-tab"));
            if (targetPane) targetPane.classList.remove("d-none");

            // Lazy load tab data
            if (tab.id === "navInventory") loadInventory();
            if (tab.id === "navSales") loadSalesHistory();
            if (tab.id === "navDashboard") loadDashboard();
        });
    });
}

// ==========================================
// 4. PRODUCT CATALOG & CART LOGIC
// ==========================================
async function loadProducts() {
    const container = document.getElementById("productList");
    try {
        const data = await apiRequest("/api/v1/products");
        state.products = data || [];
        populateCategories();
        renderProducts(state.products);
    } catch (err) {
        container.innerHTML = `
            <div class="col-12 text-center py-5 text-danger">
                <i class="bi bi-wifi-off fs-1 d-block mb-2"></i>
                Failed to connect to POS Backend API. Make sure Spring Boot is running on port 8080.
            </div>
        `;
    }
}

function populateCategories() {
    const select = document.getElementById("categoryFilter");
    state.categories = new Set(state.products.map(p => p.category));
    select.innerHTML = `<option value="ALL">All Categories</option>`;
    state.categories.forEach(cat => {
        select.innerHTML += `<option value="${cat}">${cat}</option>`;
    });
}

function setupSearchAndFilter() {
    const searchInput = document.getElementById("searchInput");
    const categoryFilter = document.getElementById("categoryFilter");

    const filter = () => {
        const query = searchInput.value.toLowerCase().trim();
        const selectedCat = categoryFilter.value;

        const filtered = state.products.filter(p => {
            const matchesQuery = p.name.toLowerCase().includes(query) ||
                p.sku.toLowerCase().includes(query) ||
                (p.barcode && p.barcode.toLowerCase().includes(query));
            const matchesCategory = (selectedCat === "ALL" || p.category === selectedCat);
            return matchesQuery && matchesCategory;
        });

        renderProducts(filtered);
    };

    searchInput.addEventListener("input", filter);
    categoryFilter.addEventListener("change", filter);
}

function renderProducts(items) {
    const container = document.getElementById("productList");
    if (!items || items.length === 0) {
        container.innerHTML = `
            <div class="col-12 text-center py-5 text-muted">
                <i class="bi bi-inbox fs-2 d-block mb-2"></i> No products match your search.
            </div>
        `;
        return;
    }

    container.innerHTML = items.map(p => {
        const outOfStock = p.quantity <= 0;
        const lowStock = p.lowStock && !outOfStock;

        let badge = `<span class="badge bg-success">In Stock (${p.quantity})</span>`;
        if (outOfStock) {
            badge = `<span class="badge bg-danger">Out of Stock</span>`;
        } else if (lowStock) {
            badge = `<span class="badge bg-warning text-dark badge-low-stock">Low Stock (${p.quantity})</span>`;
        }

        return `
            <div class="col">
                <div class="card h-100 product-card shadow-sm ${outOfStock ? 'out-of-stock' : ''}" 
                     onclick="${outOfStock ? '' : `addToCart(${p.id})`}">
                    <div class="card-body d-flex flex-column">
                        <div class="d-flex justify-content-between align-items-start mb-2">
                            <span class="badge bg-light text-secondary border">${p.category}</span>
                            ${badge}
                        </div>
                        <h6 class="card-title fw-bold text-dark mb-1 text-truncate">${p.name}</h6>
                        <small class="text-muted mb-2 font-monospace">SKU: ${p.sku}</small>
                        <div class="mt-auto d-flex justify-content-between align-items-center pt-2 border-top">
                            <span class="fs-5 fw-bold text-primary">$${Number(p.price).toFixed(2)}</span>
                            <button class="btn btn-outline-primary btn-sm rounded-circle" ${outOfStock ? 'disabled' : ''} title="Add to Cart">
                                <i class="bi bi-plus-lg"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join("");
}

function addToCart(productId) {
    const product = state.products.find(p => p.id === productId);
    if (!product || product.quantity <= 0) return;

    const existing = state.cart.find(item => item.product.id === productId);
    if (existing) {
        if (existing.quantity >= product.quantity) {
            showAlert(`Cannot add more than available stock (${product.quantity}) for ${product.name}`, "warning");
            return;
        }
        existing.quantity += 1;
    } else {
        state.cart.push({ product, quantity: 1 });
    }

    renderCart();
}

function updateCartItemQty(productId, delta) {
    const item = state.cart.find(i => i.product.id === productId);
    if (!item) return;

    const newQty = item.quantity + delta;
    if (newQty <= 0) {
        state.cart = state.cart.filter(i => i.product.id !== productId);
    } else {
        if (newQty > item.product.quantity) {
            showAlert(`Cannot exceed available stock (${item.product.quantity})`, "warning");
            return;
        }
        item.quantity = newQty;
    }

    renderCart();
}

function removeFromCart(productId) {
    state.cart = state.cart.filter(i => i.product.id !== productId);
    renderCart();
}

function clearCart() {
    state.cart = [];
    renderCart();
}

function renderCart() {
    const list = document.getElementById("cartItemsList");
    const checkoutBtn = document.getElementById("checkoutBtn");

    if (state.cart.length === 0) {
        list.innerHTML = `
            <div class="text-center text-muted py-5">
                <i class="bi bi-basket3 fs-1 d-block mb-2 text-secondary"></i>
                Cart is empty. Click a product to add it.
            </div>
        `;
        checkoutBtn.disabled = true;
        updateCartTotals();
        return;
    }

    list.innerHTML = state.cart.map(item => `
        <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
            <div class="me-2" style="max-width: 160px;">
                <div class="fw-semibold text-truncate small">${item.product.name}</div>
                <small class="text-muted">$${Number(item.product.price).toFixed(2)} each</small>
            </div>
            <div class="d-flex align-items-center gap-1">
                <button class="btn btn-sm btn-outline-secondary py-0 px-2" onclick="updateCartItemQty(${item.product.id}, -1)">-</button>
                <span class="px-2 fw-bold small">${item.quantity}</span>
                <button class="btn btn-sm btn-outline-secondary py-0 px-2" onclick="updateCartItemQty(${item.product.id}, 1)">+</button>
            </div>
            <div class="text-end ms-2">
                <div class="fw-bold small text-success">$${(item.product.price * item.quantity).toFixed(2)}</div>
                <button class="btn btn-sm text-danger p-0" onclick="removeFromCart(${item.product.id})">
                    <i class="bi bi-x-circle"></i>
                </button>
            </div>
        </div>
    `).join("");

    checkoutBtn.disabled = false;
    updateCartTotals();
}

function updateCartTotals() {
    const subtotal = state.cart.reduce((sum, item) => sum + (item.product.price * item.quantity), 0);
    const discountInput = document.getElementById("cartDiscount");
    let discount = parseFloat(discountInput.value) || 0;
    if (discount > subtotal) {
        discount = subtotal;
        discountInput.value = subtotal.toFixed(2);
    }

    const taxableAmount = Math.max(0, subtotal - discount);
    const tax = taxableAmount * TAX_RATE;
    const total = taxableAmount + tax;

    document.getElementById("cartSubtotal").textContent = `$${subtotal.toFixed(2)}`;
    document.getElementById("cartTax").textContent = `$${tax.toFixed(2)}`;
    document.getElementById("cartTotal").textContent = `$${total.toFixed(2)}`;
}

// ==========================================
// 5. CHECKOUT & RECEIPT PROCESSING
// ==========================================
function openCheckoutModal() {
    if (!state.token) {
        showAlert("Please log in with cashier or admin credentials to perform checkout.", "warning");
        loginModal.show();
        return;
    }

    const totalText = document.getElementById("cartTotal").textContent;
    document.getElementById("checkoutModalTotal").textContent = totalText;
    document.getElementById("cashTenderedInput").value = "";
    document.getElementById("changeDueText").textContent = "$0.00";
    document.getElementById("checkoutPaymentMethod").value = "CASH";
    handlePaymentMethodChange();

    checkoutModal.show();
}

function handlePaymentMethodChange() {
    const method = document.getElementById("checkoutPaymentMethod").value;
    const cashFields = document.getElementById("cashFields");
    const cardFields = document.getElementById("cardFields");

    if (method === "CASH") {
        cashFields.classList.remove("d-none");
        cardFields.classList.add("d-none");
    } else {
        cashFields.classList.add("d-none");
        cardFields.classList.remove("d-none");
    }
}

function calculateChange() {
    const totalText = document.getElementById("checkoutModalTotal").textContent.replace("$", "");
    const total = parseFloat(totalText) || 0;
    const tendered = parseFloat(document.getElementById("cashTenderedInput").value) || 0;
    const change = Math.max(0, tendered - total);
    document.getElementById("changeDueText").textContent = `$${change.toFixed(2)}`;
}

async function submitCheckout() {
    const method = document.getElementById("checkoutPaymentMethod").value;
    const discount = parseFloat(document.getElementById("cartDiscount").value) || 0;
    const totalText = document.getElementById("checkoutModalTotal").textContent.replace("$", "");
    const total = parseFloat(totalText) || 0;

    let cashTendered = null;
    if (method === "CASH") {
        cashTendered = parseFloat(document.getElementById("cashTenderedInput").value);
        if (isNaN(cashTendered) || cashTendered < total) {
            showAlert("Cash tendered must be equal to or greater than the total amount.", "warning");
            return;
        }
    }

    const checkoutPayload = {
        items: state.cart.map(item => ({
            productId: item.product.id,
            quantity: item.quantity
        })),
        paymentMethod: method,
        discountAmount: discount,
        cashTendered: cashTendered
    };

    const confirmBtn = document.getElementById("confirmPaymentBtn");
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span> Processing...`;

    try {
        const response = await apiRequest("/api/v1/sales/checkout", "POST", checkoutPayload);
        checkoutModal.hide();
        clearCart();
        loadProducts(); // Refresh stock counts in UI

        // Display Receipt
        showReceipt(response);
    } catch (err) {
        showAlert(err.message || "Checkout failed", "danger");
    } finally {
        confirmBtn.disabled = false;
        confirmBtn.innerHTML = `<i class="bi bi-check-circle me-1"></i> Authorize & Complete Sale`;
    }
}

function showReceipt(sale) {
    const content = document.getElementById("receiptContent");
    const itemsHtml = sale.items.map(item => `
        <div class="d-flex justify-content-between small">
            <span>${item.productName} (x${item.quantity})</span>
            <span>$${Number(item.subtotal).toFixed(2)}</span>
        </div>
    `).join("");

    content.innerHTML = `
        <div class="text-center mb-3">
            <h5 class="fw-bold mb-0">CLOUD POS 3-TIER ENTERPRISE</h5>
            <small class="text-muted">High-Availability Retail Services</small><br>
            <small class="text-muted">${new Date(sale.createdAt).toLocaleString()}</small>
        </div>
        <div class="receipt-border py-2 mb-2">
            <div class="d-flex justify-content-between small">
                <span>Invoice:</span>
                <strong>${sale.invoiceNumber}</strong>
            </div>
            <div class="d-flex justify-content-between small">
                <span>Cashier:</span>
                <span>${sale.cashierName}</span>
            </div>
            <div class="d-flex justify-content-between small">
                <span>Payment:</span>
                <span>${sale.paymentMethod} (${sale.transactionReference})</span>
            </div>
        </div>
        <div class="mb-2">
            ${itemsHtml}
        </div>
        <div class="receipt-border py-2 mb-2">
            <div class="d-flex justify-content-between small">
                <span>Subtotal:</span>
                <span>$${Number(sale.subtotal).toFixed(2)}</span>
            </div>
            <div class="d-flex justify-content-between small">
                <span>Tax (8%):</span>
                <span>$${Number(sale.taxAmount).toFixed(2)}</span>
            </div>
            ${sale.discountAmount > 0 ? `
            <div class="d-flex justify-content-between small text-danger">
                <span>Discount:</span>
                <span>-$${Number(sale.discountAmount).toFixed(2)}</span>
            </div>` : ''}
            <div class="d-flex justify-content-between fw-bold fs-6 mt-1">
                <span>TOTAL:</span>
                <span class="text-success">$${Number(sale.totalAmount).toFixed(2)}</span>
            </div>
            ${sale.cashTendered ? `
            <div class="d-flex justify-content-between small mt-1">
                <span>Cash Tendered:</span>
                <span>$${Number(sale.cashTendered).toFixed(2)}</span>
            </div>
            <div class="d-flex justify-content-between small">
                <span>Change Due:</span>
                <span>$${Number(sale.changeDue).toFixed(2)}</span>
            </div>` : ''}
        </div>
        <div class="text-center text-muted small mt-3">
            Thank you for your business!<br>
            Customer Support: support@pos.internal
        </div>
    `;

    receiptModal.show();
}

// ==========================================
// 6. INVENTORY MANAGEMENT TAB
// ==========================================
async function loadInventory() {
    const tbody = document.getElementById("inventoryTableBody");
    const banner = document.getElementById("lowStockAlertBanner");

    try {
        const [products, alerts] = await Promise.all([
            apiRequest("/api/v1/products"),
            apiRequest("/api/v1/inventory/alerts").catch(() => [])
        ]);

        if (alerts && alerts.length > 0) {
            banner.classList.remove("d-none");
            banner.innerHTML = `<i class="bi bi-exclamation-triangle-fill me-2"></i> <strong>Low Stock Warning:</strong> ${alerts.length} item(s) are below safety threshold!`;
        } else {
            banner.classList.add("d-none");
        }

        if (!products || products.length === 0) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4">No products found.</td></tr>`;
            return;
        }

        tbody.innerHTML = products.map(p => {
            const isLow = p.quantity <= p.lowStockThreshold;
            return `
                <tr>
                    <td class="font-monospace fw-semibold">${p.sku}</td>
                    <td class="font-monospace text-muted small">${p.barcode || 'N/A'}</td>
                    <td class="fw-semibold">${p.name}</td>
                    <td><span class="badge bg-light text-dark border">${p.category}</span></td>
                    <td class="fw-bold text-success">$${Number(p.price).toFixed(2)}</td>
                    <td class="text-muted small">$${Number(p.costPrice).toFixed(2)}</td>
                    <td>
                        <span class="fw-bold fs-6 ${isLow ? 'text-danger' : 'text-dark'}">${p.quantity}</span>
                        <small class="text-muted">(Min: ${p.lowStockThreshold})</small>
                    </td>
                    <td>
                        ${isLow ? '<span class="badge bg-danger">Low Stock</span>' : '<span class="badge bg-success">Optimal</span>'}
                    </td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-dark" onclick="openAdjustStockModal(${p.id}, '${p.name}')">
                            <i class="bi bi-arrow-repeat me-1"></i> Stock
                        </button>
                    </td>
                </tr>
            `;
        }).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4">${err.message}</td></tr>`;
    }
}

function openNewProductModal() {
    if (!state.token) {
        showAlert("Please log in with Admin or Inventory Manager privileges.", "warning");
        loginModal.show();
        return;
    }
    document.getElementById("newProductForm").reset();
    newProductModal.show();
}

async function handleCreateProduct(e) {
    e.preventDefault();
    const payload = {
        sku: document.getElementById("npSku").value.trim(),
        barcode: document.getElementById("npBarcode").value.trim() || null,
        name: document.getElementById("npName").value.trim(),
        category: document.getElementById("npCategory").value.trim(),
        price: parseFloat(document.getElementById("npPrice").value),
        costPrice: parseFloat(document.getElementById("npCost").value),
        initialQuantity: parseInt(document.getElementById("npStock").value, 10),
        lowStockThreshold: parseInt(document.getElementById("npThreshold").value, 10)
    };

    try {
        await apiRequest("/api/v1/products", "POST", payload);
        newProductModal.hide();
        showAlert(`Product '${payload.name}' created successfully!`, "success");
        loadProducts();
        loadInventory();
    } catch (err) {
        showAlert(err.message || "Failed to create product", "danger");
    }
}

function openAdjustStockModal(id, name) {
    if (!state.token) {
        showAlert("Please log in with Admin or Inventory Manager privileges.", "warning");
        loginModal.show();
        return;
    }
    document.getElementById("asProductId").value = id;
    document.getElementById("asProductName").textContent = name;
    document.getElementById("asQuantity").value = 10;
    adjustStockModal.show();
}

async function handleAdjustStock(e) {
    e.preventDefault();
    const id = document.getElementById("asProductId").value;
    const payload = {
        operation: document.getElementById("asOperation").value,
        quantity: parseInt(document.getElementById("asQuantity").value, 10)
    };

    try {
        await apiRequest(`/api/v1/inventory/${id}/adjust`, "POST", payload);
        adjustStockModal.hide();
        showAlert("Inventory stock level updated successfully!", "success");
        loadProducts();
        loadInventory();
    } catch (err) {
        showAlert(err.message || "Failed to adjust stock", "danger");
    }
}

// ==========================================
// 7. SALES HISTORY & DASHBOARD TAB
// ==========================================
async function loadSalesHistory() {
    const tbody = document.getElementById("salesTableBody");
    try {
        const sales = await apiRequest("/api/v1/sales/history");
        if (!sales || sales.length === 0) {
            tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4">No transactions recorded yet.</td></tr>`;
            return;
        }

        tbody.innerHTML = sales.map(s => `
            <tr>
                <td class="font-monospace fw-bold text-primary">${s.invoiceNumber}</td>
                <td>${s.cashierName}</td>
                <td><span class="badge bg-secondary">${s.items.length} items</span></td>
                <td class="fw-bold text-success">$${Number(s.totalAmount).toFixed(2)}</td>
                <td><span class="badge bg-light text-dark border">${s.paymentMethod}</span></td>
                <td class="font-monospace small text-muted">${s.transactionReference}</td>
                <td class="small text-muted">${new Date(s.createdAt).toLocaleString()}</td>
                <td><span class="badge bg-success">${s.status}</span></td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center text-danger py-4">${err.message}</td></tr>`;
    }
}

async function loadDashboard() {
    try {
        const summary = await apiRequest("/api/v1/reports/summary");
        document.getElementById("dashTotalSales").textContent = summary.totalSalesCount || 0;
        document.getElementById("dashTotalRevenue").textContent = `$${Number(summary.totalRevenue || 0).toFixed(2)}`;
        document.getElementById("dashTotalProducts").textContent = summary.totalProductsCount || 0;
        document.getElementById("dashLowStock").textContent = summary.lowStockCount || 0;
    } catch (err) {
        // Fallback for non-admin users or if endpoint fails
        console.warn("Dashboard stats restricted to ADMIN: ", err.message);
    }
}
