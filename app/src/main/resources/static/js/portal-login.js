/**
 * ORACLE WEBCENTER PORTAL - SCRIPT DE AUTENTICAÇÃO UNIFICADA COM CAPTCHA
 * Perfis de acesso resolvidos 100% via grupos do OpenLDAP
 */

document.addEventListener('DOMContentLoaded', () => {
    const wcpLoginForm = document.getElementById('wcpLoginForm');
    const usernameInput = document.getElementById('usernameInput');
    const passwordInput = document.getElementById('passwordInput');
    const captchaDisplayBox = document.getElementById('captchaDisplayBox');
    const captchaQuestionText = document.getElementById('captchaQuestionText');
    const captchaAnswerInput = document.getElementById('captchaAnswerInput');
    const btnReloadCaptcha = document.getElementById('btnReloadCaptcha');
    const loginFeedbackBox = document.getElementById('loginFeedbackBox');
    const btnSubmitLogin = document.getElementById('btnSubmitLogin');

    // Botões de simulação e perfis
    const btnUserEnterprise = document.getElementById('btnUserEnterprise');
    const btnUserPremium = document.getElementById('btnUserPremium');
    const btnUserPartner = document.getElementById('btnUserPartner');
    const btnSimulateSqli = document.getElementById('btnSimulateSqli');
    const btnSimulateXss = document.getElementById('btnSimulateXss');
    const btnSimulateWrongCaptcha = document.getElementById('btnSimulateWrongCaptcha');

    let currentCaptchaId = '';

    // =========================================================================
    // 1. Carrega desafio CAPTCHA via Agente de Cibersegurança
    // =========================================================================
    async function loadCaptchaChallenge() {
        try {
            if (captchaQuestionText) captchaQuestionText.textContent = '...';
            const resp = await fetch('/api/portal/captcha');
            if (!resp.ok) throw new Error('Falha ao obter desafio do servidor');
            const data = await resp.json();
            currentCaptchaId = data.captchaId;

            // Renderiza a imagem SVG de segurança se disponível, com fallback para texto
            if (data.svgImage && captchaDisplayBox) {
                captchaDisplayBox.innerHTML = data.svgImage;
            } else if (captchaQuestionText) {
                captchaQuestionText.textContent = data.question || data.challengeText || '12 + 8 = ?';
            }

            if (captchaAnswerInput) captchaAnswerInput.value = '';
        } catch (err) {
            console.error('Erro ao carregar Captcha:', err);
            if (captchaDisplayBox) {
                captchaDisplayBox.innerHTML = '<span class="portal-captcha-code" style="color:#ef4444; font-size:0.85rem;">Erro de carga</span>';
            }
        }
    }

    btnReloadCaptcha.addEventListener('click', loadCaptchaChallenge);

    // =========================================================================
    // 2. Submissão do Formulário de Login Único (Perfil derivado do LDAP)
    // =========================================================================
    wcpLoginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await performLogin(
            usernameInput.value.trim(),
            passwordInput.value,
            captchaAnswerInput.value.trim(),
            '192.168.1.100'
        );
    });

    async function performLogin(username, password, captchaAns, clientIp) {
        showFeedback('info', 'Consultando árvore do OpenLDAP e validando CAPTCHA com o Agente de Cibersegurança...');
        btnSubmitLogin.disabled = true;

        try {
            const resp = await fetch('/api/portal/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: username,
                    password: password,
                    captchaId: currentCaptchaId,
                    captchaAnswer: captchaAns,
                    clientIp: clientIp
                })
            });

            const data = await resp.json();

            if (!resp.ok || !data.authenticated) {
                if (data.blockedByCyberAgent) {
                    showFeedback('danger', `🛡️ <strong>BLOQUEIO DO AGENTE DE CIBERSEGURANÇA:</strong> ${escapeHtml(data.defenseReport?.message || 'Ataque neutralizado em tempo real!')}`);
                } else {
                    showFeedback('danger', `⚠️ <strong>Falha na Autenticação:</strong> ${escapeHtml(data.error || 'Credenciais inválidas.')}`);
                }
                loadCaptchaChallenge();
                return;
            }

            // Sucesso na autenticação única! O perfil foi determinado pelo LDAP
            const resolvedRole = data.resolvedPortalView?.ldapGroup || 'CLIENT_ENTERPRISE';
            showFeedback('success', `✅ <strong>Autenticado via OpenLDAP!</strong> Perfil atribuído: <strong>${escapeHtml(resolvedRole)}</strong>. Carregando portal...`);

            // Salva os metadados do WebCenter Portal na sessão
            sessionStorage.setItem('portalUserSession', JSON.stringify(data.resolvedPortalView));

            setTimeout(() => {
                window.location.href = '/portal.html';
            }, 600);

        } catch (err) {
            console.error('Erro ao conectar ao gateway:', err);
            showFeedback('danger', 'Erro ao conectar ao servidor do Portal: ' + err.message);
        } finally {
            btnSubmitLogin.disabled = false;
        }
    }

    function showFeedback(type, htmlContent) {
        loginFeedbackBox.className = `portal-alert-box ${type}`;
        loginFeedbackBox.innerHTML = htmlContent;
        loginFeedbackBox.style.display = 'block';
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }

    // =========================================================================
    // 3. Simulações Rápidas de Perfis LDAP e Ataques
    // =========================================================================
    btnUserEnterprise?.addEventListener('click', () => {
        usernameInput.value = 'joao.silva';
        showFeedback('info', 'Usuário <code>joao.silva</code> selecionado (associado ao grupo <strong>CLIENT_ENTERPRISE</strong> no OpenLDAP). Resolva o captcha e clique em Acessar.');
        captchaAnswerInput.focus();
    });

    btnUserPremium?.addEventListener('click', () => {
        usernameInput.value = 'maria.gold';
        showFeedback('info', 'Usuário <code>maria.gold</code> selecionado (associado ao grupo <strong>CLIENT_PREMIUM</strong> no OpenLDAP). Resolva o captcha e clique em Acessar.');
        captchaAnswerInput.focus();
    });

    btnUserPartner?.addEventListener('click', () => {
        usernameInput.value = 'carlos.parceiro';
        showFeedback('info', 'Usuário <code>carlos.parceiro</code> selecionado (associado ao grupo <strong>CLIENT_PARTNER</strong> no OpenLDAP). Resolva o captcha e clique em Acessar.');
        captchaAnswerInput.focus();
    });

    btnSimulateSqli?.addEventListener('click', () => {
        usernameInput.value = "admin' OR 1=1--";
        captchaAnswerInput.value = '999';
        performLogin("admin' OR 1=1--", "123", '999', '198.51.100.77');
    });

    btnSimulateXss?.addEventListener('click', () => {
        usernameInput.value = "<script>alert('xss')</script>";
        captchaAnswerInput.value = '999';
        performLogin("<script>alert('xss')</script>", "123", '999', '198.51.100.92');
    });

    btnSimulateWrongCaptcha?.addEventListener('click', () => {
        usernameInput.value = "joao.silva";
        captchaAnswerInput.value = '99999';
        performLogin("joao.silva", "senha123", '99999', '192.168.1.50');
    });

    // Início
    loadCaptchaChallenge();
});
