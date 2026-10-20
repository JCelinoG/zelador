export default function Loading() {
  return (
    <main className="mx-auto max-w-4xl px-6 py-10">
      <div className="mb-4 h-4 w-20 animate-pulse rounded bg-gray-200" />
      <div className="mb-6 flex items-start justify-between">
        <div className="space-y-2">
          <div className="h-6 w-64 animate-pulse rounded bg-gray-200" />
          <div className="h-4 w-48 animate-pulse rounded bg-gray-200" />
        </div>
        <div className="h-6 w-24 animate-pulse rounded bg-gray-200" />
      </div>
      <div className="mb-6 grid grid-cols-2 gap-4 md:grid-cols-4">
        {[...Array(4)].map((_, i) => (
          <div key={i} className="h-20 animate-pulse rounded-lg border border-gray-200 bg-white px-4 py-3" />
        ))}
      </div>
      <div className="h-64 animate-pulse rounded-lg border border-gray-200 bg-white" />
    </main>
  );
}