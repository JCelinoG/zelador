'use client';

import { useCallback, useEffect, useState } from 'react';
import type { Delivery, DeliveryStatus, Page } from '@/lib/api';
import { listarDeliveries } from '@/lib/api';
import { DeliveryTable } from '@/components/DeliveryTable';
import { DlqCard } from '@/components/DlqCard';

const STATUS_FILTERS: { value: DeliveryStatus | ''; label: string }[] = [
  { value: '', label: 'Todos' },
  { value: 'PENDENTE', label: 'Pendente' },
  { value: 'EM_PROCESSAMENTO', label: 'Processando' },
  { value: 'ENTREGUE', label: 'Entregue' },
  { value: 'AGUARDANDO_RETRY', label: 'Aguardando Retry' },
  { value: 'DEAD_LETTER', label: 'Dead Letter' },
];

export default function Home() {
  const [status, setStatus] = useState<DeliveryStatus | ''>('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState<Page<Delivery> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await listarDeliveries({
        status: status || undefined,
        page,
        size: 20,
      });
      setData(result);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro desconhecido');
    } finally {
      setLoading(false);
    }
  }, [status, page]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <main className="mx-auto max-w-6xl px-6 py-10">
      <header className="mb-8 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Zelador</h1>
          <p className="mt-1 text-sm text-gray-600">
            Entrega confiável de webhooks de notas fiscais
          </p>
        </div>
        <DlqCard />
      </header>

      <section className="mb-4 flex flex-wrap gap-2">
        {STATUS_FILTERS.map((f) => (
          <button
            key={f.value}
            onClick={() => {
              setStatus(f.value);
              setPage(0);
            }}
            className={`rounded-full border px-3 py-1 text-sm transition ${
              status === f.value
                ? 'border-blue-600 bg-blue-600 text-white'
                : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
            }`}
          >
            {f.label}
          </button>
        ))}
      </section>

      {error && (
        <div className="mb-4 rounded border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading && !data ? (
        <div className="rounded-lg border border-gray-200 bg-white p-8 text-center text-gray-500">
          Carregando...
        </div>
      ) : (
        <DeliveryTable
          deliveries={data?.content ?? []}
          onRefresh={load}
        />
      )}

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
          <span>
            Página {data.page + 1} de {data.totalPages} · {data.totalElements} deliveries
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:opacity-50"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= data.totalPages}
              className="rounded border border-gray-300 px-3 py-1 hover:bg-gray-50 disabled:opacity-50"
            >
              Próxima
            </button>
          </div>
        </div>
      )}
    </main>
  );
}