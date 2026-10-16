# Zelador

Serviço de entrega confiável de webhooks de notas fiscais.

## O problema

Sistemas que emitem NF-e precisam notificar ERPs, estoques e sistemas
financeiros quando uma nota é autorizada. Esses destinos falham: caem,
demoram, respondem erro ou recebem a mesma notificação duas vezes.
Entregar webhook de nota fiscal não é `POST` e esquecer — é garantir
que a notificação chegue, sem duplicar, mesmo quando o outro lado
não coopera.

## O que o Zelador faz

- Recebe eventos de nota fiscal e enfileira entregas por tenant.
- Garante idempotência: a mesma nota pro mesmo destino gera uma
  única entrega, mesmo se o produtor reenviar.
- Tenta de novo com backoff exponencial e jitter.
- Isola falhas por tenant (um cliente problemático não derruba os outros).
- Move entregas que esgotaram tentativas para uma dead-letter queue.
- Permite replay manual de dead-letters via dashboard.

## Arquitetura

- **API de Ingestão** — Spring Boot, recebe webhooks, valida, cria Delivery.
- **Fila** — Redis Streams com consumer groups.
- **Worker Pool** — consumidores Java que executam as entregas HTTP.
- **Retry Scheduler** — backoff exponencial com jitter.
- **Dead Letter** — entregas falhas persistentes, com replay.
- **Dashboard** — Next.js + TypeScript para métricas e replay.

## Stack

- Java 21 + Spring Boot 3
- Redis Streams
- PostgreSQL
- Docker Compose
- Next.js + TypeScript (dashboard)

## Como rodar

```bash
docker compose up