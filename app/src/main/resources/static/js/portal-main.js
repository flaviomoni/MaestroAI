/**
 * ORACLE WEBCENTER PORTAL - SCRIPT DA PÁGINA PRINCIPAL DO PORTAL (MULTI-TENANT)
 */

document.addEventListener('DOMContentLoaded', () => {
    // Referências do DOM
    const portalTitleText = document.getElementById('portalTitleText');
    const userNameLabel = document.getElementById('userNameLabel');
    const userRoleLabel = document.getElementById('userRoleLabel');
    const portalNavItems = document.getElementById('portalNavItems');
    const sessionGreeting = document.getElementById('sessionGreeting');
    const sessionDetails = document.getElementById('sessionDetails');
    const currentColorCode = document.getElementById('currentColorCode');
    const metaTargetVersion = document.getElementById('metaTargetVersion');
    const metaRestEndpoint = document.getElementById('metaRestEndpoint');
    const btnLogout = document.getElementById('btnLogout');
    const btnRefreshServices = document.getElementById('btnRefreshServices');

    // =========================================================================
    // 1. Carrega dados da Sessão Autenticada
    // =========================================================================
    let sessionData = null;
    try {
        const raw = sessionStorage.getItem('portalUserSession');
        if (raw) {
            sessionData = JSON.parse(raw);
        }
    } catch (e) {
        console.warn('Erro ao ler sessão do portal:', e);
    }

    // Se não houver sessão ativa, usa valores padrão para demonstração
    if (!sessionData) {
        sessionData = {
            portalKey: "portal-enterprise",
            displayName: "Portal Corporativo Enterprise",
            targetVersion: "Oracle WebCenter Portal 14c (14.1.2.0.0) / 12c (12.2.1.4.0)",
            restApiEndpoint: "/rest/api/v1/portals/portal-enterprise",
            pageTemplate: "Universal Portal Template (portal_universal_pagetemplate.aar)",
            skinFamily: "assetSkinUniversal",
            ldapGroup: "CLIENT_ENTERPRISE",
            authenticatedUser: "joao.silva",
            customAttributes: {
                corPrimaria: "#004e8b",
                formatoData: "auto",
                urlPortal: "https://portal.corp.internal/portal-enterprise"
            },
            visibleModules: ["Dashboard SLA", "Topologia Dedicada", "Faturas Corporativas", "Trouble Ticket", "Alterar Senha"]
        };
    }

    // =========================================================================
    // 2. Aplica Identidade Visual Dinâmica (Tokens CSS e Dados do Cliente)
    // =========================================================================
    const primaryColor = sessionData.customAttributes?.corPrimaria || '#004e8b';
    document.documentElement.style.setProperty('--portal-primary', primaryColor);

    if (portalTitleText) portalTitleText.textContent = sessionData.displayName || 'Portal Corporativo';
    if (userNameLabel) userNameLabel.textContent = sessionData.authenticatedUser || 'usuário';
    if (userRoleLabel) userRoleLabel.textContent = sessionData.ldapGroup || 'CLIENT_ENTERPRISE';
    if (currentColorCode) currentColorCode.textContent = primaryColor;
    if (metaTargetVersion) metaTargetVersion.textContent = sessionData.targetVersion || 'WCP 14c / 12c';
    if (metaRestEndpoint) metaRestEndpoint.textContent = sessionData.restApiEndpoint || '/rest/api/v1/portals';

    if (sessionGreeting) {
        sessionGreeting.textContent = `Bem-vindo ao ${sessionData.displayName || 'Portal Corporativo'}`;
    }
    if (sessionDetails) {
        sessionDetails.innerHTML = `Entrada única resolvida via OpenLDAP para o usuário <strong>${escapeHtml(sessionData.authenticatedUser)}</strong> • Page Template Universal ativo com a cor primária <code>${primaryColor}</code>.`;
    }

    // =========================================================================
    // 3. Renderiza Menu de Navegação Horizontal dos Módulos do Perfil
    // =========================================================================
    if (portalNavItems && sessionData.visibleModules && sessionData.visibleModules.length > 0) {
        portalNavItems.innerHTML = sessionData.visibleModules.map((mod, idx) => {
            const activeClass = idx === 0 ? 'active' : '';
            return `<button class="portal-nav-link ${activeClass}" data-module="${escapeHtml(mod)}">📌 ${escapeHtml(mod)}</button>`;
        }).join('');

        // Event listener de troca de abas
        portalNavItems.querySelectorAll('.portal-nav-link').forEach(btn => {
            btn.addEventListener('click', (e) => {
                portalNavItems.querySelectorAll('.portal-nav-link').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                handleTabClick(btn.getAttribute('data-module'));
            });
        });
    }

    function handleTabClick(moduleName) {
        console.log('Módulo ativado no WebCenter Portal:', moduleName);
    }

    // =========================================================================
    // 4. Logout e Atualizações
    // =========================================================================
    btnLogout?.addEventListener('click', () => {
        sessionStorage.removeItem('portalUserSession');
        window.location.href = '/login.html';
    });

    btnRefreshServices?.addEventListener('click', () => {
        btnRefreshServices.textContent = 'Atualizando...';
        setTimeout(() => {
            btnRefreshServices.textContent = '🔄 Atualizar Dados';
        }, 500);
    });

    function escapeHtml(text) {
        if (!text) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }
});
