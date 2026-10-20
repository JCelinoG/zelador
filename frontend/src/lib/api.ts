const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export type DeliveryStatus =
  | 'PENDENTE'
  | 'EM_PROCESSAMENTO'
  | 'ENTREGUE'
  | 'AGUARDANDO_RETRY'
  | 'DEAD_LETTER';

export interface Delivery {
  id: string;
  status: DeliveryStatus;
  attemptsCount: number;
  idempotencyKey: string;
  tenantId: string;
  createdAt: string;
  nextAttemptAt: string;
}

export interface Attempt {
  number: number;
  statusCode: number;
  error: string;
  latencyMs: number;
  at: string;
}

export interface DeliveryDetail extends Delivery {
  attempts: Attempt[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DlqItem {
  id: string;
  deliveryId: string;
  reason: string;
  movedAt: string;
}

export async function listarDeliveries(params: {
  status?: DeliveryStatus;
  page?: number;
  size?: number;
}): Promise<Page<Delivery>> {
  const query = new URLSearchParams();
  if (params.status) query.set('status', params.status);
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 20));

  const res = await fetch(`${API_BASE}/deliveries?${query}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Falha ao listar deliveries');
  return res.json();
}

export async function detalheDelivery(id: string): Promise<DeliveryDetail> {
  const res = await fetch(`${API_BASE}/deliveries/${id}`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Delivery não encontrada');
  return res.json();
}

export async function dlqCount(): Promise<number> {
  const res = await fetch(`${API_BASE}/dlq/count`, { cache: 'no-store' });
  if (!res.ok) throw new Error('Falha ao buscar DLQ');
  const data = await res.json();
  return data.count as number;
}

export async function replayDelivery(id: string): Promise<void> {
  const res = await fetch(`${API_BASE}/deliveries/${id}/replay`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error('Falha no replay');
}