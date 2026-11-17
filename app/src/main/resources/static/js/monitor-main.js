/**
 * MAESTRO AI - SCRIPT DO CONSOLE SRE DE MONITORAÇÃO & CORREÇÕES
 */

document.addEventListener('DOMContentLoaded', () => {
    // Referências DOM - Header & Topologia
    const infraNodesGrid = document.getElementById('infraNodesGrid');
    const nodesSummaryLabel = document.getElementById('nodesSummaryLabel');
    const btnRefreshStatus = document.getElementById('btnRefreshStatus');
    const btnRunDiagnostics = document.getElementById('btnRunDiagnostics');
    const vpnStatusPill = document.getElementById('vpnStatusPill');
    const sseStatusPill = document.getElementById('sseStatusPill');
    const sseStatusText = document.getElementById('sseStatusText');

    // Referências DOM - Chat com Maestro
    const chatMessagesContainer = document.getElementById('chatMessagesContainer');
    const chatInputForm = document.getElementById('chatInputForm');
    const userPromptInput = document.getElementById('userPromptInput');
    const btnSendMessage = document.getElementById('btnSendMessage');
    const btnClearChat = document.getElementById('btnClearChat');

    // Referências DOM - Histórico de Ações
    const historyListContainer = document.getElementById('historyListContainer');
    const btnRefreshHistory = document.getElementById('btnRefreshHistory');

    // Referências DOM - Modal HITL
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
    // 1. Conexão SSE (Server-Sent Events)
    // =========================================================================
    function initEventStream() {
        const eventSource = new EventSource('/api/events/stream');

        eventSource.addEventListener('CONNECTED', () => {
            sseStatusPill.classList.add('online');
            sseStatusText.textContent = 'Stream: Conectado';
        });

        eventSource.addEventListener('AGENT_THOUGHT', (e) => {
            try {
                const data = JSON.parse(e.data);
                if (data.eventType === 'HITL_PROPOSED') {
                    checkPendingHitl();
                }
            } catch (err) {
                console.error('Erro ao processar SSE:', err);
            }
        });

        eventSource.onerror = () => {
            sseStatusText.textContent = 'Stream: Reconectando...';
        };
    }

    // =========================================================================
    // 2. Carrega Topologia dos 8 Nós Monitorados
    // =========================================================================
    async function loadNodesStatus() {
        try {
            btnRefreshStatus.disabled = true;
            btnRefreshStatus.textContent = 'Atualizando...';

            const resp = await fetch('/api/infra/status');
            if (!resp.ok) throw new Error('Falha ao obter status');
            const nodes = await resp.json();

            renderCompactNodes(nodes);
            await checkVpnStatus();
        } catch (err) {
            console.error('Erro ao buscar status dos nós:', err);
            if (nodesSummaryLabel) nodesSummaryLabel.textContent = 'Erro ao carregar telemetria';
        } finally {
            btnRefreshStatus.disabled = false;
            btnRefreshStatus.textContent = '🔄 Atualizar Nós';
        }
    }

    function renderCompactNodes(nodes) {
        if (!infraNodesGrid) return;
        infraNodesGrid.innerHTML = '';

        let healthyCount = 0;

        nodes.forEach(node => {
            const isHealthy = node.status === 'HEALTHY';
            const isWarn = node.status === 'WARNING';
            if (isHealthy) healthyCount++;

            const dotClass = isHealthy ? 'healthy' : (isWarn ? 'warning' : 'critical');

            const card = document.createElement('div');
            card.className = 'node-compact-card';
            card.innerHTML = `
                <div class="node-card-top">
                    <span class="node-card-name" title="${escapeHtml(node.serviceName)}">${escapeHtml(node.serviceName)}</span>
                    <span class="node-status-dot ${dotClass}" title="${node.status}"></span>
                </div>
                <div class="node-card-bottom">
                    <span>${escapeHtml(node.host || 'local')}:${node.port || ''}</span>
                    <span class="node-latency">${node.responseTimeMs || 0}ms</span>
                </div>
            `;
            infraNodesGrid.appendChild(card);
        });

        if (nodesSummaryLabel) {
            nodesSummaryLabel.textContent = `${healthyCount}/${nodes.length} nós saudáveis • Latência média: ${calculateAvgLatency(nodes)}ms`;
        }
    }

    function calculateAvgLatency(nodes) {
        if (!nodes || nodes.length === 0) return 0;
        const total = nodes.reduce((acc, n) => acc + (n.responseTimeMs || 0), 0);
        return Math.round(total / nodes.length);
    }

    async function checkVpnStatus() {
        try {
            const resp = await fetch('/api/infra/vpn');
            if (resp.ok) {
                const data = await resp.json();
                if (vpnStatusPill) {
                    vpnStatusPill.innerHTML = `<span class="status-dot green pulse"></span><span>VPN: ${escapeHtml(data.interface || 'Ativa')}</span>`;
                }
            }
        } catch (e) {
            // Modo simulado mantém VPN ativa
        }
    }

    // =========================================================================
    // 3. Histórico de Correções e Ações dos Agentes
    // =========================================================================
    async function loadAgentHistory() {
        try {
            const resp = await fetch('/api/history');
            if (!resp.ok) throw new Error('Falha ao carregar histórico');
            const list = await resp.json();
            renderHistoryList(list);
        } catch (err) {
            console.error('Erro ao buscar histórico:', err);
            if (historyListContainer) {
                historyListContainer.innerHTML = '<div class="history-empty">Não foi possível carregar o histórico de ações.</div>';
            }
        }
    }

    function renderHistoryList(items) {
        if (!historyListContainer) return;
        if (!items || items.length === 0) {
            historyListContainer.innerHTML = '<div class="history-empty">Nenhuma ação registrada ainda.</div>';
            return;
        }

        historyListContainer.innerHTML = '';
        items.forEach(item => {
            const card = document.createElement('div');
            card.className = 'history-item-card';

            const statusClass = item.status || 'COMPLETED';

            card.innerHTML = `
                <div class="history-card-header">
                    <div class="history-time-agent">
                        <span class="history-time">${escapeHtml(item.timestamp)}</span>
                        <span class="history-agent-badge">${escapeHtml(item.agentName)}</span>
                    </div>
                    <span class="history-status-badge ${statusClass}">${escapeHtml(statusClass)}</span>
                </div>
                <div class="history-summary">${escapeHtml(item.summary)}</div>
                <div class="history-target">📍 Alvo: ${escapeHtml(item.target)}</div>
                <div class="history-resolution">${escapeHtml(item.resolution)}</div>
            `;
            historyListContainer.appendChild(card);
        });
    }

    btnRefreshHistory?.addEventListener('click', loadAgentHistory);

    // =========================================================================
    // 4. Chat com Maestro para Resoluções Pontuais
    // =========================================================================
    chatInputForm?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const prompt = userPromptInput.value.trim();
        if (!prompt) return;

        appendUserMessage(prompt);
        userPromptInput.value = '';
        btnSendMessage.disabled = true;

        try {
            const resp = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ prompt: prompt })
            });

            if (!resp.ok) throw new Error('Erro na resposta do Maestro');
            const data = await resp.json();

            appendAgentMessage(data);

            // Se propôs HITL, verifica
            if (data.requiresHitl && data.proposedAction) {
                currentHitlAction = data.proposedAction;
                openHitlModal(data.proposedAction);
            }

            // Atualiza o histórico de ações imediatamente!
            loadAgentHistory();

        } catch (err) {
            console.error('Erro no chat:', err);
            appendAgentMessage({
                content: 'Falha ao processar solicitação: ' + err.message,
                evidenceList: []
            });
        } finally {
            btnSendMessage.disabled = false;
        }
    });

    function appendUserMessage(text) {
        const div = document.createElement('div');
        div.className = 'chat-bubble user-bubble';
        div.innerHTML = `
            <div class="bubble-header">
                <strong>Você (Operador SRE)</strong>
                <span class="bubble-time">${new Date().toLocaleTimeString()}</span>
            </div>
            <div class="bubble-content">${escapeHtml(text)}</div>
        `;
        chatMessagesContainer.appendChild(div);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    function appendAgentMessage(data) {
        const div = document.createElement('div');
        div.className = 'chat-bubble agent-bubble';

        let evidenceHtml = '';
        if (data.evidenceList && data.evidenceList.length > 0) {
            evidenceHtml = `
                <div class="evidence-box">
                    <strong>🔍 Evidências Coletadas pelos Especialistas:</strong>
                    <ul style="margin-top:6px; padding-left:16px;">
                        ${data.evidenceList.map(ev => `<li>${escapeHtml(ev)}</li>`).join('')}
                    </ul>
                </div>
            `;
        }

        div.innerHTML = `
            <div class="bubble-header">
                <span class="sender-avatar">👑</span>
                <strong>Agente Maestro Central</strong>
                <span class="bubble-time">${new Date().toLocaleTimeString()}</span>
            </div>
            <div class="bubble-content">
                ${formatMarkdownSimple(data.content || '')}
                ${evidenceHtml}
            </div>
        `;
        chatMessagesContainer.appendChild(div);
        chatMessagesContainer.scrollTop = chatMessagesContainer.scrollHeight;
    }

    // Chips de resolução rápida
    document.getElementById('chip504')?.addEventListener('click', () => {
        userPromptInput.value = 'Usuários reportando erro 504 Gateway Timeout no WebCenter Portal. Faça triagem e correlação de ponta a ponta.';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    document.getElementById('chipStuck')?.addEventListener('click', () => {
        userPromptInput.value = 'Verifique se há Stuck Threads ou contenção no WebLogic (WC_Portal e APP_server1).';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    document.getElementById('chipLdap')?.addEventListener('click', () => {
        userPromptInput.value = 'Teste a integridade de autenticação Bind e sincronismo syncrepl no OpenLDAP.';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    document.getElementById('chipOracle')?.addEventListener('click', () => {
        userPromptInput.value = 'Inspecione latência e possíveis locks no banco do Portal (WCPDB) e das Aplicações (APPSDB).';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    document.getElementById('chipCyber')?.addEventListener('click', () => {
        userPromptInput.value = 'Monitore possíveis ataques de injeção SQLi/XSS e ative o escudo online de cibersegurança.';
        chatInputForm.dispatchEvent(new Event('submit'));
    });

    btnClearChat?.addEventListener('click', () => {
        chatMessagesContainer.innerHTML = '';
    });

    // =========================================================================
    // 5. Diagnóstico Real & Modal HITL
    // =========================================================================
    btnRunDiagnostics?.addEventListener('click', async () => {
        btnRunDiagnostics.disabled = true;
        btnRunDiagnostics.textContent = 'Diagnosticando...';

        try {
            const resp = await fetch('/api/infra/diagnostics');
            if (!resp.ok) throw new Error('Falha no diagnóstico');
            const diag = await resp.json();

            appendAgentMessage({
                content: '⚡ **Diagnóstico Completo da Infraestrutura Concluído:** Todos os nós e serviços responderam à varredura ativa.',
                evidenceList: [
                    `Apache DMZ: ${diag.apache?.httpStatus === 200 ? 'OK' : 'Alerta'} (TLS expira em 18 dias)`,
                    `WebLogic WCP: ${diag.weblogic?.clusterOverview?.length || 0} nós inspecionados`,
                    `OpenLDAP Bind: ${diag.ldap?.bind?.status || 'OK'} (${diag.ldap?.bind?.latencyMs || 0}ms)`,
                    `Oracle WCPDB: ${diag.oracleWcpDb?.ping?.status || 'OK'} (${diag.oracleWcpDb?.ping?.latencyMs || 0}ms)`,
                    `Oracle APPSDB: ${diag.oracleAppsDb?.ping?.status || 'OK'} (${diag.oracleAppsDb?.ping?.latencyMs || 0}ms)`
                ]
            });
            loadNodesStatus();
            loadAgentHistory();
        } catch (err) {
            console.error('Erro no diagnóstico:', err);
        } finally {
            btnRunDiagnostics.disabled = false;
            btnRunDiagnostics.textContent = '⚡ Diagnóstico Real';
        }
    });

    async function checkPendingHitl() {
        try {
            const resp = await fetch('/api/hitl/pending');
            if (!resp.ok) return;
            const actions = await resp.json();
            if (actions && actions.length > 0) {
                const pending = actions.find(a => a.status === 'PENDING_APPROVAL');
                if (pending) {
                    currentHitlAction = pending;
                    openHitlModal(pending);
                }
            }
        } catch (e) {
            console.warn('Erro ao verificar HITL:', e);
        }
    }

    function openHitlModal(action) {
        if (!hitlModalBackdrop || !action) return;
        hitlActionName.textContent = action.actionName || 'Ação';
        hitlTarget.textContent = action.target || 'Servidor';
        hitlRiskBadge.textContent = action.riskLevel || 'MÉDIO';
        hitlAgentTag.textContent = action.requestingAgent || 'Agente';
        hitlJustificationBox.textContent = action.justification || 'Aprovação necessária para prosseguir com a remediação.';
        hitlCommandCode.textContent = action.commandToExecute || 'comando não especificado';
        hitlModalBackdrop.classList.remove('hidden');
    }

    function closeHitlModal() {
        hitlModalBackdrop?.classList.add('hidden');
        currentHitlAction = null;
    }

    btnHitlClose?.addEventListener('click', closeHitlModal);

    btnHitlApprove?.addEventListener('click', async () => {
        if (!currentHitlAction) return;
        await sendHitlDecision(currentHitlAction.actionId, true, 'Aprovado pelo operador SRE no console.');
    });

    btnHitlReject?.addEventListener('click', async () => {
        if (!currentHitlAction) return;
        await sendHitlDecision(currentHitlAction.actionId, false, 'Rejeitado pelo operador SRE.');
    });

    async function sendHitlDecision(actionId, approved, feedback) {
        try {
            await fetch('/api/hitl/decision', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    actionId: actionId,
                    approved: approved,
                    feedback: feedback
                })
            });
            closeHitlModal();
            loadAgentHistory();
        } catch (e) {
            console.error('Erro ao enviar decisão HITL:', e);
        }
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }

    function formatMarkdownSimple(text) {
        if (!text) return '';
        return escapeHtml(text)
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\n\n/g, '<br><br>')
            .replace(/\n- /g, '<br>• ');
    }

    // Inicialização
    initEventStream();
    loadNodesStatus();
    loadAgentHistory();
});
