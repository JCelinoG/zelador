'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import type { DeliveryDetail } from '@/lib/api';
import { detalheDelivery, replayDelivery } from '@/lib/api';
import { StatusBadge } from '@/components/StatusBadge';

export default function DeliveryDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [data, setData] = useState<DeliveryDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [replaying, setReplaying] = useState(false);

  useEffect(() => {
    if (!params.id) return;
    detalheDelivery(params.id)
      .then(setData)
      .catch((e) => setError(e.message));
  }, [params.id]);

  const handleReplay = async () => {
    if (!data) return;
    setReplaying(true);
    try {
      await replayDelivery(data.id);
      const refreshed = await detalheDelivery(data.id);
      setData(refreshed);
    } catch {
      alert('Falha no replay');
    } finally {
      setReplaying(false);
    }
  };

  if (error) {
    return (
      <main className="mx-auto max-w-4xl px-6 py-10">
        <div className="rounded border border-red-300 bg-red-50 px-4 py-3 text-sm text-red-700">
          {error}
        </div>
      </main>
    );
  }

  if (!data) {
    return (
      <main className="mx-auto max-w-4xl px-6 py-10 text-gray-500">
        Carregando...
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <button
        onClick={() => router.push('/')}
        className="mb-4 text-sm text-blue-600 hover:underline"
      >
        ← Voltar
      </button>

      <header className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="font-mono text-lg text-gray-900">{data.id}</h1>
          <p className="mt-1 text-sm text-gray-600">
            Criada em {new Date(data.createdAt).toLocaleString('pt-BR')}
          </p>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={data.status} />
          {data.status === 'DEAD_LETTER' && (
            <button
              onClick={handleReplay}
              disabled={replaying}
              className="rounded bg-red-600 px-4 py-2 text-sm font-medium text-white hover:bg-red-700 disabled:opacity-50"
            >
              {replaying ? 'Reprocessando...' : 'Reprocessar'}
            </button>
          )}
        </div>
      </header>

      <section className="mb-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        <InfoCard label="Tentativas" value={String(data.attemptsCount)} />
        <InfoCard label="Tenant" value={data.tenantId.slice(0, 8)} mono />
        <InfoCard label="Idempotency Key" value={data.idempotencyKey} mono />
        <InfoCard
          label="Próxima tentativa"
          value={data.nextAttemptAt ? new Date(data.nextAttemptAt).toLocaleString('pt-BR') : '—'}
        />
      </section>

      <section>
        <h2 className="mb-3 text-lg font-semibold text-gray-900">
          Histórico de tentativas
        </h2>
        {data.attempts.length === 0 ? (
          <div className="rounded-lg border border-gray-200 bg-white p-6 text-sm text-gray-500">
            Nenhuma tentativa registrada.
          </div>
        ) : (
          <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
            <table className="w-full text-sm">
              <thead className="bg-gray-50 text-left text-xs uppercase text-gray-600">
                <tr>
                  <th className="px-4 py-3">#</th>
                  <th className="px-4 py-3">Status HTTP</th>
                  <th className="px-4 py-3">Latência</th>
                  <th className="px-4 py-3">Erro</th>
                  <th className="px-4 py-3">Quando</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {data.attempts.map((a) => (
                  <tr key={a.number}>
                    <td className="px-4 py-3 text-gray-700">{a.number}</td>
                    <td className="px-4 py-3 text-gray-700">
                      {a.statusCode || '—'}
                    </td>
                    <td className="px-4 py-3 text-gray-700">{a.latencyMs} ms</td>
                    <td className="px-4 py-3 text-xs text-red-600">
                      {a.error || '—'}
                    </td>
                    <td className="px-4 py-3 text-xs text-gray-600">
                      {new Date(a.at).toLocaleString('pt-BR')}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}

function InfoCard({
  label,
  value,
  mono,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white px-4 py-3">
      <div className="text-xs font-medium uppercase text-gray-500">{label}</div>
      <div className={`mt-1 text-sm text-gray-900 ${mono ? 'font-mono' : ''}`}>
        {value}
      </div>
    </div>
  );
}