---
tags:
  - webcenter-portal
  - layout-padrao
  - rest-api
  - multi-tenant
agent: Agente WebCenter Portal
---

# 🎨 Memória de Aprendizado: Especialista em WebCenter Portal & Layout Padrão

## 1. Padrão Obrigatório de Layout para Novos Portais
Todo novo portal provisionado pelo **Agente Especialista em WebCenter Portal** DEVE utilizar como padrão os artefatos já homologados no diretório local:
- **Diretório Base:** `layout/portal-universal-template`
- **Pacote AAR (Page Template):** `assetPageTemplateUniversal/deploy/portal_universal_pagetemplate.aar`
- **Código-Fonte JSPX:** `assetPageTemplateUniversal/public_html/portal-universal/Portal-Universal.jspx`
- **Skin Padrão:** `assetSkinUniversal`
- **Script Empacotador:** `create_aar.py` (Validação XML estrita sem BOM UTF-8 `\xef\xbb\xbf`).

---

## 2. Entrada Única com Visualizações Distintas por Cliente (Multi-Tenant Views)
Não duplicamos o arquivo `.jspx` para cada cliente. O template universal `Portal-Universal.jspx` adapta-se dinamicamente a cada perfil de cliente através dos **Atributos Customizados do Portal** combinados com os grupos do **OpenLDAP**:

| Perfil de Cliente (Grupo OpenLDAP) | Portal / Visão Alvo | Atributo `corPrimaria` (`--portal-primary`) | Atributo `formatoData` | Módulos Visíveis no Menu (`<af:iterator>`) |
| :--- | :--- | :--- | :--- | :--- |
| `cn=CLIENT_ENTERPRISE` | Portal Corporativo Enterprise | `#004e8b` (Azul Corporativo) | `auto` | Dashboard SLA, Topologia Dedicada, Faturas, Trouble Ticket |
| `cn=CLIENT_PREMIUM` | Portal Clientes Premium / Gold | `#b71c1c` (Vermelho Executivo) | `auto` | Painel de Serviços, Relatórios Executivos, Suporte Prioritário |
| `cn=CLIENT_PARTNER` | Portal Parceiros & Integradores | `#1b5e20` (Verde Parceiro) | `apenas_data` | Catálogo de APIs, Provisionamento, Documentação Técnica |
| `cn=PORTAL_ADMIN` | Visão Global de Administração | `#311b92` (Roxo Admin) | `auto` | Administração Geral, Segurança, Todos os Portais e Diagnósticos |

---

## 3. Uso das REST APIs Nativas do WebCenter Portal (14.1.2 / 12.2.1.4)
Para garantir automação no desenvolvimento e provisionamento:
- **Listagem e Descoberta (HATEOAS):** `GET /rest/api/v1/resourceIndex`
- **Provisionamento de Novo Portal:** `POST /rest/api/v1/portals` (Header `X-Requested-By: Maestro-AI-Agent`, `Content-Type: application/json`)
- **Aplicação do Page Template Padrão:** `PATCH /rest/api/v1/portals/{portalName}` definindo `pageTemplate: "Universal Portal Template"` e atributos customizados (`corPrimaria`, `urlPortal`, `formatoData`).
- **Gerenciamento de Membros/Perfis:** `POST /rest/api/v1/portals/{portalName}/members` vinculando os grupos do OpenLDAP.

---

## 4. Histórico de Aprendizados Registrados pelos Agentes
*(Novos provisionamentos e aprendizados são anexados automaticamente abaixo pelo `RAGService`)*

- **[2026-11-13 | BASELINE]** Vinculado o `WebCenterPortalSkill` ao diretório `layout/portal-universal-template` para validar o pacote `.aar` e provisionar novos portais multi-cliente com entrada única.

- **[2026-11-13 13:03 | Squad Provisionamento (WCP + WLS + LDAP + DBA)]** **Caso:** Provisionar novo portal para cliente usando o layout padrão do WebCenter → **Solução/Defesa Aplicada:** Portal 'Portal Único Multi-Cliente v2' (portal-corp-v2) provisionado via REST API com layout padrão 'Universal Portal Template', corPrimaria=#004e8b e grupo LDAP=CLIENT_ENTERPRISE.
- **[2026-11-13 13:46 | Squad Provisionamento (WCP + WLS + LDAP + DBA)]** **Caso:** Provisionar novo portal para cliente usando o layout padrão do WebCenter → **Solução/Defesa Aplicada:** Portal 'Portal Único Multi-Cliente v2' (portal-corp-v2) provisionado via REST API com layout padrão 'Universal Portal Template', corPrimaria=#004e8b e grupo LDAP=CLIENT_ENTERPRISE.
- **[2026-11-13 13:51 | Squad Provisionamento (WCP + WLS + LDAP + DBA)]** **Caso:** Provisionar novo portal para cliente usando o layout padrão do WebCenter → **Solução/Defesa Aplicada:** Portal 'Portal Único Multi-Cliente v2' (portal-corp-v2) provisionado via REST API com layout padrão 'Universal Portal Template', corPrimaria=#004e8b e grupo LDAP=CLIENT_ENTERPRISE.
- **[2026-11-17 10:51 | Squad Provisionamento (WCP + WLS + LDAP + DBA)]** **Caso:** Provisionar novo portal para cliente usando o layout padrão do WebCenter → **Solução/Defesa Aplicada:** Portal 'Portal Único Multi-Cliente v2' (portal-corp-v2) provisionado via REST API com layout padrão 'Universal Portal Template', corPrimaria=#004e8b e grupo LDAP=CLIENT_ENTERPRISE.
- **[2026-11-17 11:04 | Squad Provisionamento (WCP + WLS + LDAP + DBA)]** **Caso:** Provisionar novo portal para cliente usando o layout padrão do WebCenter → **Solução/Defesa Aplicada:** Portal 'Portal Único Multi-Cliente v2' (portal-corp-v2) provisionado via REST API com layout padrão 'Universal Portal Template', corPrimaria=#004e8b e grupo LDAP=CLIENT_ENTERPRISE.