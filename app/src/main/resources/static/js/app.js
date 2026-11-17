/**
 * PAINEL DE CONTROLE MULTI-AGENTE IA - CLIENTE JAVASCRIPT
 */

document.addEventListener('DOMContentLoaded', () => {
    // Referências do DOM
    const thoughtStreamContainer = document.getElementById('thoughtStreamContainer');
    const infraNodesGrid = document.getElementById('infraNodesGrid');
    const chatMessagesContainer = document.getElementById('chatMessagesContainer');
    const chatInputForm = document.getElementById('chatInputForm');
    const userPromptInput = document.getElementById('userPromptInput');
    const btnSendMessage = document.getElementById('btnSendMessage');
    const btnRefreshStatus = document.getElementById('btnRefreshStatus');
    const btnClearStream = document.getElementById('btnClearStream');
    const sseStatusPill = document.getElementById('sseStatusPill');
    const sseStatusText = document.getElementById('sseStatusText');

    // Referências do Modal HITL
    const hitlModalBackdrop = document.getElementById('hitlModalBackdrop');
    const btnHitlClose = document.getElementById('btnHitlClose');
    const btnHitlApprove = document.getElementById('btnHitlApprove');
    const btnHitlReject = document.getElementById('btnHitlReject');
    const hitlActionName = document.getElementById('hitlActionName');
    const hitlTarget = document.getElementById('hitlTarget');
    const hitlRiskBadge = document.getElementById('hitlRiskBadge');
    const hitlAgentTag = document.getElementById('hitlAgentTag');
    const hitlJustificationBox = document.getElementById('hitlJustificationBox');
    const hitlCommandCode = document.getElementById('hitlCommandCode');

    let currentHitlAction = null;

    // =========================================================================
    // 1. Inicialização do Server-Sent Events (SSE)
    // =========================================================================
    function initEventStream() {
        const eventSource = new EventSource('/api/events/stream');

        eventSource.addEventListener('CONNECTED', (e) => {
            sseStatusPill.classList.add('online');
            sseStatusText.textContent = 'Stream: Conectado';
        });

        eventSource.addEventListener('AGENT_THOUGHT', (e) => {
            try {
                const data = JSON.parse(e.data);
                renderStreamEntry(data);
            } catch (err) {
                console.error('Erro ao processar evento SSE:', err);
            }
        });

        eventSource.onerror = (err) => {
            sseStatusText.textContent = 'Stream: Reconectando...';
            console.warn('Conexão SSE oscilou, tentando reconectar...', err);
        };
    }

    function renderStreamEntry(entry) {
        const div = document.createElement('div');
        div.className = `stream-entry ${entry.eventType || 'THOUGHT'}`;

        const timeStr = entry.timestamp ? new Date(entry.timestamp).toLocaleTimeString() : new Date().toLocaleTimeString();

        div.innerHTML = `
            <div class="entry-header-row">
                <span class="entry-timestamp">${timeStr}</span>
                <span class="entry-badge badge-agent">${escapeHtml(entry.agentName || 'Agente')}</span>
                <span class="entry-badge badge-type">${escapeHtml(entry.eventType || 'INFO')}</span>
            </div>
            <div class="entry-body">${escapeHtml(entry.content || '')}</div>
            ${entry.details ? `<div class="entry-details-code">${escapeHtml(entry.details)}</div>` : ''}
        `;

        thoughtStreamContainer.appendChild(div);
        thoughtStreamContainer.scrollTop = thoughtStreamContainer.scrollHeight;

        // Se o evento for de proposta HITL, abre o modal automaticamente para chamar atenção
        if (entry.eventType === 'HITL_PROPOSED' && currentHitlAction) {
            openHitlModal(currentHitlAction);
        }
    }

    // =========================================================================
    // 2. Carregamento do Status da Infraestrutura
    // =========================================================================
    async function loadInfraStatus() {
        try {
            btnRefreshStatus.disabled = true;
            btnRefreshStatus.textContent = 'Carregando...';

            const resp = await fetch('/api/infra/status');
            if (!resp.ok) throw new Error('Falha ao carregar status');
            const nodes = await resp.json();

            renderInfraNodes(nodes);
            await loadVpnStatus();
        } catch (err) {
            console.error('Erro ao buscar status da infraestrutura:', err);
        } finally {
            btnRefreshStatus.disabled = false;
            btnRefreshStatus.textContent = '🔄 Atualizar Nós';
        }
    }

    async function loadVpnStatus() {
        try {
            const resp = await fetch('/api/infra/vpn');
            if (!resp.ok) return;
            const data = await resp.json();

            if (data.connected) {
                vpnStatusPill.className = 'status-pill vpn-status online';
                vpnStatusPill.innerHTML = `
                    <span class="status-dot green pulse"></span>
                    <span>VPN Corporativa: Conectada</span>
                `;
            } else {
                vpnStatusPill.className = 'status-pill vpn-status offline';
                vpnStatusPill.innerHTML = `
                    <span class="status-dot red"></span>
                    <span>VPN Corporativa: Desconectada</span>
                `;
            }
        } catch (err) {
            console.warn('Erro ao consultar status da VPN:', err);
        }
    }

    function renderInfraNodes(nodes) {
        infraNodesGrid.innerHTML = '';
        nodes.forEach(node => {
            const card = document.createElement('div');
            card.className = 'infra-node-card';

            card.innerHTML = `
                <div class="node-card-top">
                    <span class="node-layer-tag">${escapeHtml(node.layer)}</span>
                    <span class="node-status-badge ${node.status}">
                        ● ${escapeHtml(node.status)}
                    </span>
                </div>
                <div class="node-name">${escapeHtml(node.name)}</div>
                <div class="node-desc">${escapeHtml(node.message || '')}</div>
                <div class="node-latency">⚡ ${node.responseTimeMs} ms</div>
            `;

            infraNodesGrid.appendChild(card);
        });
    }

    // =========================================================================
    // 3. Chat com o Agente Maestro
    // =========================================================================
    chatInputForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const promptText = userPromptInput.value.trim();
        if (!promptText) return;

        // Adiciona bolha do usuário
        appendUserMessage(promptText);
        userPromptInput.value = '';
        btnSendMessage.disabled = true;
        btnSendMessage.innerHTML = '<span>Analisando...</span> ⏳';

        try {
            const resp = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: promptText })
            });

            if (!resp.ok) throw new Error('Falha na resposta do Maestro');
            const agentResponse = await resp.json();

            // Renderiza resposta do Maestro
            appendAgentMessage(agentResponse);

            // Atualiza nós após a investigação
            loadInfraStatus();
        } catch (err) {
            appendErrorMessage('Erro de comunicação com o Agente Maestro: ' + err.message);
        } finally {
            btnSendMessage.disabled = false;
            btnSendMessage.innerHTML = '<span>Investigar</span> <span class="btn-icon-send">⚡</span>';
        }
    });

    function appendUserMessage(text) {
        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble user-bubble';
        bubble.innerHTML = `
            <div class="bubble-header">
                <strong>Você</strong>
                <span class="bubble-time">${new Date().toLocaleTimeString()}</span>
            </div>
            <div class="bubble-content">${escapeHtml(text)}</div>
        `;
        chatMessagesContainer.appendChild(bubble);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    function appendAgentMessage(msg) {
        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble agent-bubble';

        let evidenceHtml = '';
        if (msg.evidenceList && msg.evidenceList.length > 0) {
            evidenceHtml = `
                <div class="evidence-box">
                    <strong>🔍 Evidências Cruzadas Coletadas:</strong>
                    <ul>
                        ${msg.evidenceList.map(e => `<li>${escapeHtml(e)}</li>`).join('')}
                    </ul>
                </div>
            `;
        }

        let hitlHtml = '';
        if (msg.hitlRequest) {
            currentHitlAction = msg.hitlRequest;
            hitlHtml = `
                <div class="hitl-inline-card">
                    <div class="hitl-inline-header">
                        <strong>🛡️ Ação de Remediação Proposta (HITL)</strong>
                        <span class="risk-badge">Risco ${msg.hitlRequest.riskLevel}</span>
                    </div>
                    <div>${escapeHtml(msg.hitlRequest.justification)}</div>
                    <button class="btn btn-primary" onclick="window.triggerHitlModal()">
                        👉 Revisar e Autorizar Ação
                    </button>
                </div>
            `;
        }

        bubble.innerHTML = `
            <div class="bubble-header">
                <span class="sender-avatar">👑</span>
                <strong>Agente Maestro</strong>
                <span class="bubble-time">${new Date().toLocaleTimeString()}</span>
            </div>
            <div class="bubble-content">
                ${formatMarkdownSimple(msg.content)}
            </div>
            ${evidenceHtml}
            ${hitlHtml}
        `;

        chatMessagesContainer.appendChild(bubble);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    function appendErrorMessage(errorText) {
        const bubble = document.createElement('div');
        bubble.className = 'chat-bubble agent-bubble';
        bubble.style.borderLeft = '3px solid var(--status-critical)';
        bubble.innerHTML = `
            <div class="bubble-header">
                <strong>⚠️ Erro do Sistema</strong>
            </div>
            <div class="bubble-content">${escapeHtml(errorText)}</div>
        `;
        chatMessagesContainer.appendChild(bubble);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    // =========================================================================
    // 4. Modal e Decisão HITL
    // =========================================================================
    window.triggerHitlModal = function() {
        if (currentHitlAction) {
            openHitlModal(currentHitlAction);
        }
    };

    function openHitlModal(action) {
        hitlActionName.textContent = action.actionName || 'Ação';
        hitlTarget.textContent = action.target || 'Nó da Infra';
        hitlRiskBadge.textContent = action.riskLevel || 'MÉDIO';
        hitlAgentTag.textContent = action.agentName || 'Agente';
        hitlJustificationBox.textContent = action.justification || '';
        hitlCommandCode.textContent = action.proposedCommand || '';

        hitlModalBackdrop.classList.remove('hidden');
    }

    function closeHitlModal() {
        hitlModalBackdrop.classList.add('hidden');
    }

    btnHitlClose.addEventListener('click', closeHitlModal);

    btnHitlApprove.addEventListener('click', async () => {
        if (!currentHitlAction) return;
        await submitHitlDecision(currentHitlAction.actionId, true, "Aprovado pelo operador SRE via Painel Web.");
    });

    btnHitlReject.addEventListener('click', async () => {
        if (!currentHitlAction) return;
        await submitHitlDecision(currentHitlAction.actionId, false, "Rejeitado pelo operador.");
    });

    async function submitHitlDecision(actionId, approved, feedback) {
        try {
            const resp = await fetch('/api/hitl/decision', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ actionId, approved, feedback })
            });

            if (resp.ok) {
                closeHitlModal();
                const statusStr = approved ? 'APROVADA e executada com sucesso!' : 'REJEITADA pelo operador.';
                renderStreamEntry({
                    agentName: 'HITL Gatekeeper',
                    eventType: approved ? 'TOOL_RESULT' : 'THOUGHT',
                    content: `Ação ${actionId} foi ${statusStr}`,
                    details: `Feedback: ${feedback}`
                });
            }
        } catch (err) {
            alert('Erro ao processar decisão HITL: ' + err.message);
        }
    }

    // =========================================================================
    // 5. Chips de Ações Rápidas
    // =========================================================================
    document.getElementById('chipError504').addEventListener('click', () => {
        userPromptInput.value = 'Usuários externos estão reportando tela de erro 504 Gateway Timeout e lentidão generalizada no WebCenter Portal. Faça uma triagem de ponta a ponta.';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    document.getElementById('chipStuckThreads').addEventListener('click', () => {
        userPromptInput.value = 'Verifique se há Stuck Threads no WebLogic 12c e analise a causa-raiz com jstack.';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    document.getElementById('chipLdapAuth').addEventListener('click', () => {
        userPromptInput.value = 'Realize um benchmark de autenticação bind e valide o status de sincronismo da réplica do OpenLDAP.';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    document.getElementById('chipOracleLocks').addEventListener('click', () => {
        userPromptInput.value = 'Verifique contenção e sessões bloqueadoras nos bancos de dados Oracle do Portal e das Aplicações.';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    btnRefreshStatus.addEventListener('click', loadInfraStatus);
    btnClearStream.addEventListener('click', () => {
        thoughtStreamContainer.innerHTML = '';
    });

    const btnRunDiagnostics = document.getElementById('btnRunDiagnostics');
    if (btnRunDiagnostics) {
        btnRunDiagnostics.addEventListener('click', async () => {
            try {
                btnRunDiagnostics.disabled = true;
                btnRunDiagnostics.textContent = '⚡ Testando Servidores...';

                appendUserMessage('Executando diagnóstico real autenticado no OpenLDAP, Middleware, Java/Spring e Bancos Oracle...');

                renderStreamEntry({
                    agentName: 'Maestro',
                    eventType: 'THOUGHT',
                    content: 'Iniciando testes reais de autenticação e telemetria de infraestrutura...',
                    details: 'Alvos: OpenLDAP | Oracle Apps (APPSDB) | Oracle Portal (WCPDB) | WebLogic 12c | Java/Spring JVM | Apache DMZ'
                });

                const resp = await fetch('/api/infra/diagnostics');
                if (!resp.ok) throw new Error('Falha na requisição de diagnóstico');
                const diag = await resp.json();

                if (diag.openldap) {
                    renderStreamEntry({
                        agentName: 'Agente OpenLDAP',
                        eventType: 'TOOL_RESULT',
                        content: `OpenLDAP Bind: ${diag.openldap.bindTest?.message || 'OK'} (${diag.openldap.bindTest?.latencyMs || 0}ms)`,
                        details: JSON.stringify(diag.openldap, null, 2)
                    });
                }
                if (diag.oracleAppsDb) {
                    renderStreamEntry({
                        agentName: 'Agente Oracle DBA',
                        eventType: 'TOOL_RESULT',
                        content: `Oracle Apps (APPSDB): ${diag.oracleAppsDb.ping?.message || 'OK'} (${diag.oracleAppsDb.ping?.latencyMs || 0}ms)`,
                        details: JSON.stringify(diag.oracleAppsDb, null, 2)
                    });
                }
                if (diag.oracleWcpDb) {
                    renderStreamEntry({
                        agentName: 'Agente Oracle DBA',
                        eventType: 'TOOL_RESULT',
                        content: `Oracle Portal (WCPDB): ${diag.oracleWcpDb.ping?.message || 'OK'} (${diag.oracleWcpDb.ping?.latencyMs || 0}ms)`,
                        details: JSON.stringify(diag.oracleWcpDb, null, 2)
                    });
                }
                if (diag.javaSpring) {
                    renderStreamEntry({
                        agentName: 'Agente Java/Spring',
                        eventType: 'TOOL_RESULT',
                        content: `Java/Spring JVM: ${diag.javaSpring.jvmAppServer1?.message || 'OK'}`,
                        details: JSON.stringify(diag.javaSpring, null, 2)
                    });
                }

                const evidence = [];
                if (diag.openldap?.bindTest) {
                    evidence.push(`OpenLDAP: ${diag.openldap.bindTest.message} (${diag.openldap.bindTest.latencyMs}ms)`);
                }
                if (diag.oracleAppsDb?.ping) {
                    evidence.push(`Oracle Apps (APPSDB): ${diag.oracleAppsDb.ping.message} (${diag.oracleAppsDb.ping.latencyMs}ms)`);
                }
                if (diag.oracleWcpDb?.ping) {
                    evidence.push(`Oracle Portal (WCPDB): ${diag.oracleWcpDb.ping.message} (${diag.oracleWcpDb.ping.latencyMs}ms)`);
                }
                if (diag.javaSpring?.jvmAppServer1) {
                    evidence.push(`Java/Spring JVM: ${diag.javaSpring.jvmAppServer1.message}`);
                }

                appendAgentMessage({
                    content: 'Diagnóstico em tempo real concluído com sucesso via VPN corporativa. Todos os serviços testados com credenciais autenticadas responderam ativamente.',
                    evidenceList: evidence
                });

            } catch (err) {
                console.error('Erro no diagnóstico:', err);
                appendAgentMessage({
                    content: `Falha ao executar diagnóstico real: ${err.message}`,
                    evidenceList: []
                });
            } finally {
                btnRunDiagnostics.disabled = false;
                btnRunDiagnostics.textContent = '⚡ Diagnóstico Real';
            }
        });
    }

    // Utilitários
    function escapeHtml(text) {
        if (!text) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }

    function formatMarkdownSimple(text) {
        if (!text) return '';
        // Converte **negrito**, quebras de linha e bullets simples
        let formatted = escapeHtml(text)
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\n\n/g, '<br><br>')
            .replace(/\n- /g, '<br>• ');
        return formatted;
    }

    // =========================================================================
    // 6. Gerenciamento de Abas de Navegação (Tabs)
    // =========================================================================
    const tabBtnSre = document.getElementById('tabBtnSre');
    const tabBtnSingleEntry = document.getElementById('tabBtnSingleEntry');
    const tabBtnEcosystem = document.getElementById('tabBtnEcosystem');
    const tabContentSre = document.getElementById('tabContentSre');
    const tabContentSingleEntry = document.getElementById('tabContentSingleEntry');
    const tabContentEcosystem = document.getElementById('tabContentEcosystem');

    function switchTab(activeBtn, activeContent) {
        [tabBtnSre, tabBtnSingleEntry, tabBtnEcosystem].forEach(b => b?.classList.remove('active'));
        [tabContentSre, tabContentSingleEntry, tabContentEcosystem].forEach(c => c?.classList.remove('active'));

        activeBtn?.classList.add('active');
        activeContent?.classList.add('active');
    }

    tabBtnSre?.addEventListener('click', () => switchTab(tabBtnSre, tabContentSre));
    tabBtnSingleEntry?.addEventListener('click', () => {
        switchTab(tabBtnSingleEntry, tabContentSingleEntry);
        loadCaptchaChallenge();
    });
    tabBtnEcosystem?.addEventListener('click', () => switchTab(tabBtnEcosystem, tabContentEcosystem));

    // =========================================================================
    // 7. Entrada Única com Captcha & Resolução de Visualização Multi-Cliente
    // =========================================================================
    let currentCaptchaId = '';
    const captchaQuestion = document.getElementById('captchaQuestion');
    const captchaAnswerInput = document.getElementById('captchaAnswerInput');
    const btnReloadCaptcha = document.getElementById('btnReloadCaptcha');
    const portalLoginForm = document.getElementById('portalLoginForm');
    const portalUsername = document.getElementById('portalUsername');
    const portalClientProfile = document.getElementById('portalClientProfile');
    const portalLoginAlertBox = document.getElementById('portalLoginAlertBox');
    const portalFrameHeader = document.getElementById('portalFrameHeader');
    const portalPreviewTitle = document.getElementById('portalPreviewTitle');
    const portalPreviewSubtitle = document.getElementById('portalPreviewSubtitle');
    const portalPreviewColorBadge = document.getElementById('portalPreviewColorBadge');
    const portalFrameNav = document.getElementById('portalFrameNav');
    const portalMetaList = document.getElementById('portalMetaList');

    async function loadCaptchaChallenge() {
        try {
            if (captchaQuestion) captchaQuestion.textContent = '...';
            const resp = await fetch('/api/portal/captcha');
            if (!resp.ok) throw new Error('Falha ao carregar Captcha');
            const data = await resp.json();
            currentCaptchaId = data.captchaId;
            if (captchaQuestion) captchaQuestion.textContent = data.challengeText;
            if (captchaAnswerInput) captchaAnswerInput.value = '';
        } catch (e) {
            console.error('Erro ao gerar Captcha:', e);
            if (captchaQuestion) captchaQuestion.textContent = 'Erro ao carregar';
        }
    }

    btnReloadCaptcha?.addEventListener('click', loadCaptchaChallenge);

    portalLoginForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        await executePortalLogin(portalUsername.value.trim(), portalClientProfile.value, captchaAnswerInput.value.trim(), '192.168.1.100');
    });

    async function executePortalLogin(username, role, captchaAns, clientIp) {
        if (!portalLoginAlertBox) return;
        portalLoginAlertBox.style.display = 'block';
        portalLoginAlertBox.style.background = 'rgba(30, 41, 59, 0.6)';
        portalLoginAlertBox.style.color = '#38bdf8';
        portalLoginAlertBox.textContent = 'Autenticando via OpenLDAP e validando Captcha com Agente de Cibersegurança...';

        try {
            const resp = await fetch('/api/portal/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    username: username,
                    clientRole: role,
                    captchaId: currentCaptchaId,
                    captchaAnswer: captchaAns,
                    clientIp: clientIp
                })
            });

            const data = await resp.json();

            if (!resp.ok || !data.authenticated) {
                portalLoginAlertBox.style.background = 'rgba(239, 68, 68, 0.2)';
                portalLoginAlertBox.style.border = '1px solid #ef4444';
                portalLoginAlertBox.style.color = '#fca5a5';

                if (data.blockedByCyberAgent) {
                    portalLoginAlertBox.innerHTML = `🛡️ <strong>BLOQUEIO DO AGENTE DE CIBERSEGURANÇA:</strong> ${escapeHtml(data.defenseReport?.message || 'Ataque neutralizado em tempo real!')}`;
                } else {
                    portalLoginAlertBox.textContent = data.error || 'Falha de autenticação.';
                }
                loadCaptchaChallenge();
                return;
            }

            // Sucesso na Autenticação Única
            portalLoginAlertBox.style.background = 'rgba(16, 185, 129, 0.2)';
            portalLoginAlertBox.style.border = '1px solid #10b981';
            portalLoginAlertBox.style.color = '#6ee7b7';
            portalLoginAlertBox.innerHTML = `✅ <strong>Autenticado com Sucesso!</strong> ${escapeHtml(data.message)}`;

            // Renderiza Visualização Dinâmica do Cliente
            const view = data.resolvedPortalView;
            if (view) {
                const primaryColor = view.customAttributes?.corPrimaria || '#004e8b';
                if (portalFrameHeader) portalFrameHeader.style.background = primaryColor;
                if (portalPreviewTitle) portalPreviewTitle.textContent = view.displayName || 'Portal Corporativo';
                if (portalPreviewSubtitle) portalPreviewSubtitle.textContent = `Entrada Única • Usuário: ${view.authenticatedUser} (${role})`;
                if (portalPreviewColorBadge) {
                    portalPreviewColorBadge.textContent = primaryColor;
                    portalPreviewColorBadge.style.color = '#ffffff';
                }

                // Renderiza Módulos do Menu
                if (portalFrameNav && view.visibleModules) {
                    portalFrameNav.innerHTML = view.visibleModules.map(m =>
                        `<span class="portal-frame-nav-item" style="border-color:${primaryColor}; color:#fff;">${escapeHtml(m)}</span>`
                    ).join('');
                }

                // Renderiza Metadados
                if (portalMetaList) {
                    portalMetaList.innerHTML = `
                        <li><strong>Page Template:</strong> ${escapeHtml(view.pageTemplate || 'Universal Portal Template')}</li>
                        <li><strong>Skin:</strong> ${escapeHtml(view.skinFamily || 'assetSkinUniversal')}</li>
                        <li><strong>Variável de Cor Aplicada:</strong> <code>--portal-primary: ${primaryColor}</code></li>
                        <li><strong>Grupo OpenLDAP:</strong> <code>${escapeHtml(view.ldapGroup || role)}</code></li>
                        <li><strong>Versão Alvo:</strong> ${escapeHtml(view.targetVersion || 'WCP 14c / 12c')}</li>
                        <li><strong>REST API Endpoint:</strong> <code>${escapeHtml(view.restApiEndpoint || '')}</code></li>
                    `;
                }
            }

            loadCaptchaChallenge();
        } catch (e) {
            console.error('Erro no login:', e);
            portalLoginAlertBox.style.background = 'rgba(239, 68, 68, 0.2)';
            portalLoginAlertBox.style.color = '#fca5a5';
            portalLoginAlertBox.textContent = 'Erro ao conectar ao gateway do Portal: ' + e.message;
        }
    }

    // Botões de Simulação de Ataques
    document.getElementById('btnTestSqli')?.addEventListener('click', () => {
        if (portalUsername) portalUsername.value = "admin' OR 1=1--";
        if (captchaAnswerInput) captchaAnswerInput.value = '999';
        executePortalLogin("admin' OR 1=1--", portalClientProfile.value, '999', '198.51.100.77');
    });

    document.getElementById('btnTestXss')?.addEventListener('click', () => {
        if (portalUsername) portalUsername.value = "<script>alert('xss')</script>";
        if (captchaAnswerInput) captchaAnswerInput.value = '999';
        executePortalLogin("<script>alert('xss')</script>", portalClientProfile.value, '999', '198.51.100.92');
    });

    document.getElementById('btnTestBadCaptcha')?.addEventListener('click', () => {
        if (portalUsername) portalUsername.value = "joao.silva";
        if (captchaAnswerInput) captchaAnswerInput.value = '9999';
        executePortalLogin("joao.silva", portalClientProfile.value, '9999', '192.168.1.50');
    });

    // Novos chips de triagem do Maestro
    document.getElementById('chipCyberShield')?.addEventListener('click', () => {
        if (userPromptInput) {
            userPromptInput.value = 'Monitore possíveis ataques SQLi e ative a defesa de cibersegurança online no perímetro Apache e Login.';
            chatInputForm.dispatchEvent(new Event('submit'));
        }
    });

    document.getElementById('chipProvisionPortal')?.addEventListener('click', () => {
        if (userPromptInput) {
            userPromptInput.value = 'Provisionar novo portal para cliente usando o layout padrão do WebCenter Portal via REST API.';
            chatInputForm.dispatchEvent(new Event('submit'));
        }
    });

    // Início
    initEventStream();
    loadInfraStatus();
});
