---
tags:
  - weblogic
  - oracle-dba
  - openldap
  - linux-logs
  - monitoramento
agent: Squad Infraestrutura
---

# ⚙️ Memória de Aprendizado: Middleware, WebLogic, Oracle DBA, OpenLDAP & Linux

## 1. Provisionamento e Operação no WebLogic (14c / 12.2.1.4)
- **Criar Novo Managed Server via REST Management API:**
  - Endpoint: `POST /management/weblogic/latest/edit/servers` (após iniciar sessão de edição em `/management/weblogic/latest/edit/changeManager/startEdit`).
  - Ativação das alterações: `POST /management/weblogic/latest/edit/changeManager/activate`.
- **Criar / Ajustar JDBC DataSource:**
  - Endpoint: `POST /management/weblogic/latest/edit/JDBCSystemResources` apontando para `WCPDB` (Banco do Portal) ou `APPSDB` (Banco das Aplicações).

## 2. Provisionamento no Banco de Dados Oracle (`WCPDB` e `APPSDB`)
- **Banco WebCenter Portal (`WCPDB`):** Dedicado aos schemas do RCU (`_MDS`, `_WEBCENTER`, `_OPSS`).
- **Banco das Aplicações (`APPSDB`):** Dedicado aos schemas das aplicações Spring Boot e portlets corporativos.
- **Script Padrão de Provisionamento de Novo Schema de Aplicação:**
  ```sql
  CREATE USER APP_PORTAL_V2 IDENTIFIED BY "<SENHA_FORTE>" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA UNLIMITED ON USERS;
  GRANT CONNECT, RESOURCE, CREATE VIEW, CREATE SYNONYM TO APP_PORTAL_V2;
  ```

## 3. Autenticação e Autorização no OpenLDAP
- **Estrutura da Árvore LDAP para Entrada Única Multi-Cliente:**
  - `ou=Users,dc=empresa,dc=com,dc=br`: Contas individuais dos usuários (`uid=...`).
  - `ou=ClientProfiles,dc=empresa,dc=com,dc=br`: Grupos que determinam a visualização do portal (`cn=CLIENT_ENTERPRISE`, `cn=CLIENT_PREMIUM`, `cn=CLIENT_PARTNER`, `cn=PORTAL_ADMIN`).

## 4. Monitoramento de Logs Linux & Observabilidade SRE
- **Arquivos Críticos Monitorados pelo `LinuxLogSkill`:**
  - Apache DMZ: `/var/log/httpd/access_log` e `/var/log/httpd/error_log`
  - WebLogic / WebCenter: `$DOMAIN_HOME/servers/WC_Portal_server1/logs/WC_Portal_server1.out` e `WC_Portal_server1-diagnostic.log`
- **Regras de Ouro para Erros `WCS#` no Log do WebCenter:**
  - Sempre correlacionar o timestamp do código `WCS#YYYY.MM.DD.HH.MM.SS` no arquivo `WC_Portal_server1-diagnostic.log` para identificar a causa-raiz exata no ADF/MDS.

---

## 5. Histórico de Aprendizados Registrados pelos Agentes
*(Novos aprendizados operacionais são anexados automaticamente abaixo pelo `RAGService`)*

- **[2026-11-13 | BASELINE]** Estruturada a base de conhecimento Local-First para WebLogic, Oracle DBA, OpenLDAP, Monitoração SRE e Linux Logs.

