# Contrato de Verificacao de Token para Gateway

## Escopo
Define como o Gateway deve validar tokens de acesso emitidos pelo servico de Auth.

## Versao do Contrato
- Versao: `v1`
- Dono: Auth service
- Consumidor: Gateway service

## Formato do Token
- Tipo: JWT de acesso
- Header: `Authorization: Bearer <token>`
- Claims obrigatorias:
  - `sub`: ID do usuario no Auth (UUID)
  - `iss`: emissor (`authservice-api`)
  - `iat`: instante de emissao
  - `exp`: instante de expiracao
  - `email`: email normalizado do usuario
  - `provider`: `LOCAL` ou `GOOGLE`

## Estrategia de Validacao
- Modo atual (transicao): assinatura simetrica (HS256) com segredo compartilhado entre Auth e Gateway.
- Modo alvo (recomendado): assinatura assimetrica (`RS256`/`ES256`) com JWKS publicado pelo Auth.
- O Gateway deve rejeitar token quando:
  - assinatura invalida
  - `exp` expirado
  - `iss` diferente do emissor configurado
  - claims obrigatorias ausentes

## Convencao de Endpoints
- Prefixo publico de Auth: `/api/v1/auth`
- Prefixo interno de Auth: `/internal/v1`
- Endpoints internos nao devem ser expostos publicamente pelo Gateway.

## Rotacao de Chaves e Modelo de Confianca
- Modo atual:
  - rotacao por rollout coordenado de segredo (Auth e Gateway)
  - janela de sobreposicao obrigatoria durante deploy
- Modo alvo:
  - Auth publica JWKS
  - Gateway faz cache das chaves por TTL e refresh por `kid`
  - Auth publica chave nova e antiga durante janela de rotacao
  - Gateway confia apenas no JWKS oficial do Auth

## Semantica de Falhas
- Token invalido/expirado: `401 Unauthorized`
- Token valido sem permissao de rota: `403 Forbidden`
- JWKS indisponivel no modo alvo:
  - usar cache valido dentro da politica
  - sem chave valida em cache, falhar fechado (`401`)

## Notas de Migracao
- Manter validacao atual para compatibilidade ate a entrada do Gateway.
- Migrar para modo assimetrico como parte do ciclo de desacoplamento async.
