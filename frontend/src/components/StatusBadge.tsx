import type { DeliveryStatus } from '@/lib/api';

const COLORS: Record<DeliveryStatus, string> = {
  PENDENTE: 'bg-gray-100 text-gray-800 border-gray-300',
  EM_PROCESSAMENTO: 'bg-blue-100 text-blue-800 border-blue-300',
  ENTREGUE: 'bg-green-100 text-green-800 border-green-300',
  AGUARDANDO_RETRY: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  DEAD_LETTER: 'bg-red-100 text-red-800 border-red-300',
};

const LABELS: Record<DeliveryStatus, string> = {
  PENDENTE: 'Pendente',
  EM_PROCESSAMENTO: 'Processando',
  ENTREGUE: 'Entregue',
  AGUARDANDO_RETRY: 'Retry',
  DEAD_LETTER: 'Dead Letter',
};

export function StatusBadge({ status }: { status: DeliveryStatus }) {
  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium ${COLORS[status]}`}
    >
      {LABELS[status]}
    </span>
  );
}