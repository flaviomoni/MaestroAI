---
tags:
  - ciberseguranca
  - waf
  - defesa-online
  - aprendizado-agente
agent: Agente Ciberseguranca
---

# 🛡️ Memória de Aprendizado: Especialista em Cibersegurança (Defesa Online)

## 1. Diretrizes de Defesa Ativa Online
O **Agente Especialista em Cibersegurança** monitora continuamente o perímetro Apache DMZ, o endpoint de Login Único com Captcha e as tentativas de autenticação no OpenLDAP.

### Ações de Defesa Online Automática (Auto-Shield)
| Vetor de Ataque Detectado | Assinatura / Indicador | Defesa Online Imediata (Em Memória / Filtro) | Orientação de Hardening Definitivo (Apache / Firewall) |
| :--- | :--- | :--- | :--- |
| **Brute-Force Login / LDAP** | > 5 falhas de senha ou Captcha inválido em 60s do mesmo IP | Bloqueio imediato do IP na `ActiveThreatBlocklist` por 30 min + invalidação de sessão | Aplicar `mod_evasive` no Apache DMZ e política `pwdMaxFailure: 5` no OpenLDAP (`ppolicy`). |
| **SQL Injection (SQLi)** | Payloads contendo `' OR 1=1`, `UNION SELECT`, `--`, `DBMS_PIPE` | Intercepção HTTP 403 imediata + inclusão automática do IP atacante na Blacklist | Ativar regra OWASP CRS `942100` no ModSecurity do Apache DMZ. |
| **Cross-Site Scripting (XSS)** | Payloads contendo `<script>`, `javascript:`, `onerror=`, `onload=` | Sanitização/Bloqueio HTTP 403 imediato + alerta no stream SSE | Forçar header `Content-Security-Policy: default-src 'self'` no Apache DMZ. |
| **Path Traversal / LFI** | Tentativas de acesso a `../`, `/etc/passwd`, `/WEB-INF/web.xml` | Bloqueio instantâneo do IP + bloqueio de rota no Proxy Reverso | Restringir `<LocationMatch "(?i).*(WEB-INF|META-INF).*">` com `Require all denied` no Apache. |
| **Bypass de Console WebLogic** | Acesso externo a `/console`, `/em` ou `/management` via DMZ | Bloqueio imediato no Reverse Proxy (HTTP 403) | Garantir que `/console` e `:17001` respondam apenas na sub-rede interna de gerência. |

---

## 2. Histórico de Aprendizados Registrados pelos Agentes
*(Novos incidentes e ataques mitigados são anexados automaticamente abaixo pelo `RAGService`)*

- **[2026-11-13 | BASELINE]** Configurado escudo online (`CyberSecuritySkill`) com inspeção de payloads SQLi/XSS/PathTraversal, validação obrigatória de Captcha na tela de login único e bloqueio dinâmico de IPs maliciosos.

- **[2026-11-13 13:03 | Agente Cibersegurança]** **Caso:** Monitore possíveis ataques SQLi e ative defesa de cibersegurança online → **Solução/Defesa Aplicada:** Escudo Online Ativo: Ameaça SQL_INJECTION (OWASP A03) originada de 198.51.100.77 foi neutralizada em tempo real (3 IPs bloqueados no escudo).
- **[2026-11-13 13:46 | Agente Cibersegurança]** **Caso:** Monitore possíveis ataques SQLi e ative defesa de cibersegurança online → **Solução/Defesa Aplicada:** Escudo Online Ativo: Ameaça SQL_INJECTION (OWASP A03) originada de 198.51.100.77 foi neutralizada em tempo real (3 IPs bloqueados no escudo).
- **[2026-11-13 13:51 | Agente Cibersegurança]** **Caso:** Monitore possíveis ataques SQLi e ative defesa de cibersegurança online → **Solução/Defesa Aplicada:** Escudo Online Ativo: Ameaça SQL_INJECTION (OWASP A03) originada de 198.51.100.77 foi neutralizada em tempo real (3 IPs bloqueados no escudo).
- **[2026-11-17 10:51 | Agente Cibersegurança]** **Caso:** Monitore possíveis ataques SQLi e ative defesa de cibersegurança online → **Solução/Defesa Aplicada:** Escudo Online Ativo: Ameaça SQL_INJECTION (OWASP A03) originada de 198.51.100.77 foi neutralizada em tempo real (3 IPs bloqueados no escudo).
- **[2026-11-17 10:58 | Agente Cibersegurança]** **Caso:** Tentativa de injeção no login único (/api/portal/login): <script>alert('xss')</script> → **Solução/Defesa Aplicada:** Escudo Online Ativo: Ameaça CROSS_SITE_SCRIPTING_XSS (OWASP A03) originada de 198.51.100.92 foi neutralizada em tempo real (3 IPs bloqueados no escudo).
- **[2026-11-17 10:58 | Agente Cibersegurança]** **Caso:** Tentativa de injeção no login único (/api/portal/login): admin' OR 1=1-- → **Solução/Defesa Aplicada:** Escudo Online Ativo: Ameaça SQL_INJECTION (OWASP A03) originada de 198.51.100.77 foi neutralizada em tempo real (4 IPs bloqueados no escudo).
- **[2026-11-17 11:04 | Agente Cibersegurança]** **Caso:** Monitore possíveis ataques SQLi e ative defesa de cibersegurança online → **Solução/Defesa Aplicada:** Escudo Online Ativo: Ameaça SQL_INJECTION (OWASP A03) originada de 198.51.100.77 foi neutralizada em tempo real (3 IPs bloqueados no escudo).