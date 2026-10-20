'use client';

import { useEffect, useState } from 'react';
import { dlqCount } from '@/lib/api';

export function DlqCard() {
  const [count, setCount] = useState<number | null>(null);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      try {
        const c = await dlqCount();
        if (mounted) setCount(c);
      } catch {
        if (mounted) setCount(null);
      }
    };
    load();
    const t = setInterval(load, 5000);
    return () => {
      mounted = false;
      clearInterval(t);
    };
  }, []);

  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-6 py-4">
      <div className="text-sm font-medium text-red-700">Dead Letter Queue</div>
      <div className="mt-1 text-3xl font-semibold text-red-900">
        {count === null ? '—' : count}
      </div>
      <div className="mt-1 text-xs text-red-600">
        {count === 0 ? 'Tudo em ordem' : count === null ? '' : 'Aguardando replay'}
      </div>
    </div>
  );
}