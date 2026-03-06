# Checklist de regressao (backend conversionflow)

Este checklist deve ser usado antes de considerar uma entrega do backend pronta para merge/release.

## Pre-condicoes

- Java 21 ativo
- Banco PostgreSQL disponivel e com migracoes aplicadas
- Variaveis de ambiente configuradas (ver `docs/CONFIGURACAO_E_SEGURANCA.md`)
- Credenciais Stripe validas para ambiente de teste

## Build e testes

1. Compilar projeto:
   - `./mvnw -DskipTests compile`
2. Rodar suite de testes:
   - `./mvnw test`
3. Se houver bloqueio de ambiente no CI/sandbox, validar localmente no IDE e registrar evidencias.

## Fluxo principal de conversao

1. Criar lead via `POST /leads`.
2. Iniciar checkout via `POST /checkout`.
3. Validar que URL de checkout contem caminho Stripe e que o success URL final inclui `session_id`.
4. Confirmar pagamento (webhook Stripe):
   - `POST /webhooks/stripe`
   - esperado: lead transiciona para `WON`, pagamento `PAID`.

## Fluxo de pixel-event (fallback controlado)

1. Chamar `POST /pixel-events/purchase` com dados de atribuicao.
2. Chamar `POST /pixel-events/purchase` com `checkoutSessionId` valido de sessao paga.
3. Verificar:
   - atribuicao atualizada de forma nao destrutiva;
   - fallback pode marcar `WON` mesmo sem webhook;
   - endpoint de pixel nao cria registro de pagamento (`Payment`);
   - quando webhook chegar depois, pagamento e registrado sem dupla promocao para `WON`.

## Dispatch e scheduler

1. Verificar criacao de dispatches apos evento de conversao:
   - esperado: registros para `GOOGLE`, `META`, `PIPEDRIVE`.
2. Validar execucao do scheduler (`@EnableScheduling` ativo):
   - consumo de itens `PENDING/FAILED`;
   - atualizacao de status `SUCCESS/FAILED`;
   - respeito a `DISPATCH_RETRY_MAX_ATTEMPTS` e `DISPATCH_RETRY_INTERVAL_MS`.

## Endpoints de saude

1. `GET /health` retorna `200` e `status=UP`.
2. `GET /health/liveness` retorna `200` e `status=UP`.
3. `GET /health/readiness`:
   - com DB disponivel: `200` e `database=UP`;
   - sem DB: `503` e `database=DOWN`.

## Admin stats

1. `GET /admin/stats/overview` com autenticacao:
   - validar totais e taxas coerentes.
2. `GET /admin/stats/timeseries` com autenticacao:
   - validar preenchimento de dias sem dados.
3. `GET /admin/stats/dispatch/failures` com autenticacao:
   - validar limite e ordenacao.

## Seguranca

1. Publicos sem autenticacao:
   - `/leads/**`, `/checkout/**`, `/pixel-events/**`, `/webhooks/stripe`, `/health/**`.
2. Rotas de admin exigem autenticacao (`HTTP Basic`).

## Criterio de saida

- Todos os itens acima verificados.
- Nao ha regressao funcional no fluxo lead -> checkout -> webhook -> dispatch.
- Pixel fallback e webhook nao geram dupla conversao/dispatch para o mesmo lead.
- Evidencias de teste registradas (comandos executados + resultado).