- **[2026-11-13 13:03 | Maestro (Correlação 10 Agentes)]** **Caso:** Usuários reportando erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-13 13:03 | Agente Java/Spring]** **Caso:** Verifique memória JVM, Heap e Actuator das aplicações Spring → **Solução/Defesa Aplicada:** JVM APP_server1 saudável: OldGen 58.7%, Metaspace 71.0%, Full GC: 3
- **[2026-11-13 13:03 | Squad Arquitetura, Monitoração & Linux]** **Caso:** Avaliar arquitetura de middleware, monitoração SLA e logs Linux → **Solução/Defesa Aplicada:** Especialista Middleware: Arquitetura Oracle Middleware (Cluster 2 Nós + Dual DB + DMZ Apache) validada com alta disponibilidade e afinidade de sessão ativa. | Especialista Monitoração: Monitoração SRE: 7/8 nós saudáveis | Latência média: 49ms | Disponibilidade estimada: 93,75%. | Especialista Linux: Linux Logs: 0 erros internos WCS# no Portal; 2 alertas BEA-000337 em APP_server1.out; partição /u01 saudável (62%).
- **[2026-11-13 13:03 | Maestro (Correlação 10 Agentes)]** **Caso:** Erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-13 13:46 | Maestro (Correlação 10 Agentes)]** **Caso:** Usuários reportando erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-13 13:46 | Agente Java/Spring]** **Caso:** Verifique memória JVM, Heap e Actuator das aplicações Spring → **Solução/Defesa Aplicada:** JVM APP_server1 saudável: OldGen 58.7%, Metaspace 71.0%, Full GC: 3
- **[2026-11-13 13:46 | Squad Arquitetura, Monitoração & Linux]** **Caso:** Avaliar arquitetura de middleware, monitoração SLA e logs Linux → **Solução/Defesa Aplicada:** Especialista Middleware: Arquitetura Oracle Middleware (Cluster 2 Nós + Dual DB + DMZ Apache) validada com alta disponibilidade e afinidade de sessão ativa. | Especialista Monitoração: Monitoração SRE: 7/8 nós saudáveis | Latência média: 49ms | Disponibilidade estimada: 93,75%. | Especialista Linux: Linux Logs: 0 erros internos WCS# no Portal; 2 alertas BEA-000337 em APP_server1.out; partição /u01 saudável (62%).
- **[2026-11-13 13:46 | Maestro (Correlação 10 Agentes)]** **Caso:** Erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-13 13:51 | Maestro (Correlação 10 Agentes)]** **Caso:** Usuários reportando erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-13 13:51 | Agente Java/Spring]** **Caso:** Verifique memória JVM, Heap e Actuator das aplicações Spring → **Solução/Defesa Aplicada:** JVM APP_server1 saudável: OldGen 58.7%, Metaspace 71.0%, Full GC: 3
- **[2026-11-13 13:51 | Squad Arquitetura, Monitoração & Linux]** **Caso:** Avaliar arquitetura de middleware, monitoração SLA e logs Linux → **Solução/Defesa Aplicada:** Especialista Middleware: Arquitetura Oracle Middleware (Cluster 2 Nós + Dual DB + DMZ Apache) validada com alta disponibilidade e afinidade de sessão ativa. | Especialista Monitoração: Monitoração SRE: 7/8 nós saudáveis | Latência média: 49ms | Disponibilidade estimada: 93,75%. | Especialista Linux: Linux Logs: 0 erros internos WCS# no Portal; 2 alertas BEA-000337 em APP_server1.out; partição /u01 saudável (62%).
- **[2026-11-13 13:51 | Maestro (Correlação 10 Agentes)]** **Caso:** Erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-17 10:51 | Maestro (Correlação 10 Agentes)]** **Caso:** Usuários reportando erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-17 10:51 | Agente Java/Spring]** **Caso:** Verifique memória JVM, Heap e Actuator das aplicações Spring → **Solução/Defesa Aplicada:** JVM APP_server1 saudável: OldGen 58.7%, Metaspace 71.0%, Full GC: 3
- **[2026-11-17 10:51 | Squad Arquitetura, Monitoração & Linux]** **Caso:** Avaliar arquitetura de middleware, monitoração SLA e logs Linux → **Solução/Defesa Aplicada:** Especialista Middleware: Arquitetura Oracle Middleware (Cluster 2 Nós + Dual DB + DMZ Apache) validada com alta disponibilidade e afinidade de sessão ativa. | Especialista Monitoração: Monitoração SRE: 7/8 nós saudáveis | Latência média: 49ms | Disponibilidade estimada: 93,75%. | Especialista Linux: Linux Logs: 0 erros internos WCS# no Portal; 2 alertas BEA-000337 em APP_server1.out; partição /u01 saudável (62%).
- **[2026-11-17 10:51 | Maestro (Correlação 10 Agentes)]** **Caso:** Erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-17 11:04 | Maestro (Correlação 10 Agentes)]** **Caso:** Usuários reportando erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.
- **[2026-11-17 11:04 | Agente Java/Spring]** **Caso:** Verifique memória JVM, Heap e Actuator das aplicações Spring → **Solução/Defesa Aplicada:** JVM APP_server1 saudável: OldGen 58.7%, Metaspace 71.0%, Full GC: 3
- **[2026-11-17 11:04 | Squad Arquitetura, Monitoração & Linux]** **Caso:** Avaliar arquitetura de middleware, monitoração SLA e logs Linux → **Solução/Defesa Aplicada:** Especialista Middleware: Arquitetura Oracle Middleware (Cluster 2 Nós + Dual DB + DMZ Apache) validada com alta disponibilidade e afinidade de sessão ativa. | Especialista Monitoração: Monitoração SRE: 7/8 nós saudáveis | Latência média: 49ms | Disponibilidade estimada: 93,75%. | Especialista Linux: Linux Logs: 0 erros internos WCS# no Portal; 2 alertas BEA-000337 em APP_server1.out; partição /u01 saudável (62%).
- **[2026-11-17 11:04 | Maestro (Correlação 10 Agentes)]** **Caso:** Erro 504 Gateway Timeout no Portal → **Solução/Defesa Aplicada:** WebCenter Portal íntegro; 2 Stuck Threads identificadas em APP_server1 com proposta HITL de jstack.