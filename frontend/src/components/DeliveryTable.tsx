'use client';

import Link from 'next/link';
import { useState } from 'react';
import type { Delivery } from '@/lib/api';
import { replayDelivery } from '@/lib/api';
import { StatusBadge } from './StatusBadge';

export function DeliveryTable({
  deliveries,
  onRefresh,
}: {
  deliveries: Delivery[];
  onRefresh: () => void;
}) {
  const [replaying, setReplaying] = useState<string | null>(null);

  const handleReplay = async (id: string) => {
    setReplaying(id);
    try {
      await replayDelivery(id);
      onRefresh();
    } catch {
      alert('Falha no replay');
    } finally {
      setReplaying(null);
    }
  };

  if (deliveries.length === 0) {
    return (
      <div className="rounded-lg border border-gray-200 bg-white p-8 text-center text-gray-500">
        Nenhuma delivery encontrada.
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
      <table className="w-full text-sm">
        <thead className="bg-gray-50 text-left text-xs uppercase text-gray-600">
          <tr>
            <th className="px-4 py-3">ID</th>
            <th className="px-4 py-3">Status</th>
            <th className="px-4 py-3">Tentativas</th>
            <th className="px-4 py-3">Idempotency Key</th>
            <th className="px-4 py-3">Criada em</th>
            <th className="px-4 py-3 text-right">Ações</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {deliveries.map((d) => (
            <tr key={d.id} className="hover:bg-gray-50">
              <td className="px-4 py-3 font-mono text-xs text-gray-700">
                <Link href={`/deliveries/${d.id}`} className="hover:underline">
                  {d.id.slice(0, 8)}
                </Link>
              </td>
              <td className="px-4 py-3">
                <StatusBadge status={d.status} />
              </td>
              <td className="px-4 py-3 text-gray-700">{d.attemptsCount}</td>
              <td className="px-4 py-3 font-mono text-xs text-gray-600">
                {d.idempotencyKey}
              </td>
              <td className="px-4 py-3 text-xs text-gray-600">
                {new Date(d.createdAt).toLocaleString('pt-BR')}
              </td>
              <td className="px-4 py-3 text-right">
                {d.status === 'DEAD_LETTER' && (
                  <button
                    onClick={() => handleReplay(d.id)}
                    disabled={replaying === d.id}
                    className="rounded bg-red-600 px-3 py-1 text-xs font-medium text-white hover:bg-red-700 disabled:opacity-50"
                  >
                    {replaying === d.id ? '...' : 'Replay'}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}